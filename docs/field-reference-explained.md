# Field Reference Explained

This document explains what each field in the database schema means, why it exists, and how it's
used, organized by module in plain language. It's a companion to
[`field-reference.md`](field-reference.md) (the mechanical, generated-from-source table of every
field) — this one is prose, written for a human reading it top to bottom rather than looking up
one entity.

---

## The Three Universal Fields

Every table has these three fields, which is why they're not repeated:

| Field | Column | Type | Required | Meaning |
|---|---|---|---|---|
| `id` | `id` | `Long` | Yes | Surrogate key — a `bigserial`-style auto-incrementing identity. This is the internal database ID. |
| `createdAt` | `created_at` | `Instant` | Yes | Set once on insert, never changed. Tells you when the row was created. |
| `updatedAt` | `updated_at` | `Instant` | No | Bumped by Hibernate on every update. Tells you when the row was last modified. |

**Why these three?** They give every row a stable identity (`id`) and a basic audit trail
(`createdAt`/`updatedAt`) without repeating the same columns in every table definition.

---

## How to Read the Tables

- **Field** — the Java entity field name (e.g., `publicId`)
- **Column** — the actual database column name (e.g., `public_id`)
- **Type** — the Java type (e.g., `Long`, `String`, `Instant`, `BigDecimal`)
- **Required** — whether the column is `NOT NULL`
- **Note** — what the field is for and how to use it

---

## Authentication & Access

### `User` — `users`

Every human on the platform: candidates, proctors, reviewers, administrators.

| Field | Meaning |
|---|---|
| `publicId` | A UUID exposed to the outside world instead of the internal `id`. Prevents ID enumeration. |
| `email` | Unique login identifier. |
| `passwordHash` | Hashed password — never the plaintext. |
| `fullName` | Display name. |
| `externalRef` | Student/employee number from an external system (e.g., the university's SIS). Lets you match records across systems. |
| `status` | Account lifecycle: `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DISABLED`. |
| `emailVerifiedAt` | When the email was confirmed. Null means unverified. |
| `enrolmentPhotoPath` | Reference photo for face matching. If null, the candidate can't sit an exam requiring identity checks. |
| `voiceprintPath` | Reference voiceprint for second-voice detection. |
| `phoneNumber` | Used for SMS MFA or contact. |
| `timeZone` / `locale` | User preferences for formatting and quiet hours. |
| `lastLoginAt` | Last successful login. |
| `failedLoginCount` | Consecutive failures — used for lockout logic. |
| `lockedUntil` | Account lockout expiry. |
| `mfaEnabled` / `mfaMethod` / `mfaSecretEncrypted` | MFA configuration. `mfaSecretEncrypted` is the encrypted TOTP seed (only for TOTP; SMS/EMAIL don't need a stored secret). |
| `mfaEnrolledAt` | When MFA was set up. |

**Why no role column?** Roles are many-to-many via `UserRole`, so one account can be both a
candidate and a reviewer without duplication.

---

### `Role` / `Permission` / `RolePermission` / `UserRole`

**RBAC (Role-Based Access Control) chain:** User → UserRole → Role → RolePermission → Permission

| Table | Meaning |
|---|---|
| `Role` | Named permission bundle (STUDENT, TEACHER, REVIEWER, ADMIN). A table, not an enum, so operators can add roles without a redeploy. |
| `Permission` | A single authority named `resource:action` (e.g., `exam:publish`, `evidence:download`). |
| `RolePermission` | Grants one permission to one role. Has its own audit trail (`grantedByUserId`, `grantedAt`). |
| `UserRole` | Grants one role to one user. Carries who granted it, when, and when it expires — important for temporary REVIEWER roles. |

**Why entities instead of bare join tables?** The grant itself matters — who gave it, when, and
why — especially for sensitive roles.

---

### `UserSession` — `user_sessions`

Authenticated login session (the JWT refresh side). Distinct from `ProctoringSession`: this is
"who is signed in," not "who is being watched."

| Field | Meaning |
|---|---|
| `refreshTokenHash` | Hash of the refresh token — never the token itself. A stolen table dump must not be replayable. |
| `ipAddress` / `userAgent` / `deviceFingerprint` | Where and how the session was established. |
| `issuedAt` / `expiresAt` | Session lifetime. |
| `lastSeenAt` | Last activity — for detecting stale sessions. |
| `revokedAt` / `revokedReason` | Manual revocation (e.g., admin kick, password change). |

---

### `SecurityToken` — `security_tokens`

One-time tokens for email verification, password reset, invitations, email change. A single
table discriminated by `purpose` because every such token has the same lifecycle: issued →
expires → used once → dead.

| Field | Meaning |
|---|---|
| `purpose` | `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_INVITATION`, `EMAIL_CHANGE`. |
| `tokenHash` | Hash of the token. |
| `issuedAt` / `expiresAt` | Validity window. |
| `usedAt` / `invalidatedAt` | When it was consumed or explicitly killed. |
| `requestedIp` / `redeemedIp` | Where the token was requested vs. redeemed — a mismatch is worth alerting on. |

---

### `LoginAttempt` — `login_attempts`

Login history, successes and failures. Separate from `UserSession` because a failed attempt
creates no session; separate from `AuditLog` because it's written on an unauthenticated path.

| Field | Meaning |
|---|---|
| `userId` | Null when the email matched no account. |
| `email` | The address tried. |
| `outcome` | `SUCCESS`, `BAD_CREDENTIALS`, `ACCOUNT_LOCKED`, etc. |
| `attemptedAt` | When. |
| `ipAddress` / `userAgent` / `deviceFingerprint` / `geoCountry` | Where from. |
| `failureDetail` | Extra detail for debugging. |

---

### `ApiClient` — `api_clients`

A non-human caller: AI inference workers, evidence uploaders, SIS integrations.

| Field | Meaning |
|---|---|
| `clientId` / `clientSecretHash` | Credentials. |
| `status` | `ACTIVE`, `SUSPENDED`, `REVOKED`, `EXPIRED`. |
| `allowedScopes` | Space-separated permission codes. |
| `allowedIpRanges` | CIDR allow-list. |
| `rateLimitPerMinute` | Throttling. |
| `createdByUserId` | Who created it. |
| `secretRotatedAt` / `expiresAt` / `lastUsedAt` / `lastUsedIp` | Lifecycle tracking. |
| `revokedAt` / `revokedReason` | Revocation record. |

---

## Payment

### `PaymentCustomer` — `payment_customers`

A candidate's billing profile with one payment processor. Most processors (Stripe included) require
a Customer object to exist before a card can be tokenized against it — this table is that object.

| Field | Meaning |
|---|---|
| `userId` | Whose billing profile this is. |
| `defaultPaymentCardId` | The card this customer's charges default to, when none is specified. |
| `provider` | `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL` — which processor this profile lives on. |
| `providerCustomerId` | The processor's own customer id. |
| `billingEmail` / `billingName` / `billingPhone` | Contact details sent with charges. |
| `billingAddressLine1` / `billingAddressLine2` / `billingCity` / `billingState` / `billingPostalCode` / `billingCountry` | Billing address, for receipts and AVS. |
| `taxId` | VAT/tax id, when the candidate needs one on their receipt. |
| `preferredCurrency` | ISO 4217 code. Plain text, not an enum — currencies are an open-ended list that shouldn't need a migration to grow. |
| `delinquent` | Set when the processor reports unresolved failed payments on this customer. |
| `metadata` | Free-form JSON for provider-specific extras. |

**Why a separate table from `PaymentCard`?** One user can have a billing profile with more than
one processor (a `PaymentCustomer` per `provider`), and every card that customer adds belongs to
one `PaymentCustomer`. Splitting them mirrors how the processors themselves model it — a Customer
that owns zero or more PaymentMethods — so this table's shape maps directly onto the API calls
`PaymentCustomerService` makes.

### `PaymentCard` — `payment_cards`

A candidate's payment card on file, for exam fees. Stores tokenized metadata only — the actual
card number and CVV never reach this database.

| Field | Meaning |
|---|---|
| `paymentCustomerId` | The billing profile this card belongs to. |
| `userId` | Denormalized from the customer, so listing "this candidate's cards" doesn't need a join — same reasoning as `ExamResult` carrying both `examId` and `candidateUserId`. |
| `provider` | `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL` — which processor tokenized it. |
| `providerPaymentMethodId` | The processor's token for this specific card — never the card number itself. |
| `providerCardToken` | Some processors also issue a separate raw card token distinct from the payment method id. |
| `brand` | `VISA`, `MASTERCARD`, `AMEX`, `DISCOVER`, `JCB`, `DINERS_CLUB`, `UNIONPAY`, `MAESTRO`, `ELO`, `OTHER`. |
| `funding` | `CREDIT`, `DEBIT`, `PREPAID`, `CHARGE`, `UNKNOWN` — how the card draws funds. |
| `status` | `ACTIVE`, `EXPIRED`, `PENDING_VERIFICATION`, `SUSPENDED`, `REVOKED`. |
| `last4` | Last four digits — display only. |
| `bin` | Bank identification number, the first 6-8 digits — routing metadata, not sensitive like a full PAN. |
| `expiryMonth` / `expiryYear` | Card expiry. |
| `cardholderName` | Name on the card. |
| `issuer` / `issuerCountry` | The bank that issued the card, and where. |
| `billingCountry` / `billingPostalCode` | For address verification (AVS). |
| `fingerprint` | A processor-computed hash that's the same across accounts for the same physical card, without exposing the PAN — used for duplicate-card and shared-card detection. |
| `cvvCheck` / `avsLine1Check` / `avsPostalCodeCheck` | `NOT_ATTEMPTED`, `PASSED`, `FAILED`, `UNAVAILABLE`, `UNRECOGNIZED` — fraud-signal results from the processor. |
| `threeDsSupported` / `threeDsEnrolled` | Whether this card can do 3-D Secure, and whether the cardholder is enrolled. |
| `defaultCard` | Whether this is charged by default. |
| `lastUsedAt` | Last time it was charged. |
| `revokedAt` / `revokedReason` | Set on any transition to `REVOKED` — a candidate removing it and an admin invalidating it both land here. The row stays; past charges still reference it. |
| `metadata` | Free-form JSON for provider-specific extras. |

**Why no card number, ever?** The same reasoning already applied to `User.passwordHash` and
`ApiClient.clientSecretHash`: never store the secret, only a reference to it. The actual PAN and
CVV go straight to a PCI-compliant processor, which hands back an opaque token
(`providerPaymentMethodId`) — that's the only thing this table stores that identifies the card
to anyone.

**Why is "at most one default card per customer" not a database constraint?** Hibernate's schema
generation — this project's actual source of DDL — has no annotation for a partial unique index.
`PaymentCardService.setDefault` enforces it instead: unset every other card for the customer,
then set this one, in one service-layer operation.

### `PaymentTransaction` — `payment_transactions`

One attempt by a processor to move money: an authorization, a capture, a refund, a payout, and so
on. Every real charge, refund, or payout against a card produces one of these rows.

| Field | Meaning |
|---|---|
| `paymentCustomerId` / `paymentCardId` | Who and what was charged. |
| `parentTransactionId` | The transaction this one acts on — a `REFUND`'s parent is the `SALE` it refunds, a `CAPTURE`'s parent is its `AUTHORIZATION`. |
| `initiatedByUserId` | Who triggered it, when a human did — null for processor-initiated events like a chargeback. |
| `provider` | `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL`. |
| `providerTransactionId` / `providerIntentId` | The processor's own ids for this transaction and its parent payment intent. |
| `currency` | ISO 4217 code. |
| `amountMinor` / `amountRefundedMinor` | Integer minor units (cents) — never a floating type, to avoid rounding error in money math. |
| `type` | `AUTHORIZATION`, `CAPTURE`, `SALE`, `REFUND`, `VOID`, `CHARGEBACK`, `PAYOUT`. |
| `status` | `INITIATED`, `PENDING`, `AUTHORIZED`, `CAPTURED`, `SETTLED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `DISPUTED`, `CHARGEBACK`. |
| `failureCode` / `failureMessage` | Why it failed, when it did. |
| `description` / `statementDescriptor` | What shows up on the candidate's bank statement. |
| `reference` | Free-text correlation key back to the domain object this charge is for (e.g. an exam attempt). |
| `idempotencyKey` | Prevents the same charge from being submitted to the processor twice — unique when present, unlimited nulls allowed, same shape as `Notification.idempotencyKey`. |
| `threeDsAuthenticated` | Whether 3-D Secure actually completed for this transaction. |
| `capturedAt` | When funds were actually captured (for a two-step authorize-then-capture flow). |
| `metadata` | Free-form JSON for provider-specific extras. |

**Why does a `REFUND` reference its `SALE` through `parentTransactionId` instead of a separate
refunds table?** A refund is still a movement of money with its own provider id, amount, and
status — it doesn't need a different shape, just a pointer back to what it's refunding. The same
self-reference shape already backs `RetakeGrant.supersedesGrantId`.

---

## Student Groups

### `StudentGroup` — `student_groups`

A class, cohort, or ad-hoc group. Self-referencing so a programme can contain classes.

| Field | Meaning |
|---|---|
| `code` / `name` / `description` | Identity. |
| `groupType` | `CLASS`, `COHORT`, `DEPARTMENT`, `PROGRAM`, `CUSTOM`. |
| `parentGroupId` | Self-reference for hierarchy. |
| `ownerUserId` | Teacher/coordinator responsible. |
| `academicTerm` | e.g., "2026-Spring". |
| `active` | Soft delete. |

### `GroupMembership` — `group_memberships`

Membership of one student in one group. `leftAt` rather than deletion so old rosters stay
explicable.

| Field | Meaning |
|---|---|
| `studentGroupId` / `userId` | The pair. |
| `addedByUserId` | Who added them. |
| `joinedAt` / `leftAt` | Membership window. |
| `active` | Soft delete. |

---

## Exam Definition

### `Exam` — `exams`

The exam blueprint, not a sitting. Once PUBLISHED, its structure is frozen — attempts reference
sections and questions through `ExamQuestion`, so edits would rewrite history.

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `code` / `title` / `description` / `instructions` | Identity and content. |
| `createdByUserId` | Author. |
| `status` | `DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `CLOSED`, `ARCHIVED`. |
| `version` | Incremented on structural edits. |
| `durationMinutes` | Wall-clock budget per attempt. Null = only closing time limits. |
| `opensAt` / `closesAt` | Exam window. |
| `maxAttempts` | Default attempts allowed. |
| `totalPoints` / `passingScore` | Scoring. |
| `gradingMode` | `AUTO`, `MANUAL`, `HYBRID`. |
| `shuffleSections` | Randomize section order. |
| `holdResultsForReview` | Withhold scores until integrity review closes. |
| `showResultImmediately` | Release scores on submit. |
| `proctoringPolicy` | Embedded value object — see `ProctoringPolicy`. |
| `excelPolicy` | Embedded value object — see `ExcelPolicy`. |

---

### `ProctoringPolicy` *(embedded value object)*

Supervision rules embedded in `Exam`. No identity or lifecycle apart from the exam.

| Field | Meaning |
|---|---|
| `mode` | `NONE`, `AI_ONLY`, `LIVE_PROCTOR`, `RECORD_AND_REVIEW`, `HYBRID`. |
| `requireWebcam` / `requireScreenShare` / `requireMicrophone` | Media requirements. |
| `requireIdentityCheck` / `requireEnvironmentScan` | Pre-flight requirements. |
| `forceFullscreen` / `monitorScreen` / `detectTabSwitch` | Environment control. |
| `detectFace` / `detectMultipleFaces` / `detectGaze` / `detectObjects` / `detectAudio` | AI detection toggles. |
| `retainAudioTranscript` | Whether to keep speech excerpts. |
| `blockCopyPaste` | Block clipboard. |
| `allowedTabSwitches` | Tolerance before escalation. Null = unlimited. |
| `autoReviewRiskThreshold` | Risk score at which to auto-flag for review. |
| `autoTerminateRiskThreshold` | Risk score at which to auto-terminate. |
| `evidenceRetentionDays` | How long to keep evidence. |

---

### `ExcelPolicy` *(embedded value object)*

How an exam's embedded Excel runtime behaves, for exams with SPREADSHEET questions. Same
reasoning as `ProctoringPolicy`: no identity apart from the exam. Deployment-level concerns
(installed engines, resource quotas, global function/add-in whitelists) live in `SystemSetting`
instead — this only holds what varies exam to exam.

| Field | Meaning |
|---|---|
| `runtimeRequired` | Exam must be taken inside the embedded Excel runtime. |
| `externalAppBlocked` | Flags a candidate who opens the workbook outside this runtime. |
| `macrosAllowed` | Gate for every question's own `Question.excelMacroPolicy` — off here overrides on there. |
| `copyPastePolicy` / `cutDragFillPolicy` | `ALLOW`, `BLOCK`, `LOG` per UI action. |
| `autosaveIntervalSeconds` / `snapshotIntervalMinutes` | Null falls back to the system default. |
| `recalcMode` | `AUTOMATIC`, `MANUAL`, `ITERATIVE`. |
| `iterativeCalcMaxIterations` | Only meaningful when `recalcMode = ITERATIVE`. |
| `precisionAsDisplayed` | Round formula results to what's shown before further calculation. |
| `volatileFunctionsPinned` | Pins NOW()/TODAY()/RAND() to a per-session seed rather than letting them vary on recalc. |

---

### `ExamSection` — `exam_sections`

Ordered part of an exam (e.g., "Section A — Multiple choice").

| Field | Meaning |
|---|---|
| `examId` | Parent exam. |
| `title` / `description` / `instructions` | Content. |
| `sequenceNo` | Order. |
| `timeLimitMinutes` | Per-section time limit. |
| `sectionPoints` | Total points for the section. |
| `shuffleQuestions` | Randomize question order. |
| `questionsToDraw` | How many of the linked questions each candidate receives. Null = all. |
| `lockOnExit` | Once left, can't return. |

---

### `ExamQuestion` — `exam_questions`

Placement of a bank `Question` inside an `ExamSection`. Resolves many-to-many between exams and
questions.

| Field | Meaning |
|---|---|
| `examSectionId` / `questionId` | The pair. |
| `sequenceNo` | Order within the section. |
| `points` | Overrides the question's default weight for this exam. |
| `negativePoints` | Penalty for wrong answers. |
| `required` | Whether it must be answered. |
| `shuffleOptions` | Randomize option order. |

---

### `ExamAssignment` — `exam_assignments`

Entitlement of one candidate to sit one exam.

| Field | Meaning |
|---|---|
| `examId` / `candidateUserId` | The pair. |
| `assignedByUserId` | Who assigned. |
| `status` | `ASSIGNED`, `NOTIFIED`, `STARTED`, `SUBMITTED`, `EXPIRED`, `CANCELLED`. |
| `assignedAt` / `notifiedAt` | Timing. |
| `windowStartAt` / `windowEndAt` / `dueAt` | Candidate-specific window. |
| `attemptsAllowed` | Overrides `Exam.maxAttempts`. |
| `extraTimeMinutes` | Accessibility accommodation. |
| `accessCodeHash` | Hashed one-time code. |
| `cancelledAt` / `cancelReason` | Cancellation record. |

---

### `ExamGroupAssignment` — `exam_group_assignments`

Assignment of an exam to a whole group. Fans out to per-candidate `ExamAssignment` rows — those
remain the source of truth.

| Field | Meaning |
|---|---|
| `examId` / `studentGroupId` | The pair. |
| `assignedByUserId` / `assignedAt` | Who/when. |
| `windowStartAt` / `windowEndAt` / `dueAt` | Window. |
| `autoEnrollNewMembers` | Keep fanning out to new group members. |
| `expandedAt` / `expandedCount` | When/how many assignments were created. |
| `cancelledAt` | Cancellation. |

---

### `ExamInvitation` — `exam_invitations`

One invitation sent for one assignment. Distinct from the assignment because invitations are
resent (bounce, reminder, window change).

| Field | Meaning |
|---|---|
| `examAssignmentId` | Parent assignment. |
| `channel` | `EMAIL`, `SMS`, `IN_APP`. |
| `status` | `PENDING`, `SENT`, `DELIVERED`, `OPENED`, `ACCEPTED`, `EXPIRED`, `CANCELLED`, `FAILED`. |
| `sentTo` | Actual address/number used. |
| `tokenHash` | Secure token. |
| `sequenceNo` | 1 = original, 2+ = reminder. |
| `reminder` | Boolean flag. |
| `sentAt` / `deliveredAt` / `openedAt` / `acceptedAt` / `expiresAt` | Lifecycle timestamps. |
| `failureReason` | Bounce/failure detail. |

---

### `ExamPrerequisite` — `exam_prerequisites`

One eligibility rule gating an exam.

| Field | Meaning |
|---|---|
| `examId` | The exam being gated. |
| `requiredExamId` | Null when the rule is coursework-based. |
| `minScore` | Minimum score required. |
| `courseReference` | External LMS/coursework code. |
| `description` | Shown to a candidate who fails. |
| `active` | Soft delete. |

---

### `ExamPaymentRequirement` — `exam_payment_requirements`

A fee an exam requires before a candidate can start it — same shape as `ExamPrerequisite`: one
row per rule, a flat `active` flag instead of a status enum.

| Field | Meaning |
|---|---|
| `examId` | The exam this fee gates. |
| `amountMinor` | The fee, in integer minor units (cents). |
| `currency` | ISO 4217 code. |
| `description` | Shown to the candidate when asked to pay. |
| `active` | Retire a fee rule without losing the history of what gated past charges. |

---

### `ExamPaymentCharge` — `exam_payment_charges`

One candidate's obligation to pay a specific `ExamPaymentRequirement`, and the record of whether
they have.

| Field | Meaning |
|---|---|
| `examId` / `candidateUserId` | Who owes this fee, and for which exam. |
| `examAttemptId` | Set once the candidate starts an attempt against this charge — at most one charge per attempt. |
| `examAssignmentId` | The assignment this charge is tied to, when relevant. |
| `examPaymentRequirementId` | The fee rule this charge was created from. |
| `paymentTransactionId` | The transaction that settles this charge, once one exists. |
| `currency` / `amountMinor` | Copied from the requirement at charge-creation time — not read live from it, so a later fee change can't reprice a charge the candidate already owes. |
| `status` | Reuses `PaymentTransactionStatus` rather than a parallel enum, since a charge's lifecycle is the same shape a transaction's is. |
| `paidAt` / `refundedAt` | When it was settled or reversed. |

**Why freeze `amountMinor`/`currency` instead of joining to `ExamPaymentRequirement` for the
price?** The same reasoning as `RiskAssessment.thresholdVersion`: a past decision — what this
candidate actually owed — must not be reinterpreted just because an admin changed the exam's fee
afterward.

---

### `RetakeGrant` — `retake_grants`

A retake authorised for one candidate, separate from the `ReviewDecision` that judged it
warranted.

| Field | Meaning |
|---|---|
| `examAssignmentId` / `candidateUserId` | Who. |
| `reviewDecisionId` | The decision authorising it. |
| `grantedByUserId` / `grantedAt` | Who/when. |
| `additionalAttempts` | How many extra attempts. |
| `expiresAt` | Expiry. |
| `consumedAt` / `consumedByAttemptId` | When/how it was used. |
| `reason` | Required justification. |
| `supersedesGrantId` | If this grant replaces another. |

---

### `ExamWindowOverride` — `exam_window_overrides`

A documented exception widening one candidate's sitting window.

| Field | Meaning |
|---|---|
| `examAssignmentId` | Scoped to one candidate, never the whole exam. |
| `overriddenWindowStartAt` / `overriddenWindowEndAt` | New window. |
| `reason` / `justificationRef` | Required justification. |
| `grantedByUserId` / `grantedAt` | Who/when. |
| `expiresAt` | Expiry. |
| `supersedesOverrideId` | If this replaces another. |

---

## Question Bank

### `Question` — `questions`

Reusable item in the bank. Never hard-deleted once used — retired instead. Fixing or improving
a question that's already been sat can't edit the row in place for the same reason, so a new
version is a new row that inherits from the one it replaces (see `parentQuestionId` below)
rather than being authored from scratch.

| Field | Meaning |
|---|---|
| `code` | Optional short code. |
| `questionType` | `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_ANSWER`, `ESSAY`, `NUMERIC`, `MATCHING`, `ORDERING`, `FILL_IN_BLANK`, `CODE`, `SPREADSHEET`. |
| `stem` | The prompt. |
| `explanation` | Shown after grading. |
| `answerKey` | Expected answer for auto-graded types. |
| `numericTolerance` | Tolerance for numeric answers. |
| `mediaPath` | Image/audio attachment. |
| `difficulty` | `EASY`, `MEDIUM`, `HARD`, `EXPERT`. |
| `status` | `DRAFT`, `ACTIVE`, `RETIRED`. |
| `defaultPoints` | Default weight. |
| `topic` | Free-text topic. |
| `questionCategoryId` | Subject placement in the taxonomy. |
| `expectedSeconds` | Expected time to answer. |
| `createdByUserId` | Author. |
| `version` | Question version. |
| `parentQuestionId` | The question this one was created as a new version of. Null for an original with no predecessor. |
| `programmingLanguage` | For CODE questions. |
| `starterCode` / `executionTimeLimitMs` / `executionMemoryLimitMb` | For CODE questions. |
| `workbookFileType` / `workbookStoragePath` / `workbookChecksumSha256` / `workbookSizeBytes` / `workbookSheetCount` | For SPREADSHEET questions — the authored template. |
| `workbookHasMacros` / `workbookHasExternalLinks` | For SPREADSHEET questions — what the uploaded file actually contains. |
| `excelMacroPolicy` | For SPREADSHEET questions. `OFF`, `SANDBOXED`, `ALLOWED_WHITELIST` — only enforceable when the exam's `ExcelPolicy.macrosAllowed` is also true. |

**Why a self-reference instead of just editing the row?** Once a question has been placed into
an exam and sat, editing it in place would silently change what every past `AttemptAnswer` and
`ResultDetail` says the candidate saw — the same reason questions are retired, not deleted. To
fix or improve a question, insert a **new** row with `parentQuestionId` pointing at the old one
and `version = parent.version + 1`, then place the new row into future exams and call
`retireQuestion` on the old one. Every existing `ExamQuestion` keeps pointing at the exact row it
always did; nothing needs to be re-authored from scratch to pick up the fix, since the new row
can be created by copying the parent's fields and applying only the deltas.

This is also how a SPREADSHEET question's workbook gets corrected — re-uploading a fixed template
for a question that's already been sat is the same act as fixing any other question, so it goes
through `createNextVersion` too, rather than a separate workbook-version table.

---

### `QuestionOption` — `question_options`

One selectable choice. Hangs off `Question`, not `ExamQuestion`.

| Field | Meaning |
|---|---|
| `questionId` | Parent question. |
| `label` | Display label ("A", "B", "1"). |
| `content` | Option text. |
| `mediaPath` | Optional media. |
| `correct` | Whether it's a correct choice. |
| `sequenceNo` | Order. |
| `optionWeight` | Partial credit weight. |
| `feedback` | Shown when this option was picked. |
| `matchKey` | Right-hand value for MATCHING questions. |

---

### `QuestionCategory` — `question_categories`

Subject taxonomy tree.

| Field | Meaning |
|---|---|
| `code` / `name` / `description` | Identity. |
| `parentCategoryId` | Self-reference. |
| `path` | Materialized path ("/MATH/ALGEBRA/") for fast subtree queries. |
| `depth` | Tree depth. |
| `active` | Soft delete. |

---

### `QuestionTag` / `QuestionTagLink`

Free-form labels on questions. A table, not a comma-separated column, because authors filter by
tag and typos are worse than no tag.

| Field | Meaning |
|---|---|
| `QuestionTag.name` / `description` | Identity. |
| `QuestionTag.usageCount` | How many questions use it. |
| `QuestionTagLink.questionId` / `questionTagId` | The many-to-many pair. |

---

### `CodeTestCase` — `code_test_cases`

One input/expected-output pair for a CODE question. Hangs off `Question`.

| Field | Meaning |
|---|---|
| `questionId` | Parent question. |
| `sequenceNo` | Order. |
| `inputData` | Input. |
| `expectedOutput` | Expected output. |
| `visibility` | `SAMPLE` (shown) or `HIDDEN` (held back). |
| `points` | Weight; null = equal weighting. |

---

### `ExcelCellBinding` — `excel_cell_bindings`

One graded cell or range within a SPREADSHEET question's workbook. Hangs off `Question`, same
reasoning as `CodeTestCase`. Locked/protected cells that aren't graded aren't modelled here — cell
protection is a property of the workbook file itself, not a rubric concern.

| Field | Meaning |
|---|---|
| `questionId` | Parent question. |
| `sequenceNo` | Order. |
| `sheetName` / `cellRef` | Which cell. |
| `rangeRef` | Set instead of `cellRef` for a RANGE/chart-source/pivot-source binding. |
| `answerKind` | `VALUE`, `FORMULA`, `RANGE`, `CHART`, `PIVOT_TABLE`, `CONDITIONAL_FORMATTING`, `NAMED_RANGE`, `MACRO_OUTPUT`, `MANUAL`. |
| `label` | Shown to the candidate, e.g. "Q1: Total Revenue". |
| `expectedValue` / `expectedFormula` | What's being checked against. |
| `tolerance` | For a numeric VALUE comparison. |
| `points` | Weight; null = equal weighting across the question's bindings. |

---

### `QuestionCalibration` — `question_calibrations`

Classic test theory statistics for one question over one window.

| Field | Meaning |
|---|---|
| `questionId` | The question. |
| `windowStartAt` / `windowEndAt` | Measurement window. |
| `responseCount` / `correctCount` | Sample size. |
| `difficultyIndex` | Proportion correct (1.0 = giveaway, 0.0 = mis-keyed). |
| `discriminationIndex` | Point-biserial correlation with total score. |
| `averageTimeSeconds` | Average time spent. |
| `flaggedReason` | `TOO_EASY`, `TOO_HARD`, `NEGATIVE_DISCRIMINATION`, `MIS_KEY_SUSPECTED`, `NONE`. |
| `recommendedAction` | `KEEP`, `REVIEW`, `RETIRE`, `REWEIGHT`. |
| `computedAt` | When computed. |

---

## Pre-Exam System Check

### `SystemCheck` — `system_checks`

One pre-flight run: the candidate's device tested against the exam's requirements.

| Field | Meaning |
|---|---|
| `candidateUserId` / `examId` | Who/what. |
| `examAttemptId` | Set once the candidate proceeds. |
| `status` | `IN_PROGRESS`, `PASSED`, `PASSED_WITH_WARNINGS`, `FAILED`, `EXPIRED`. |
| `attemptNo` | Which attempt this cleared. |
| `startedAt` / `completedAt` / `validUntil` | Timing. |
| `deviceFingerprint` / `userAgent` / `ipAddress` | Environment. |
| `downloadMbps` / `uploadMbps` / `latencyMs` | Network measurements. |
| `failedCheckCount` | How many items failed. |
| `overridden` / `overriddenByUserId` / `overrideReason` | Manual override record. |

---

### `SystemCheckItem` — `system_check_items`

Result of one item within a pre-flight run.

| Field | Meaning |
|---|---|
| `systemCheckId` | Parent run. |
| `checkType` | `BROWSER_COMPATIBILITY`, `CAMERA`, `MICROPHONE`, `SPEAKER`, `NETWORK_BANDWIDTH`, `SCREEN_SHARE_PERMISSION`, `FULLSCREEN`, `ENVIRONMENT_SCAN`, `OS_COMPATIBILITY`, `SECOND_SCREEN`. |
| `result` | `NOT_RUN`, `PASSED`, `WARNING`, `FAILED`, `SKIPPED`. |
| `required` | Whether it's blocking. |
| `checkedAt` | When. |
| `message` | Human-readable outcome. |
| `measurement` | Measured detail (device labels, resolution, etc.). |
| `retryCount` | Retries. |

---

## Identity Verification

### `IdentityVerification` — `identity_verifications`

A single identity check. Many rows per attempt — a failed match followed by a manual override is
two checks.

| Field | Meaning |
|---|---|
| `candidateUserId` / `examAttemptId` | Who/which attempt. |
| `proctoringSessionId` | Associated session. |
| `method` | `FACE_MATCH`, `ID_DOCUMENT`, `MANUAL_PROCTOR`, `KNOWLEDGE_CHALLENGE`, `SECOND_FACTOR`. |
| `status` | `PENDING`, `IN_PROGRESS`, `PASSED`, `FAILED`, `MANUAL_OVERRIDE`, `EXPIRED`. |
| `sequenceNo` | Which check in the sequence. |
| `matchScore` / `matchThreshold` | Similarity and threshold at the time. |
| `livenessScore` | Anti-spoofing score. |
| `referencePhotoPath` | Enrolment photo compared against. |
| `capturedEvidenceId` | Live capture used. |
| `documentType` / `documentLast4` | ID document details (last 4 only). |
| `verifiedAt` / `verifiedByUserId` | Who/when. |
| `failureReason` / `overrideReason` | Why it failed or was overridden. |

---

## Consent & Privacy

### `PrivacyNotice` — `privacy_notices`

One version of the privacy/consent text. Effective-dated, never edited in place.

| Field | Meaning |
|---|---|
| `noticeCode` / `version` / `locale` | Identity. |
| `bodyMarkdown` | Content. |
| `effectiveFrom` / `effectiveTo` | Validity window. |
| `active` | Soft delete. |

### `ConsentRecord` — `consent_records`

One candidate's acceptance of one notice version for one attempt.

| Field | Meaning |
|---|---|
| `examAttemptId` | The attempt. |
| `privacyNoticeId` / `noticeVersion` | The notice accepted. |
| `consentedAt` | When. |
| `ipAddress` / `userAgent` | Where from. |
| `withdrawnAt` | If withdrawn. |

---

## Exam Session & Attempts

### `ExamAttempt` — `exam_attempts`

One sitting of one exam by one candidate — the spine of the model.

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `examId` / `candidateUserId` | Who/what. |
| `examAssignmentId` | Null for open exams. |
| `attemptNo` | 1, 2, 3... |
| `status` | `NOT_STARTED`, `IN_PROGRESS`, `PAUSED`, `SUBMITTED`, `AUTO_SUBMITTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `GRADED`. |
| `startedAt` / `expiresAt` / `submittedAt` | Timing. |
| `lastActivityAt` | Last candidate action. |
| `sessionTokenHash` | Attempt-scoped session token. |
| `lastHeartbeatAt` | Last agent ping. |
| `timeSpentSeconds` / `pausedSeconds` | Time accounting. |
| `currentSectionId` | Where the candidate is. |
| `examVersion` | Snapshot of exam version delivered. |
| `ipAddress` / `userAgent` | Environment. |
| `invalidatedAt` / `invalidationReason` | Invalidation record. |

---

### `AttemptAnswer` — `attempt_answers`

The candidate's response to one placed question. One row per (attempt, exam_question).

| Field | Meaning |
|---|---|
| `examAttemptId` / `examQuestionId` | The pair. |
| `responseText` | Free-text response. |
| `responseNumeric` | Numeric response. |
| `attachmentPath` | File attachment. |
| `answeredAt` | When. |
| `revisionCount` | How many times edited. |
| `timeSpentSeconds` | Time on this question. |
| `flaggedByCandidate` | "Come back to this" marker. |
| `gradingStatus` | `NOT_REQUIRED`, `PENDING`, `IN_PROGRESS`, `GRADED`, `REGRADED`. |
| `correct` | Null until graded. |
| `pointsAwarded` | Score. |
| `gradedByUserId` / `gradedAt` / `graderComment` | Grading record. |

---

### `AttemptAnswerOption` — `attempt_answer_options`

One option the candidate selected. Resolves many-to-many between answers and options.

| Field | Meaning |
|---|---|
| `attemptAnswerId` / `questionOptionId` | The pair. |
| `sequenceNo` | Position for ordering/matching questions. |

---

### `AnswerRevision` — `answer_revisions`

Append-only history of one answer.

| Field | Meaning |
|---|---|
| `attemptAnswerId` | Parent answer. |
| `revisionNo` | 1, 2, 3... |
| `savedAt` | When saved. |
| `autoSaved` | Timer vs. manual save. |
| `responseText` / `responseNumeric` / `selectedOptionIds` | Snapshot at this revision. |
| `clientTimestamp` | Client-side time. |
| `ipAddress` | Where from. |

---

### `QuestionState` — `question_states`

Delivery and navigation state of one question within one attempt.

| Field | Meaning |
|---|---|
| `examAttemptId` / `examQuestionId` | The pair. |
| `state` | `UNSEEN`, `VIEWED`, `ANSWERED`, `FLAGGED`, `SKIPPED`, `LOCKED`. |
| `displayOrder` | Position in the shuffled paper. |
| `optionOrder` | Order options were rendered in. |
| `firstViewedAt` / `lastViewedAt` | Viewing window. |
| `viewCount` | How many times viewed. |
| `timeOnQuestionSeconds` | Time spent. |
| `lockedAt` | When locked. |

---

### `AttemptPauseRequest` — `attempt_pause_requests`

A request to pause a live attempt, and its approval.

| Field | Meaning |
|---|---|
| `examAttemptId` | The attempt. |
| `requestedAt` / `requestedByUserId` | Who/when. Null = system-initiated. |
| `reason` | Why. |
| `status` | `PENDING`, `APPROVED`, `DENIED`, `AUTO_APPROVED`. |
| `decidedByUserId` / `decidedAt` / `decisionNote` | Decision record. |
| `resumedAt` | When actually resumed. |

---

### `AttemptResumption` — `attempt_resumptions`

A candidate reconnecting after a client drop.

| Field | Meaning |
|---|---|
| `examAttemptId` | The attempt. |
| `reason` | `BROWSER_CRASH`, `NETWORK_DROP`, `DEVICE_REBOOT`, `POWER_LOSS`, `PROCTOR_INITIATED`. |
| `previousSessionTokenHash` / `newSessionTokenHash` | Token rotation. |
| `resumedAt` | When. |
| `timeAwaySeconds` | Gap duration. |
| `autoApproved` | Whether it needed a proctor decision. |
| `approvedByUserId` | Who approved. |
| `riskEventId` | If the gap was scored as a risk factor. |

---

### `CodeExecutionResult` — `code_execution_results`

Outcome of running one CODE answer against one test case.

| Field | Meaning |
|---|---|
| `attemptAnswerId` / `codeTestCaseId` | The pair. |
| `passed` | Whether it passed. |
| `actualOutput` | What the code produced. |
| `runtimeMs` / `memoryKb` | Performance. |
| `errorMessage` | Errors. |
| `executedAt` | When. |

---

### `ExcelGradeResult` — `excel_grade_results`

Outcome of grading one SPREADSHEET answer against one of its question's `ExcelCellBinding`s. Same
reason `CodeExecutionResult` exists for CODE: `AttemptAnswer` is a single-row summary, and a
question with several graded cells needs somewhere for "which ones were correct" to live.

| Field | Meaning |
|---|---|
| `attemptAnswerId` / `excelCellBindingId` | The pair. |
| `graderType` | Same values as `ExcelCellBinding.answerKind` — carried here too since an automated CHART/PIVOT check that came back inconclusive falls back to `MANUAL`, and this records that it did. |
| `correct` | Whether it was correct. |
| `pointsAwarded` | Points given. |
| `gradedValue` / `gradedFormula` | What the candidate's cell actually held at grading time. |
| `graderNote` | Free-text detail. |
| `gradedAt` | When. |

---

### `QuestionFormFingerprint` — `question_form_fingerprints`

A hash of the shuffled paper one attempt received. Two attempts sharing a hash is a collusion
signal.

| Field | Meaning |
|---|---|
| `examAttemptId` / `examId` | The attempt. |
| `formHash` | SHA-256 of the ordered question/option tuple. |
| `questionCount` / `sectionCount` | Paper shape. |
| `computedAt` | When. |
| `collisionCount` / `collisionAttemptIds` | Other attempts with the same form. |

---

### `AnswerTimingAnomaly` — `answer_timing_anomalies`

An answer that was correct in implausibly little time.

| Field | Meaning |
|---|---|
| `examAttemptId` / `attemptAnswerId` / `examQuestionId` | References. |
| `expectedSeconds` / `actualSeconds` / `ratio` | Timing. |
| `answerCorrect` | Only fast *correct* answers are flagged. |
| `revisionCount` | How many edits. |
| `anomalyType` | `TOO_FAST_CORRECT`, `TOO_FAST_HIGH_SCORE`, `ZERO_TIME_CORRECT`, `BURST_SUBMIT`. |
| `flaggedAt` | When flagged. |

---

## Excel Runtime

### `ExcelSession` — `excel_sessions`

One candidate's live Excel runtime session for an attempt — the sandboxed container instance
holding their working copy of the workbook. One per `ExamAttempt`, the same 1:1 shape as
`ProctoringSession`. Snapshots aren't a separate table — they reuse `EvidenceFile` (kind
`EXCEL_WORKBOOK_SNAPSHOT` / `EXCEL_FINAL_WORKBOOK`).

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `examAttemptId` | The attempt. |
| `proctoringSessionId` | Null when the exam's proctoring mode is NONE. |
| `engineUsed` | `LIBREOFFICE`, `ONLYOFFICE`, `SHEETJS_HYPERFORMULA`, `OFFICE_SCRIPTS`. |
| `sandboxContainerId` | The runtime's own container reference. |
| `status` | `PENDING`, `ACTIVE`, `SUBMITTED`, `CRASHED`, `RECOVERED`. |
| `startedAt` / `submittedAt` | Lifecycle timestamps. |
| `finalChecksumSha256` | SHA-256 of the workbook exactly as submitted. |
| `integrityStatus` | `VALID`, `TAMPERED`, `INCONCLUSIVE` — compares `finalChecksumSha256` against the runtime's own replay. |

---

### `ExcelCellEdit` — `excel_cell_edits`

Append-only history of one cell edit — the same role `AnswerRevision` plays for a regular attempt
answer. Recovers a crashed session, and is what a diff/replay view steps through in
`sequenceNo` order.

| Field | Meaning |
|---|---|
| `excelSessionId` | The session. |
| `sequenceNo` | Order. |
| `sheetName` / `cellRef` | Which cell. |
| `oldValue` / `newValue` | Value change. |
| `oldFormula` / `newFormula` | Formula change. |
| `editedAt` | When. |

---

### `ExcelSheetOperation` — `excel_sheet_operations`

A structural change to a sheet — insert, delete, rename, hide, protect — kept separate from
`ExcelCellEdit` since it isn't a value change on any one cell.

| Field | Meaning |
|---|---|
| `excelSessionId` | The session. |
| `operationType` | `INSERT`, `DELETE`, `RENAME`, `HIDE`, `UNHIDE`, `PROTECT`, `UNPROTECT`. |
| `sheetName` | Which sheet. |
| `occurredAt` | When. |
| `detail` | Old name for a RENAME, or other operation-specific detail. |

---

### `ExcelMacroExecution` — `excel_macro_executions`

One macro run inside a session. A blocked-but-attempted run is still logged, not dropped — it's
itself the proctoring signal behind `EXCEL_MACRO_RUN`.

| Field | Meaning |
|---|---|
| `excelSessionId` | The session. |
| `macroName` | Which macro. |
| `allowed` | Whether it was on the whitelist at execution time. |
| `inputSummary` / `outputSummary` | What went in and came out. |
| `durationMs` / `exitCode` | Execution result. |
| `executedAt` | When. |

---

## Proctoring Sessions

### `ProctoringSession` — `proctoring_sessions`

The supervision envelope around one attempt.

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `examAttemptId` | The attempt. |
| `status` | `PENDING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `TERMINATED`, `FAILED`. |
| `mode` | Copied from exam policy at start. |
| `assignedProctorUserId` | For LIVE_PROCTOR/HYBRID. |
| `startedAt` / `endedAt` | Session window. |
| `consentAcceptedAt` | When consent was given. |
| `identityVerified` / `identityVerifiedAt` / `identityVerifiedByUserId` | Identity check. |
| `environmentScanCompletedAt` | Environment scan. |
| `lastHeartbeatAt` | Last agent ping. |
| `heartbeatMissCount` | Missed heartbeats. |
| `eventCount` / `criticalEventCount` / `warningIssuedCount` | Running counters. |
| `terminatedAt` / `terminationReason` | Termination record. |

---

### `ProctoringEvent` — `proctoring_events`

One observation during a session. Append-only and highest-volume.

| Field | Meaning |
|---|---|
| `proctoringSessionId` | Parent session. |
| `deviceSessionId` | Which device raised it. |
| `eventType` | The full `ProctoringEventType` enum (tab switch, heartbeat miss, etc.). |
| `severity` | `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `source` | `BROWSER_AGENT`, `AI_ENGINE`, `HUMAN_PROCTOR`, `SYSTEM`. |
| `occurredAt` / `receivedAt` | When it happened vs. when received. |
| `offsetMs` / `durationMs` | For timeline playback. |
| `description` | Human-readable. |
| `payload` | Type-specific detail (JSON). |
| `autoAction` | `NONE`, `LOG_ONLY`, `WARN_CANDIDATE`, `PAUSE_ATTEMPT`, `LOCK_SCREEN`, `NOTIFY_PROCTOR`, `TERMINATE_ATTEMPT`. |
| `acknowledgedAt` | When dismissed. |
| `idempotencyKey` | Deduplication. |

---

### `DeviceSession` — `device_sessions`

One device connected to a proctoring session.

| Field | Meaning |
|---|---|
| `proctoringSessionId` | Parent session. |
| `deviceFingerprint` | Device identity. |
| `primary` | Main device vs. second camera. |
| `deviceLabel` / `operatingSystem` / `osVersion` / `browser` / `browserVersion` / `userAgent` | Device details. |
| `screenResolution` / `monitorCount` | Display. |
| `cameraLabel` / `microphoneLabel` | Media devices. |
| `ipAddress` / `geoCountry` / `geoCity` / `timezoneOffsetMinutes` | Location. |
| `vpnSuspected` / `virtualMachineSuspected` | Integrity signals. |
| `connectedAt` / `disconnectedAt` | Connection window. |

---

### `EvidenceFile` — `evidence_files`

Metadata for a captured artefact. Only pointer and checksum in DB; bytes in object storage.

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `proctoringSessionId` / `proctoringEventId` | References. |
| `kind` | `WEBCAM_SNAPSHOT`, `WEBCAM_CLIP`, `SCREEN_SNAPSHOT`, `SCREEN_CLIP`, `AUDIO_CLIP`, `ID_DOCUMENT`, `ENVIRONMENT_SCAN`, `KEYSTROKE_LOG`, `EVENT_LOG_BUNDLE`. |
| `storagePath` / `fileName` / `contentType` / `sizeBytes` | Storage details. |
| `checksumSha256` | Tamper check. |
| `capturedAt` / `offsetMs` / `durationMs` | Timing. |
| `widthPx` / `heightPx` | Dimensions. |
| `uploadStatus` | `PENDING`, `UPLOADING`, `UPLOADED`, `FAILED`, `QUARANTINED`, `PURGED`. |
| `uploadedAt` / `retentionUntil` / `purgedAt` | Lifecycle. |

---

### `EvidenceAccessLog` — `evidence_access_logs`

Who looked at which evidence, when, and why. Every read is logged.

| Field | Meaning |
|---|---|
| `evidenceFileId` / `actorUserId` | Who/what. |
| `action` | `VIEW`, `STREAM`, `DOWNLOAD`, `EXPORT`, `SHARE_LINK`, `PURGE`. |
| `accessedAt` | When. |
| `contextReference` | Case/ticket reference. |
| `purpose` | Why. |
| `ipAddress` / `userAgent` | Where from. |
| `granted` / `deniedReason` | Whether allowed. |

---

### `EvidenceCustodyRecord` — `evidence_custody_records`

One step in an evidence file's chain of custody.

| Field | Meaning |
|---|---|
| `evidenceFileId` | The file. |
| `sequenceNo` | Step number. |
| `transition` | `CAPTURED`, `HASHED`, `UPLOADED`, `VERIFIED`, `ACCESSED`, `EXPORTED`, `PURGED`. |
| `actorUserId` / `actorApiClientId` | Who. |
| `occurredAt` | When. |
| `checksumBefore` / `checksumAfter` | Must match for all but PURGED. |
| `storagePathBefore` / `storagePathAfter` | Path changes. |
| `note` | Notes. |

---

### `LiveSessionStatus` — `live_session_status`

Current live state of one session, as the invigilator's wall reads it.

| Field | Meaning |
|---|---|
| `proctoringSessionId` | The session. |
| `candidateStatus` | `ACTIVE`, `IDLE`, `SUSPENDED`. |
| `connectionStatus` | `CONNECTED`, `UNSTABLE`, `RECONNECTING`, `DISCONNECTED`. |
| `cameraStatus` / `microphoneStatus` / `screenStatus` | `UNAVAILABLE`, `ACTIVE`, `DEGRADED`, `INTERRUPTED`, `STOPPED`. |
| `fullscreen` / `windowFocused` | Environment. |
| `riskScore` / `riskLevel` | Mirrored from latest risk assessment. |
| `openAlertCount` | Number of open alerts. |
| `currentQuestionNo` / `answeredCount` / `remainingSeconds` | Progress. |
| `lastHeartbeatAt` / `lastEventAt` / `networkLatencyMs` | Health. |
| `websocketId` / `updatedByNode` | Infrastructure. |

---

### `DeviceTrustRecord` — `device_trust_records`

Running tally of one device fingerprint's sightings within one exam.

| Field | Meaning |
|---|---|
| `deviceFingerprint` / `examId` | The pair. |
| `firstSeenAt` / `lastSeenAt` | Sighting window. |
| `sightingCount` | How many times seen. |
| `distinctCandidateCount` / `distinctCandidateIds` | How many different candidates used it. |
| `trustLevel` | `UNKNOWN`, `KNOWN`, `SHARED_SUSPECTED`, `SHARED_CONFIRMED`, `BLOCKED`. |
| `flaggedAt` / `flaggedReason` | Flag record. |

---

### `ProctorShift` — `proctor_shifts`

One proctor's assignment window over one session.

| Field | Meaning |
|---|---|
| `proctoringSessionId` / `proctorUserId` | The pair. |
| `shiftStartAt` / `shiftEndAt` | Shift window. Null end = currently on duty. |
| `handoverNote` | Note for the incoming proctor. |
| `eventsDuringShift` / `flagsRaisedDuringShift` | Counters. |
| `outcome` | `COMPLETED`, `HANDED_OVER`, `ESCALATED`, `ABANDONED`. |

---

### `ProctorAction` — `proctor_actions`

A live human proctor's action on a session.

| Field | Meaning |
|---|---|
| `proctoringSessionId` / `proctorUserId` / `proctorShiftId` | Who/where. |
| `actionType` | `WARN`, `MESSAGE`, `FLAG`, `PAUSE`, `RESUME`, `TERMINATE`, `ESCALATE`, `NO_ACTION`. |
| `reason` | Required for most actions. |
| `proctoringEventId` | Linked event. |
| `occurredAt` | When. |
| `candidateNotified` | Whether the candidate was told. |
| `supervisorApprovalUserId` / `supervisorApprovedAt` | Two-person approval for TERMINATE. |

---

## AI Detection

### `AiDetection` — `ai_detections`

One inference produced by an AI model over one frame/window.

| Field | Meaning |
|---|---|
| `detectionType` | `FACE`, `OBJECT`, `BEHAVIOR`, `AUDIO` — tells you which detail table to look in. |
| `proctoringSessionId` / `proctoringEventId` / `evidenceFileId` | References. |
| `aiModelVersionId` | Registry entry for the model version. |
| `modelName` / `modelVersion` | Authoritative model identity snapshot. |
| `confidence` | 0.0000–1.0000. |
| `capturedAt` / `processedAt` / `processingTimeMs` / `offsetMs` | Timing. |
| `anomaly` | True when this row needs human attention. |
| `rawOutput` | Verbatim model output for re-scoring/appeals. |

---

### `FaceDetection` — `face_detections`

Face-model findings for one frame.

| Field | Meaning |
|---|---|
| `aiDetectionId` | Parent detection. |
| `faceCount` | 0 = left frame, >1 = extra person. |
| `identityMatchScore` / `identityMatched` | Similarity to enrolment photo. |
| `gazeDirection` / `gazeOffScreenMs` | Where they're looking. |
| `headYaw` / `headPitch` / `headRoll` | Head pose. |
| `eyesClosed` | Eyes. |
| `maskOrOcclusionDetected` | Face cover. |
| `livenessScore` / `spoofSuspected` | Anti-spoofing. |
| `boundingBox` | Embedded value object (x, y, width, height). |

---

### `ObjectDetection` — `object_detections`

Object-model findings for one frame. One row per detected instance.

| Field | Meaning |
|---|---|
| `aiDetectionId` | Parent detection. |
| `objectClass` | `MOBILE_PHONE`, `BOOK`, `PAPER_NOTES`, `SECOND_LAPTOP`, `SECOND_MONITOR`, `EARPHONE`, `SMARTWATCH`, `CAMERA`, `ADDITIONAL_PERSON`, `UNKNOWN_OBJECT`. |
| `objectLabel` | Raw model label. |
| `objectCount` | How many. |
| `prohibited` | Whether it's prohibited. |
| `proximityScore` | How close to hands/face. |
| `persistedFrames` | Consecutive frames visible. |
| `boundingBox` | Embedded value object. |

---

### `BehaviorDetection` — `behavior_detections`

Behavior-model findings over a time window.

| Field | Meaning |
|---|---|
| `aiDetectionId` | Parent detection. |
| `behaviorType` | `LOOKING_AWAY`, `LEAVING_SEAT`, `FACE_OUT_OF_FRAME`, `TALKING`, `BACKGROUND_VOICE`, `SUSPICIOUS_MOVEMENT`, `RAPID_TYPING_BURST`, `PROLONGED_IDLE`, `SCREEN_OCCLUSION`, `REPEATED_TAB_SWITCH`. |
| `windowStartAt` / `windowEndAt` / `durationMs` | Window. |
| `occurrenceCount` | Repeats. |
| `intensityScore` | How pronounced. |
| `baselineDeviation` | Standard deviations from candidate's own baseline. |
| `audioRelated` | Whether audio-related. |

---

### `AudioDetection` — `audio_detections`

Audio-model findings over a listening window.

| Field | Meaning |
|---|---|
| `aiDetectionId` | Parent detection. |
| `audioEventType` | `SPEECH_DETECTED`, `MULTIPLE_VOICES`, `BACKGROUND_NOISE`, `AUDIO_INTERRUPTION`, `PROLONGED_SILENCE`, `SUSPICIOUS_AUDIO`. |
| `windowStartAt` / `windowEndAt` / `durationMs` | Window. |
| `speakerCount` | Distinct voices. |
| `unknownSpeaker` | Voice not matching enrolled voiceprint. |
| `peakDb` / `averageDb` / `signalToNoiseRatio` / `speechRatio` | Audio measurements. |
| `languageCode` | Language. |
| `transcriptExcerpt` | Short excerpt (only if policy allows). |

---

### `SuspiciousActivity` — `suspicious_activities`

A correlated alert — the layer above raw detections.

| Field | Meaning |
|---|---|
| `proctoringSessionId` / `examAttemptId` | References. |
| `activityType` | `REPEATED_LOOKING_AWAY`, `MULTIPLE_FACES`, `PHONE_DETECTED`, `NO_FACE`, `SUSPICIOUS_OBJECT`, `TAB_SWITCHING`, `FULLSCREEN_EXIT`, `AUDIO_EVENT`, `IMPERSONATION_SUSPECTED`, `COMBINED_BEHAVIOR`. |
| `severity` | Severity. |
| `confidence` | Combined confidence. |
| `firstSeenAt` / `lastSeenAt` / `durationMs` | Window. |
| `occurrenceCount` / `detectionCount` / `eventCount` | Counts. |
| `primaryDetectionId` | Most indicative detection. |
| `contributingSignals` | Ids of contributing detections/events. |
| `ruleCode` | Correlation rule. |
| `description` | Human-readable. |
| `verdict` | `DETECTED`, `CONFIRMED`, `FALSE_POSITIVE`, `UNDER_REVIEW`, `DISMISSED`. |
| `adjudicatedByUserId` / `adjudicatedAt` / `adjudicationNote` | Human adjudication. |

---

## Risk Engine

### `RiskAssessment` — `risk_assessments`

Aggregated integrity score for one attempt.

| Field | Meaning |
|---|---|
| `examAttemptId` / `proctoringSessionId` | References. |
| `version` / `latest` | Versioning. |
| `riskScore` | 0–100. |
| `riskLevel` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `recommendation` | `ALLOW`, `MONITOR`, `FLAG_FOR_REVIEW`, `INVALIDATE_ATTEMPT`, `REQUIRE_RETAKE`. |
| `scoringModel` / `scoringModelVersion` / `thresholdVersion` | What computed it. |
| `computedAt` / `recomputeReason` | When/why. |
| `identityScore` / `faceAnomalyScore` / `objectAnomalyScore` / `behaviorAnomalyScore` / `environmentScore` | Component scores. |
| `totalEventCount` / `criticalEventCount` | Counts. |
| `autoActionApplied` | Whether auto-action was taken. |

---

### `RiskEvent` — `risk_events`

One line of the risk score's working — which signal contributed, with what weight.

| Field | Meaning |
|---|---|
| `riskAssessmentId` | Parent assessment. |
| `proctoringEventId` / `aiDetectionId` | Source signal. |
| `factorCode` / `factorLabel` | Scoring rule identity. |
| `severity` | Severity. |
| `weight` | Rule weight at scoring time. |
| `occurrenceCount` | Repeats. |
| `contributedPoints` | Points added. |
| `occurredAt` | When. |
| `note` | Notes. |

---

## Review System

### `ReviewCase` — `review_cases`

A human investigation into one attempt.

| Field | Meaning |
|---|---|
| `caseNumber` | Human-quotable reference (e.g., RC-2026-000481). |
| `examAttemptId` / `riskAssessmentId` | References. |
| `status` | `OPEN`, `ASSIGNED`, `IN_REVIEW`, `PENDING_INFO`, `ESCALATED`, `RESOLVED`, `CLOSED`. |
| `priority` | `LOW`, `NORMAL`, `HIGH`, `URGENT`. |
| `openedAt` / `openedByUserId` / `openReason` | Opening. |
| `assignedReviewerUserId` / `assignedAt` / `dueAt` / `slaBreached` | Assignment. |
| `closedAt` / `finalOutcome` | Closing. |
| `summary` | Summary. |
| `candidateNotified` | Whether candidate was told. |

---

### `ReviewDecision` — `review_decisions`

One judgement recorded against a case. Append-only and ordered.

| Field | Meaning |
|---|---|
| `reviewCaseId` / `reviewerUserId` | Who/what. |
| `sequenceNo` | Order. |
| `decisionType` | `CLEAR`, `WARN_CANDIDATE`, `ADJUST_SCORE`, `INVALIDATE_ATTEMPT`, `GRANT_RETAKE`, `ESCALATE`, `REQUEST_MORE_INFO`. |
| `decidedAt` | When. |
| `rationale` | Required justification. |
| `evidenceRefs` | Evidence relied on. |
| `scoreAdjustment` | Signed score change. |
| `finalDecision` | Whether it's the final decision. |
| `supersedesDecisionId` | If it overturns another. |

---

### `ReviewFinding` — `review_findings`

A reviewer's verdict on one flagged item inside a case.

| Field | Meaning |
|---|---|
| `reviewCaseId` | Parent case. |
| `itemKind` | `PROCTORING_EVENT`, `AI_DETECTION`, `SUSPICIOUS_ACTIVITY`, `EVIDENCE_FILE`, `RISK_FACTOR`. |
| `itemId` | The item. |
| `verdict` | `VALID`, `FALSE_POSITIVE`, `NEEDS_INVESTIGATION`, `INCONCLUSIVE`. |
| `reviewerUserId` / `reviewedAt` | Who/when. |
| `comment` | Notes. |
| `weightDisputed` | Whether reviewer disagrees with weighting. |

---

### `ReviewNote` — `review_notes`

A comment on a case.

| Field | Meaning |
|---|---|
| `reviewCaseId` / `authorUserId` | Who/what. |
| `body` | Comment text. |
| `candidateVisible` | Whether the candidate can see it. |
| `attachmentPath` | Attachment. |
| `editedAt` | When edited. |

---

## Exam Results

### `ExamResult` — `exam_results`

The published outcome of an attempt. One row per attempt.

| Field | Meaning |
|---|---|
| `examAttemptId` / `examId` / `candidateUserId` | References. |
| `rawScore` / `scoreAdjustment` / `finalScore` / `maxScore` / `percentage` / `grade` | Scoring. |
| `passed` | Null while provisional. |
| `status` | `PROVISIONAL`, `PENDING_REVIEW`, `WITHHELD`, `FINAL`, `VOID`. |
| `integrityStatus` | `CLEAN`, `FLAGGED`, `UNDER_REVIEW`, `INVALIDATED`. |
| `riskScore` / `riskLevel` | Risk at finalisation (frozen). |
| `reviewCaseId` | Gating review. |
| `gradingMode` | How graded. |
| `gradedAt` / `gradedByUserId` / `publishedAt` / `releasedToCandidate` | Lifecycle. |
| `correctCount` / `incorrectCount` / `unansweredCount` / `pendingManualCount` | Counts. |
| `timeSpentSeconds` | Time. |
| `certificateSerial` | Certificate. |

---

### `ResultDetail` — `result_details`

Per-question line of a result.

| Field | Meaning |
|---|---|
| `examResultId` / `examQuestionId` / `examSectionId` / `attemptAnswerId` | References. |
| `sequenceNo` | Order. |
| `pointsAwarded` / `pointsPossible` / `correct` / `answered` | Scoring. |
| `timeSpentSeconds` | Time. |
| `scoringNote` | Note. |

---

### `ResultWithholding` — `result_withholdings`

Why one result is withheld, for how long, and who released it.

| Field | Meaning |
|---|---|
| `examResultId` | The result. |
| `reason` | `RISK_THRESHOLD`, `OPEN_REVIEW_CASE`, `PENDING_MANUAL_GRADING`, `APPEAL_FILED`, `EXTERNAL_HOLD`. |
| `reviewCaseId` / `riskAssessmentId` / `thresholdVersion` | References. |
| `withheldAt` / `withheldByUserId` | When/by whom. |
| `releasedAt` / `releasedByUserId` / `releaseReason` | Release record. |

---

## Proctoring Reports

### `ProctoringReport` — `proctoring_reports`

A rendered, immutable dossier for one attempt.

| Field | Meaning |
|---|---|
| `publicId` | External UUID. |
| `examAttemptId` / `proctoringSessionId` / `riskAssessmentId` / `reviewCaseId` | References. |
| `version` | Version. |
| `status` | `QUEUED`, `GENERATING`, `AVAILABLE`, `FAILED`, `EXPIRED`. |
| `format` | `PDF`, `HTML`, `JSON`, `ZIP_BUNDLE`. |
| `requestedByUserId` / `requestedAt` / `generatedAt` | Lifecycle. |
| `storagePath` / `sizeBytes` / `checksumSha256` | Storage + integrity seal. |
| `summarySnapshot` | Figures as rendered. |
| `riskScore` / `finalIntegrityStatus` | Risk/integrity at render time. |
| `includesEvidence` | Whether evidence is bundled. |
| `expiresAt` / `failureReason` | Lifecycle. |

---

### `AttemptTimeline` — `attempt_timelines`

A rendered, unified timeline for one attempt.

| Field | Meaning |
|---|---|
| `examAttemptId` | The attempt. |
| `version` | Version. |
| `renderedAt` | When rendered. |
| `entryCount` | Number of entries. |
| `timelineJson` | JSON array of `{offsetMs, kind, refId, label, severity}`. |
| `checksumSha256` | Integrity seal. |
| `generatedByUserId` | Who generated. |

---

## Notifications

### `Notification` — `notifications`

One message to one recipient on one channel — the outbox.

| Field | Meaning |
|---|---|
| `recipientUserId` | Who. |
| `notificationType` | Business trigger. |
| `channel` | `EMAIL`, `SMS`, `IN_APP`, `PUSH`, `WEBHOOK`. |
| `status` | `PENDING`, `QUEUED`, `SENT`, `DELIVERED`, `OPENED`, `FAILED`, `CANCELLED`. |
| `templateId` | Template used. |
| `sentTo` | Address actually used. |
| `subject` / `body` / `variables` | Rendered content. |
| `relatedEntityType` / `relatedEntityId` | What it's about. |
| `scheduledFor` / `sentAt` / `deliveredAt` / `openedAt` | Lifecycle. |
| `retryCount` / `nextRetryAt` / `failureReason` / `providerMessageId` | Delivery. |
| `idempotencyKey` | Deduplication. |

---

### `NotificationTemplate` — `notification_templates`

Editable subject and body for one (type, channel, locale).

| Field | Meaning |
|---|---|
| `notificationType` / `channel` / `locale` | Identity. |
| `subjectTemplate` / `bodyTemplate` | Content. |
| `expectedVariables` | Placeholders. |
| `active` / `version` | Lifecycle. |

---

### `NotificationSuppression` — `notification_suppressions`

A rule that defers, throttles, or blocks notifications.

| Field | Meaning |
|---|---|
| `recipientUserId` / `notificationType` / `channel` | Scope. |
| `suppressionKind` | `QUIET_HOURS`, `OPT_OUT`, `RATE_LIMIT`, `DUPLICATE_WINDOW`. |
| `windowStartLocal` / `windowEndLocal` | Quiet hours. |
| `maxPerWindow` / `windowSeconds` | Rate limit. |
| `active` / `createdByUserId` | Lifecycle. |

---

## Audit

### `AuditLog` — `audit_logs`

Append-only record of every consequential action.

| Field | Meaning |
|---|---|
| `actorUserId` / `actorRole` | Who. |
| `action` | `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `PUBLISH`, `ASSIGN`, `START_ATTEMPT`, `SUBMIT_ATTEMPT`, `GRADE`, `SCORE_OVERRIDE`, `TERMINATE_ATTEMPT`, `VIEW_EVIDENCE`, `EXPORT`, `CONFIG_CHANGE`, `PERMISSION_CHANGE`, `MODEL_CHANGE`. |
| `outcome` | `SUCCESS`, `FAILURE`, `DENIED`. |
| `entityType` / `entityId` / `entityLabel` | What. |
| `occurredAt` | When. |
| `beforeState` / `afterState` | Before/after JSON. |
| `reason` | Justification. |
| `ipAddress` / `userAgent` / `requestId` | Where from. |

---

## AI Model Registry

### `AiModel` — `ai_models`

Registry entry for one model.

| Field | Meaning |
|---|---|
| `code` / `name` / `purpose` / `vendor` / `description` | Identity. |
| `runsOnClient` | Where inference runs. |
| `active` | Soft delete. |

---

### `AiModelVersion` — `ai_model_versions`

One deployable version of a model.

| Field | Meaning |
|---|---|
| `aiModelId` / `version` | Identity. |
| `status` | `DRAFT`, `CANDIDATE`, `SHADOW`, `ACTIVE`, `DEPRECATED`, `RETIRED`. |
| `artifactRef` / `artifactChecksum` | Artefact. |
| `confidenceThreshold` / `detectionThreshold` | Thresholds at this version. |
| `configuration` | Other knobs. |
| `activatedAt` / `deprecatedAt` / `activatedByUserId` | Lifecycle. |
| `releaseNotes` | Notes. |

---

### `ModelPerformanceMetric` — `model_performance_metrics`

One measured metric for one model version over one window.

| Field | Meaning |
|---|---|
| `aiModelVersionId` | The model version. |
| `metricType` | `PRECISION`, `RECALL`, `F1_SCORE`, `ACCURACY`, `FALSE_POSITIVE_RATE`, `FALSE_NEGATIVE_RATE`, `LATENCY_P95_MS`, `THROUGHPUT_FPS`. |
| `metricValue` | Value. |
| `windowStartAt` / `windowEndAt` / `sampleCount` | Window. |
| `truePositiveCount` / `falsePositiveCount` / `falseNegativeCount` | Confusion matrix. |
| `computedAt` / `note` | When/notes. |

---

### `ShadowEvaluation` — `shadow_evaluations`

A measured comparison of a SHADOW model version against the ACTIVE one.

| Field | Meaning |
|---|---|
| `shadowVersionId` / `activeVersionId` | The two versions. |
| `windowStartAt` / `windowEndAt` | Window. |
| `agreementCount` / `disagreementCount` | Agreement. |
| `shadowOnlyDetections` / `activeOnlyDetections` | What each found alone. |
| `shadowFalsePositiveRate` / `activeFalsePositiveRate` | FP rates. |
| `recommendation` | `PROMOTE`, `HOLD`, `RETIRE`. |
| `computedAt` | When. |

---

## System Configuration

### `SystemSetting` — `system_settings`

Runtime configuration an administrator can change without a deploy.

| Field | Meaning |
|---|---|
| `category` | `GENERAL`, `PROCTORING`, `RISK`, `EXAM`, `STORAGE`, `NOTIFICATION`, `AI`, `SECURITY`, `EXCEL`. |
| `settingKey` | Unique key. |
| `settingValue` / `valueType` / `defaultValue` | Value and parsing. |
| `description` / `validationRule` | Metadata. |
| `secret` / `requiresRestart` / `editable` | Flags. |
| `updatedByUserId` / `lastChangedAt` | Change record. |

---

### `RiskFactorConfig` — `risk_factor_configs`

The risk engine's rule book.

| Field | Meaning |
|---|---|
| `factorCode` / `factorLabel` / `configVersion` | Identity. |
| `triggerKind` / `triggerCode` | What triggers it. |
| `severity` | Severity. |
| `baseWeight` / `confidenceMultiplier` / `frequencyIncrement` / `durationWeightPerSecond` / `maxContribution` | Scoring. |
| `decayHalfLifeSeconds` | Time decay. |
| `graceOccurrences` / `minConfidence` | Thresholds. |
| `immediateCritical` | Escalation flag. |
| `active` / `effectiveFrom` / `effectiveTo` | Lifecycle. |

---

### `RiskLevelThreshold` — `risk_level_thresholds`

The band boundaries that turn a numeric score into LOW/MEDIUM/HIGH/CRITICAL.

| Field | Meaning |
|---|---|
| `configVersion` / `riskLevel` | Identity. |
| `minScore` / `maxScore` | Band bounds. |
| `recommendation` / `autoAction` | What to do. |
| `opensReviewCase` / `withholdsResult` / `alertsProctor` | Actions. |
| `displayColour` | Dashboard colour. |
| `active` / `effectiveFrom` / `effectiveTo` | Lifecycle. |

---

## Summary: The Key Design Patterns

1. **`publicId` (UUID)** — Every externally-visible entity has one. Prevents ID enumeration.
2. **Soft deletes** — `active`, `leftAt`, `RETIRED` statuses rather than hard deletes. Audit
   trails stay intact.
3. **Append-only history** — `AnswerRevision`, `AuditLog`, `ProctoringEvent`,
   `EvidenceCustodyRecord`. Never update, never delete.
4. **Embedded value objects** — `ProctoringPolicy` (in `Exam`), `BoundingBox` (in
   `FaceDetection`/`ObjectDetection`). No identity of their own.
5. **Associative entities** — `ExamQuestion`, `AttemptAnswerOption`, `RolePermission`, `UserRole`,
   `QuestionTagLink`. Resolve many-to-many and carry their own data.
6. **Denormalized counters** — `eventCount`, `heartbeatMissCount`, `sightingCount`, `usageCount`.
   Kept on the parent for fast dashboard reads.
7. **Frozen snapshots** — `examVersion` on attempts, `matchThreshold` on verifications, `weight`
   on risk events, `riskScore` on results. A later change must not rewrite history.
8. **JSON payloads** — `payload` on events, `rawOutput` on detections, `timelineJson` on
   timelines, `variables` on notifications. Flexible shape for type-specific detail.
9. **Idempotency keys** — On `ProctoringEvent` and `Notification`. Guards against duplicate
   processing on retry.
10. **Checksums** — On `EvidenceFile`, `ProctoringReport`, `AttemptTimeline`,
    `EvidenceCustodyRecord`. Tamper detection.

This schema is designed for **auditability, defensibility, and forensic reconstruction** — every
decision can be traced back to the signals that produced it, and every signal can be traced back
to its source.
