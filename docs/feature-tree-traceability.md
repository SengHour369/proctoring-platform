# Proctoring Schema Docket

Every branch of the 24-section feature tree, mapped to the exact entity, field, or enum constant
that implements it — plus the places where the mapping isn't 1:1, and why.

**64** tables · **66** entity classes · **67** enums · **122** foreign keys · **0** JPA associations
(relations are plain `Long …Id` columns, enforced by `db/schema-foreign-keys.sql`, indexed in
`docs/entity-relationships.md`).

**Why some rows point at the same table.** STUDENT / TEACHER / REVIEWER / ADMIN are rows in
`roles`, not four tables. Every person is one `User` row; capacities come from which roles are
granted via `UserRole` — so one account can be both a teacher and a reviewer without duplication.

**Why relationships aren't in the Java.** By request, the model carries no `@ManyToOne` /
`@OneToMany` / `@JoinColumn`. Every relationship below is a plain `Long …Id` column; referential
integrity lives in a companion SQL script, not in the entity classes.

---

## Contents

1. [Authentication & Authorization](#1-authentication--authorization)
2. [User Management](#2-user-management) 
3. [Exam Management](#3-exam-management)
4. [Exam Assignment](#4-exam-assignment)
5. [Pre-Exam System Check](#5-pre-exam-system-check)
6. [Identity Verification](#6-identity-verification)
7. [Exam Session](#7-exam-session)
8. [Proctoring Session](#8-proctoring-session)
9. [Browser & Screen Monitoring](#9-browser--screen-monitoring)
10. [AI Computer Vision](#10-ai-computer-vision)
11. [AI Audio Detection](#11-ai-audio-detection)
12. [AI Suspicious Activity Detection](#12-ai-suspicious-activity-detection)
13. [AI Event Engine](#13-ai-event-engine)
14. [Evidence Management](#14-evidence-management)
15. [Risk Engine](#15-risk-engine)
16. [Real-Time Monitoring](#16-real-time-monitoring)
17. [Review System](#17-review-system)
18. [Exam Result](#18-exam-result)
19. [Proctoring Report](#19-proctoring-report)
20. [Admin Dashboard](#20-admin-dashboard)
21. [Notification](#21-notification)
22. [Audit & Security](#22-audit--security)
23. [AI Model Management](#23-ai-model-management)
24. [System Configuration](#24-system-configuration)

---

## 1. Authentication & Authorization

Login, tokens and one-time flows all key off a single `User` row; who a user is allowed to do
lives entirely in the grant tables below it.

### Authentication

| Task | Modeled by | Note |
|---|---|---|
| Login | `LoginAttempt` | One row per attempt, success or fail, with `LoginOutcome` — feeds rate limiting before a user row even matches. |
| Login (MFA) | `User.mfaEnabled` / `.mfaMethod` / `.mfaSecretEncrypted` | `MfaMethod`: TOTP, SMS, EMAIL — null method means MFA is off. `LoginOutcome.MFA_REQUIRED` / `MFA_FAILED` cover the extra outcomes. |
| Logout | `UserSession.revokedAt` | Sessions are revoked server-side, not just discarded client-side. |
| Refresh Token | `UserSession.refreshTokenHash` | Hash only, never the token — a table dump can't be replayed. |
| Forgot Password | `SecurityToken` | One table for verify / reset / invite / email-change, discriminated by `TokenPurpose`. |
| Reset Password | `SecurityToken.usedAt` | Row stays after redemption — deleting it would lose proof a reset happened. |
| Email Verification | `SecurityToken` (`TokenPurpose.EMAIL_VERIFICATION`) | Same table as password reset — same lifecycle, same rules. |

### Authorization

| Task | Modeled by | Note |
|---|---|---|
| Role Management | `roles` | A table an operator can add to, not an enum baked into code. |
| Permission Management | `Permission`, `RolePermission` | `resource:action` codes, re-scoped by editing grants — no redeploy. |
| Access Control | `UserRole`, `ApiClient` | Human grants and machine credentials are separate tables — see §22. |

### Roles

STUDENT, TEACHER, REVIEWER, ADMIN are seeded rows in `roles`, joined to `users` through
`UserRole` — never a fixed enum, so a fifth role (external invigilator, read-only auditor) is a
data change.

---

## 2. User Management

No `Student`, `Teacher`, `Reviewer` or `Admin` table exists. Every person is one `User` row; the
section title describes a role grant, not a distinct entity.

### Student Management

| Task | Modeled by | Note |
|---|---|---|
| Create Student | `User` + `UserRole` | Insert the user, grant the STUDENT role. |
| Update Student | `User` fields | fullName, phoneNumber, timeZone, locale, enrolmentPhotoPath, voiceprintPath. |
| Disable Student | `User.status` | `UserStatus.SUSPENDED` / `DISABLED` — no row deletion. |
| Student Profile | `User` | externalRef (SIS id), enrolmentPhotoPath (identity-check reference), voiceprintPath (audio-check reference). |

| Task | Modeled by | Note |
|---|---|---|
| Teacher / Reviewer / Admin Management | `User` + `UserRole` | Identical mechanics to Student Management — only the granted role differs. |
| User Activity | `AuditLog`, `LoginAttempt`, `UserSession` | Three angles on "what has this account done": changes, sign-ins, live sessions. |

---

## 3. Exam Management

The largest single section, and the one entity that gets most of its own module: `Exam` at the
root, sectioned and questioned below it, ruled by an embedded policy object.

### Exam

| Task | Modeled by | Note |
|---|---|---|
| Create / Update / Delete Exam | `Exam` | Standard CRUD row; "delete" past first publish is a status change, not a row deletion. |
| Publish Exam | `Exam.status = PUBLISHED` | Structure freezes here — attempts reference sections/questions by id, so post-publish edits bump `version` instead. |
| Draft Exam | `ExamStatus.DRAFT` | Default status on creation. |
| Activate Exam | `ExamStatus.ACTIVE` | The open-for-sitting state between PUBLISHED and CLOSED. |
| Close Exam | `ExamStatus.CLOSED` | No further attempts can start. |

### Exam Information

| Task | Modeled by | Note |
|---|---|---|
| Title / Description | `Exam.title` / `.description` | |
| Duration | `Exam.durationMinutes` | Null means the closing time is the only limit. |
| Start / End Time | `Exam.opensAt` / `.closesAt` | Narrowed per candidate by `ExamAssignment.windowStartAt/EndAt`. |
| Attempt Limit | `Exam.maxAttempts` | Overridable per candidate via `ExamAssignment.attemptsAllowed` (retake approvals). |

### Sections

| Task | Modeled by | Note |
|---|---|---|
| Create Section | `ExamSection` | |
| Section Ordering | `ExamSection.sequenceNo` | Unique per exam. |
| Section Time | `ExamSection.timeLimitMinutes` | Per-part budget on top of the exam's overall duration. |

### Questions

| Task | Modeled by | Note |
|---|---|---|
| Multiple Choice / True-False | `QuestionType.SINGLE_CHOICE` / `TRUE_FALSE` | |
| Multiple Answer | `QuestionType.MULTIPLE_CHOICE` | Answers stored via `AttemptAnswerOption` (many rows per answer). |
| Short Answer / Essay | `QuestionType.SHORT_ANSWER` / `ESSAY` | Graded manually by default — see `GradingStatus` in §7. |
| Coding Question | `QuestionType.CODE` + `Question.programmingLanguage`/`.starterCode`/`.executionTimeLimitMs`/`.executionMemoryLimitMb` | Test cases live in a child table, `CodeTestCase` (`inputData`, `expectedOutput`, `visibility`: SAMPLE/HIDDEN, `points`) — same reasoning as `QuestionOption`: a property of the bank item, not the placement. Grading outcome per test case is `CodeExecutionResult` (`passed`, `actualOutput`, `runtimeMs`, `memoryKb`, `errorMessage`), keyed off `AttemptAnswer` the same way `AttemptAnswerOption` is. |

> The type set actually has ten members — it also covers NUMERIC, MATCHING, ORDERING and
> FILL_IN_BLANK, none of which the tree lists.

### Question Bank

| Task | Modeled by | Note |
|---|---|---|
| Categories | `QuestionCategory` | Self-referencing tree with a materialised path, so a subtree draw is one query. |
| Difficulty | `Question.difficulty` (`QuestionDifficulty`) | |
| Tags | `QuestionTag` + `QuestionTagLink` | A real table, not a comma-separated column — a typo'd tag is worse than none. |
| Question Pool | `ExamSection.questionsToDraw` | Deliberate simplification: a section already is a pool with a draw size. A reusable named pool across exams would be a new entity if ever needed. |

### Randomization

| Task | Modeled by | Note |
|---|---|---|
| Random Questions | `ExamSection.shuffleQuestions` / `questionsToDraw` | |
| Random Answers | `ExamQuestion.shuffleOptions` | |
| Question Ordering | `QuestionState.displayOrder` / `optionOrder` | The actual shuffle each candidate received, stored — not just the rule that produced it. |

### Exam Rules

| Task | Modeled by | Note |
|---|---|---|
| Camera / Microphone Required | `ProctoringPolicy.requireWebcam` / `requireMicrophone` | |
| Screen Monitoring | `.requireScreenShare` / `.monitorScreen` | Two flags: requiring the share vs. actively watching it. |
| Fullscreen Required | `.forceFullscreen` | |
| Tab Switch Detection | `.detectTabSwitch` / `allowedTabSwitches` | |
| Face / Multiple Face Detection | `.detectFace` / `.detectMultipleFaces` | |
| Phone Detection | `.detectObjects` | Covers the whole prohibited-object catalogue, not phones alone. |
| Audio Detection | `.detectAudio` / `.retainAudioTranscript` | |

> **Design decision.** Every toggle above lives on `ProctoringPolicy`, embedded directly in `Exam`
> — not a shared, editable settings row. Requiring a camera is about admission to the sitting;
> enabling a detector is about what's done with the stream afterwards, and each one enabled is a
> model call per frame plus rows in `ai_detections` for the whole retention period. Keeping this
> per-exam means a global default change can never silently alter the rules of a sitting already
> in progress.

---

## 4. Exam Assignment

`ExamAssignment` is the single source of truth for who may sit an exam; group and class
assignment fan out into it rather than replacing it.

| Task | Modeled by | Note |
|---|---|---|
| Assign Exam to Student | `ExamAssignment` | One row per (exam, candidate); carries window, dueAt, attemptsAllowed, extraTimeMinutes. |
| Assign Exam to Group / Class | `ExamGroupAssignment` → `StudentGroup` | Fans out into one `ExamAssignment` per member; autoEnrollNewMembers keeps the fan-out live. |
| Exam Invitation | `ExamInvitation` | Separate from the assignment — resends (bounce, reminder) get their own token and delivery state without touching the entitlement. |
| Exam Access Code | `ExamAssignment.accessCodeHash` | Hashed one-time code. |
| Exam Eligibility | `ExamAssignment.status` / `windowStartAt` / `windowEndAt` + `ExamPrerequisite` | `AssignmentStatus`: ASSIGNED, NOTIFIED, STARTED, SUBMITTED, EXPIRED, CANCELLED. Prerequisite *rules* ("passed exam X at ≥score" / "completed course Y") are `ExamPrerequisite` rows, one per rule, evaluated against a candidate's history in service code — not embedded in `Exam`, since a rule is 1:many and `isActive` lets one retire without losing the history of what gated past assignments. |

---

## 5. Pre-Exam System Check

One catalogue table, not one column per check — the catalogue can grow without a migration.

| Task | Modeled by | Note |
|---|---|---|
| Browser Check | `SystemCheckType.BROWSER_COMPATIBILITY` | |
| Camera / Microphone / Speaker Check | `CAMERA` / `MICROPHONE` / `SPEAKER` | |
| Network Check | `NETWORK_BANDWIDTH` | `SystemCheck.downloadMbps` / `uploadMbps` / `latencyMs`. |
| Screen Permission | `SCREEN_SHARE_PERMISSION` | |
| Fullscreen Check | `FULLSCREEN` | |
| Environment Check | `ENVIRONMENT_SCAN` | Also OS_COMPATIBILITY, SECOND_SCREEN beyond the tree's list. |

Each row is a `SystemCheckItem` under a parent `SystemCheck` run, which links to the
`ExamAttempt` only once the candidate proceeds — a proctor override (overridden, overriddenBy,
overrideReason) is recorded on the run, not silently discarded.

---

## 6. Identity Verification

| Task | Modeled by | Note |
|---|---|---|
| Student Authentication | §1 `User` / `UserSession` | Account login — distinct from in-exam identity below. |
| Face Verification | `IdentityVerification.method = FACE_MATCH` | matchScore / matchThreshold / livenessScore against referencePhotoPath. |
| Identity Matching | `.matchScore` vs. `User.enrolmentPhotoPath` | Threshold stored per check — a later retune can't reinterpret an old verdict. |
| Verification Status | `VerificationStatus` | PENDING, IN_PROGRESS, PASSED, FAILED, MANUAL_OVERRIDE, EXPIRED. |
| Verification Audit | `.verifiedBy` / `verifiedAt` / `overrideReason` | Many rows per attempt — a failed match followed by a proctor override is two rows, both kept. |

---

## 7. Exam Session

`ExamAttempt` is the spine of the whole model — everything downstream (answers, proctoring,
risk, review, results) hangs off it, not off the exam or the candidate directly.

| Task | Modeled by | Note |
|---|---|---|
| Start Exam / Create Session | `ExamAttempt.startedAt` | |
| Session Token | `.sessionTokenHash` | Scoped to the attempt, not the login session — a second tab can't drive the same sitting. |
| Session Heartbeat | `.lastHeartbeatAt` | |

### Question Navigation

| Task | Modeled by | Note |
|---|---|---|
| Next / Previous | `QuestionState.displayOrder` | |
| Mark for Review | `QuestionStateType.FLAGGED` | Distinct from `AttemptAnswer.flaggedByCandidate` — an integrity flag vs. a "come back to this" marker. |
| Question List | `QuestionState` rows for the attempt | Ordered by displayOrder. |

### Answer Management

| Task | Modeled by | Note |
|---|---|---|
| Save Answer | `AttemptAnswer` | One row per (attempt, question) — current state, overwritten in place. |
| Auto Save | `AnswerRevision.autoSaved` | Append-only history — recovers an answer after a crash. |
| Update Answer | `AttemptAnswer.revisionCount` | |
| Answer History | `AnswerRevision` | Every save, manual or auto, timestamped and never overwritten. |

### Timer

| Task | Modeled by | Note |
|---|---|---|
| Exam Timer | `ExamAttempt.expiresAt` / `timeSpentSeconds` | |
| Warning | `ProctoringEventType.TIME_WARNING_ISSUED` | |
| Timeout | `AttemptStatus.EXPIRED` / `AUTO_SUBMITTED` | |

### Pause / Resume & Submit

| Task | Modeled by | Note |
|---|---|---|
| Pause / Resume | `AttemptStatus.PAUSED`, `.pausedSeconds` + `AttemptPauseRequest` | Pausing needs approval, and an event log (`ProctoringEventType.ATTEMPT_PAUSED`) has no PENDING state to gate a live decision on — `AttemptPauseRequest` (`status`: PENDING/APPROVED/DENIED/AUTO_APPROVED, `decidedByUserId`, `resumedAt`) is the proctor's actionable queue, same shape as `IdentityVerification`/`SystemCheck`. `pausedSeconds` still tracks cumulative duration once resumed. |
| Submit Exam | `AttemptStatus.SUBMITTED` / `AUTO_SUBMITTED`, `.submittedAt` | |

---

## 8. Proctoring Session

One `ProctoringSession` per attempt — the supervision envelope; live state and event history are
split into separate tables below it.

| Task | Modeled by | Note |
|---|---|---|
| Start Proctoring | `ProctoringSession.status = PENDING → ACTIVE` | |
| Camera / Microphone / Screen Monitoring | `LiveSessionStatus.cameraStatus` / `microphoneStatus` / `screenStatus` | `StreamStatus` per stream. |
| Browser Monitoring | `ProctoringEvent` (§9 types) | |
| Fullscreen Monitoring | `.fullscreen`, `FULLSCREEN_ENTERED`/`EXITED` | |
| Network Monitoring | `.networkLatencyMs`, `ConnectionStatus` | |
| Device Monitoring | `DeviceSession` | fingerprint, OS, monitorCount, vpnSuspected, virtualMachineSuspected — two overlapping rows with different fingerprints is itself a signal. |

---

## 9. Browser & Screen Monitoring

Every leaf is a constant of `ProctoringEventType`. Each transition keeps its counterpart —
*entered* as well as *exited* — so a duration, not just an occurrence, can be computed.

| Task | Enum constant(s) |
|---|---|
| Tab Switch | `TAB_SWITCHED` |
| Window Blur / Focus | `WINDOW_BLURRED`, `WINDOW_FOCUSED` |
| Fullscreen Exit | `FULLSCREEN_EXITED`, `FULLSCREEN_ENTERED` |
| Screen Share | `SCREEN_SHARE_STARTED`, `SCREEN_SHARE_STOPPED` |
| Browser Visibility | `BROWSER_HIDDEN`, `BROWSER_VISIBLE` |
| Copy / Paste / Print | `COPY_ATTEMPT`, `PASTE_ATTEMPT`, `PRINT_ATTEMPT` |
| Keyboard Events | `KEYBOARD_SHORTCUT_BLOCKED`, `SUSPICIOUS_KEY_SEQUENCE` |

---

## 10. AI Computer Vision

Vision findings land in `FaceDetection` and `ObjectDetection`, each keyed to a parent
`AiDetection` row that carries confidence, model version and the source frame.

| Task | Modeled by | Note |
|---|---|---|
| Face Present / Face Count | `FaceDetection.faceCount` | 0 = candidate left frame. |
| Multiple Person Detection | `.faceCount > 1` | |
| No Face Detection | `.faceCount = 0` | `ProctoringEventType.NO_FACE_DETECTED` raised alongside. |
| Face Verification | `.identityMatchScore` / `identityMatched` | Cross-referenced against §6's `IdentityVerification`. |
| Head Pose (Left/Right/Up/Down) | `.headYaw` / `headPitch` / `headRoll` | Stored as continuous degrees, not four booleans — direction is derived from the angle at read time. |
| Gaze Estimation | `.gazeDirection` (`GazeDirection`), `.gazeOffScreenMs` | |
| Object Detection (Phone/Book/Laptop/Unauthorized) | `ObjectDetection.objectClass` (`DetectedObjectClass`) | MOBILE_PHONE, BOOK, SECOND_LAPTOP, UNKNOWN_OBJECT + 6 more classes. |
| Person Detection | `.objectClass = ADDITIONAL_PERSON` | Same table as object detection. |

---

## 11. AI Audio Detection

| Task | Modeled by | Note |
|---|---|---|
| Speech Detection | `AudioDetection.audioEventType = SPEECH_DETECTED` | |
| Multiple Voice Detection | `MULTIPLE_VOICES`, `.speakerCount`, `.unknownSpeaker` | Compared against `User.voiceprintPath`. |
| Background Noise | `BACKGROUND_NOISE` | `.averageDb` / `signalToNoiseRatio`. |
| Audio Interruption | `AUDIO_INTERRUPTION` | |
| Suspicious Audio Event | `SUSPICIOUS_AUDIO` | `.transcriptExcerpt` is short and gated by `ProctoringPolicy.retainAudioTranscript`. |

---

## 12. AI Suspicious Activity Detection

A correlation layer above raw detections: `SuspiciousActivity` folds several signals inside one
time window into a single incident a human can act on.

| Task | Modeled by | Note |
|---|---|---|
| Repeated Looking Away | `SuspiciousActivityType.REPEATED_LOOKING_AWAY` | |
| Multiple Faces / Phone Detected / No Face | `MULTIPLE_FACES`, `PHONE_DETECTED`, `NO_FACE` | Each links back to the originating `AiDetection` row via primaryDetection. |
| Suspicious Object | `SUSPICIOUS_OBJECT` | |
| Tab Switching / Fullscreen Exit | `TAB_SWITCHING`, `FULLSCREEN_EXIT` | |
| Audio Event | `AUDIO_EVENT` | |
| Combined Behavior Analysis | `COMBINED_BEHAVIOR`, `.contributingSignals` | JSON list of every detection/event id folded into the incident. |

---

## 13. AI Event Engine

This section names the mechanics that §10–12 already use — there's no separate "engine" table,
only the shape of `AiDetection` itself.

| Task | Modeled by | Note |
|---|---|---|
| Detection Event | `AiDetection` | Concrete parent row; detail tables key back to it by id. |
| Event Classification | `.detectionType` (FACE / OBJECT / BEHAVIOR / AUDIO) | A stored value, not a JPA discriminator — reading the detail is an explicit second query. |
| Confidence Score | `.confidence` | |
| Event Severity | `ProctoringEvent.severity` (`EventSeverity`) | Detections carry confidence; events carry severity — `RiskFactorConfig` converts one into the other. |
| Event Timestamp / Duration | `.capturedAt` / `.offsetMs`, `windowStartAt`/`EndAt` | |
| Event Metadata | `.rawOutput` (JSON) | Verbatim model output, kept for defending a decision on appeal. |

---

## 14. Evidence Management

| Task | Modeled by | Note |
|---|---|---|
| Screenshot / Video / Audio Evidence | `EvidenceFile.kind` (`EvidenceKind`) | WEBCAM_SNAPSHOT/CLIP, SCREEN_SNAPSHOT/CLIP, AUDIO_CLIP, ID_DOCUMENT + more. |
| Detection Metadata | `AiDetection.evidenceFileId` | The frame or clip an inference ran over, when retained. |
| Timestamp | `.capturedAt` / `offsetMs` / `durationMs` | |
| Evidence Hash | `.checksumSha256` | Evidence that can't be shown unaltered isn't evidence. |
| Evidence Storage | `.storagePath`, `.uploadStatus`, `.retentionUntil` | Only the pointer lives in the database; bytes go to object storage. |
| Evidence Access Control | `EvidenceAccessLog` | Every read logged, not just every change — footage of someone's home is the most sensitive data the system holds. |

---

## 15. Risk Engine

| Task | Modeled by | Note |
|---|---|---|
| Event Weight / Confidence Weight | `RiskFactorConfig.baseWeight` / `confidenceMultiplier` | |
| Frequency | `.frequencyIncrement`, `.graceOccurrences` | Extra points per repeat; a tolerated number before scoring even starts. |
| Duration | `.durationWeightPerSecond`, `.maxContribution` | Ceiling so one noisy signal can't dominate the total. |
| Risk Calculation / Accumulation | `RiskAssessment.riskScore` + `RiskEvent` | One row per contributing signal — the working behind the number, so "72, of which 30 came from a phone visible 40s" is reconstructable. |
| Risk Decay | `RiskFactorConfig.decayHalfLifeSeconds` | A glance twenty minutes ago weighs less than one now. |

### Threshold & Suspicion Level

**LOW** · **MEDIUM** · **HIGH** · **CRITICAL**

> **Gap closed in this pass.** The band boundaries lived nowhere: `RiskAssessment` stored a score
> *and* a level, `RiskFactorConfig` stored the weights producing the score, but the mapping
> between them was implicit in application code — exactly the part most likely to be tuned by a
> non-developer and most likely to be challenged in an appeal.
>
> New: `RiskLevelThreshold` — per band, a score range, a `RiskRecommendation`, an `AutoAction`, and
> flags for opensReviewCase / withholdsResult / alertsProctor. Versioned by `configVersion` to
> match `RiskAssessment.thresholdVersion`, so the bands in force at decision time are always
> reconstructable.

---

## 16. Real-Time Monitoring

One row per session, updated in place — deliberately separate from `ProctoringEvent`'s durable
history, so a heartbeat never rewrites the row that events and evidence hold foreign keys into.

| Task | Modeled by | Note |
|---|---|---|
| WebSocket | `LiveSessionStatus.websocketId` | |
| Live Student Status | `.candidateStatus` (`CandidateLiveStatus`), `.currentQuestionNo`, `.answeredCount`, `.remainingSeconds` | `candidateStatus` is ACTIVE / IDLE / SUSPENDED — the candidate's own behavioral state, distinct from the stream/connection health fields below. |
| Live Risk Score | `.riskScore` / `riskLevel` | Mirrored from the latest `RiskAssessment`. |
| Live AI Events | `.openAlertCount`, `.lastEventAt` | |
| Camera / Microphone Status | `.cameraStatus` / `microphoneStatus` (`StreamStatus`) | |
| Network / Connection Status | `.networkLatencyMs`, `.connectionStatus` | |

---

## 17. Review System

The case (what happens to the candidate) and the findings (which signals earned it) are
deliberately separate tables — a case can close while three of its eight flags were genuine.

| Task | Modeled by | Note |
|---|---|---|
| Suspicious Session Queue | `ReviewCase.status` (`ReviewCaseStatus`), `.priority`, `.dueAt` | |
| Event / Evidence / Risk Review | `ReviewFinding` (itemKind: PROCTORING_EVENT / AI_DETECTION / EVIDENCE_FILE / RISK_FACTOR) | The only honest source of false-positive rates for §23. |
| Reviewer Assignment | `ReviewCase.assignedReviewerUserId` | |
| Review Notes | `ReviewNote.candidateVisible` | Distinguishes internal discussion from anything quoted in an appeal. |

### Review Decision

| Task | Modeled by | Note |
|---|---|---|
| Valid / False Positive / Needs Investigation | `ReviewFinding.verdict` (`ItemVerdict`) | Per flagged item, not per case. |
| Final Decision | `ReviewDecision` (append-only), `ReviewCase.finalOutcome` | An overturned decision is never edited — the superseding row points back at it, which is what makes an appeal auditable. |

---

## 18. Exam Result

| Task | Modeled by | Note |
|---|---|---|
| Score / Correct / Wrong / Unanswered | `ExamResult.rawScore` / `finalScore`, `.correctCount` / `incorrectCount` / `unansweredCount` | `ResultDetail` freezes the per-question breakdown, so a later re-weighting of the exam can't rewrite a published result. |
| Time Used | `.timeSpentSeconds` | |
| Risk Score | `.riskScore` / `riskLevel` | Copied from the assessment at finalisation — frozen even if the attempt is later re-scored. |
| Proctoring Status | `.integrityStatus` (`IntegrityStatus`) | CLEAN, FLAGGED, UNDER_REVIEW, INVALIDATED. |
| Final Result | `.status` (`ResultStatus`), `.passed`, `.publishedAt` | PROVISIONAL → PENDING_REVIEW → FINAL, or WITHHELD / VOID. |

---

## 19. Proctoring Report

A rendered, immutable dossier — not a live query. It leaves the system (handed to a board,
attached to an appeal), so it's versioned and checksummed rather than recomputed on read.

| Task | Modeled by | Note |
|---|---|---|
| Student / Exam / Session Information | `ProctoringReport.attemptId` / `sessionId` | References, not a copy of every field. |
| AI Events / Evidence / Risk Score | `.summarySnapshot` (JSON), `.riskScore` | Frozen figures as rendered — later data drift can't alter a report already issued. |
| Reviewer Decision / Final Status | `.reviewCaseId`, `.finalIntegrityStatus` | |

`.checksumSha256` lets a copy in circulation be proven genuine; a recompute produces version 2,
never an edit to version 1.

---

## 20. Admin Dashboard

**No entity.** Every tile here is a query over tables defined elsewhere in this document. Adding
a table for the dashboard itself would only be a cache of a query result.

| Tile | Query |
|---|---|
| Total / Active Exams | `count(exams)` / `count(status=ACTIVE)` |
| Students | `count(user_role, role=STUDENT)` |
| High Risk Sessions | `risk_assessments where level in (HIGH, CRITICAL)` |
| Pending Reviews | `review_cases where status = OPEN` |
| Live Monitoring | `live_session_status` |
| Risk Analytics | `risk_events`, `model_performance_metrics` |

---

## 21. Notification

| Task | Modeled by | Note |
|---|---|---|
| Email | `NotificationTemplate` | Per type + channel + locale, editable in the database — wording gets fixed without a release. |
| Exam Invitation / Reminder | `NotificationType.EXAM_INVITATION` / `EXAM_REMINDER` | |
| Exam Started / Submitted | `EXAM_STARTED`, `EXAM_SUBMITTED` | |
| Review Completed | `REVIEW_COMPLETED` | |
| Result Published | `RESULT_PUBLISHED` | |

Each send is a `Notification` row with the rendered body stored, not re-derived — the template it
came from will be edited, and "what were they actually told, and when" is a question that gets
asked.

---

## 22. Audit & Security

| Task | Modeled by | Note |
|---|---|---|
| Audit Log | `AuditLog` | Generic entityType/entityId + before/after JSON — a per-table history would miss "what did this admin touch last Tuesday". |
| Login History | `LoginAttempt` | |
| Session History | `UserSession` | |
| Evidence Access Log | `EvidenceAccessLog` | Also listed under §14 — it's both an evidence control and a security control. |
| Admin Activity | `AuditLog` where action in CONFIG_CHANGE, PERMISSION_CHANGE, SCORE_OVERRIDE | |
| Data Encryption | *— out of scope —* | A deployment/infrastructure concern (TDE, column encryption at rest), not a schema-level entity. |
| RBAC | `Role`, `Permission`, `RolePermission`, `UserRole` | See §1. |
| API Security | `ApiClient` | Machine credentials for the AI inference workers — a caller that can write detections about a candidate needs its own scoped credential, not a human login. |

---

## 23. AI Model Management

| Task | Modeled by | Note |
|---|---|---|
| Model Registry | `AiModel` (`ModelPurpose`) | Detections name a producer by key, not a free-text string. |
| Model Version | `AiModelVersion.status` (`ModelVersionStatus`) | SHADOW lets a candidate version score alongside the active one without affecting outcomes. |
| Model Configuration | `.configuration` (JSON) | Frame rate, window length, NMS, per-class overrides. |
| Confidence / Detection Threshold | `.confidenceThreshold` / `detectionThreshold` | Live on the version, not the model — raising a floor changes what counts as evidence, and every detection scored under the old floor must stay interpretable. |
| Model Performance | `ModelPerformanceMetric` (`MetricType`) | truePositive/falsePositive/falseNegative counts sourced from §17's `ReviewFinding` adjudications — the only real ground truth. |

---

## 24. System Configuration

| Task | Modeled by | Note |
|---|---|---|
| Proctoring / Exam Configuration | `SystemSetting.category` (`SettingCategory`) | Typed key-value, editable without a deploy. |
| Risk Configuration | `RiskFactorConfig` + `RiskLevelThreshold` | Structured rows, not key-value — the fields have a fixed, meaningful shape. |
| Storage / Notification / AI Configuration | `SettingCategory.STORAGE` / `NOTIFICATION` / `AI` | |

Anything an exam depends on per-sitting is **not** here — it's in `Exam`.ProctoringPolicy (§3) —
so changing a global default can never alter the rules of an exam already in progress.

---

*Reference model for the AI Online Exam Proctoring System — entities only, no service layer.*
*See also: [`db/schema-reference.sql`](../legacy-monolith/src/main/resources/db/schema-reference.sql) (generated
DDL), [`db/schema-foreign-keys.sql`](../legacy-monolith/src/main/resources/db/schema-foreign-keys.sql) (114
constraints, applied separately), [`entity-relationships.md`](entity-relationships.md) (full
relationship index).*
