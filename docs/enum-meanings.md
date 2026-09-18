# Enum Meanings

What every constant of every enum actually means, in plain language. 78 enums, 499 constants
total. This is a different cut from the other two enum documents in this folder:
[`field-reference.md`](field-reference.md)'s appendix lists the constants (no explanation),
[`enum-workflow.md`](enum-workflow.md) shows how the 20 state-machine enums transition between
values. This document is the dictionary — what does `SHADOW`, or `PROLONGED_IDLE`, or
`REQUIRE_RETAKE` actually mean, one entry at a time.

Grouped by the same 18 modules used throughout. Every enum name and constant here was extracted
directly from the Java source, not retyped from memory.

---

## Contents

- [Authentication & Access](#authentication--access)
- [Payment](#payment)
- [Student Groups](#student-groups)
- [Exam Definition](#exam-definition)
- [Question Bank](#question-bank)
- [Pre-Exam System Check](#pre-exam-system-check)
- [Identity Verification](#identity-verification)
- [Exam Session & Attempts](#exam-session--attempts)
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

### `UserStatus`
Where an account sits in its account lifecycle.

- **`PENDING_VERIFICATION`** — Signed up, but hasn't confirmed their email yet. Can't log in.
- **`ACTIVE`** — Normal, usable account.
- **`SUSPENDED`** — Temporarily blocked, usually pending investigation or a policy violation. Reversible.
- **`DISABLED`** — Permanently deactivated (left the institution, terminated for cause). Not meant to be reversed casually.

### `MfaMethod`
Which second factor a user has enrolled in.

- **`TOTP`** — A time-based one-time code from an authenticator app (Google Authenticator, Authy). The seed is stored encrypted.
- **`SMS`** — A code texted to the phone number on file.
- **`EMAIL`** — A code emailed to the address on file.

### `LoginOutcome`
What happened on one login attempt — written once per attempt, success or failure.

- **`SUCCESS`** — Credentials and (if enabled) MFA both checked out; a session was issued.
- **`BAD_CREDENTIALS`** — Wrong password, or no account matches the email at all.
- **`ACCOUNT_LOCKED`** — Too many recent failures; the account is in its lockout window.
- **`ACCOUNT_DISABLED`** — The account itself isn't usable right now (suspended or disabled).
- **`EMAIL_NOT_VERIFIED`** — Correct password, but the account is still `PENDING_VERIFICATION`.
- **`TOKEN_EXPIRED`** — A token-based login step (rare path) expired before it was completed.
- **`MFA_REQUIRED`** — Password was correct; the second factor is still needed.
- **`MFA_FAILED`** — The MFA code was wrong or expired.

### `TokenPurpose`
What a one-time `SecurityToken` is allowed to be redeemed for. A token issued for one purpose is rejected if presented for another.

- **`EMAIL_VERIFICATION`** — Confirms a new account's email address.
- **`PASSWORD_RESET`** — Authorizes setting a new password.
- **`ACCOUNT_INVITATION`** — Lets someone activate an account created on their behalf (e.g. by an admin).
- **`EMAIL_CHANGE`** — Confirms a change to the address on file.

### `ApiClientStatus`
Lifecycle of a machine credential (an AI inference worker, an integration).

- **`ACTIVE`** — Usable.
- **`SUSPENDED`** — Temporarily blocked, reversible.
- **`REVOKED`** — Permanently killed — a compromised or retired credential.
- **`EXPIRED`** — Past its `expiresAt`; needs reissuing, not un-suspending.

---

## Payment

### `PaymentProvider`
Which PCI-compliant processor tokenized a stored card, or moved money on a transaction.

- **`STRIPE`**, **`ADYEN`**, **`BRAINTREE`**, **`SQUARE`**, **`PAYPAL`** — the processor that holds
  the real card data and issued the token this row stores.
- **`MANUAL`** — Recorded outside any processor (e.g. an offline/administrative payment
  arrangement), not backed by a live tokenized card.

### `CardBrand`
The card network, as reported by the processor.

- **`VISA`**, **`MASTERCARD`**, **`AMEX`**, **`DISCOVER`**, **`JCB`**, **`DINERS_CLUB`**,
  **`UNIONPAY`**, **`MAESTRO`**, **`ELO`** — self-explanatory.
- **`OTHER`** — A network the platform doesn't specifically distinguish.

### `PaymentCardFunding`
How a card draws funds — from the processor's card metadata, not something the platform infers.

- **`CREDIT`**, **`DEBIT`**, **`PREPAID`**, **`CHARGE`** — self-explanatory (a charge card, e.g.
  some Amex products, settles in full each period rather than revolving or drawing from a balance).
- **`UNKNOWN`** — The processor didn't report it.

### `PaymentCardholderVerification`
Result of one fraud-signal check (CVV or AVS) the processor ran at tokenization or charge time.

- **`NOT_ATTEMPTED`** — The check wasn't run for this card/transaction.
- **`PASSED`** — The value matched what the issuer has on file.
- **`FAILED`** — The value didn't match.
- **`UNAVAILABLE`** — The issuer doesn't support this check.
- **`UNRECOGNIZED`** — The issuer returned a result the processor couldn't map to a known outcome.

### `PaymentCardStatus`
Lifecycle of one stored payment card.

- **`PENDING_VERIFICATION`** — Tokenized but not yet confirmed usable (e.g. awaiting a 3-D Secure
  or micro-deposit check). The card cannot be charged yet.
- **`ACTIVE`** — Verified and usable for a new charge.
- **`EXPIRED`** — Past its expiry date; still on file, no longer chargeable.
- **`SUSPENDED`** — Temporarily blocked (e.g. a fraud hold) without the candidate having removed
  it — may return to `ACTIVE` once cleared.
- **`REVOKED`** — Permanently off — the candidate removed it, or an admin invalidated it. See
  `revokedReason`. The row stays; past charges still reference it.

### `PaymentTransactionType`
What kind of money movement one `PaymentTransaction` row represents.

- **`AUTHORIZATION`** — Funds reserved on the card but not yet taken.
- **`CAPTURE`** — Previously authorized funds actually taken.
- **`SALE`** — Authorization and capture in one step.
- **`REFUND`** — Money returned to the candidate against a prior `SALE`/`CAPTURE`.
- **`VOID`** — A prior `AUTHORIZATION` cancelled before it was captured.
- **`CHARGEBACK`** — The cardholder's bank forcibly reversed a charge.
- **`PAYOUT`** — Money moved out to a third party rather than charged to a candidate (e.g. a refund
  settlement to an institution) — the same enum covers both directions of money movement.

### `PaymentTransactionStatus`
Lifecycle of one `PaymentTransaction` — also reused directly by `ExamPaymentCharge.status`, since a
charge's lifecycle is the same shape.

- **`INITIATED`** — Created locally; not yet sent to the processor.
- **`PENDING`** — Sent to the processor; outcome not yet known.
- **`AUTHORIZED`** — Funds reserved, awaiting capture.
- **`CAPTURED`** — Funds taken from the card.
- **`SETTLED`** — Funds have moved all the way through to payout/reconciliation.
- **`FAILED`** — The processor declined it.
- **`CANCELLED`** — Withdrawn before it reached the processor, or voided.
- **`REFUNDED`** — Fully returned to the candidate.
- **`PARTIALLY_REFUNDED`** — Some, but not all, of the amount returned.
- **`DISPUTED`** — The cardholder has raised a dispute; not yet a confirmed chargeback.
- **`CHARGEBACK`** — The dispute resolved against the merchant; funds forcibly reversed.

---

## Student Groups

### `GroupType`
What kind of grouping a `StudentGroup` represents — purely descriptive, no different behavior per value.

- **`CLASS`** — A single taught class/section.
- **`COHORT`** — A batch of students who started together (e.g. an intake year).
- **`DEPARTMENT`** — An academic department.
- **`PROGRAM`** — A degree/certificate program, often the parent of several classes or cohorts.
- **`CUSTOM`** — Anything else — an ad-hoc list an admin assembled by hand.

---

## Exam Definition

### `ExamStatus`
Where an exam sits in its publication lifecycle.

- **`DRAFT`** — Being authored. Structure is fully editable.
- **`SCHEDULED`** — Finished but not yet open; has a future `opensAt`.
- **`PUBLISHED`** — Structure is frozen; visible to assigned candidates, not necessarily open yet.
- **`ACTIVE`** — Open for sitting right now.
- **`CLOSED`** — Sitting window has ended; no new attempts.
- **`ARCHIVED`** — Retired from active use, kept for historical record.

### `GradingMode`
How an exam's answers are scored — appears on both `Exam` and (copied) `ExamResult`.

- **`AUTO`** — Every question type on this exam is machine-gradable.
- **`MANUAL`** — At least one question needs a human grader, and none are auto-graded.
- **`HYBRID`** — A mix — some questions auto-grade, others (essays, etc.) need a human.

### `ProctoringMode`
How closely an exam is supervised. Snapshotted onto each `ProctoringSession` at start, so a later policy change never alters a sitting already underway.

- **`NONE`** — No supervision at all.
- **`AI_ONLY`** — Automated monitoring (camera/screen/audio analysis), no human watching live.
- **`LIVE_PROCTOR`** — A human proctor is watching in real time.
- **`RECORD_AND_REVIEW`** — Recorded for a human to review afterward, not watched live.
- **`HYBRID`** — AI monitoring plus a human proctor, both active.

### `AssignmentStatus`
Where one candidate's entitlement to sit an exam stands.

- **`ASSIGNED`** — Entitlement created, not yet notified.
- **`NOTIFIED`** — An invitation has been sent.
- **`STARTED`** — The candidate has begun an attempt against this assignment.
- **`SUBMITTED`** — That attempt has been submitted.
- **`EXPIRED`** — The sitting window closed and no attempt was ever started.
- **`CANCELLED`** — Explicitly withdrawn before use.

### `InvitationChannel`
How an `ExamInvitation` is delivered.

- **`EMAIL`** / **`SMS`** / **`IN_APP`** — Self-explanatory delivery transports.

### `InvitationStatus`
Delivery and engagement state of one sent invitation.

- **`PENDING`** — Created, not yet dispatched.
- **`SENT`** — Handed off to the delivery channel.
- **`DELIVERED`** — Confirmed arrival (a delivery webhook fired).
- **`OPENED`** — The candidate opened it.
- **`ACCEPTED`** — The candidate clicked through / acted on it.
- **`EXPIRED`** — Its token window passed unused.
- **`CANCELLED`** — Withdrawn (the underlying assignment was cancelled).
- **`FAILED`** — The channel itself couldn't deliver it (bounced email, bad number).

### `ExcelRecalcMode`
How the Excel runtime recalculates formulas for an exam.

- **`AUTOMATIC`** — Recalculates on every change, the normal spreadsheet behavior.
- **`MANUAL`** — Only recalculates when the candidate explicitly triggers it.
- **`ITERATIVE`** — Allows circular references, resolved up to `iterativeCalcMaxIterations` passes.

### `ExcelUiActionPolicy`
How an exam's Excel runtime treats one candidate-initiated UI action.

- **`ALLOW`** — Permitted, no signal raised.
- **`BLOCK`** — Prevented outright.
- **`LOG`** — Permitted, but raises a proctoring event.

---

## Question Bank

### `QuestionType`
The shape of a bank question — what kind of answer it expects and how it can be graded.

- **`SINGLE_CHOICE`** — Exactly one correct option among several.
- **`MULTIPLE_CHOICE`** — One or more correct options; partial credit possible.
- **`TRUE_FALSE`** — A binary single-choice question.
- **`SHORT_ANSWER`** — A brief free-text response, usually manually graded.
- **`ESSAY`** — A long free-text response, always manually graded.
- **`NUMERIC`** — A number, graded against an answer key within a tolerance.
- **`MATCHING`** — Pairs up items from two lists.
- **`ORDERING`** — Candidate arranges items into a correct sequence.
- **`FILL_IN_BLANK`** — Free text slotted into a sentence/passage.
- **`CODE`** — A program, graded by running it against test cases.
- **`SPREADSHEET`** — An Excel workbook, graded cell-by-cell by its `ExcelCellBinding`s.

### `ExcelWorkbookFileType`
File format of an uploaded SPREADSHEET question's workbook.

- **`XLSX`**, **`XLSM`**, **`XLSB`**, **`CSV`**, **`ODS`** — self-explanatory. `XLSM` is the only format that can carry macros.

### `ExcelMacroPolicy`
Whether a SPREADSHEET question's macros may run, and under what constraint. Only enforceable when the owning exam's `ExcelPolicy.macrosAllowed` is also true.

- **`OFF`** — Macros never run, regardless of what the workbook contains.
- **`SANDBOXED`** — Macros run, but isolated with no filesystem/network/shell access.
- **`ALLOWED_WHITELIST`** — Only macros on an approved whitelist run; anything else is blocked and logged.

### `ExcelAnswerKind`
What kind of check one `ExcelCellBinding` represents, and — reused on `ExcelGradeResult` — what kind of check was actually applied to grade it.

- **`VALUE`** — Compares the cell's computed value to an expected value, within `tolerance` for numbers.
- **`FORMULA`** — Compares the cell's formula string, normalized.
- **`RANGE`** — Compares a whole range cell-by-cell.
- **`CHART`** — Validates chart type, series, categories, and source range.
- **`PIVOT_TABLE`** — Validates pivot row/column/value fields and computed aggregates.
- **`CONDITIONAL_FORMATTING`** — Validates rule type, formula, and applied range.
- **`NAMED_RANGE`** — Validates existence and target of a named range.
- **`MACRO_OUTPUT`** — Runs the candidate's macro against a hidden test workbook and compares output.
- **`MANUAL`** — No automated check; a reviewer grades it directly. Also what an inconclusive automated check (e.g. CHART) falls back to.

### `QuestionDifficulty`
The author's own declared difficulty rating — independent of, and never automatically reconciled with, the empirically *measured* difficulty a `QuestionCalibration` run computes.

- **`EASY`** / **`MEDIUM`** / **`HARD`** / **`EXPERT`** — Self-explanatory, increasing difficulty.

### `QuestionStatus`
Bank lifecycle of one question — separate from where it's used in any particular exam.

- **`DRAFT`** — Being authored, not yet usable in an exam.
- **`ACTIVE`** — Ready to be placed into exams.
- **`RETIRED`** — No longer offered for new placements, but never deleted — past attempts still reference it.

### `ProgrammingLanguage`
Which language/runtime a `CODE` question's test cases execute under.

- **`PYTHON`**, **`JAVA`**, **`JAVASCRIPT`**, **`TYPESCRIPT`**, **`CPP`**, **`C`**, **`CSHARP`**, **`GO`**, **`RUST`**, **`SQL`** — self-explanatory.

### `TestCaseVisibility`
Whether a `CodeTestCase` is shown to the candidate or held back for grading only.

- **`SAMPLE`** — Visible to the candidate before/during the attempt, to sanity-check their solution.
- **`HIDDEN`** — Used only for scoring; the candidate never sees its input/expected output.

### `CalibrationFlag`
What a `QuestionCalibration` run concluded about one question's psychometric behavior.

- **`TOO_EASY`** — Nearly everyone gets it right; it isn't discriminating between strong and weak candidates.
- **`TOO_HARD`** — Nearly everyone gets it wrong.
- **`NEGATIVE_DISCRIMINATION`** — Stronger candidates (by overall score) get it wrong more often than weaker ones — a red flag on the question itself.
- **`MIS_KEY_SUSPECTED`** — The pattern is consistent with the answer key being simply wrong.
- **`NONE`** — Nothing notable; the question behaves as expected.

### `CalibrationAction`
What a calibration run recommends — never applied automatically; a human still has to act.

- **`KEEP`** — No change needed.
- **`REVIEW`** — Worth a human look, but not clearly broken.
- **`RETIRE`** — Recommend pulling it from future use.
- **`REWEIGHT`** — Recommend changing its point value rather than removing it.

---

## Pre-Exam System Check

### `SystemCheckType`
Which pre-flight item is being tested — the catalogue is a table/enum precisely so it can grow without a migration.

- **`BROWSER_COMPATIBILITY`** — Browser version/settings meet requirements.
- **`CAMERA`** / **`MICROPHONE`** / **`SPEAKER`** — The respective device is present and working.
- **`NETWORK_BANDWIDTH`** — Connection is fast/stable enough.
- **`SCREEN_SHARE_PERMISSION`** — Screen-sharing has been granted.
- **`FULLSCREEN`** — Fullscreen mode can be entered.
- **`ENVIRONMENT_SCAN`** — The physical surroundings pass a visual check.
- **`OS_COMPATIBILITY`** — Operating system meets requirements.
- **`SECOND_SCREEN`** — Checks for (and, depending on policy, flags) a second connected display.
- **`EXCEL_RUNTIME`** — Verifies the embedded spreadsheet engine loads and can open a sample workbook.
- **`FORMULA_ENGINE`** — Verifies recalculation works and volatile functions behave as configured.
- **`WORKBOOK_LOAD`** — Verifies the exam's actual template opens without repair prompts or broken links.

### `CheckResult`
Outcome of one pre-flight item.

- **`NOT_RUN`** — Hasn't been attempted yet.
- **`PASSED`** — Clean pass.
- **`WARNING`** — Passed, but with something worth noting (borderline bandwidth, say).
- **`FAILED`** — Did not meet the requirement.
- **`SKIPPED`** — Deliberately not run (e.g. not required by this exam's policy).

### `SystemCheckStatus`
Overall verdict of a whole pre-flight run, once all its items have been checked.

- **`IN_PROGRESS`** — Still running.
- **`PASSED`** — Every required item passed cleanly.
- **`PASSED_WITH_WARNINGS`** — Required items passed; only optional items carried warnings.
- **`FAILED`** — At least one required item failed.
- **`EXPIRED`** — The run's clearance window (`validUntil`) has lapsed; must be re-run.

---

## Identity Verification

### `VerificationMethod`
How a candidate's identity was established for one check.

- **`FACE_MATCH`** — Live capture compared against the enrolment photo by a model.
- **`ID_DOCUMENT`** — A government/institution ID was scanned and checked.
- **`MANUAL_PROCTOR`** — A human proctor confirmed identity directly (used for `manualOverride`).
- **`KNOWLEDGE_CHALLENGE`** — Answered a knowledge-based verification question.
- **`SECOND_FACTOR`** — Verified via the same second factor used for MFA.

### `VerificationStatus`
Outcome of one identity check row.

- **`PENDING`** — Submitted, not yet processed.
- **`IN_PROGRESS`** — Actively being evaluated.
- **`PASSED`** — Cleared.
- **`FAILED`** — Did not clear — terminal for *this row*; a separate `MANUAL_OVERRIDE` row is what recovers it.
- **`MANUAL_OVERRIDE`** — A human proctor vouched for the candidate after an automated failure.
- **`EXPIRED`** — Went stale before completing.

---

## Exam Session & Attempts

### `AttemptStatus`
The central state machine of the whole system — where one candidate's sitting stands.

- **`NOT_STARTED`** — Entitlement exists, attempt hasn't begun.
- **`IN_PROGRESS`** — Actively being sat right now.
- **`PAUSED`** — Temporarily halted (approved pause, or mid-resumption).
- **`SUBMITTED`** — Candidate submitted before time ran out.
- **`AUTO_SUBMITTED`** — The clock ran out and the system submitted on the candidate's behalf.
- **`ABANDONED`** — Started, then never finished and never auto-submitted (the candidate simply left).
- **`EXPIRED`** — Never started, and the window to start it has closed.
- **`INVALIDATED`** — Terminated for cause (proctor/system termination) — the sitting doesn't count.
- **`GRADED`** — All grading on this attempt has settled.

### `GradingStatus`
Grading progress of one answer — most types resolve immediately; essays and code sit in the queue.

- **`NOT_REQUIRED`** — Nothing to grade (e.g. a question that doesn't contribute to scoring).
- **`PENDING`** — Awaiting grading (manual, or an automated run that hasn't happened yet).
- **`IN_PROGRESS`** — A grading pass is actively running/being done.
- **`GRADED`** — Final score is set.
- **`REGRADED`** — Has been graded, then deliberately re-graded (e.g. after an appeal).

### `QuestionStateType`
Where one question stands in a candidate's navigation of the paper.

- **`UNSEEN`** — Never displayed to the candidate.
- **`VIEWED`** — Displayed at least once, no answer saved.
- **`ANSWERED`** — Has a saved answer.
- **`FLAGGED`** — Candidate marked "come back to this" — distinct from an integrity flag.
- **`SKIPPED`** — Candidate moved on without answering.
- **`LOCKED`** — No longer accessible (e.g. the section it's in has `lockOnExit` and was left).

### `PauseRequestStatus`
Status of one request to pause a live attempt.

- **`PENDING`** — Awaiting a decision; only one may be pending per attempt at a time.
- **`APPROVED`** — A proctor approved it.
- **`DENIED`** — A proctor denied it.
- **`AUTO_APPROVED`** — Approved automatically under policy (e.g. a system-detected disconnect), no human decision needed.

### `ResumptionReason`
Why a candidate's session had to be re-established mid-attempt.

- **`BROWSER_CRASH`** — The browser/tab crashed.
- **`NETWORK_DROP`** — Connectivity was lost.
- **`DEVICE_REBOOT`** — The machine restarted.
- **`POWER_LOSS`** — Power failed.
- **`PROCTOR_INITIATED`** — A proctor forced the reconnection (e.g. to move the candidate to a new device).

### `TimingAnomalyType`
The specific shape of an implausible answer-timing pattern.

- **`TOO_FAST_CORRECT`** — Correct, answered far faster than the question's expected time.
- **`TOO_FAST_HIGH_SCORE`** — Same idea, for a partially-scored (not strictly binary-correct) answer.
- **`ZERO_TIME_CORRECT`** — Correct with essentially no time recorded on the question at all.
- **`BURST_SUBMIT`** — Many answers saved in an implausibly tight window, across questions.

---

## Excel Runtime

### `ExcelSessionStatus`
Lifecycle of one candidate's Excel runtime session for an attempt.

- **`PENDING`** — Created, sandbox not yet started.
- **`ACTIVE`** — The candidate is editing the workbook.
- **`SUBMITTED`** — Finalized and locked.
- **`CRASHED`** — The runtime died mid-session — recoverable from the last snapshot plus replayed edits.
- **`RECOVERED`** — Successfully restored after a crash.

### `ExcelRuntimeEngine`
Which spreadsheet runtime hosted one Excel session.

- **`LIBREOFFICE`**, **`ONLYOFFICE`**, **`SHEETJS_HYPERFORMULA`**, **`OFFICE_SCRIPTS`** — self-explanatory.

### `ExcelIntegrityStatus`
Result of checking a submitted workbook's final hash against what the session actually produced.

- **`VALID`** — The hashes match.
- **`TAMPERED`** — They don't — the submitted file isn't what the runtime actually produced.
- **`INCONCLUSIVE`** — The check couldn't be completed (e.g. the runtime crashed before a final hash was ever computed).

### `ExcelSheetOperationType`
A structural change to a sheet within an Excel session, as opposed to a cell-value edit.

- **`INSERT`** / **`DELETE`** — A sheet was added or removed.
- **`RENAME`** — A sheet's name changed — `detail` holds the old name.
- **`HIDE`** / **`UNHIDE`** — Visibility toggled.
- **`PROTECT`** / **`UNPROTECT`** — Sheet-level locking toggled.

---

## Proctoring Sessions

### `ProctoringSessionStatus`
Lifecycle of the supervision session wrapping one attempt.

- **`PENDING`** — Being set up (consent, identity, environment scan in progress).
- **`ACTIVE`** — Supervision is live.
- **`PAUSED`** — Supervision paused, generally mirroring the attempt's own pause.
- **`COMPLETED`** — Ended normally alongside a finished attempt.
- **`TERMINATED`** — Ended for cause (a proctor or auto-action shut it down).
- **`FAILED`** — Ended due to a technical failure rather than a decision (e.g. camera never established).

### `EventSeverity`
How serious one `ProctoringEvent` (or a `RiskEvent`'s underlying signal) is, at face value.

- **`INFO`** — Routine, not concerning on its own (a heartbeat, a benign navigation event).
- **`LOW`** — Minor and common (a single tab switch).
- **`MEDIUM`** — Worth noting; not alarming in isolation.
- **`HIGH`** — A strong individual signal.
- **`CRITICAL`** — Severe enough to warrant immediate attention on its own.

### `EventSource`
Who or what reported a `ProctoringEvent`.

- **`BROWSER_AGENT`** — The candidate's browser-side monitoring script.
- **`AI_ENGINE`** — An AI model's inference pipeline.
- **`HUMAN_PROCTOR`** — A live proctor's own action.
- **`SYSTEM`** — The platform itself (a scheduled sweep, an infrastructure event).

### `AutoAction`
What the platform did on its own, with no human in the loop, in response to an event.

- **`NONE`** — Nothing.
- **`LOG_ONLY`** — Recorded, no visible response.
- **`WARN_CANDIDATE`** — The candidate was shown a warning.
- **`PAUSE_ATTEMPT`** — The attempt was auto-paused.
- **`LOCK_SCREEN`** — The candidate's screen was locked.
- **`NOTIFY_PROCTOR`** — A live proctor was alerted.
- **`TERMINATE_ATTEMPT`** — The attempt was ended outright.

### `ProctoringEventType`
The full vocabulary of observations a session can log — 55 values, grouped here by what they're about (constants are exactly as declared; see [`field-reference.md`](field-reference.md) for the bare list).

- **Session lifecycle:** `SESSION_STARTED`, `SESSION_ENDED`, `CONSENT_ACCEPTED`.
- **Identity/environment setup:** `IDENTITY_CHECK_PASSED`, `IDENTITY_CHECK_FAILED`, `ENVIRONMENT_SCAN_COMPLETED`.
- **Connection/device health:** `HEARTBEAT_MISSED`, `NETWORK_DROP`, `CAMERA_BLOCKED`, `CAMERA_DISCONNECTED`, `MICROPHONE_MUTED`, `DEVICE_CHANGED`, `VIRTUAL_MACHINE_SUSPECTED`, `MULTIPLE_DISPLAYS_DETECTED`.
- **Browser/screen behavior:** `SCREEN_SHARE_STARTED`/`SCREEN_SHARE_STOPPED`, `FULLSCREEN_ENTERED`/`FULLSCREEN_EXITED`, `TAB_SWITCHED`, `WINDOW_BLURRED`/`WINDOW_FOCUSED`, `BROWSER_HIDDEN`/`BROWSER_VISIBLE`, `COPY_ATTEMPT`, `PASTE_ATTEMPT`, `PRINT_ATTEMPT`, `RIGHT_CLICK_BLOCKED`, `KEYBOARD_SHORTCUT_BLOCKED`, `SUSPICIOUS_KEY_SEQUENCE`, `DEV_TOOLS_OPENED`.
- **Vision/audio findings surfaced as events:** `NO_FACE_DETECTED`, `MULTIPLE_FACES_DETECTED`, `FACE_MISMATCH`, `GAZE_OFF_SCREEN`, `PROHIBITED_OBJECT_DETECTED`, `VOICE_DETECTED`, `ABNORMAL_BEHAVIOR`.
- **Human proctor actions logged as events:** `PROCTOR_MESSAGE`, `PROCTOR_WARNING`, `PROCTOR_MANUAL_FLAG`.
- **Attempt lifecycle mirrored into the event log:** `TIME_WARNING_ISSUED`, `ATTEMPT_SUBMITTED`, `ATTEMPT_AUTO_SUBMITTED`, `ATTEMPT_PAUSED`, `ATTEMPT_RESUMED`, `ATTEMPT_TERMINATED`.
- **Excel runtime activity:** `EXCEL_CELL_EDIT`, `EXCEL_BULK_PASTE`, `EXCEL_SHEET_OP`, `EXCEL_MACRO_RUN`, `EXCEL_EXTERNAL_LINK`, `EXCEL_ADDIN_LOAD`, `EXCEL_FOCUS_LOST`, `EXCEL_IDLE_ANSWER_CELL`, `EXCEL_ENGINE_ERROR`, `EXCEL_INTEGRITY_FAIL` — mirrored from the richer `ExcelCellEdit`/`ExcelSheetOperation`/`ExcelMacroExecution` rows into the same event stream every other signal feeds, so the risk engine scores them the same way.

### `EvidenceKind`
What kind of artefact a captured `EvidenceFile` actually is.

- **`WEBCAM_SNAPSHOT`** / **`WEBCAM_CLIP`** — A still frame or short video from the candidate's camera.
- **`SCREEN_SNAPSHOT`** / **`SCREEN_CLIP`** — Same, from the screen share.
- **`AUDIO_CLIP`** — A recorded audio segment.
- **`ID_DOCUMENT`** — A scanned identity document.
- **`ENVIRONMENT_SCAN`** — Captured footage of the room/desk scan.
- **`KEYSTROKE_LOG`** — A log of keyboard activity (not full content — see the audit/privacy notes elsewhere).
- **`EVENT_LOG_BUNDLE`** — A packaged export of proctoring events, not raw media.
- **`EXCEL_WORKBOOK_SNAPSHOT`** — A periodic full-workbook capture during an `ExcelSession`.
- **`EXCEL_FINAL_WORKBOOK`** — The signed, hashed workbook exactly as submitted.
- **`EXCEL_EDIT_LOG_BUNDLE`** — A packaged export of an `ExcelSession`'s cell-edit/sheet-operation history.
- **`EXCEL_DIFF_REPORT`** — A generated diff between the template and a submission.

### `EvidenceUploadStatus`
Where an evidence file sits in its capture-to-storage pipeline.

- **`PENDING`** — Captured, not yet being uploaded.
- **`UPLOADING`** — Transfer in progress.
- **`UPLOADED`** — Safely in object storage, checksum verified.
- **`FAILED`** — The upload itself failed.
- **`QUARANTINED`** — Uploaded, but the pre/post-upload checksums didn't match — treated as potentially tampered, not usable as evidence until resolved.
- **`PURGED`** — Deleted per retention policy. Terminal — a purged file is never re-uploaded under the same id.

### `EvidenceAccessAction`
What someone did with a piece of evidence — every one of these writes an `EvidenceAccessLog` row.

- **`VIEW`** — Looked at it in the UI.
- **`STREAM`** — Played it back as a stream (e.g. video scrubbing).
- **`DOWNLOAD`** — Downloaded a local copy.
- **`EXPORT`** — Included it in an export/report bundle.
- **`SHARE_LINK`** — Generated a shareable link to it.
- **`PURGE`** — Deleted it (the terminal action, logged like every other access).

### `CustodyTransition`
One step in an evidence file's chain of custody — proves the bytes weren't altered between steps.

- **`CAPTURED`** — The artefact was captured from the source stream.
- **`HASHED`** — A checksum was computed over it.
- **`UPLOADED`** — It reached object storage; the checksum is re-verified here.
- **`VERIFIED`** — An explicit, later integrity check confirmed the checksum still matches.
- **`ACCESSED`** — Someone read it (mirrors `EvidenceAccessLog`, but as part of the custody chain).
- **`EXPORTED`** — It left the system as part of an export.
- **`PURGED`** — Deleted — the only truly terminal transition.

### `CandidateLiveStatus`
The candidate's own behavioral state right now, as read by the live monitoring wall — distinct from the connection/stream health enums below.

- **`ACTIVE`** — Behaving normally, currently engaged.
- **`IDLE`** — No recent activity.
- **`SUSPENDED`** — Monitoring/session has been suspended for this candidate.

### `ConnectionStatus`
Health of the client-server transport link.

- **`CONNECTED`** — Normal.
- **`UNSTABLE`** — Connected but flaky (packet loss, jitter).
- **`RECONNECTING`** — Actively trying to re-establish after a drop.
- **`DISCONNECTED`** — No connection right now.

### `StreamStatus`
Health of one media stream (camera, microphone, or screen share) — the same enum reused for all three fields on `LiveSessionStatus`.

- **`UNAVAILABLE`** — Never established.
- **`ACTIVE`** — Streaming normally.
- **`DEGRADED`** — Working, but poor quality.
- **`INTERRUPTED`** — Momentarily cut out.
- **`STOPPED`** — Deliberately or permanently stopped.

### `DeviceTrustLevel`
Standing of one device fingerprint against one exam, built up over repeated sightings.

- **`UNKNOWN`** — Not enough history yet to have an opinion.
- **`KNOWN`** — Seen repeatedly, always under one candidate — unremarkable.
- **`SHARED_SUSPECTED`** — Seen under multiple distinct candidates in the same exam — an automatic flag, not a verdict.
- **`SHARED_CONFIRMED`** — A human reviewer confirmed the sharing was real misconduct (never reached by the system alone).
- **`BLOCKED`** — Barred from starting any new attempt on this exam.

### `ProctorShiftOutcome`
How a live proctor's shift on one session ended.

- **`COMPLETED`** — Ended normally, nothing further needed.
- **`HANDED_OVER`** — Ended by passing the session to another proctor.
- **`ESCALATED`** — Ended by escalating to someone more senior.
- **`ABANDONED`** — Ended without a proper handover (a gap in coverage — worth flagging operationally).

### `ProctorActionType`
What a live human proctor did.

- **`WARN`** — Issued a warning to the candidate.
- **`MESSAGE`** — Sent a plain message (not necessarily a warning).
- **`FLAG`** — Manually flagged something for later review.
- **`PAUSE`** — Paused the attempt.
- **`RESUME`** — Resumed a paused attempt.
- **`TERMINATE`** — Ended the attempt outright (may require a second approver, per policy).
- **`ESCALATE`** — Handed the situation to someone else.
- **`NO_ACTION`** — Reviewed something and deliberately chose to do nothing — itself worth recording.

---

## AI Detection

### `DetectionType`
Which detail table an `AiDetection` row's findings live in.

- **`FACE`** — See `FaceDetection`.
- **`OBJECT`** — See `ObjectDetection`.
- **`BEHAVIOR`** — See `BehaviorDetection`.
- **`AUDIO`** — See `AudioDetection`.

### `GazeDirection`
Where the candidate appeared to be looking in an analysed frame.

- **`ON_SCREEN`** — Looking at their own screen — the expected state.
- **`LEFT`** / **`RIGHT`** / **`UP`** / **`DOWN`** — Looking away in that direction.
- **`AWAY`** — Looking away, direction not clearly one of the above.
- **`UNKNOWN`** — Couldn't be determined from the frame.

### `DetectedObjectClass`
The prohibited-item taxonomy the object-detection model recognizes.

- **`MOBILE_PHONE`** — A phone.
- **`BOOK`** — A book.
- **`PAPER_NOTES`** — Loose paper/notes.
- **`SECOND_LAPTOP`** — A second computer.
- **`SECOND_MONITOR`** — An extra display.
- **`EARPHONE`** — Earbuds/headphones.
- **`SMARTWATCH`** — A wearable device.
- **`CAMERA`** — A second camera device (not the exam's own webcam).
- **`ADDITIONAL_PERSON`** — Another person visible in frame.
- **`UNKNOWN_OBJECT`** — Something flagged as out of place that doesn't fit a known class.

### `BehaviorType`
Patterns recognized over a time window rather than a single frame.

- **`LOOKING_AWAY`** — Sustained gaze diversion.
- **`LEAVING_SEAT`** — The candidate got up/left frame.
- **`FACE_OUT_OF_FRAME`** — Face not visible for a sustained period (without necessarily "leaving").
- **`TALKING`** — Sustained mouth movement consistent with speech.
- **`BACKGROUND_VOICE`** — A voice other than the candidate's, sustained.
- **`SUSPICIOUS_MOVEMENT`** — Unusual physical movement pattern.
- **`RAPID_TYPING_BURST`** — An atypical burst of fast typing.
- **`PROLONGED_IDLE`** — No meaningful activity for a long stretch.
- **`SCREEN_OCCLUSION`** — Something is blocking the screen/camera view.
- **`REPEATED_TAB_SWITCH`** — Tab-switching recurring often enough to be a behavior pattern, not a one-off.

### `AudioEventType`
What the audio model heard in an analysed window.

- **`SPEECH_DETECTED`** — Someone spoke.
- **`MULTIPLE_VOICES`** — More than one distinct voice.
- **`BACKGROUND_NOISE`** — Sustained ambient noise.
- **`AUDIO_INTERRUPTION`** — The audio feed itself cut out or glitched.
- **`PROLONGED_SILENCE`** — Unusually long silence (can itself be a signal — e.g. a muted mic hiding something).
- **`SUSPICIOUS_AUDIO`** — Something audio-based that doesn't fit the other categories but warrants a look.

### `SuspiciousActivityType`
What a correlation rule concluded from several signals co-occurring.

- **`REPEATED_LOOKING_AWAY`** — Gaze-diversion pattern crossed a correlation threshold.
- **`MULTIPLE_FACES`** — Correlated from repeated multi-face detections.
- **`PHONE_DETECTED`** — Correlated from object detections.
- **`NO_FACE`** — Correlated from sustained face-absence.
- **`SUSPICIOUS_OBJECT`** — A prohibited object pattern beyond a single sighting.
- **`TAB_SWITCHING`** — A tab-switch pattern crossing a threshold.
- **`FULLSCREEN_EXIT`** — Repeated or sustained fullscreen exits.
- **`AUDIO_EVENT`** — An audio-based pattern.
- **`IMPERSONATION_SUSPECTED`** — Signals consistent with someone other than the enrolled candidate.
- **`COMBINED_BEHAVIOR`** — A correlation across *different kinds* of signal (e.g. phone + looking away + a second voice), rather than repetition of one kind.

### `ActivityVerdict`
Where a `SuspiciousActivity` stands in being adjudicated — distinct from `ItemVerdict`, which is a reviewer's per-item call inside a review case.

- **`DETECTED`** — Raised by correlation, not yet looked at.
- **`CONFIRMED`** — Looked at and judged genuine.
- **`FALSE_POSITIVE`** — Looked at and judged not genuine.
- **`UNDER_REVIEW`** — Currently being looked at.
- **`DISMISSED`** — Closed without a strong verdict either way (e.g. inconclusive, not worth pursuing further).

---

## Risk Engine

### `RiskLevel`
The band a numeric risk score resolves into.

- **`LOW`** — Nothing concerning.
- **`MEDIUM`** — Some signal, not conclusive.
- **`HIGH`** — Multiple or serious signals.
- **`CRITICAL`** — Severe, sustained, or clearly deliberate.

### `RiskRecommendation`
What the risk engine advises for an attempt at its current score — advisory; a human can override it.

- **`ALLOW`** — No action needed.
- **`MONITOR`** — Keep watching, nothing to do yet.
- **`FLAG_FOR_REVIEW`** — Open (or escalate toward) a human review case.
- **`INVALIDATE_ATTEMPT`** — Recommend the sitting doesn't count.
- **`REQUIRE_RETAKE`** — Recommend the candidate be allowed/required to sit again.

---

## Review System

### `ReviewCaseStatus`
Workflow state of a human integrity investigation.

- **`OPEN`** — Just created, unassigned.
- **`ASSIGNED`** — A reviewer has been assigned.
- **`IN_REVIEW`** — The reviewer is actively working it.
- **`PENDING_INFO`** — Waiting on more information before it can proceed.
- **`ESCALATED`** — Handed to someone more senior.
- **`RESOLVED`** — A final decision has been recorded.
- **`CLOSED`** — Fully wrapped up (candidate notified where applicable).

### `ReviewPriority`
Queue ordering for review cases.

- **`LOW`** / **`NORMAL`** / **`HIGH`** / **`URGENT`** — Self-explanatory increasing urgency.

### `ReviewDecisionType`
The kind of action one recorded decision represents.

- **`CLEAR`** — The candidate/attempt is cleared of suspicion.
- **`WARN_CANDIDATE`** — A formal warning is issued, short of invalidating anything.
- **`ADJUST_SCORE`** — The result's score is being changed.
- **`INVALIDATE_ATTEMPT`** — The sitting is voided.
- **`GRANT_RETAKE`** — A retake is authorized (this is the precondition `RetakeGrantService.grantRetake` requires).
- **`ESCALATE`** — Pass the case up to someone else.
- **`REQUEST_MORE_INFO`** — Can't decide yet; more information is needed.

### `ReviewOutcome`
The settled, case-level label once a case closes — mirrors whichever decision ended up final.

- **`CLEARED`** — Ended in a clean bill.
- **`WARNING_ISSUED`** — Ended with a formal warning.
- **`SCORE_ADJUSTED`** — Ended with the score changed.
- **`ATTEMPT_INVALIDATED`** — Ended with the sitting voided.
- **`RETAKE_GRANTED`** — Ended with a retake authorized.
- **`ESCALATED`** — Handed off rather than resolved here.
- **`NO_ACTION`** — Reviewed and deliberately closed with nothing done.

### `ItemVerdict`
A reviewer's judgement on one specific flagged item (an event, a detection, a piece of evidence) inside a case — not the case's overall outcome.

- **`VALID`** — The flag was a genuine finding.
- **`FALSE_POSITIVE`** — The flag was wrong — this is what feeds AI model performance metrics.
- **`NEEDS_INVESTIGATION`** — Can't call it yet; needs more digging.
- **`INCONCLUSIVE`** — Looked into it, still can't say either way.

### `ReviewItemKind`
Which of five tables a `ReviewFinding.itemId` points into.

- **`PROCTORING_EVENT`**, **`AI_DETECTION`**, **`SUSPICIOUS_ACTIVITY`**, **`EVIDENCE_FILE`**, **`RISK_FACTOR`** — self-explanatory; tells the reader which table to join `itemId` against.

---

## Exam Results

### `ResultStatus`
Release state of one exam result.

- **`PROVISIONAL`** — Computed, not yet finalized — can still change.
- **`PENDING_REVIEW`** — Blocked from finalizing by an open integrity review.
- **`WITHHELD`** — Explicitly held back from the candidate (see `ResultWithholding` for why).
- **`FINAL`** — Settled; nothing further will change it barring an appeal reopening the case.
- **`VOID`** — Nullified after the fact (the sitting was invalidated).

### `IntegrityStatus`
The integrity verdict attached to a result — runs in parallel with `ResultStatus`, not inside it (a result can be `FINAL` and still `FLAGGED`).

- **`CLEAN`** — No integrity concerns.
- **`FLAGGED`** — Something raised a concern.
- **`UNDER_REVIEW`** — A human review case is open on it.
- **`INVALIDATED`** — The review concluded the sitting shouldn't count.

### `WithholdingReason`
Why one result is being held back from release.

- **`RISK_THRESHOLD`** — The resolved risk band requires withholding.
- **`OPEN_REVIEW_CASE`** — There's an open integrity investigation.
- **`PENDING_MANUAL_GRADING`** — Some answers still await a human grader.
- **`APPEAL_FILED`** — The candidate (or someone) has filed an appeal.
- **`EXTERNAL_HOLD`** — Held for a reason outside this system (e.g. an institutional hold on the account).

---

## Proctoring Reports

### `ReportStatus`
Generation state of one rendered report.

- **`QUEUED`** — Requested, not yet started.
- **`GENERATING`** — Being rendered right now.
- **`AVAILABLE`** — Ready to download/view.
- **`FAILED`** — Rendering failed.
- **`EXPIRED`** — Past its retention window.

### `ReportFormat`
The rendered file format.

- **`PDF`**, **`HTML`**, **`JSON`**, **`ZIP_BUNDLE`** — self-explanatory; `ZIP_BUNDLE` typically packages the report alongside evidence files.

---

## Notifications

### `NotificationChannel`
Delivery transport for one notification.

- **`EMAIL`**, **`SMS`**, **`IN_APP`**, **`PUSH`**, **`WEBHOOK`** — self-explanatory; `WEBHOOK` is for system-to-system delivery (e.g. an institution's own notification pipeline).

### `NotificationType`
The business event that triggered a notification.

- **`ACCOUNT_VERIFICATION`** — Verify-your-email prompt.
- **`PASSWORD_RESET`** — Reset-your-password link.
- **`EXAM_INVITATION`** — You've been assigned an exam.
- **`EXAM_REMINDER`** — Your exam is coming up (e.g. 24h/1h before).
- **`EXAM_STARTED`** — Confirms an attempt began.
- **`EXAM_SUBMITTED`** — Confirms an attempt was submitted.
- **`ATTEMPT_TERMINATED`** — An attempt was ended for cause.
- **`REVIEW_ASSIGNED`** — A review case was assigned to a reviewer.
- **`REVIEW_COMPLETED`** — A review case closed.
- **`RESULT_PUBLISHED`** — A result is now available.
- **`HIGH_RISK_ALERT`** — A live safety/integrity alert (exempt from suppression rules).
- **`SYSTEM_ALERT`** — A platform-level alert (also exempt from suppression).

### `NotificationStatus`
Delivery state of one outbound message.

- **`PENDING`** — Created, not yet dispatched.
- **`QUEUED`** — Deliberately deferred (e.g. by a quiet-hours or rate-limit suppression rule).
- **`SENT`** — Handed off to the channel.
- **`DELIVERED`** — Confirmed arrival.
- **`OPENED`** — Recipient opened it.
- **`FAILED`** — Delivery failed after retries were exhausted.
- **`CANCELLED`** — Deliberately never sent (e.g. blocked by an opt-out rule).

### `SuppressionKind`
Which mechanism a `NotificationSuppression` rule applies.

- **`QUIET_HOURS`** — Defer sends during a local time window.
- **`OPT_OUT`** — Recipient opted out of this type/channel entirely.
- **`RATE_LIMIT`** — Cap how many can go out in a window.
- **`DUPLICATE_WINDOW`** — Block near-identical sends within a window (distinct from `idempotencyKey`, which only catches a retried trigger, not a fresh one).

---

## Audit

### `AuditAction`
The category of consequential action one `AuditLog` row records.

- **`CREATE`** / **`UPDATE`** / **`DELETE`** — Generic row lifecycle actions.
- **`LOGIN`** / **`LOGOUT`** — Session lifecycle.
- **`PUBLISH`** — An exam (or similar) was published.
- **`ASSIGN`** — An exam/role/reviewer assignment was made.
- **`START_ATTEMPT`** / **`SUBMIT_ATTEMPT`** — Attempt lifecycle milestones.
- **`GRADE`** — A grading action.
- **`SCORE_OVERRIDE`** — A score was manually adjusted — reason required.
- **`TERMINATE_ATTEMPT`** — An attempt was ended for cause — reason required.
- **`VIEW_EVIDENCE`** — Evidence was accessed.
- **`EXPORT`** — Data left the system as an export.
- **`CONFIG_CHANGE`** — A system setting or risk/config value changed — reason required.
- **`PERMISSION_CHANGE`** — A role/permission grant changed — reason required.
- **`MODEL_CHANGE`** — An AI model version was registered/promoted.
- **`PAYMENT_METHOD_ADDED`** / **`PAYMENT_METHOD_REMOVED`** — A candidate's payment card was added
  to, or revoked from, their account.
- **`PAYMENT_METHOD_SET_DEFAULT`** — A candidate changed which card is charged by default.
- **`PAYMENT_REFUNDED`** — A payment transaction was refunded.
- **`PAYMENT_DISPUTED`** — A cardholder disputed or charged back a payment.

### `AuditOutcome`
Whether the audited operation actually succeeded.

- **`SUCCESS`** — Went through.
- **`FAILURE`** — Attempted, failed (a technical error).
- **`DENIED`** — Attempted, rejected (a permission/business-rule check failed it).

---

## AI Model Registry

### `ModelPurpose`
What job a registered model does.

- **`FACE_DETECTION`**, **`FACE_VERIFICATION`**, **`HEAD_POSE`**, **`GAZE_ESTIMATION`**, **`OBJECT_DETECTION`**, **`PERSON_DETECTION`**, **`SPEECH_DETECTION`**, **`VOICE_DIARIZATION`**, **`BEHAVIOR_ANALYSIS`**, **`RISK_SCORING`**, **`EXCEL_ANOMALY_DETECTION`** — each names the specific inference task; a `ShadowEvaluation` only ever compares two versions with the same purpose. `EXCEL_ANOMALY_DETECTION` scores Excel editing patterns (bulk paste, instant fill, macro abuse) the same way the vision/audio models score their own signals.

### `ModelVersionStatus`
Rollout state of one model version.

- **`DRAFT`** — Registered, not yet in any evaluation pipeline.
- **`CANDIDATE`** — Being prepared for shadow testing.
- **`SHADOW`** — Running in parallel with the active version on real traffic, but never driving an auto-action — purely for comparison.
- **`ACTIVE`** — The one actually used for live decisions.
- **`DEPRECATED`** — Replaced by a newer active version, kept for reference.
- **`RETIRED`** — No longer in use at all.

### `MetricType`
Which dimension of model performance a `ModelPerformanceMetric` row measures.

- **`PRECISION`**, **`RECALL`**, **`F1_SCORE`**, **`ACCURACY`** — standard classification metrics, computed from reviewer-labelled ground truth.
- **`FALSE_POSITIVE_RATE`** / **`FALSE_NEGATIVE_RATE`** — how often the model is wrong, and in which direction.
- **`LATENCY_P95_MS`** — 95th-percentile inference latency.
- **`THROUGHPUT_FPS`** — Frames processed per second.

### `ShadowRecommendation`
What a shadow-vs-active comparison recommends.

- **`PROMOTE`** — The shadow version performs at least as well; promote it to active.
- **`HOLD`** — Not enough evidence yet either way.
- **`RETIRE`** — The shadow version performs worse; abandon it.

---

## System Configuration

### `SettingCategory`
Which of the nine configuration areas a `SystemSetting` belongs to — matches the areas the product's own feature tree names.

- **`GENERAL`**, **`PROCTORING`**, **`RISK`**, **`EXAM`**, **`STORAGE`**, **`NOTIFICATION`**, **`AI`**, **`SECURITY`** — self-explanatory.
- **`EXCEL`** — Deployment-level Excel runtime config: installed engines, per-session resource quotas, the global allowed-functions/add-ins whitelist. Per-exam Excel policy lives on `Exam.excelPolicy` instead, not here.

### `SettingValueType`
How a setting's stored string should be parsed.

- **`STRING`** — Plain text.
- **`INTEGER`** — Whole number.
- **`DECIMAL`** — Fractional number.
- **`BOOLEAN`** — true/false.
- **`JSON`** — A structured value.
- **`DURATION`** — A time span.
- **`ENUM`** — One of a fixed set of string values (validated against `validationRule`).

### `RiskFactorTrigger`
What kind of signal a `RiskFactorConfig` rule reacts to.

- **`PROCTORING_EVENT`** — A raw event from the browser/AI/proctor.
- **`AI_DETECTION`** — A raw model inference.
- **`SUSPICIOUS_ACTIVITY`** — A correlated alert, not a raw signal.
- **`DEVICE_SIGNAL`** — Something from device/session tracking (e.g. a shared-device flag).
- **`IDENTITY_SIGNAL`** — Something from identity verification.

---

## A note on scope

This document explains every constant's *meaning*. For how a state-machine enum's values connect
to each other over time, see [`enum-workflow.md`](enum-workflow.md); for which entity/column each
one lives on, see [`field-reference.md`](field-reference.md).
