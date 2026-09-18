# Service Layer Plan

A plan for the service layer that would sit on top of the 82-entity data model — what each
service does, the functions it exposes, and the business rules it enforces. **Nothing here is
implemented.** The rest of this repo is entities only, by design (see the other docs in this
folder); this document is the bridge to writing the actual `@Service` classes later, and every
rule below cites the real entity/enum/field it depends on so implementation can't drift from the
model.

For the exact precondition/step/postcondition procedure behind each function listed below — not
just the one-line rule — see [`service-layer-logic.md`](service-layer-logic.md).

Organized by the same 19 modules as the entity packages (`consent` was added alongside the
integrity-focused services below). Each service lists its **functions** (what it does, not a
method signature) and the **rules** it must enforce — the second list is usually the harder part
to get right, and the part a generic CRUD layer would miss.

---

## Contents

- [Authentication & Access](#authentication--access)
- [Payment](#payment)
- [Student Groups](#student-groups)
- [Exam Definition](#exam-definition)
- [Question Bank](#question-bank)
- [Pre-Exam System Check](#pre-exam-system-check)
- [Identity Verification](#identity-verification)
- [Consent & Privacy](#consent--privacy)
- [Exam Session & Attempts](#exam-session--attempts)
- [Excel Runtime](#excel-runtime)
- [Proctoring Sessions](#proctoring-sessions)
- [AI Detection](#ai-detection)
- [Risk Engine](#risk-engine)
- [Review System](#review-system)
- [Exam Results](#exam-results)
- [Proctoring Reports](#proctoring-reports)
- [Notifications](#notifications)
- [Audit](#audit)
- [AI Model Registry](#ai-model-registry)
- [System Configuration](#system-configuration)

---

## Authentication & Access

### `AuthService`
Login, tokens, and the one-time flows (verify/reset/invite).

**Functions**
- `login(email, password)` — validate credentials, check MFA, issue a session.
- `logout(sessionId)` — revoke a session server-side.
- `refreshToken(refreshToken)` — exchange a refresh token for a new access token.
- `forgotPassword(email)` / `resetPassword(token, newPassword)`
- `verifyEmail(token)`
- `enrollMfa(userId, method)` / `verifyMfaCode(userId, code)`

**Rules**
- Login fails closed if `User.status` isn't `ACTIVE`, or if `lockedUntil` is in the future.
- Every attempt writes a `LoginAttempt` row with a `LoginOutcome` — including `BAD_CREDENTIALS`
  against an email that matches no account, so credential-stuffing is visible before any `User`
  row is touched.
- `failedLoginCount` increments on `BAD_CREDENTIALS`; crossing a configured threshold sets
  `lockedUntil` and resets the counter on next successful login.
- If `User.mfaEnabled`, a correct password produces `LoginOutcome.MFA_REQUIRED`, not `SUCCESS` —
  the session is only issued after the second factor passes (`MFA_FAILED` otherwise).
- Every `SecurityToken` is single-use (`usedAt` gates redemption) and rejected once past
  `expiresAt`; a token's `purpose` must match the flow using it — an `EMAIL_VERIFICATION` token
  cannot reset a password.
- `verifyEmail` moves `User.status` from `PENDING_VERIFICATION` to `ACTIVE` — this is the only
  path that does.

### `RoleService` / `PermissionService`
Manage the RBAC tables (`Role`, `Permission`, `RolePermission`, `UserRole`).

**Functions**
- `createRole` / `renameRole` / `deleteRole`
- `grantPermission(roleId, permissionId)` / `revokePermission`
- `grantRole(userId, roleId, grantedBy, expiresAt)` / `revokeRole`
- `checkAccess(userId, permissionCode)` — the enforcement point every other service calls through.

**Rules**
- Roles where `Role.system = true` cannot be deleted or have their code changed.
- `checkAccess` unions permissions across every `UserRole` grant that is not past its
  `expiresAt` — an expired grant is inert without needing a cleanup job.
- Machine callers go through `ApiClientService` below, never through `UserRole` — a bot has no
  `User` row.

### `ApiClientService`
Credentials for the AI inference workers and other machine callers.

**Functions**
- `registerClient(name, allowedScopes)` / `rotateSecret(clientId)` / `revoke(clientId, reason)`
- `authenticate(clientId, secret)` — verifies against `clientSecretHash`, checks `status`,
  `expiresAt`, and `allowedIpRanges`.

**Rules**
- The plaintext secret is returned exactly once, at creation or rotation, and never again —
  only `clientSecretHash` is ever stored or queried back.
- A request outside `allowedIpRanges` or over `rateLimitPerMinute` is rejected before it reaches
  any business logic, not logged and allowed through.

---

## Payment

### `PaymentCustomerService`
**Functions**
- `createCustomer(userId, provider, providerCustomerId, billingDetails)`

**Rules**
- `(userId, provider)` is unique — one billing profile per user per processor.
- The processor-side customer must already exist; this service only ever records its id, it
  never talks to the processor directly.

### `PaymentCardService`
**Functions**
- `addCard(paymentCustomerId, providerToken, cardDetails)` / `verifyCard(cardId, verified, ...)`
  / `setDefault(cardId)` / `removeCard(cardId, reason)`
- `suspendCard(cardId, reason)` / `reinstateCard(cardId)`
- `sweepExpiredCards` — scheduled sweep flipping `ACTIVE` cards past their own expiry to `EXPIRED`.

**Rules**
- The raw card number and CVV never reach this system — `providerToken` is what the payment
  processor's own tokenization step hands back, the same reasoning already applied to
  `User.passwordHash` and `ApiClient.clientSecretHash`.
- `(provider, providerPaymentMethodId)` is unique — the same tokenized card can't be stored twice.
- A card starts `PENDING_VERIFICATION` and is never chargeable until `verifyCard` resolves it to
  `ACTIVE` or `REVOKED` — `addCard` never sets `ACTIVE` directly.
- At most one `ACTIVE` card per customer has `defaultCard = true`.
- `removeCard` is a status change (`REVOKED`), never a delete — a past charge must keep a valid
  reference to the card that was actually used at the time.

### `PaymentTransactionService`
**Functions**
- `chargeCard(paymentCardId, amountMinor, currency, type, reference)`
- `refundTransaction(transactionId, amountMinor)`
- `recordChargeback(transactionId, chargebackDetails)` — processor-webhook-driven, not a
  candidate/admin action.

**Rules**
- Amounts are integer minor units (cents) — never a floating type.
- A refund's `amountMinor` can never exceed what's left unrefunded on its parent transaction.
- `parentTransactionId` links a `REFUND`/`CHARGEBACK` back to the transaction it acts on, the same
  self-reference shape `RetakeGrant.supersedesGrantId` already uses.
- `idempotencyKey` is unique when present — prevents the same charge reaching the processor twice
  on a retried call.

### `ExamPaymentService`
**Functions**
- `setRequirement(examId, amountMinor, currency, description)` — one row per exam, updated in
  place (no versioning needed; charges freeze their own amount).
- `createCharge(examId, candidateUserId, examAssignmentId)`
- `payCharge(chargeId, paymentCardId)` / `refundCharge(chargeId, reason)`

**Rules**
- `ExamPaymentCharge.amountMinor`/`currency` are copied from the requirement at charge-creation
  time, never read live — a later fee change must not reprice a charge the candidate already owes.
- At most one live charge (`INITIATED` through `SETTLED`) per `examAssignmentId`.
- `ExamPaymentCharge.status` reuses `PaymentTransactionStatus` rather than a parallel enum.

---

## Student Groups

### `StudentGroupService`
**Functions**
- `createGroup(name, groupType)` / `addMember(groupId, userId)` / `removeMember(groupId, userId)`
- `listActiveMembers(groupId)`

**Rules**
- Removing a member sets `GroupMembership.leftAt` and `active = false` — it never deletes the
  row, because an exam assigned to the group while they were a member must stay explicable.
- A `StudentGroup.parent` cycle (A's parent is B, B's parent is A) is rejected at write time.

---

## Exam Definition

### `ExamService`
**Functions**
- `createExam` (starts `DRAFT`) / `updateExam` / `publishExam` / `activateExam` / `closeExam` /
  `archiveExam`

**Rules**
- Once `status` passes `DRAFT`, a structural edit (add/remove a section or question, change
  points) doesn't mutate the live exam — it bumps `Exam.version` instead, so an attempt already
  in progress keeps referencing the version it was delivered under (`ExamAttempt.examVersion`).
- `publishExam` requires at least one `ExamSection` with at least one `ExamQuestion` — an empty
  exam cannot go `PUBLISHED`.
- `closesAt` must be after `opensAt`; `maxAttempts >= 1`.

### `ExamPrerequisiteService`
**Functions**
- `addRule(examId, requiredExamId?, minScore?, courseReference?)`
- `checkEligibility(examId, candidateId)` — evaluated before `ExamAssignmentService.assign` and
  again before `ExamAttemptService.startAttempt`.

**Rules**
- Every `ExamPrerequisite` row with `isActive = true` must pass (AND, not OR) for the candidate
  to be eligible; an inactive rule is skipped, not evaluated as satisfied.
- `requiredExamId` set → look up the candidate's best `ExamResult.passed`/`finalScore` for that
  exam. `courseReference` set instead → the check is delegated to whatever external LMS
  integration exists; this service only stores the rule, not the coursework record.

### `ExamAssignmentService`
**Functions**
- `assignToStudent(examId, candidateId)` / `assignToGroup(examId, groupId)` — the latter expands
  into one `ExamAssignment` per `GroupMembership`, and continues expanding to new members if
  `ExamGroupAssignment.autoEnrollNewMembers` is set.
- `cancelAssignment(reason)`

**Rules**
- `(examId, candidateUserId)` is unique — assigning twice updates the existing row, it doesn't
  duplicate it.
- The assignment's `windowStartAt`/`windowEndAt` must fall inside the exam's own
  `opensAt`/`closesAt` — narrower is fine, wider is rejected.
- `ExamPrerequisiteService.checkEligibility` runs before an assignment is created; failing it
  blocks the assignment outright rather than creating one the candidate can never start.

### `ExamInvitationService`
**Functions**
- `sendInvitation(assignmentId)` / `resend(assignmentId)` / `recordOpen` / `recordAccept`

**Rules**
- A resend creates a new `ExamInvitation` row with an incremented `sequenceNo` and a fresh
  `tokenHash` — it does not reuse or extend the previous token.
- A token past `expiresAt` is rejected even if the underlying assignment is still open.

### `RetakeGrantService`
**Functions**
- `grantRetake(assignmentId, reviewDecisionId, additionalAttempts, reason)` /
  `consume(grantId, attemptId)` / `revoke(grantId, reason)`

**Rules**
- A grant cannot exist without a `ReviewDecision` of `decisionType = GRANT_RETAKE` behind it —
  the entitlement and the judgement are separate rows precisely so the entitlement survives an
  appeal that supersedes the original decision.
- `additionalAttempts` raises the effective cap for the named candidate only; it never mutates
  `ExamAssignment.attemptsAllowed` or `Exam.maxAttempts`.
- `ExamAttemptService.startAttempt` must add unconsumed, unexpired `RetakeGrant` rows to the
  effective attempt cap, not just read `attemptsAllowed`.
- `consume` sets `consumedAt`/`consumedByAttemptId` exactly once, atomically with the new
  attempt — a grant cannot fund two attempts.
- Revoking a grant never edits the row — it writes a new one with `supersedesGrantId` pointing
  back, the same shape as `ReviewDecision`'s own supersede chain.

### `ExamWindowOverrideService`
**Functions**
- `grantOverride(assignmentId, newWindow, reason, justificationRef)` / `revoke(overrideId, reason)`

**Rules**
- An override widens one `ExamAssignment`'s window — `Exam.opensAt`/`closesAt` is never touched,
  so no other candidate is affected.
- `justificationRef` is required; an override with only a free-text reason and no external
  reference is rejected at write time.
- The override does not bypass `ExamPrerequisiteService.checkEligibility` — a missed window and
  a failed prerequisite are different problems with different remedies.
- Past `expiresAt`, the assignment reverts to its original window with no cleanup job needed.
- Revocation is append-only, via `supersedesOverrideId`, same as `RetakeGrant`.

---

## Question Bank

### `QuestionService`
**Functions**
- `createQuestion` (starts `DRAFT`) / `publishQuestion` (`ACTIVE`) / `retireQuestion` (`RETIRED`)
- `addOption` / `addTestCase` (CODE only) / `addCellBinding` (SPREADSHEET only)
- `uploadWorkbook(questionId, file)` — validates the file (macro policy, external-link policy,
  formula-injection guard on CSV imports), stores it, and sets `Question.workbookFileType`/
  `workbookStoragePath`/`workbookChecksumSha256`/`workbookSizeBytes`/`workbookSheetCount`/
  `workbookHasMacros`/`workbookHasExternalLinks`.
- `createNextVersion(questionId, changes)` — inherits from an existing question instead of
  authoring one from scratch.

**Rules**
- Questions are never hard-deleted — `RETIRED` only. A retired question stays fully readable by
  every past `AttemptAnswer` and `ResultDetail` that reference it, and is simply excluded from
  new pool draws.
- `SINGLE_CHOICE`/`TRUE_FALSE` require exactly one `QuestionOption.correct = true`;
  `MULTIPLE_CHOICE` requires at least one.
- `CODE` questions require at least one `CodeTestCase` with `visibility = HIDDEN` (used for
  grading); a `SAMPLE` one is recommended but not required, since a question can be entirely
  blind-tested.
- `SPREADSHEET` questions require a workbook and at least one `ExcelCellBinding`. Re-uploading a
  corrected workbook for a question that's already been sat is not an in-place edit — it goes
  through `createNextVersion` like any other fix.
- `createNextVersion` never edits the parent — it copies every field (plus `QuestionOption`/
  `CodeTestCase`/`ExcelCellBinding` rows) onto a brand-new `DRAFT` row with `parentQuestionId` set
  and `version = parent.version + 1`. Editing an already-sat question in place would silently
  change what past attempts are on record as having seen, the same reason `retireQuestion` exists
  rather than a delete.
- A new version doesn't retire its parent automatically — the caller does that once the new
  version is actually placed into an exam, so a question mid-revision doesn't vanish from banks
  still relying on the old one.

### `QuestionBankService`
**Functions**
- `createCategory` / `moveCategory` (recomputes `path`/`depth` for the moved subtree)
- `tagQuestion` / `untagQuestion`
- `searchBank(category, difficulty, tags)`

**Rules**
- Moving a category updates the materialised `path` on every descendant, not just the moved
  node — a subtree search is one `LIKE` query and must stay correct after any move.

### `QuestionCalibrationService`
**Functions**
- `computeCalibration(questionId, window)` — difficulty and discrimination indices from
  `ResultDetail` rows in the window.

**Rules**
- Only `ExamResult.status = FINAL` results feed calibration — a provisional score hasn't settled.
- `NEGATIVE_DISCRIMINATION` (strong candidates miss it, weak candidates get it) sets
  `flaggedReason = MIS_KEY_SUSPECTED` — the most common real cause is a wrong `answerKey`.
- The service only ever recommends (`recommendedAction`); nothing here calls
  `QuestionService.retireQuestion` automatically — a human acts on the recommendation.
- `difficultyIndex`/`discriminationIndex` are copied at computation time; a later reweighting
  must not reinterpret an old calibration run.

---

## Pre-Exam System Check

### `SystemCheckService`
**Functions**
- `startCheck(candidateId, examId)` / `runCheckItem(checkType)` / `overrideFailure(proctorId, reason)`

**Rules**
- Every `SystemCheckType` the exam's `ProctoringPolicy` requires must reach `PASSED` (or be
  explicitly `overridden`) before `ExamAttemptService.startAttempt` will proceed — a check the
  policy doesn't require can fail without blocking anything.
- A check's `validUntil` expiring means the candidate must re-run it, even if it previously
  passed — a camera that worked five minutes ago is not guaranteed to work now.

---

## Identity Verification

### `IdentityVerificationService`
**Functions**
- `verifyFace(candidateId, attemptId, capture)` — compares against `User.enrolmentPhotoPath`.
- `manualOverride(attemptId, proctorId, reason)`

**Rules**
- If the exam's policy requires an identity check, `ExamAttemptService.startAttempt` requires at
  least one `IdentityVerification` row for the attempt in `PASSED` or `MANUAL_OVERRIDE` status —
  `FAILED` alone does not block a subsequent retry or override.
- `matchThreshold` is copied onto the row at verification time; changing the threshold later
  must not reinterpret an old verdict.

---

## Consent & Privacy

### `PrivacyNoticeService`
**Functions**
- `publishNotice(noticeCode, version, locale, body)` — new notices are effective-dated, never
  edited in place.

**Rules**
- Publishing a new version of a notice code sets `effectiveTo` on the previous active one rather
  than deleting or overwriting it — old wording must stay retrievable to explain an old consent.

### `ConsentService`
**Functions**
- `recordConsent(attemptId, noticeId)` / `withdrawConsent(consentId)`

**Rules**
- Consent is required before `ExamAttemptService.startAttempt` proceeds whenever the exam's
  `ProctoringPolicy.mode` isn't `NONE` — a proctored sitting without recorded consent is a legal
  defect, not a missing row to backfill later.
- `noticeVersion` is copied onto the `ConsentRecord` at consent time; a later edit to the notice
  text must never change what a candidate is deemed to have agreed to.
- Withdrawal sets `withdrawnAt` and never deletes the row — a withdrawn-then-reconsented history
  is itself part of the record.
- `ProctoringReportService.generateReport` includes the attempt's consent record — a report
  without one is not a complete dossier.

---

## Exam Session & Attempts

### `ExamAttemptService`
**Functions**
- `startAttempt(assignmentId)` — the heaviest function in the model: checks eligibility, system
  check, identity verification, and attempt-count limits, then creates the `ExamAttempt`,
  computes `expiresAt`, and generates one `QuestionState` per delivered question in shuffled
  order.
- `heartbeat(attemptId)` / `navigateTo(questionId)` / `markForReview(questionId)`
- `submit(attemptId)` — sets `SUBMITTED`, triggers `ExamResultService.computeRawScore`.
- `autoSubmitOnTimeout` — a scheduled sweep over attempts past `expiresAt`.

**Rules**
- `attemptNo` cannot exceed `ExamAssignment.attemptsAllowed` (or `Exam.maxAttempts` if the
  assignment doesn't override it); a candidate with an `IN_PROGRESS`/`PAUSED` attempt cannot
  start a new one.
- `expiresAt = startedAt + Exam.durationMinutes + ExamAssignment.extraTimeMinutes`, computed
  once at start — a later change to the exam's duration doesn't retroactively change an attempt
  already in progress.
- `sessionTokenHash` is scoped to one attempt; a second concurrent session for the same attempt
  is rejected, not merged.

### `AnswerService`
**Functions**
- `saveAnswer` / `autoSave` (both write `AttemptAnswer` and append an `AnswerRevision`)
- `gradeAutomatically(answerId)` — for auto-gradable types; for `CODE`, runs each
  `CodeTestCase` and writes a `CodeExecutionResult` per test case; for `SPREADSHEET`, delegates to
  `ExcelGradingService.gradeAnswer`, which writes one `ExcelGradeResult` per `ExcelCellBinding`.

**Rules**
- No write to `AttemptAnswer` is accepted once the parent attempt is `SUBMITTED` or later.
- `gradingStatus` starts `PENDING` for `ESSAY`/`SHORT_ANSWER`/`CODE`/`SPREADSHEET` answers
  awaiting a test run. `CODE` resolves automatically from the aggregate of its
  `CodeExecutionResult` rows; `SPREADSHEET` resolves automatically too, unless one of its
  `ExcelGradeResult` rows fell back to `graderType = MANUAL`, in which case it stays `PENDING`
  for a reviewer.
- Every save — manual or auto — appends an `AnswerRevision`; it never overwrites history, even
  though `AttemptAnswer` itself is overwritten in place.

### `AttemptPauseService`
**Functions**
- `requestPause(attemptId, reason)` / `decide(requestId, approve/deny, note)` / `resume(requestId)`

**Rules**
- Only one `AttemptPauseRequest` may be `PENDING` per attempt at a time — a second request while
  one is open is rejected, not queued.
- `resume` is only valid from `APPROVED` or `AUTO_APPROVED`; it sets `resumedAt` (distinct from
  `decidedAt`) and adds the elapsed pause time to `ExamAttempt.pausedSeconds`.
- Whether a pause auto-approves is a policy decision (e.g. a dropped-connection pause vs. a
  candidate-requested one) — `requestedByUserId = null` marks the system-initiated case.

### `AttemptResumptionService`
**Functions**
- `resume(attemptId, reason)` — rotates the session token and reconnects a crashed/dropped
  candidate to their still-live attempt.

**Rules**
- Valid only from `AttemptStatus.PAUSED` or `IN_PROGRESS` — never from `SUBMITTED` or later, the
  same boundary `AnswerService` enforces on writes.
- The previous `sessionTokenHash` is invalidated in the same operation that issues the new one —
  a resumption is a token rotation, never a second concurrent session.
- `timeAwaySeconds` (from `lastHeartbeatAt` to `resumedAt`) is added to
  `ExamAttempt.pausedSeconds`, so total time spent stays honest.
- A gap past the policy's grace window writes a `RiskEvent` with `factorCode = ATTEMPT_GAP` — the
  gap is a signal in its own right, not merely a technical event.
- `ExamAttemptService.startAttempt`'s "already has a live attempt" check must route to this
  service instead of rejecting outright, when the existing attempt is resumable.
- The row is append-only — a second crash produces a second `AttemptResumption`, never an edit.

### `QuestionFormFingerprintService`
**Functions**
- `computeFingerprint(attemptId)` — hashes the delivered `QuestionState` order at attempt start.
- `checkCollisions(examId, formHash)`

**Rules**
- The hash is computed once, from the persisted shuffle, never re-derived from the shuffle
  *rule* — two candidates can draw the same rule and land on different forms.
- When `ExamSection.questionsToDraw` narrows the pool, the hash covers the drawn subset only —
  two attempts that drew disjoint subsets must not collide.
- A collision writes a `SuspiciousActivity` (`ruleCode = FORM_COLLISION`) and nothing more — it's
  a signal for `ReviewCaseService` to act on, not a verdict.
- Scoped to `examId` — a matching hash across two different exams means nothing.

### `AnswerTimingAnomalyService`
**Functions**
- `checkTiming(answerId)` — run after grading, comparing `QuestionState.timeOnQuestionSeconds`
  against `Question.expectedSeconds`.

**Rules**
- Only correct or high-scoring answers are evaluated — a fast wrong answer isn't suspicious.
- `expectedSeconds` is copied onto the flag at check time; a later edit to the question's pacing
  must not reinterpret an old flag.
- Writes a `RiskEvent`, never a `ProctoringEvent` — this is a derived signal, not an observation.
- `BURST_SUBMIT` is detected from `AnswerRevision.savedAt` clustering, not from
  `AttemptAnswer.answeredAt` alone.
- Survives a re-grade as a new row — the anomaly record is never edited in place.

---

## Excel Runtime

### `ExcelSessionService`
**Functions**
- `createSession(examAttemptId)` (`PENDING`) / `startSession(sessionId)` (`ACTIVE`)
- `recordCellEdit` / `recordSheetOperation` / `runMacro`
- `takeSnapshot(sessionId, snapshotType)` — writes an `EvidenceFile` (`EXCEL_WORKBOOK_SNAPSHOT` or
  `EXCEL_FINAL_WORKBOOK`), reusing the same evidence pipeline every other proctoring artefact uses.
- `submitSession(sessionId)` (`SUBMITTED`) — verifies the final workbook hash by replaying every
  `ExcelCellEdit`, and sets `integrityStatus`.
- `markCrashed(sessionId)` (`CRASHED`) / `recoverSession(sessionId)` (`RECOVERED`)

**Rules**
- One `ExcelSession` per `ExamAttempt`, the same 1:1 shape as `ProctoringSession`.
- A macro run is always logged via `ExcelMacroExecution`, whether it was actually allowed to
  execute or not — a blocked-but-attempted run is itself the proctoring signal.
- `takeSnapshot` requires `proctoringSessionId` to be set — an exam with proctoring mode `NONE`
  has no evidence trail for its SPREADSHEET workbook at all, the same limitation every other
  evidence type already has.
- `RECOVERED` behaves exactly like `ACTIVE` for every further cell edit, sheet operation, macro
  run, or snapshot — it exists purely so the audit trail shows the session survived a crash.
- Macro execution is gated by both the exam's `ExcelPolicy.macrosAllowed` and the question's own
  `Question.excelMacroPolicy` — either one being `false`/`OFF` blocks it.

### `ExcelGradingService`
**Functions**
- `gradeAnswer(attemptAnswerId)` — called from `AnswerService.gradeAutomatically` for
  `SPREADSHEET` answers; writes one `ExcelGradeResult` per `ExcelCellBinding`.

**Rules**
- Dispatches by `ExcelCellBinding.answerKind`: `VALUE`/`FORMULA`/`RANGE` compare directly;
  `CHART`/`PIVOT_TABLE`/`CONDITIONAL_FORMATTING`/`NAMED_RANGE` run a structural comparison and
  fall back to `MANUAL` when inconclusive rather than guessing; `MACRO_OUTPUT` re-runs the bound
  macro against a hidden test workbook.
- Requires the owning `ExcelSession.status = SUBMITTED` — grading always reads the final,
  hash-verified workbook, never a live one still being edited.

---

## Proctoring Sessions

### `ProctoringSessionService`
**Functions**
- `startSession(attemptId)` — snapshots the exam's `ProctoringMode` onto the session.
- `recordHeartbeat` / `updateLiveStatus` (writes `LiveSessionStatus`) / `endSession`

**Rules**
- `heartbeatMissCount` increments on a missed interval; crossing a configured threshold is
  itself a `ProctoringEventType.HEARTBEAT_MISSED` event, which the risk engine can weight.
- `LiveSessionStatus` is the only row heartbeats touch — the durable trail stays in
  `ProctoringEvent`, so a heartbeat never contends on a row that evidence/detections reference.

### `ProctoringEventService`
**Functions**
- `ingestEvent(sessionId, type, severity, source, payload)`
- `applyAutoAction(event)` — consults the session's policy and current risk to decide
  `NONE`/`WARN_CANDIDATE`/`PAUSE_ATTEMPT`/`TERMINATE_ATTEMPT`, among others.

**Rules**
- `idempotencyKey` deduplicates a retried client submission — the same key seen twice is one
  event, not two.
- `autoAction` escalates on accumulated risk, not a single event in isolation — one tab switch
  is `LOG_ONLY`; the same event recurring past a `RiskFactorConfig.graceOccurrences` threshold is
  what escalates.

### `EvidenceService`
**Functions**
- `captureEvidence(sessionId, kind, sourceEvent?)` / `computeChecksum` / `uploadToStorage`
- `viewEvidence(fileId, actorId, purpose)` — every call writes an `EvidenceAccessLog` row first.
- `purgeExpired` — a scheduled sweep against `retentionUntil`.

**Rules**
- `retentionUntil` is computed at capture time from `ProctoringPolicy.evidenceRetentionDays` in
  force *then* — a later policy change doesn't retroactively shorten or extend it.
- Access is logged on every read, not just every write — footage of a candidate's home is the
  most sensitive data this system holds.
- Every `viewEvidence` call also writes an `EvidenceCustodyRecord` with `transition = ACCESSED` —
  the access log is the *who looked*, the custody record is the *chain*.
- `captureEvidence`/`uploadToStorage` each write their own `EvidenceCustodyRecord` transition
  (`CAPTURED`, `HASHED`, `UPLOADED`); a checksum mismatch between consecutive transitions means
  the file was altered in flight and the pipeline must reject it, not just log it.

### `DeviceTrustService`
**Functions**
- `recordSighting(examId, deviceFingerprint, candidateId)` — updates or creates the exam-scoped
  `DeviceTrustRecord`.

**Rules**
- Crossing the policy's `distinctCandidateCount` threshold moves the record to
  `SHARED_SUSPECTED` and writes a `SuspiciousActivity` (`ruleCode = SHARED_DEVICE`) — the rule is
  configured, not hardcoded, matching `SuspiciousActivityCorrelationService`.
- The record never drives an auto-action on its own — it only feeds
  `RiskFactorConfig.triggerKind = DEVICE_SIGNAL`.
- `SHARED_CONFIRMED` is reached only through a `ReviewFinding` with `verdict = VALID` — this
  service does not get to declare intent on its own.
- `BLOCKED` prevents a *new* `ExamAttempt` from being created for the exam; it never deletes an
  attempt already in progress.
- The row is updated in place (a running counter), but every status transition writes an
  `AuditLog` row regardless.

### `ProctorShiftService`
**Functions**
- `startShift(sessionId, proctorId)` / `handOver(shiftId, note)` / `endShift(shiftId, outcome)`

**Rules**
- At most one `ProctorShift` per session may have a null `shiftEndAt` — the proctor currently on
  duty.
- `ProctoringSession.assignedProctorUserId` mirrors the open shift's proctor; this service writes
  the shift row first and the session column second, never the reverse.
- `handoverNote` is required when `outcome = HANDED_OVER` — a handover without one is a gap in
  supervision, not a formality.
- Shift changes are append-only: a handover closes one row and opens another, never edits.

### `ProctorActionService`
**Functions**
- `recordAction(sessionId, proctorId, actionType, reason?)` — warn, message, flag, pause,
  terminate, escalate.

**Rules**
- `TERMINATE` requires `supervisorApprovalUserId` when the exam's policy demands two-person
  approval — one proctor cannot unilaterally end a sitting under that policy.
- `reason` is required for `WARN`, `FLAG`, `PAUSE`, `TERMINATE` — a silent action is unauditable.
- Every action writes both a `ProctoringEvent` (`source = HUMAN_PROCTOR`, keeping the unified
  timeline intact) and an `AuditLog` row with the matching `AuditAction`.
- `candidateNotified` is set only once the candidate actually sees the action — a warning never
  shown is not a warning.

---

## AI Detection

### `AiDetectionIngestService`
**Functions**
- `receiveInference(sessionId, modelVersionId, confidence, rawOutput)` — writes the parent
  `AiDetection` plus one `FaceDetection`/`ObjectDetection`/`BehaviorDetection`/`AudioDetection`
  row keyed to it.

**Rules**
- Below `AiModelVersion.confidenceThreshold`, the row is written but advisory only — no event.
- At or above `detectionThreshold`, a `ProctoringEvent` is raised alongside it.
- Only an authenticated `ApiClient` scoped for detection ingest may call this — a caller that
  can write findings about a candidate needs its own credential (see `ApiClientService`).

### `SuspiciousActivityCorrelationService`
**Functions**
- `correlateWindow(sessionId, windowStart, windowEnd)` — groups co-occurring detections/events
  into `SuspiciousActivity` rows per configured `ruleCode`.

**Rules**
- Correlation rules are data (a `ruleCode` catalogue), not hardcoded conditionals — adding
  "phone + looking away within 10s" as a new rule is a data change.
- `verdict` starts `DETECTED` and is only moved by `ReviewSystem`, never by this service itself.

---

## Risk Engine

### `RiskScoringService`
**Functions**
- `computeScore(attemptId)` — the core algorithm: for every applicable `RiskFactorConfig`, sum
  `baseWeight × confidenceMultiplier`, plus `frequencyIncrement` per repeat past
  `graceOccurrences`, plus `durationWeightPerSecond` where relevant, capped at
  `maxContribution`, decayed by `decayHalfLifeSeconds` against event age. Writes a new
  `RiskAssessment` and one `RiskEvent` per contributing factor.
- `resolveLevel(score)` — maps the total onto the active `RiskLevelThreshold` band.

**Rules**
- A recompute always creates a new `RiskAssessment` version; exactly one row per attempt carries
  `is_latest = true` at any time, flipped atomically with the insert.
- `RiskLevelThreshold.opensReviewCase`/`withholdsResult`/`alertsProctor` are read off the
  resolved band and drive `ReviewCaseService`/`ExamResultService` directly — the score alone
  triggers nothing without a matching threshold row.

---

## Review System

### `ReviewCaseService`
**Functions**
- `openCase(attemptId, reason, riskAssessmentId?)` — automatic (risk threshold) or manual.
- `assignReviewer` / `addFinding(itemKind, itemId, verdict)` / `addNote` /
  `recordDecision(type, rationale)` / `closeCase(finalOutcome)`

**Rules**
- A case auto-opens when the resolved `RiskLevelThreshold.opensReviewCase = true`; the
  triggering `RiskAssessment` is recorded, but a later recompute producing a new version must
  not orphan the open case.
- `ReviewDecision` rows are append-only — reopening a case (an appeal) adds a new decision whose
  `supersedes` points at the one it overturns; nothing is edited or deleted.
- A case is not unique per attempt — a cleared attempt can be reopened as a separate case later,
  and both histories survive.
- `dueAt` passing while `status` isn't terminal sets `slaBreached = true`.

---

## Exam Results

### `ExamResultService`
**Functions**
- `computeRawScore(attemptId)` — sums a `ResultDetail` per `ExamQuestion` from `AttemptAnswer`
  (and, for CODE, its `CodeExecutionResult` set).
- `applyReviewAdjustment(resultId, decision)` / `finalize(resultId)` / `publish(resultId)`

**Rules**
- `finalize` is blocked while `integrityStatus = UNDER_REVIEW` and the exam has
  `holdResultsForReview = true` — the result stays `PENDING_REVIEW` until the case closes.
- `passed = finalScore >= Exam.passingScore`; `riskScore`/`riskLevel` are copied from the
  assessment at finalize time and frozen even if the attempt is rescored afterward.
- `finalScore = rawScore + scoreAdjustment`; the adjustment traces back to the `ReviewDecision`
  that authorized it, never applied silently.

### `ResultWithholdingService`
**Functions**
- `withhold(resultId, reason, reviewCaseId?)` / `release(withholdingId, releasedBy, reason)`

**Rules**
- A withholding is only opened when the resolved `RiskLevelThreshold.withholdsResult = true` or a
  `ReviewCase` is open — read off the resolved band, never hardcoded to a status check.
- `thresholdVersion` is copied at withholding time; a later retune must not reinterpret why an
  old result was held.
- `ExamResultService.finalize` is blocked while any `ResultWithholding` on the result has
  `releasedAt = null`.
- `releaseReason` is required whenever `releasedByUserId` is set — an unexplained release is as
  indefensible as an unexplained hold.
- Release is append-only: a re-withholding after release is a new row, never an edit to the old
  one.

---

## Proctoring Reports

### `ProctoringReportService`
**Functions**
- `generateReport(attemptId)` — renders `summarySnapshot`, computes `checksumSha256`.
- `regenerate(reportId)` — produces a new `version`, never edits the existing one.

**Rules**
- A published report is immutable; anything that would change its content (a review closing, a
  rescore) produces a new version instead.
- `checksumSha256` lets a copy in circulation be proven genuine independent of the database.

### `AttemptTimelineService`
**Functions**
- `renderTimeline(attemptId)` — merges `ProctoringEvent`, `AnswerRevision`, `RiskEvent`, and
  `EvidenceFile` entries by offset/timestamp into one ordered `timelineJson`.

**Rules**
- Rendered once and versioned, exactly like `ProctoringReport` — a later event must not alter a
  timeline already shown to a reviewer; a re-render is a new `version`.
- Every entry's `refId` points back to its source row rather than duplicating that row's
  content — the timeline is a rendering, never a second copy of the underlying data.
- Never includes `AudioDetection.transcriptExcerpt` unless
  `ProctoringPolicy.retainAudioTranscript` was true at capture time — the render respects the
  policy in force *then*, not the policy in force at render time.
- `checksumSha256` lets a timeline shown in a hearing be proven identical to what the reviewer
  actually saw.

---

## Notifications

### `NotificationService`
**Functions**
- `send(type, recipientId, variables)` — renders the matching `NotificationTemplate` by
  `(type, channel, locale)`, writes a `Notification` row, dispatches, and retries on failure.
- `scheduleReminder(examId, offsetBeforeStart)` — e.g. the 24-hour and 1-hour exam reminders.

**Rules**
- `idempotencyKey` prevents a retried trigger (a retried event, a redelivered queue message)
  from sending the same notification twice.
- A failed send sets `nextRetryAt`; `retryCount` bounds how many attempts are made before the
  notification is left `FAILED` for manual attention.

### `NotificationSuppressionService`
**Functions**
- `shouldSuppress(recipientId, type, channel)` — consulted by `NotificationService.send` before
  every dispatch.

**Rules**
- A suppressed send is deferred, not dropped: `Notification.status` stays `PENDING` with
  `scheduledFor` pushed to the end of the quiet window, or is written as `CANCELLED` with
  `failureReason = SUPPRESSED_BY_RULE` for a hard block — the outbox always shows what happened.
- `QUIET_HOURS` is evaluated in the recipient's own `User.timeZone`, never the server's.
- `DUPLICATE_WINDOW` is distinct from `idempotencyKey`: the key stops one retried trigger from
  sending twice, the window stops many *different* triggers from producing near-identical sends.
- `RATE_LIMIT` suppression never applies to `HIGH_RISK_ALERT` or `SYSTEM_ALERT` — a safety signal
  is exempted before suppression logic runs, not silenced by it.
- Suppression rules are soft-deleted (`isActive = false`), never row-deleted.

---

## Audit

### `AuditService`
**Functions**
- `record(action, actorId, entityType, entityId, before, after, reason?)` — called by every
  other service around a consequential write, not queried directly by end users.

**Rules**
- Rows are append-only — never updated or deleted, including by administrators.
- `reason` is required for the actions that most need one on record: `SCORE_OVERRIDE`,
  `PERMISSION_CHANGE`, `CONFIG_CHANGE`, `TERMINATE_ATTEMPT`.

---

## AI Model Registry

### `AiModelRegistryService`
**Functions**
- `registerModel(purpose)` / `registerVersion(modelId, thresholds, config)` /
  `promote(versionId, newStatus)` / `recordMetric(versionId, metricType, window)`

**Rules**
- `confidenceThreshold`/`detectionThreshold` live on the version, not the model — retuning a
  threshold creates a new version rather than mutating one that past detections were scored
  under.
- A `SHADOW` version scores in parallel with the `ACTIVE` one but never drives an `autoAction` —
  it exists purely to compare against production traffic before promotion.
- `ModelPerformanceMetric.truePositiveCount`/`falsePositiveCount`/`falseNegativeCount` are
  computed from `ReviewFinding` verdicts, never self-reported by the model — that's the only
  ground truth this system has.

### `ShadowEvaluationService`
**Functions**
- `evaluate(shadowVersionId, activeVersionId, window)` — agreement/disagreement counts and
  false-positive rates for both versions over the same traffic.

**Rules**
- A `SHADOW` version's detections are written with `anomaly = false` and no
  `proctoringEventId` — it never drives an `autoAction`, matching `AiDetectionIngestService`.
- Both false-positive rates come only from `ReviewFinding` verdicts — the shadow does not grade
  itself, the same rule `AiModelRegistryService` already applies to the active model.
- `recommendation = PROMOTE` requires the shadow's rate to be no worse than the active one's over
  a minimum sample count — a promotion on thin data is rejected, not merely discouraged.
- A `PROMOTE` recommendation writes an `AuditLog` with `action = MODEL_CHANGE` before any actual
  promotion happens — the measurement is on record first.
- The evaluation is append-only; a re-run is a new row.

---

## System Configuration

### `SystemSettingService`
**Functions**
- `get(key)` / `set(key, value)` — validates against `valueType` and `validationRule` before
  writing.

**Rules**
- `secret = true` settings are never returned in plaintext to any read path outside the process
  that consumes them.
- `editable = false` settings reject writes outright, regardless of caller permissions.
- A setting under `SettingCategory` never overrides an exam's own `ProctoringPolicy` — the
  policy embedded on `Exam` is what a running exam actually obeys, so a global default change
  can't alter the rules of a sitting already in progress.

### `RiskFactorConfigService` / `RiskLevelThresholdService`
**Functions**
- `updateFactor(factorCode, weights)` / `updateThresholdBand(level, range)`

**Rules**
- Both are versioned and effective-dated (`effectiveFrom`/`effectiveTo`), never edited in
  place — a `RiskAssessment` records the `thresholdVersion`/config version it was computed
  under, so a retune today cannot silently reinterpret a decision from last month.
