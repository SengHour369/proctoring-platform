# Field Reference

Every column in every table, in the order it's declared in the entity class, with what
it's for and how to use it. Generated from the entity source, so field names, column
names and nullability here are exactly what's in the code — not retyped from memory.

Three columns exist on **every** table below and aren't repeated per entity:

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `id` | `id` | `Long` | yes | Surrogate key, `bigserial`-style identity. |
| `createdAt` | `created_at` | `Instant` | yes | Set once on insert, never changed. |
| `updatedAt` | `updated_at` | `Instant` | | Bumped by Hibernate on every update. |

See [`entity-relationships.md`](entity-relationships.md) for how the `…Id` foreign-key
columns connect tables, and [`feature-tree-traceability.md`](feature-tree-traceability.md)
for how each table maps back to a product feature.

## Contents

- [Authentication & Access](#authentication-access)
- [Payment](#payment)
- [Student Groups](#student-groups)
- [Exam Definition](#exam-definition)
- [Question Bank](#question-bank)
- [Pre-Exam System Check](#pre-exam-system-check)
- [Identity Verification](#identity-verification)
- [Consent & Privacy](#consent-privacy)
- [Exam Session & Attempts](#exam-session-attempts)
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

### `ApiClient` — `api_clients`

A non-human caller of the API: the AI inference workers that post detections, the evidence uploader, an institution's student-information system pushing enrolments.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `clientId` | `client_id` | `String` | yes | — |
| `name` | `name` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `clientSecretHash` | `client_secret_hash` | `String` | yes | — |
| `status` | `status` | `ApiClientStatus` | yes | One of `ACTIVE`, `SUSPENDED`, `REVOKED`, `EXPIRED`. Defaults to `ApiClientStatus.ACTIVE`. |
| `allowedScopes` | `allowed_scopes` | `String` | yes | Space-separated permission codes, drawn from the same vocabulary as PERMISSIONS. |
| `allowedIpRanges` | `allowed_ip_ranges` | `String` |  | CIDR allow-list; an inference worker calls from a known network. |
| `rateLimitPerMinute` | `rate_limit_per_minute` | `Integer` |  | — |
| `createdByUserId` | `created_by_user_id` | `Long` |  | References `users.id`. |
| `secretRotatedAt` | `secret_rotated_at` | `Instant` |  | — |
| `expiresAt` | `expires_at` | `Instant` |  | — |
| `lastUsedAt` | `last_used_at` | `Instant` |  | — |
| `lastUsedIp` | `last_used_ip` | `String` |  | — |
| `revokedAt` | `revoked_at` | `Instant` |  | — |
| `revokedReason` | `revoked_reason` | `String` |  | — |

### `LoginAttempt` — `login_attempts`

Login history, successes and failures alike. Kept separate from USER_SESSIONS because a failed attempt creates no session, and separate from AUDIT_LOGS because it is written on an unauthenticated path and read by rate limiting on every login.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `userId` | `user_id` | `Long` |  | Null when the address matched no account. |
| `email` | `email` | `String` | yes | — |
| `outcome` | `outcome` | `LoginOutcome` | yes | One of `SUCCESS`, `BAD_CREDENTIALS`, `ACCOUNT_LOCKED`, `ACCOUNT_DISABLED`, `EMAIL_NOT_VERIFIED`, `TOKEN_EXPIRED`, `MFA_REQUIRED`, `MFA_FAILED`. |
| `attemptedAt` | `attempted_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `deviceFingerprint` | `device_fingerprint` | `String` |  | — |
| `geoCountry` | `geo_country` | `String` |  | — |
| `failureDetail` | `failure_detail` | `String` |  | — |

### `Permission` — `permissions`

A single authority, named `resource:action` — `exam:publish`, `evidence:download`, `review:decide`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` | yes | — |
| `resource` | `resource` | `String` | yes | — |
| `action` | `action` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `system` | `is_system` | `boolean` | yes | True for authorities the platform relies on; these cannot be deleted by an administrator. Defaults to `false`. |

### `Role` — `roles`

Named permission bundle — STUDENT, TEACHER, REVIEWER, ADMIN. Kept as a table rather than an enum so an operator can add a role (an external invigilator, a read-only auditor) without a redeploy, and so `RolePermission` can re-scope an existing one.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` | yes | — |
| `name` | `name` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `system` | `is_system` | `boolean` | yes | Defaults to `false`. |

### `RolePermission` — `role_permission`

Grant of one `Permission` to one `Role`, with its own audit trail. Access control is evaluated as user → roles → permissions, so this table is the whole of RBAC.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `roleId` | `role_id` | `Long` | yes | References `roles.id`. |
| `permissionId` | `permission_id` | `Long` | yes | References `permissions.id`. |
| `grantedByUserId` | `granted_by_user_id` | `Long` |  | References `users.id`. |
| `grantedAt` | `granted_at` | `Instant` | yes | Defaults to `Instant.now()`. |

### `SecurityToken` — `security_tokens`

One-time token behind email verification, forgot/reset password and account invitations. A single table discriminated by `TokenPurpose` rather than one table per flow: every such token has the same lifecycle — issued, expires, used once, then dead — and the same security rules.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `purpose` | `purpose` | `TokenPurpose` | yes | One of `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_INVITATION`, `EMAIL_CHANGE`. |
| `tokenHash` | `token_hash` | `String` | yes | — |
| `issuedAt` | `issued_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `expiresAt` | `expires_at` | `Instant` | yes | — |
| `usedAt` | `used_at` | `Instant` |  | — |
| `invalidatedAt` | `invalidated_at` | `Instant` |  | — |
| `requestedIp` | `requested_ip` | `String` |  | Where the token was redeemed — a mismatch with the request origin is worth alerting on. |
| `redeemedIp` | `redeemed_ip` | `String` |  | — |

### `User` — `users`

Every human on the platform: candidates, proctors, reviewers and administrators. What a user may do is not a column here but the set of `UserRole` grants attached to them, so one account can be both a candidate and a reviewer without duplication.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `email` | `email` | `String` | yes | — |
| `passwordHash` | `password_hash` | `String` | yes | — |
| `fullName` | `full_name` | `String` | yes | — |
| `externalRef` | `external_ref` | `String` |  | Student / employee number from the system of record, when the user was provisioned there. |
| `status` | `status` | `UserStatus` | yes | One of `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DISABLED`. Defaults to `UserStatus.PENDING_VERIFICATION`. |
| `emailVerifiedAt` | `email_verified_at` | `Instant` |  | — |
| `enrolmentPhotoPath` | `enrolment_photo_path` | `String` |  | Reference photo captured at enrolment. Face detections compare against this, so a candidate without one cannot sit an exam whose policy requires an identity check. |
| `voiceprintPath` | `voiceprint_path` | `String` |  | Reference voiceprint, when the exam policy enables second-voice detection. |
| `phoneNumber` | `phone_number` | `String` |  | — |
| `timeZone` | `time_zone` | `String` |  | — |
| `locale` | `locale` | `String` |  | — |
| `lastLoginAt` | `last_login_at` | `Instant` |  | — |
| `failedLoginCount` | `failed_login_count` | `int` | yes | Defaults to `0`. |
| `lockedUntil` | `locked_until` | `Instant` |  | — |
| `mfaEnabled` | `mfa_enabled` | `boolean` | yes | Defaults to `false`. |
| `mfaMethod` | `mfa_method` | `MfaMethod` |  | Null means MFA is off. SMS/EMAIL methods reuse phoneNumber/email — no separate address column. |
| `mfaSecretEncrypted` | `mfa_secret_encrypted` | `String` |  | Encrypted TOTP seed. Unused for SMS/EMAIL, which have no secret of their own to store. |
| `mfaEnrolledAt` | `mfa_enrolled_at` | `Instant` |  | — |

### `UserRole` — `user_role`

Grant of one role to one user, resolving the many-to-many between USERS and ROLES. The grant is an entity in its own right rather than a bare pair of ids: it carries who granted it, when, and when it lapses — which matters when the role in question is REVIEWER.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `roleId` | `role_id` | `Long` | yes | References `roles.id`. |
| `grantedByUserId` | `granted_by_user_id` | `Long` |  | References `users.id`. |
| `grantedAt` | `granted_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `expiresAt` | `expires_at` | `Instant` |  | — |

### `UserSession` — `user_sessions`

Authenticated login session (the JWT refresh side). Distinct from PROCTORING_SESSIONS: this is "who is signed in", not "who is being watched". Kept server-side so an administrator can revoke a session, and so a mid-exam sign-in from a second device is detectable.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `refreshTokenHash` | `refresh_token_hash` | `String` | yes | Hash, never the token itself — a stolen table dump must not be replayable. |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `deviceFingerprint` | `device_fingerprint` | `String` |  | — |
| `issuedAt` | `issued_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `expiresAt` | `expires_at` | `Instant` | yes | — |
| `lastSeenAt` | `last_seen_at` | `Instant` |  | — |
| `revokedAt` | `revoked_at` | `Instant` |  | — |
| `revokedReason` | `revoked_reason` | `String` |  | — |


---

## Payment

### `PaymentCustomer` — `payment_customers`

A candidate's billing profile with one payment processor — the record that has to exist before a card can be tokenized against it, since processors like Stripe require a Customer object before a PaymentMethod can be attached. A user can have a separate row per provider, which is why `(userId, provider)` is unique rather than `userId` alone.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to a random `UUID`. |
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `defaultPaymentCardId` | `default_payment_card_id` | `Long` |  | References `payment_cards.id`. The card this customer's charges default to. |
| `provider` | `provider` | `PaymentProvider` | yes | One of `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL`. |
| `providerCustomerId` | `provider_customer_id` | `String` | yes | The processor's own customer id. |
| `billingEmail` | `billing_email` | `String` |  | — |
| `billingName` | `billing_name` | `String` |  | — |
| `billingPhone` | `billing_phone` | `String` |  | — |
| `billingAddressLine1` | `billing_address_line1` | `String` |  | — |
| `billingAddressLine2` | `billing_address_line2` | `String` |  | — |
| `billingCity` | `billing_city` | `String` |  | — |
| `billingState` | `billing_state` | `String` |  | — |
| `billingPostalCode` | `billing_postal_code` | `String` |  | — |
| `billingCountry` | `billing_country` | `String` |  | ISO 3166-1 alpha-2. |
| `taxId` | `tax_id` | `String` |  | — |
| `preferredCurrency` | `preferred_currency` | `String` |  | ISO 4217 alpha code. Plain text, not an enum — an open-ended vocabulary that shouldn't need a migration to grow. |
| `delinquent` | `is_delinquent` | `boolean` | yes | Defaults to `false`. |
| `metadata` | `metadata` | `String` (JSON) |  | — |

### `PaymentCard` — `payment_cards`

A candidate's payment card on file, for exam fees — tokenized metadata only. The actual card number and CVV never reach this database: they're handed straight to a PCI-compliant processor (Stripe, Adyen, ...), and this row stores only what that processor hands back — `providerPaymentMethodId`, an opaque token — plus the display and risk-signal fields (brand, last four, expiry, fingerprint, verification results) needed to show the candidate which card is on file and to reason about it without ever touching the real number. Same reasoning as `User.passwordHash` and `ApiClient.clientSecretHash`: never store the secret, only a reference to it.

Both `paymentCustomerId` and `userId` are stored, even though the user is reachable through the customer, so listing "this candidate's cards" doesn't need a join. "At most one default card per customer" is enforced by `PaymentCardService.setDefault`, not by a database constraint.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to a random `UUID`. |
| `paymentCustomerId` | `payment_customer_id` | `Long` | yes | References `payment_customers.id`. |
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `provider` | `provider` | `PaymentProvider` | yes | One of `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL`. |
| `providerPaymentMethodId` | `provider_payment_method_id` | `String` | yes | The processor's token for this specific card — never the card number itself. |
| `providerCardToken` | `provider_card_token` | `String` |  | Legacy/provider-specific raw card token, distinct from the payment method id. |
| `brand` | `brand` | `CardBrand` | yes | One of `VISA`, `MASTERCARD`, `AMEX`, `DISCOVER`, `JCB`, `DINERS_CLUB`, `UNIONPAY`, `MAESTRO`, `ELO`, `OTHER`. |
| `funding` | `funding` | `PaymentCardFunding` | yes | One of `CREDIT`, `DEBIT`, `PREPAID`, `CHARGE`, `UNKNOWN`. |
| `status` | `status` | `PaymentCardStatus` | yes | One of `ACTIVE`, `EXPIRED`, `PENDING_VERIFICATION`, `SUSPENDED`, `REVOKED`. Defaults to `PaymentCardStatus.PENDING_VERIFICATION`. |
| `last4` | `last4` | `String` | yes | — |
| `bin` | `bin` | `String` |  | Bank identification number — the first 6-8 digits. Routing metadata, not sensitive like a full PAN. |
| `expiryMonth` | `expiry_month` | `Integer` | yes | — |
| `expiryYear` | `expiry_year` | `Integer` | yes | — |
| `cardholderName` | `cardholder_name` | `String` |  | — |
| `issuer` | `issuer` | `String` |  | — |
| `issuerCountry` | `issuer_country` | `String` |  | ISO 3166-1 alpha-2. |
| `billingCountry` | `billing_country` | `String` |  | ISO 3166-1 alpha-2. |
| `billingPostalCode` | `billing_postal_code` | `String` |  | — |
| `fingerprint` | `fingerprint` | `String` |  | Lets duplicate-card detection run across accounts without ever exposing the PAN. |
| `cvvCheck` | `cvv_check` | `PaymentCardholderVerification` | yes | One of `NOT_ATTEMPTED`, `PASSED`, `FAILED`, `UNAVAILABLE`, `UNRECOGNIZED`. Defaults to `NOT_ATTEMPTED`. |
| `avsLine1Check` | `avs_line1_check` | `PaymentCardholderVerification` | yes | Same values as `cvvCheck`. Defaults to `NOT_ATTEMPTED`. |
| `avsPostalCodeCheck` | `avs_postal_code_check` | `PaymentCardholderVerification` | yes | Same values as `cvvCheck`. Defaults to `NOT_ATTEMPTED`. |
| `threeDsSupported` | `three_ds_supported` | `boolean` | yes | Defaults to `false`. |
| `threeDsEnrolled` | `three_ds_enrolled` | `boolean` | yes | Defaults to `false`. |
| `defaultCard` | `is_default` | `boolean` | yes | The card charged by default when this candidate isn't asked to pick one. Defaults to `false`. |
| `lastUsedAt` | `last_used_at` | `Instant` |  | — |
| `revokedAt` | `revoked_at` | `Instant` |  | Set on any transition to `REVOKED` — a candidate removing it and an admin invalidating it both land here. |
| `revokedReason` | `revoked_reason` | `String` |  | — |
| `metadata` | `metadata` | `String` (JSON) |  | — |

### `PaymentTransaction` — `payment_transactions`

One attempt by a processor to move money — an authorization, a capture, a refund, a payout, and so on. `parentTransactionId` links a follow-up event back to the transaction it acts on (a `REFUND` back to the `SALE` it refunds, a `CAPTURE` back to its `AUTHORIZATION`).

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to a random `UUID`. |
| `paymentCustomerId` | `payment_customer_id` | `Long` | yes | References `payment_customers.id`. |
| `paymentCardId` | `payment_card_id` | `Long` |  | References `payment_cards.id`. |
| `parentTransactionId` | `parent_transaction_id` | `Long` |  | References `payment_transactions.id`. The transaction this one acts on. |
| `initiatedByUserId` | `initiated_by_user_id` | `Long` |  | References `users.id`. Null for processor-initiated events (e.g. a chargeback). |
| `provider` | `provider` | `PaymentProvider` | yes | One of `STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL`. |
| `providerTransactionId` | `provider_transaction_id` | `String` | yes | — |
| `providerIntentId` | `provider_intent_id` | `String` |  | — |
| `currency` | `currency` | `String` | yes | ISO 4217 alpha code. |
| `amountMinor` | `amount_minor` | `long` | yes | Integer minor units (cents) — never a floating type. |
| `amountRefundedMinor` | `amount_refunded_minor` | `long` | yes | Defaults to `0`. |
| `type` | `type` | `PaymentTransactionType` | yes | One of `AUTHORIZATION`, `CAPTURE`, `SALE`, `REFUND`, `VOID`, `CHARGEBACK`, `PAYOUT`. |
| `status` | `status` | `PaymentTransactionStatus` | yes | One of `INITIATED`, `PENDING`, `AUTHORIZED`, `CAPTURED`, `SETTLED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `DISPUTED`, `CHARGEBACK`. Defaults to `INITIATED`. |
| `failureCode` | `failure_code` | `String` |  | — |
| `failureMessage` | `failure_message` | `String` |  | — |
| `description` | `description` | `String` |  | — |
| `statementDescriptor` | `statement_descriptor` | `String` |  | — |
| `reference` | `reference` | `String` |  | Free-text correlation key back to the domain object this charge is for. |
| `idempotencyKey` | `idempotency_key` | `String` |  | Unique when present, any number of nulls allowed. |
| `threeDsAuthenticated` | `three_ds_authenticated` | `boolean` | yes | Defaults to `false`. |
| `capturedAt` | `captured_at` | `Instant` |  | — |
| `metadata` | `metadata` | `String` (JSON) |  | — |


---

## Student Groups

### `GroupMembership` — `group_memberships`

Membership of one student in one group. `leftAt` rather than deletion: an exam assigned to a class must stay explicable a year later, when the class roster has moved on.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `studentGroupId` | `student_group_id` | `Long` | yes | References `student_groups.id`. |
| `userId` | `user_id` | `Long` | yes | References `users.id`. |
| `addedByUserId` | `added_by_user_id` | `Long` |  | References `users.id`. |
| `joinedAt` | `joined_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `leftAt` | `left_at` | `Instant` |  | — |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |

### `StudentGroup` — `student_groups`

A class, cohort or ad-hoc group of students, so an exam can be assigned to forty people in one action. Self-referencing so a programme can contain classes.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` | yes | — |
| `name` | `name` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `groupType` | `group_type` | `GroupType` | yes | One of `CLASS`, `COHORT`, `DEPARTMENT`, `PROGRAM`, `CUSTOM`. Defaults to `GroupType.CLASS`. |
| `parentGroupId` | `parent_group_id` | `Long` |  | References `student_groups.id`. |
| `ownerUserId` | `owner_user_id` | `Long` |  | Teacher or coordinator responsible for the group. |
| `academicTerm` | `academic_term` | `String` |  | — |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |


---

## Exam Definition

### `Exam` — `exams`

The exam definition — a blueprint, not a sitting. Once PUBLISHED its structure is frozen: attempts in flight reference sections and questions through EXAM_QUESTIONS, so edits after publication would silently rewrite history. Structural edits create a new `version`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `code` | `code` | `String` | yes | — |
| `title` | `title` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `instructions` | `instructions` | `String` |  | — |
| `createdByUserId` | `created_by_user_id` | `Long` | yes | References `users.id`. |
| `status` | `status` | `ExamStatus` | yes | One of `DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `CLOSED`, `ARCHIVED`. Defaults to `ExamStatus.DRAFT`. |
| `version` | `version` | `int` | yes | Defaults to `1`. |
| `durationMinutes` | `duration_minutes` | `Integer` |  | Wall-clock budget for one attempt. Null means the closing time is the only limit. |
| `opensAt` | `opens_at` | `Instant` |  | — |
| `closesAt` | `closes_at` | `Instant` |  | — |
| `maxAttempts` | `max_attempts` | `int` | yes | Defaults to `1`. |
| `totalPoints` | `total_points` | `BigDecimal` |  | — |
| `passingScore` | `passing_score` | `BigDecimal` |  | — |
| `gradingMode` | `grading_mode` | `GradingMode` | yes | One of `AUTO`, `MANUAL`, `HYBRID`. Defaults to `GradingMode.AUTO`. |
| `shuffleSections` | `shuffle_sections` | `boolean` | yes | Defaults to `false`. |
| `holdResultsForReview` | `hold_results_for_review` | `boolean` | yes | Withhold scores until any integrity review closes, instead of releasing on submit. Defaults to `false`. |
| `showResultImmediately` | `show_result_immediately` | `boolean` | yes | Defaults to `false`. |
| `proctoringPolicy` | `proctoring_policy` | `ProctoringPolicy` |  | Embedded value object — see its own field table. Defaults to `new ProctoringPolicy()`. |
| `excelPolicy` | `excel_policy` | `ExcelPolicy` |  | Embedded value object — see its own field table. Defaults to `new ExcelPolicy()`. |

### `ExamAssignment` — `exam_assignments`

Entitlement of one candidate to sit one exam, with an optional per-candidate window and accommodations. Attempts are only creatable against an assignment when the exam is invite-only, which is what keeps "who was allowed to sit this" auditable after the fact.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `assignedByUserId` | `assigned_by_user_id` | `Long` |  | References `users.id`. |
| `status` | `status` | `AssignmentStatus` | yes | One of `ASSIGNED`, `NOTIFIED`, `STARTED`, `SUBMITTED`, `EXPIRED`, `CANCELLED`. Defaults to `AssignmentStatus.ASSIGNED`. |
| `assignedAt` | `assigned_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `notifiedAt` | `notified_at` | `Instant` |  | — |
| `windowStartAt` | `window_start_at` | `Instant` |  | Candidate-specific window, narrowing the exam's own opens/closes range. |
| `windowEndAt` | `window_end_at` | `Instant` |  | — |
| `dueAt` | `due_at` | `Instant` |  | — |
| `attemptsAllowed` | `attempts_allowed` | `Integer` |  | Overrides `Exam.maxAttempts` for this candidate (e.g. an approved retake). |
| `extraTimeMinutes` | `extra_time_minutes` | `Integer` |  | Accessibility accommodation: extra minutes granted on top of the exam duration. |
| `accessCodeHash` | `access_code_hash` | `String` |  | Hashed one-time code the candidate must present to open the exam. |
| `cancelledAt` | `cancelled_at` | `Instant` |  | — |
| `cancelReason` | `cancel_reason` | `String` |  | — |

### `ExamGroupAssignment` — `exam_group_assignments`

Assignment of an exam to a whole group. It does not replace `ExamAssignment`: expanding this row fans out one per-candidate assignment per member, and those remain the single source of truth for eligibility.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `studentGroupId` | `student_group_id` | `Long` | yes | References `student_groups.id`. |
| `assignedByUserId` | `assigned_by_user_id` | `Long` |  | References `users.id`. |
| `assignedAt` | `assigned_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `windowStartAt` | `window_start_at` | `Instant` |  | — |
| `windowEndAt` | `window_end_at` | `Instant` |  | — |
| `dueAt` | `due_at` | `Instant` |  | — |
| `autoEnrollNewMembers` | `auto_enroll_new_members` | `boolean` | yes | Keep fanning out to students who join the group after this assignment was made. Defaults to `true`. |
| `expandedAt` | `expanded_at` | `Instant` |  | — |
| `expandedCount` | `expanded_count` | `int` | yes | Defaults to `0`. |
| `cancelledAt` | `cancelled_at` | `Instant` |  | — |

### `ExamInvitation` — `exam_invitations`

One invitation sent for one assignment. Distinct from the assignment because invitations are resent — a bounced address, a reminder, a changed sitting window — and each send has its own token, channel and delivery state, while the entitlement itself is unchanged.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAssignmentId` | `exam_assignment_id` | `Long` | yes | References `exam_assignments.id`. |
| `channel` | `channel` | `InvitationChannel` | yes | One of `EMAIL`, `SMS`, `IN_APP`. Defaults to `InvitationChannel.EMAIL`. |
| `status` | `status` | `InvitationStatus` | yes | One of `PENDING`, `SENT`, `DELIVERED`, `OPENED`, `ACCEPTED`, `EXPIRED`, `CANCELLED`, `FAILED`. Defaults to `InvitationStatus.PENDING`. |
| `sentTo` | `sent_to` | `String` | yes | Address or number actually used, kept for bounce diagnosis after a profile change. |
| `tokenHash` | `token_hash` | `String` |  | — |
| `sequenceNo` | `sequence_no` | `int` | yes | Defaults to `1`. |
| `reminder` | `is_reminder` | `boolean` | yes | Defaults to `false`. |
| `sentAt` | `sent_at` | `Instant` |  | — |
| `deliveredAt` | `delivered_at` | `Instant` |  | — |
| `openedAt` | `opened_at` | `Instant` |  | — |
| `acceptedAt` | `accepted_at` | `Instant` |  | — |
| `expiresAt` | `expires_at` | `Instant` |  | — |
| `failureReason` | `failure_reason` | `String` |  | — |

### `ExamPrerequisite` — `exam_prerequisites`

One eligibility rule gating an exam — "must have passed exam X at 70%" or "must have completed course Y." One row per rule, since an exam can carry several independent ones; not an embedded value object like `ProctoringPolicy`, because that pattern is for genuinely 1:1 objects with no identity of their own, and prerequisites are 1:many.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `requiredExamId` | `required_exam_id` | `Long` |  | Null when the rule is coursework-based rather than another exam. |
| `minScore` | `min_score` | `BigDecimal` |  | — |
| `courseReference` | `course_reference` | `String` |  | External LMS/coursework code, same shape as User.externalRef. |
| `description` | `description` | `String` |  | Shown to a candidate who fails the check. |
| `active` | `is_active` | `boolean` | yes | Retire a rule without losing the history of what gated past assignments. Defaults to `true`. |

### `ExamPaymentRequirement` — `exam_payment_requirements`

A fee an exam requires before a candidate can start it — one row per rule, same shape as `ExamPrerequisite`: a flat `active` flag rather than a status enum.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `amountMinor` | `amount_minor` | `long` | yes | Integer minor units (cents). |
| `currency` | `currency` | `String` | yes | ISO 4217 alpha code. |
| `description` | `description` | `String` |  | — |
| `active` | `active` | `boolean` | yes | Defaults to `true`. |

### `ExamPaymentCharge` — `exam_payment_charges`

One candidate's obligation to pay a specific `ExamPaymentRequirement`, and the record of whether they have. `amountMinor`/`currency` are copied from the requirement at charge-creation time rather than read live from it — a later fee change must not reprice a past charge. `status` reuses `PaymentTransactionStatus` rather than introducing a parallel enum.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to a random `UUID`. |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `examAttemptId` | `exam_attempt_id` | `Long` |  | References `exam_attempts.id`. Unique when present — at most one charge per attempt. |
| `examAssignmentId` | `exam_assignment_id` | `Long` |  | References `exam_assignments.id`. |
| `examPaymentRequirementId` | `exam_payment_requirement_id` | `Long` | yes | References `exam_payment_requirements.id`. |
| `paymentTransactionId` | `payment_transaction_id` | `Long` |  | References `payment_transactions.id`. Set once a transaction settles this charge. |
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `currency` | `currency` | `String` | yes | ISO 4217 alpha code, frozen from the requirement at charge-creation time. |
| `amountMinor` | `amount_minor` | `long` | yes | Frozen from the requirement at charge-creation time. |
| `status` | `status` | `PaymentTransactionStatus` | yes | Same values as `PaymentTransaction.status`. Defaults to `INITIATED`. |
| `paidAt` | `paid_at` | `Instant` |  | — |
| `refundedAt` | `refunded_at` | `Instant` |  | — |

### `ExamQuestion` — `exam_questions`

Placement of a bank `Question` inside an `ExamSection`: the associative entity that resolves the many-to-many between exams and questions. It is the unit answers and per-question results point at, so the same bank question can appear in two exams worth different points without either exam disturbing the other.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examSectionId` | `exam_section_id` | `Long` | yes | References `exam_sections.id`. |
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `points` | `points` | `BigDecimal` | yes | Overrides the question's default weight for this exam only. Defaults to `BigDecimal.ONE`. |
| `negativePoints` | `negative_points` | `BigDecimal` |  | — |
| `required` | `required` | `boolean` | yes | Defaults to `true`. |
| `shuffleOptions` | `shuffle_options` | `boolean` | yes | Defaults to `false`. |

### `ExamSection` — `exam_sections`

Ordered part of an exam ("Section A — Multiple choice"). Sections exist so an exam can mix question styles, carry per-part time limits, and draw a random subset from a larger pool.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `title` | `title` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `instructions` | `instructions` | `String` |  | — |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `timeLimitMinutes` | `time_limit_minutes` | `Integer` |  | — |
| `sectionPoints` | `section_points` | `BigDecimal` |  | — |
| `shuffleQuestions` | `shuffle_questions` | `boolean` | yes | Defaults to `false`. |
| `questionsToDraw` | `questions_to_draw` | `Integer` |  | How many of the linked questions each candidate actually receives. Null delivers all of them; a smaller number turns the section into a random draw from a pool. |
| `lockOnExit` | `lock_on_exit` | `boolean` | yes | Once a candidate leaves this section they cannot navigate back into it. Defaults to `false`. |

### `ExamWindowOverride` — `exam_window_overrides`

A documented exception widening one candidate's sitting window, for cases an ordinary assignment window can't cover — a verified medical absence, say. Scoped to the `ExamAssignment`, never to the `Exam` itself: widening the exam's own `opensAt`/`closesAt` to accommodate one candidate would change the window for every other candidate too.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAssignmentId` | `exam_assignment_id` | `Long` | yes | References `exam_assignments.id`. |
| `overriddenWindowStartAt` | `overridden_window_start_at` | `Instant` | yes | May fall outside the exam's own opensAt/closesAt — that's the point of an override. |
| `overriddenWindowEndAt` | `overridden_window_end_at` | `Instant` | yes | — |
| `reason` | `reason` | `String` | yes | — |
| `justificationRef` | `justification_ref` | `String` |  | External ticket/document reference — an override without one is rejected at write time. |
| `grantedByUserId` | `granted_by_user_id` | `Long` | yes | References `users.id`. |
| `grantedAt` | `granted_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `expiresAt` | `expires_at` | `Instant` |  | — |
| `supersedesOverrideId` | `supersedes_override_id` | `Long` |  | The override this one revokes and replaces, mirroring ReviewDecision.supersedesDecisionId. |

### `ProctoringPolicy` *(embedded value object)*

Supervision rules an exam enforces. Modelled as a value object embedded in EXAMS rather than a table of its own: a policy has no identity or lifecycle apart from the exam that declares it.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `mode` | `proctoring_mode` | `ProctoringMode` | yes | One of `NONE`, `AI_ONLY`, `LIVE_PROCTOR`, `RECORD_AND_REVIEW`, `HYBRID`. Defaults to `ProctoringMode.NONE`. |
| `requireWebcam` | `require_webcam` | `boolean` | yes | Defaults to `false`. |
| `requireScreenShare` | `require_screen_share` | `boolean` | yes | Defaults to `false`. |
| `requireMicrophone` | `require_microphone` | `boolean` | yes | Defaults to `false`. |
| `requireIdentityCheck` | `require_identity_check` | `boolean` | yes | Defaults to `false`. |
| `requireEnvironmentScan` | `require_environment_scan` | `boolean` | yes | Defaults to `false`. |
| `forceFullscreen` | `force_fullscreen` | `boolean` | yes | Defaults to `false`. |
| `monitorScreen` | `monitor_screen` | `boolean` | yes | Defaults to `false`. |
| `detectTabSwitch` | `detect_tab_switch` | `boolean` | yes | Defaults to `false`. |
| `detectFace` | `detect_face` | `boolean` | yes | Defaults to `false`. |
| `detectMultipleFaces` | `detect_multiple_faces` | `boolean` | yes | Defaults to `false`. |
| `detectGaze` | `detect_gaze` | `boolean` | yes | Head pose and gaze estimation — the "looking away" signals. Defaults to `false`. |
| `detectObjects` | `detect_objects` | `boolean` | yes | Prohibited-object detection: phone, book, second laptop, additional person. Defaults to `false`. |
| `detectAudio` | `detect_audio` | `boolean` | yes | Defaults to `false`. |
| `retainAudioTranscript` | `retain_audio_transcript` | `boolean` | yes | Retain a short excerpt of transcribed speech on an audio detection. Off by default. Defaults to `false`. |
| `blockCopyPaste` | `block_copy_paste` | `boolean` | yes | Defaults to `false`. |
| `allowedTabSwitches` | `allowed_tab_switches` | `Integer` |  | Tab switches tolerated before the platform escalates. Null means unlimited. |
| `autoReviewRiskThreshold` | `auto_review_risk_threshold` | `Integer` |  | Risk score at or above which the attempt is auto-flagged for human review. |
| `autoTerminateRiskThreshold` | `auto_terminate_risk_threshold` | `Integer` |  | Risk score at or above which the attempt is terminated without a human in the loop. |
| `evidenceRetentionDays` | `evidence_retention_days` | `Integer` |  | Days evidence is retained before purge, per data-protection policy. |

### `ExcelPolicy` *(embedded value object)*

How an exam's embedded Excel runtime behaves, for exams carrying one or more SPREADSHEET questions. Modelled the same way as `ProctoringPolicy`: no identity or lifecycle apart from the exam that declares it. Deployment-level concerns (installed engines, per-session resource quotas, the global allowed-functions/add-ins whitelist) are deliberately not here — those live in `SystemSetting` instead.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `runtimeRequired` | `excel_runtime_required` | `boolean` | yes | Defaults to `false`. |
| `externalAppBlocked` | `excel_external_app_blocked` | `boolean` | yes | Flags a candidate who opens the workbook in desktop Excel/Sheets/LibreOffice outside this runtime. Defaults to `true`. |
| `macrosAllowed` | `excel_macros_allowed` | `boolean` | yes | Gate for every SPREADSHEET question's own `Question.excelMacroPolicy` — `false` here means no macro runs regardless of the question's setting. Defaults to `false`. |
| `copyPastePolicy` | `excel_copy_paste_policy` | `ExcelUiActionPolicy` | yes | One of `ALLOW`, `BLOCK`, `LOG`. Defaults to `ExcelUiActionPolicy.LOG`. |
| `cutDragFillPolicy` | `excel_cut_drag_fill_policy` | `ExcelUiActionPolicy` | yes | Same values as `copyPastePolicy`. Defaults to `ExcelUiActionPolicy.ALLOW`. |
| `autosaveIntervalSeconds` | `excel_autosave_interval_seconds` | `Integer` |  | Seconds between cell-level autosaves. Null falls back to the system default. |
| `snapshotIntervalMinutes` | `excel_snapshot_interval_minutes` | `Integer` |  | Minutes between full workbook snapshots. Null falls back to the system default. |
| `recalcMode` | `excel_recalc_mode` | `ExcelRecalcMode` | yes | One of `AUTOMATIC`, `MANUAL`, `ITERATIVE`. Defaults to `ExcelRecalcMode.AUTOMATIC`. |
| `iterativeCalcMaxIterations` | `excel_iterative_calc_max_iterations` | `Integer` |  | — |
| `precisionAsDisplayed` | `excel_precision_as_displayed` | `boolean` | yes | Defaults to `false`. |
| `volatileFunctionsPinned` | `excel_volatile_functions_pinned` | `boolean` | yes | Pins NOW()/TODAY()/RAND()-family functions to a per-session seed instead of letting them vary on recalculation. Defaults to `true`. |

### `RetakeGrant` — `retake_grants`

A retake authorised for one candidate, separate from the `ReviewDecision` that judged it was warranted. Conflating the two would make the entitlement disappear the moment the decision row is superseded on appeal; keeping them apart means the grant survives, traceable back to whichever decision (current or historical) authorised it.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAssignmentId` | `exam_assignment_id` | `Long` | yes | References `exam_assignments.id`. |
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `reviewDecisionId` | `review_decision_id` | `Long` | yes | The GRANT_RETAKE decision this entitlement can't exist without. |
| `grantedByUserId` | `granted_by_user_id` | `Long` | yes | References `users.id`. |
| `grantedAt` | `granted_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `additionalAttempts` | `additional_attempts` | `int` | yes | Defaults to `1`. |
| `expiresAt` | `expires_at` | `Instant` |  | — |
| `consumedAt` | `consumed_at` | `Instant` |  | Set exactly once, atomically with the attempt it was consumed by. |
| `consumedByAttemptId` | `consumed_by_attempt_id` | `Long` |  | References `exam_attempts.id`. |
| `reason` | `reason` | `String` | yes | Required — a grant without a stated reason cannot be defended later. |
| `supersedesGrantId` | `supersedes_grant_id` | `Long` |  | The grant this one revokes and replaces, when a retake is rescinded on further review. |


---

## Question Bank

### `CodeTestCase` — `code_test_cases`

One input/expected-output pair a CODE question is graded against. Hangs off `Question`, not off a placement in an exam — same reasoning as `QuestionOption`: a test case is a property of the bank item, so reusing the question in a second exam must not clone it.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `inputData` | `input_data` | `String` |  | — |
| `expectedOutput` | `expected_output` | `String` | yes | — |
| `visibility` | `visibility` | `TestCaseVisibility` | yes | One of `SAMPLE`, `HIDDEN`. Defaults to `TestCaseVisibility.HIDDEN`. |
| `points` | `points` | `BigDecimal` |  | Null falls back to equal weighting across all of the question's test cases. |

### `Question` — `questions`

Reusable item in the question bank, independent of any exam. Questions are never hard-deleted once used — they are RETIRED — because old attempts and results must stay readable. Fixing or improving one that's already been sat can't edit the row in place for the same reason, so a new version is a new row: insert it with `parentQuestionId` pointing back at the one it replaces and `version = parent.version + 1`, then retire the parent once the new version is placed into exams. Every past `ExamQuestion`/`AttemptAnswer` keeps referencing the exact row a candidate actually saw — nothing needs re-authoring from scratch, and nothing that already happened is rewritten.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` |  | — |
| `questionType` | `question_type` | `QuestionType` | yes | One of `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_ANSWER`, `ESSAY`, `NUMERIC`, `MATCHING`, `ORDERING`, `FILL_IN_BLANK`, `CODE`, `SPREADSHEET`. |
| `stem` | `stem` | `String` | yes | The prompt shown to the candidate. |
| `explanation` | `explanation` | `String` |  | Shown after grading, not during the attempt. |
| `answerKey` | `answer_key` | `String` |  | Expected answer for auto-graded free-text and numeric types. |
| `numericTolerance` | `numeric_tolerance` | `BigDecimal` |  | Tolerance for NUMERIC answers, e.g. 0.01. |
| `mediaPath` | `media_path` | `String` |  | — |
| `difficulty` | `difficulty` | `QuestionDifficulty` | yes | One of `EASY`, `MEDIUM`, `HARD`, `EXPERT`. Defaults to `QuestionDifficulty.MEDIUM`. |
| `status` | `status` | `QuestionStatus` | yes | One of `DRAFT`, `ACTIVE`, `RETIRED`. Defaults to `QuestionStatus.DRAFT`. |
| `defaultPoints` | `default_points` | `BigDecimal` | yes | Defaults to `BigDecimal.ONE`. |
| `topic` | `topic` | `String` |  | — |
| `questionCategoryId` | `question_category_id` | `Long` |  | Subject placement in the bank taxonomy; drives pool draws by subject area. |
| `expectedSeconds` | `expected_seconds` | `Integer` |  | — |
| `createdByUserId` | `created_by_user_id` | `Long` |  | References `users.id`. |
| `version` | `version` | `int` | yes | Defaults to `1`. |
| `parentQuestionId` | `parent_question_id` | `Long` |  | The question this one is a new version of, when it was created by inheriting from an existing item rather than authored from scratch. Null for an original question that has no predecessor. |
| `programmingLanguage` | `programming_language` | `ProgrammingLanguage` |  | One of `PYTHON`, `JAVA`, `JAVASCRIPT`, `TYPESCRIPT`, `CPP`, `C`, `CSHARP`, `GO`, `RUST`, `SQL`. |
| `starterCode` | `starter_code` | `String` |  | — |
| `executionTimeLimitMs` | `execution_time_limit_ms` | `Integer` |  | — |
| `executionMemoryLimitMb` | `execution_memory_limit_mb` | `Integer` |  | — |
| `workbookFileType` | `workbook_file_type` | `ExcelWorkbookFileType` |  | One of `XLSX`, `XLSM`, `XLSB`, `CSV`, `ODS`. Only meaningful when `questionType = SPREADSHEET`. |
| `workbookStoragePath` | `workbook_storage_path` | `String` |  | — |
| `workbookChecksumSha256` | `workbook_checksum_sha256` | `String` |  | Tamper check for the authored template, the same reasoning as `EvidenceFile.checksumSha256`. |
| `workbookSizeBytes` | `workbook_size_bytes` | `Long` |  | — |
| `workbookSheetCount` | `workbook_sheet_count` | `Integer` |  | — |
| `workbookHasMacros` | `workbook_has_macros` | `boolean` | yes | Defaults to `false`. |
| `workbookHasExternalLinks` | `workbook_has_external_links` | `boolean` | yes | Defaults to `false`. |
| `excelMacroPolicy` | `excel_macro_policy` | `ExcelMacroPolicy` |  | One of `OFF`, `SANDBOXED`, `ALLOWED_WHITELIST`. Only enforceable when the owning exam's `ExcelPolicy.macrosAllowed` is also `true`. |

### `ExcelCellBinding` — `excel_cell_bindings`

One graded cell or range within a SPREADSHEET question's workbook. Hangs off `Question`, not off a placement in an exam — same reasoning as `CodeTestCase`: a binding is a property of the bank item, so reusing the question in a second exam must not clone it.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `sheetName` | `sheet_name` | `String` | yes | — |
| `cellRef` | `cell_ref` | `String` |  | — |
| `rangeRef` | `range_ref` | `String` |  | Set instead of `cellRef` for a RANGE/chart-source/pivot-source binding. |
| `answerKind` | `answer_kind` | `ExcelAnswerKind` | yes | One of `VALUE`, `FORMULA`, `RANGE`, `CHART`, `PIVOT_TABLE`, `CONDITIONAL_FORMATTING`, `NAMED_RANGE`, `MACRO_OUTPUT`, `MANUAL`. |
| `label` | `label` | `String` |  | Shown to the candidate, e.g. "Q1: Total Revenue". |
| `expectedValue` | `expected_value` | `String` |  | — |
| `expectedFormula` | `expected_formula` | `String` |  | — |
| `tolerance` | `tolerance` | `BigDecimal` |  | Tolerance for a numeric VALUE comparison. |
| `points` | `points` | `BigDecimal` |  | Null falls back to equal weighting across all of the question's bindings. |

### `QuestionCalibration` — `question_calibrations`

Classic-test-theory statistics for one question over one measurement window — difficulty (proportion correct) and discrimination (correlation with total score) — computed only from FINAL results, since a provisional one hasn't settled yet.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `windowStartAt` | `window_start_at` | `Instant` | yes | — |
| `windowEndAt` | `window_end_at` | `Instant` | yes | — |
| `responseCount` | `response_count` | `long` | yes | Defaults to `0`. |
| `correctCount` | `correct_count` | `long` | yes | Defaults to `0`. |
| `difficultyIndex` | `difficulty_index` | `BigDecimal` |  | Proportion correct — 1.0 is a giveaway, 0.0 suggests a mis-keyed answer. |
| `discriminationIndex` | `discrimination_index` | `BigDecimal` |  | Point-biserial correlation with total attempt score. |
| `averageTimeSeconds` | `average_time_seconds` | `BigDecimal` |  | — |
| `flaggedReason` | `flagged_reason` | `CalibrationFlag` | yes | One of `TOO_EASY`, `TOO_HARD`, `NEGATIVE_DISCRIMINATION`, `MIS_KEY_SUSPECTED`, `NONE`. Defaults to `CalibrationFlag.NONE`. |
| `recommendedAction` | `recommended_action` | `CalibrationAction` | yes | One of `KEEP`, `REVIEW`, `RETIRE`, `REWEIGHT`. Defaults to `CalibrationAction.KEEP`. |
| `computedAt` | `computed_at` | `Instant` | yes | Defaults to `Instant.now()`. |

### `QuestionCategory` — `question_categories`

Subject taxonomy for the question bank — "Mathematics → Algebra → Quadratics". A tree rather than a flat list because sections draw pools by subject area, and a draw at a parent node must reach its children.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` | yes | — |
| `name` | `name` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `parentCategoryId` | `parent_category_id` | `Long` |  | References `question_categories.id`. |
| `path` | `path` | `String` |  | Materialised path ("/MATH/ALGEBRA/"), so a subtree query is one LIKE rather than recursion. |
| `depth` | `depth` | `int` | yes | Defaults to `0`. |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |

### `QuestionOption` — `question_options`

One selectable choice of a choice-type question. Options hang off QUESTIONS, not off EXAM_QUESTIONS: the choices are a property of the item itself, and reusing the item in a second exam must not clone them.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `label` | `label` | `String` |  | Display label — "A", "B", "1"… independent of storage order. |
| `content` | `content` | `String` | yes | — |
| `mediaPath` | `media_path` | `String` |  | — |
| `correct` | `is_correct` | `boolean` | yes | Defaults to `false`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `optionWeight` | `option_weight` | `BigDecimal` |  | Partial credit for MULTIPLE_CHOICE scoring; null falls back to all-or-nothing. |
| `feedback` | `feedback` | `String` |  | Shown to the candidate after grading when this option was picked. |
| `matchKey` | `match_key` | `String` |  | Right-hand value for MATCHING questions. |

### `QuestionTag` — `question_tags`

Free-form label on a bank question, orthogonal to the category tree — "past-paper-2024", "needs-diagram", "high-discrimination". A table, not a comma-separated column, because authors filter the bank by tag and a typo'd tag is worse than no tag.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `name` | `name` | `String` | yes | — |
| `description` | `description` | `String` |  | — |
| `usageCount` | `usage_count` | `int` | yes | Defaults to `0`. |

### `QuestionTagLink` — `question_tag_link`

Attachment of one tag to one bank question, resolving the many-to-many between QUESTIONS and QUESTION_TAGS. Tag search runs from the tag side, so the index on `question_tag_id` is the one that carries the load.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `questionId` | `question_id` | `Long` | yes | References `questions.id`. |
| `questionTagId` | `question_tag_id` | `Long` | yes | References `question_tags.id`. |


---

## Pre-Exam System Check

### `SystemCheck` — `system_checks`

One pre-flight run: the candidate's device tested against the exam's requirements before the paper is released. A run exists before any attempt does — that is the point of it — so `attempt` is filled in only if the candidate went on to start.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `examAttemptId` | `exam_attempt_id` | `Long` |  | Set once the candidate proceeds, linking the run to the sitting it cleared. |
| `status` | `status` | `SystemCheckStatus` | yes | One of `IN_PROGRESS`, `PASSED`, `PASSED_WITH_WARNINGS`, `FAILED`, `EXPIRED`. Defaults to `SystemCheckStatus.IN_PROGRESS`. |
| `attemptNo` | `attempt_no` | `int` | yes | Defaults to `1`. |
| `startedAt` | `started_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `completedAt` | `completed_at` | `Instant` |  | — |
| `validUntil` | `valid_until` | `Instant` |  | How long the clearance is good for; a stale run must be re-run before starting. |
| `deviceFingerprint` | `device_fingerprint` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `ipAddress` | `ip_address` | `String` |  | — |
| `downloadMbps` | `download_mbps` | `java.math.BigDecimal` |  | — |
| `uploadMbps` | `upload_mbps` | `java.math.BigDecimal` |  | — |
| `latencyMs` | `latency_ms` | `Integer` |  | — |
| `failedCheckCount` | `failed_check_count` | `int` | yes | Defaults to `0`. |
| `overridden` | `overridden` | `boolean` | yes | True when a proctor let the candidate through despite a failed item. Defaults to `false`. |
| `overriddenByUserId` | `overridden_by_user_id` | `Long` |  | References `users.id`. |
| `overrideReason` | `override_reason` | `String` |  | — |

### `SystemCheckItem` — `system_check_items`

Result of one item within a pre-flight run — camera, microphone, bandwidth, screen permission. A row per item rather than a column per item on the run, so the check catalogue can grow without a migration and each item keeps its own measurement.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `systemCheckId` | `system_check_id` | `Long` | yes | References `system_checks.id`. |
| `checkType` | `check_type` | `SystemCheckType` | yes | One of `BROWSER_COMPATIBILITY`, `CAMERA`, `MICROPHONE`, `SPEAKER`, `NETWORK_BANDWIDTH`, `SCREEN_SHARE_PERMISSION`, `FULLSCREEN`, `ENVIRONMENT_SCAN`, `OS_COMPATIBILITY`, `SECOND_SCREEN`. |
| `result` | `result` | `CheckResult` | yes | One of `NOT_RUN`, `PASSED`, `WARNING`, `FAILED`, `SKIPPED`. Defaults to `CheckResult.NOT_RUN`. |
| `required` | `is_required` | `boolean` | yes | Whether the exam policy makes this item blocking; a WARNING on an optional item is fine. Defaults to `true`. |
| `checkedAt` | `checked_at` | `Instant` |  | — |
| `message` | `message` | `String` |  | Human-readable outcome shown to the candidate, e.g. "No camera permission granted". |
| `measurement` | `measurement` | `String` |  | Measured detail — device labels, resolution, sample rate, packet loss. |
| `retryCount` | `retry_count` | `int` | yes | Defaults to `0`. |


---

## Identity Verification

### `IdentityVerification` — `identity_verifications`

A single identity check on a candidate: face match against the enrolment photo, an ID document scan, or a proctor's own confirmation. Many rows per attempt — a failed match followed by a manual override is two checks, and both belong in the record.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` |  | References `proctoring_sessions.id`. |
| `method` | `method` | `VerificationMethod` | yes | One of `FACE_MATCH`, `ID_DOCUMENT`, `MANUAL_PROCTOR`, `KNOWLEDGE_CHALLENGE`, `SECOND_FACTOR`. |
| `status` | `status` | `VerificationStatus` | yes | One of `PENDING`, `IN_PROGRESS`, `PASSED`, `FAILED`, `MANUAL_OVERRIDE`, `EXPIRED`. Defaults to `VerificationStatus.PENDING`. |
| `sequenceNo` | `sequence_no` | `int` | yes | Defaults to `1`. |
| `matchScore` | `match_score` | `BigDecimal` |  | Similarity score, 0.0000–1.0000, for the biometric methods. |
| `matchThreshold` | `match_threshold` | `BigDecimal` |  | Threshold in force at the time — a later retune must not change this verdict's meaning. |
| `livenessScore` | `liveness_score` | `BigDecimal` |  | — |
| `referencePhotoPath` | `reference_photo_path` | `String` |  | The enrolment photo compared against, as it stood at verification time. |
| `capturedEvidenceId` | `captured_evidence_id` | `Long` |  | The live capture or document scan used for the comparison. |
| `documentType` | `document_type` | `String` |  | — |
| `documentLast4` | `document_last4` | `String` |  | Last four characters only — the full document number is never stored. |
| `verifiedAt` | `verified_at` | `Instant` |  | — |
| `verifiedByUserId` | `verified_by_user_id` | `Long` |  | Set for MANUAL_PROCTOR checks and for overrides of a failed automatic one. |
| `failureReason` | `failure_reason` | `String` |  | — |
| `overrideReason` | `override_reason` | `String` |  | — |


---

## Consent & Privacy

### `ConsentRecord` — `consent_records`

One candidate's acceptance of one `PrivacyNotice` version for one attempt. Required before `ExamAttemptService.startAttempt` proceeds whenever the exam's `ProctoringPolicy.mode` is anything other than NONE — a proctored sitting without recorded consent is a legal defect, not merely a missing row.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `privacyNoticeId` | `privacy_notice_id` | `Long` | yes | References `privacy_notices.id`. |
| `noticeVersion` | `notice_version` | `String` | yes | — |
| `consentedAt` | `consented_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `withdrawnAt` | `withdrawn_at` | `Instant` |  | — |

### `PrivacyNotice` — `privacy_notices`

One version of the privacy/consent text a candidate must accept before a proctored sitting. Effective-dated and never edited in place, for the same reason a risk-factor weight isn't: a model or data-handling change big enough to need a new notice must not silently apply to a consent someone already gave under the old wording.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `noticeCode` | `notice_code` | `String` | yes | — |
| `version` | `version` | `String` | yes | — |
| `locale` | `locale` | `String` | yes | — |
| `bodyMarkdown` | `body_markdown` | `String` | yes | — |
| `effectiveFrom` | `effective_from` | `Instant` | yes | Defaults to `Instant.now()`. |
| `effectiveTo` | `effective_to` | `Instant` |  | — |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |


---

## Exam Session & Attempts

### `AnswerRevision` — `answer_revisions`

Append-only history of one answer. `AttemptAnswer` holds the current response so scoring stays a single-row read; every save — manual or auto-save — also lands here.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `attemptAnswerId` | `attempt_answer_id` | `Long` | yes | References `attempt_answers.id`. |
| `revisionNo` | `revision_no` | `int` | yes | — |
| `savedAt` | `saved_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `autoSaved` | `auto_saved` | `boolean` | yes | True when the platform saved on a timer rather than the candidate pressing save. Defaults to `false`. |
| `responseText` | `response_text` | `String` |  | — |
| `responseNumeric` | `response_numeric` | `java.math.BigDecimal` |  | — |
| `selectedOptionIds` | `selected_option_ids` | `String` |  | Snapshot of the selected QUESTION_OPTIONS ids at this revision. |
| `clientTimestamp` | `client_timestamp` | `Instant` |  | — |
| `ipAddress` | `ip_address` | `String` |  | — |

### `AnswerTimingAnomaly` — `answer_timing_anomalies`

An answer that was correct, or high-scoring, in implausibly little time — a stronger signal than a tab switch, and one that only exists once grading has happened, since a fast wrong answer is not suspicious.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `attemptAnswerId` | `attempt_answer_id` | `Long` | yes | References `attempt_answers.id`. |
| `examQuestionId` | `exam_question_id` | `Long` | yes | References `exam_questions.id`. |
| `expectedSeconds` | `expected_seconds` | `Integer` |  | — |
| `actualSeconds` | `actual_seconds` | `int` | yes | — |
| `ratio` | `ratio` | `BigDecimal` |  | — |
| `answerCorrect` | `answer_correct` | `boolean` | yes | Only fast *correct* or high-scoring answers are flagged — a fast wrong one is not a signal. |
| `revisionCount` | `revision_count` | `int` | yes | A single-revision correct answer in 4 seconds outranks one edited six times. |
| `anomalyType` | `anomaly_type` | `TimingAnomalyType` | yes | One of `TOO_FAST_CORRECT`, `TOO_FAST_HIGH_SCORE`, `ZERO_TIME_CORRECT`, `BURST_SUBMIT`. |
| `flaggedAt` | `flagged_at` | `Instant` | yes | Defaults to `Instant.now()`. |

### `AttemptAnswer` — `attempt_answers`

The candidate's response to one placed question. One row per (attempt, exam_question) — a revision overwrites in place and bumps `revisionCount`, so the table stays one-row-per question and remains cheap to score.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `examQuestionId` | `exam_question_id` | `Long` | yes | References `exam_questions.id`. |
| `responseText` | `response_text` | `String` |  | Free-text response for SHORT_ANSWER, ESSAY, FILL_IN_BLANK and CODE. |
| `responseNumeric` | `response_numeric` | `BigDecimal` |  | — |
| `attachmentPath` | `attachment_path` | `String` |  | — |
| `answeredAt` | `answered_at` | `Instant` |  | — |
| `revisionCount` | `revision_count` | `int` | yes | Defaults to `0`. |
| `timeSpentSeconds` | `time_spent_seconds` | `int` | yes | Defaults to `0`. |
| `flaggedByCandidate` | `flagged_by_candidate` | `boolean` | yes | Candidate-set "come back to this" marker, not an integrity flag. Defaults to `false`. |
| `gradingStatus` | `grading_status` | `GradingStatus` | yes | One of `NOT_REQUIRED`, `PENDING`, `IN_PROGRESS`, `GRADED`, `REGRADED`. Defaults to `GradingStatus.PENDING`. |
| `correct` | `is_correct` | `Boolean` |  | Null until graded — distinct from FALSE, which means "graded and wrong". |
| `pointsAwarded` | `points_awarded` | `BigDecimal` |  | — |
| `gradedByUserId` | `graded_by_user_id` | `Long` |  | References `users.id`. |
| `gradedAt` | `graded_at` | `Instant` |  | — |
| `graderComment` | `grader_comment` | `String` |  | — |

### `AttemptAnswerOption` — `attempt_answer_options`

One option the candidate selected for one answer. Resolves the many-to-many between ATTEMPT_ANSWERS and QUESTION_OPTIONS, needed because MULTIPLE_CHOICE, MATCHING and ORDERING all select more than one.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `attemptAnswerId` | `attempt_answer_id` | `Long` | yes | References `attempt_answers.id`. |
| `questionOptionId` | `question_option_id` | `Long` | yes | References `question_options.id`. |
| `sequenceNo` | `sequence_no` | `Integer` |  | Position the candidate placed this option in, for ordering and matching types. |

### `AttemptPauseRequest` — `attempt_pause_requests`

A request to pause a live attempt, and its approval. `com.example.test.proctoring.enums.ProctoringEventType` already has ATTEMPT_PAUSED/ATTEMPT_RESUMED, but an event log has no PENDING state to gate a live decision on — a proctor needs something to act on, not just a record that a pause happened. Same mutable-state-with-audit-trail shape this model already uses for IdentityVerification, SystemCheck and ReviewDecision.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `requestedAt` | `requested_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `requestedByUserId` | `requested_by_user_id` | `Long` |  | Null covers a system-initiated pause (e.g. a dropped connection), not candidate or proctor. |
| `reason` | `reason` | `String` |  | — |
| `status` | `status` | `PauseRequestStatus` | yes | One of `PENDING`, `APPROVED`, `DENIED`, `AUTO_APPROVED`. Defaults to `PauseRequestStatus.PENDING`. |
| `decidedByUserId` | `decided_by_user_id` | `Long` |  | References `users.id`. |
| `decidedAt` | `decided_at` | `Instant` |  | — |
| `decisionNote` | `decision_note` | `String` |  | — |
| `resumedAt` | `resumed_at` | `Instant` |  | Distinct from decidedAt — approval and the candidate actually resuming aren't simultaneous. |

### `AttemptResumption` — `attempt_resumptions`

A candidate reconnecting to a live attempt after their client dropped — a crash, a network drop, a reboot. A resume is a session-token rotation, not a second concurrent session: the previous token is invalidated in the same operation that issues the new one.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `reason` | `reason` | `ResumptionReason` | yes | One of `BROWSER_CRASH`, `NETWORK_DROP`, `DEVICE_REBOOT`, `POWER_LOSS`, `PROCTOR_INITIATED`. |
| `previousSessionTokenHash` | `previous_session_token_hash` | `String` |  | Hash of the token being invalidated — never the token itself. |
| `newSessionTokenHash` | `new_session_token_hash` | `String` |  | — |
| `resumedAt` | `resumed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `timeAwaySeconds` | `time_away_seconds` | `long` | yes | From the attempt's last heartbeat to this resume — added to ExamAttempt.pausedSeconds. |
| `autoApproved` | `auto_approved` | `boolean` | yes | True when the gap was under the policy's grace window and needed no proctor decision. Defaults to `false`. |
| `approvedByUserId` | `approved_by_user_id` | `Long` |  | References `users.id`. |
| `riskEventId` | `risk_event_id` | `Long` |  | Set when the gap exceeded the grace window and was itself scored as a risk factor. |

### `CodeExecutionResult` — `code_execution_results`

Outcome of running one CODE `AttemptAnswer` against one of its question's test cases. Needed because `AttemptAnswer` is a single-row summary — correct for every other question type — and once a question has several test cases, "which ones passed" has nowhere else to live. Same reason `AttemptAnswerOption` exists as a child of `AttemptAnswer`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `attemptAnswerId` | `attempt_answer_id` | `Long` | yes | References `attempt_answers.id`. |
| `codeTestCaseId` | `code_test_case_id` | `Long` | yes | References `code_test_cases.id`. |
| `passed` | `passed` | `boolean` | yes | Defaults to `false`. |
| `actualOutput` | `actual_output` | `String` |  | — |
| `runtimeMs` | `runtime_ms` | `Integer` |  | — |
| `memoryKb` | `memory_kb` | `Integer` |  | — |
| `errorMessage` | `error_message` | `String` |  | — |
| `executedAt` | `executed_at` | `Instant` |  | — |

### `ExcelGradeResult` — `excel_grade_results`

Outcome of grading one SPREADSHEET `AttemptAnswer` against one of its question's `ExcelCellBinding`s. Needed for the same reason `CodeExecutionResult` exists for CODE: `AttemptAnswer` is a single-row summary, and once a question grades several cells independently, "which ones were correct" has nowhere else to live.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `attemptAnswerId` | `attempt_answer_id` | `Long` | yes | References `attempt_answers.id`. |
| `excelCellBindingId` | `excel_cell_binding_id` | `Long` | yes | References `excel_cell_bindings.id`. |
| `graderType` | `grader_type` | `ExcelAnswerKind` | yes | Same values as `ExcelCellBinding.answerKind`. Carried here too because the two can diverge — an automated CHART/PIVOT comparison that came back inconclusive falls back to `MANUAL` review, and this field records that the fallback happened. |
| `correct` | `is_correct` | `boolean` | yes | Defaults to `false`. |
| `pointsAwarded` | `points_awarded` | `BigDecimal` |  | — |
| `gradedValue` | `graded_value` | `String` |  | What the candidate's cell actually evaluated to at grading time. |
| `gradedFormula` | `graded_formula` | `String` |  | — |
| `graderNote` | `grader_note` | `String` |  | — |
| `gradedAt` | `graded_at` | `Instant` |  | — |

### `ExamAttempt` — `exam_attempts`

One sitting of one exam by one candidate — the spine of the whole model. Everything that happens during or after an exam (answers, supervision, risk, review, results) hangs off an attempt, not off the exam or the candidate, because the attempt is the only thing that is unique per event.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `examAssignmentId` | `exam_assignment_id` | `Long` |  | Null for open exams that need no invitation. |
| `attemptNo` | `attempt_no` | `int` | yes | Defaults to `1`. |
| `status` | `status` | `AttemptStatus` | yes | One of `NOT_STARTED`, `IN_PROGRESS`, `PAUSED`, `SUBMITTED`, `AUTO_SUBMITTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `GRADED`. Defaults to `AttemptStatus.NOT_STARTED`. |
| `startedAt` | `started_at` | `Instant` |  | — |
| `expiresAt` | `expires_at` | `Instant` |  | Hard deadline computed at start from duration + accommodations; drives auto-submit. |
| `submittedAt` | `submitted_at` | `Instant` |  | — |
| `lastActivityAt` | `last_activity_at` | `Instant` |  | — |
| `sessionTokenHash` | `session_token_hash` | `String` |  | Hash of the attempt-scoped session token. Bound to the attempt rather than to the login session, so a second tab or a second device cannot drive the same sitting. |
| `lastHeartbeatAt` | `last_heartbeat_at` | `Instant` |  | — |
| `timeSpentSeconds` | `time_spent_seconds` | `long` | yes | Defaults to `0L`. |
| `pausedSeconds` | `paused_seconds` | `long` | yes | Cumulative paused time, so time_spent stays honest across proctor-ordered pauses. Defaults to `0L`. |
| `currentSectionId` | `current_section_id` | `Long` |  | References `exam_sections.id`. |
| `examVersion` | `exam_version` | `int` | yes | Snapshot of the exam version delivered, so a later re-publish cannot rewrite this sitting. Defaults to `1`. |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `invalidatedAt` | `invalidated_at` | `Instant` |  | — |
| `invalidationReason` | `invalidation_reason` | `String` |  | — |

### `QuestionFormFingerprint` — `question_form_fingerprints`

A hash of the shuffled paper one attempt actually received, computed once from its persisted `QuestionState` rows at start. Two attempts in the same exam sharing a hash is a strong collusion signal — or a broken shuffler — and something a shuffle *rule* can never surface, because two candidates can independently draw the same rule and land on different forms.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `formHash` | `form_hash` | `String` | yes | SHA-256 over the ordered (examQuestionId, displayOrder, optionOrder) tuples delivered. |
| `questionCount` | `question_count` | `int` | yes | — |
| `sectionCount` | `section_count` | `int` | yes | — |
| `computedAt` | `computed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `collisionCount` | `collision_count` | `int` | yes | Defaults to `0`. |
| `collisionAttemptIds` | `collision_attempt_ids` | `String` |  | — |

### `QuestionState` — `question_states`

Delivery and navigation state of one question within one attempt: the shuffled order this candidate saw it in, whether it has been viewed, how long it was on screen.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `examQuestionId` | `exam_question_id` | `Long` | yes | References `exam_questions.id`. |
| `state` | `state` | `QuestionStateType` | yes | One of `UNSEEN`, `VIEWED`, `ANSWERED`, `FLAGGED`, `SKIPPED`, `LOCKED`. Defaults to `QuestionStateType.UNSEEN`. |
| `displayOrder` | `display_order` | `int` | yes | Position in the shuffled paper this candidate received. |
| `optionOrder` | `option_order` | `String` |  | Order the options were rendered in, when the placement shuffles them. |
| `firstViewedAt` | `first_viewed_at` | `Instant` |  | — |
| `lastViewedAt` | `last_viewed_at` | `Instant` |  | — |
| `viewCount` | `view_count` | `int` | yes | Defaults to `0`. |
| `timeOnQuestionSeconds` | `time_on_question_seconds` | `int` | yes | Defaults to `0`. |
| `lockedAt` | `locked_at` | `Instant` |  | — |


---

## Excel Runtime

### `ExcelSession` — `excel_sessions`

One candidate's live Excel runtime session for an attempt — the sandboxed container instance that holds their working copy of the workbook. One per `ExamAttempt`, the same 1:1 shape as `ProctoringSession`. Periodic and final workbook snapshots are not a separate table here — they reuse `EvidenceFile` (kind `EXCEL_WORKBOOK_SNAPSHOT` / `EXCEL_FINAL_WORKBOOK`), keyed by this session's `proctoringSessionId`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to a random `UUID`. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` |  | References `proctoring_sessions.id`. Null when the exam's proctoring mode is NONE. |
| `engineUsed` | `engine_used` | `ExcelRuntimeEngine` |  | One of `LIBREOFFICE`, `ONLYOFFICE`, `SHEETJS_HYPERFORMULA`, `OFFICE_SCRIPTS`. |
| `sandboxContainerId` | `sandbox_container_id` | `String` |  | — |
| `status` | `status` | `ExcelSessionStatus` | yes | One of `PENDING`, `ACTIVE`, `SUBMITTED`, `CRASHED`, `RECOVERED`. Defaults to `ExcelSessionStatus.PENDING`. |
| `startedAt` | `started_at` | `Instant` |  | — |
| `submittedAt` | `submitted_at` | `Instant` |  | — |
| `finalChecksumSha256` | `final_checksum_sha256` | `String` |  | SHA-256 of the workbook exactly as submitted — compared against the runtime's own replay to set `integrityStatus`. |
| `integrityStatus` | `integrity_status` | `ExcelIntegrityStatus` |  | One of `VALID`, `TAMPERED`, `INCONCLUSIVE`. |

### `ExcelCellEdit` — `excel_cell_edits`

Append-only history of one cell edit during an `ExcelSession` — the same role `AnswerRevision` plays for a regular attempt answer. Recovers a workbook after a crash mid-session, and is what a diff viewer or replay steps through in `sequenceNo` order.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `excelSessionId` | `excel_session_id` | `Long` | yes | References `excel_sessions.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `sheetName` | `sheet_name` | `String` | yes | — |
| `cellRef` | `cell_ref` | `String` | yes | — |
| `oldValue` | `old_value` | `String` |  | — |
| `newValue` | `new_value` | `String` |  | — |
| `oldFormula` | `old_formula` | `String` |  | — |
| `newFormula` | `new_formula` | `String` |  | — |
| `editedAt` | `edited_at` | `Instant` | yes | Defaults to `Instant.now()`. |

### `ExcelSheetOperation` — `excel_sheet_operations`

A structural change to a sheet within an `ExcelSession` — insert, delete, rename, hide, protect — kept separate from `ExcelCellEdit` because it isn't a value change on any one cell.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `excelSessionId` | `excel_session_id` | `Long` | yes | References `excel_sessions.id`. |
| `operationType` | `operation_type` | `ExcelSheetOperationType` | yes | One of `INSERT`, `DELETE`, `RENAME`, `HIDE`, `UNHIDE`, `PROTECT`, `UNPROTECT`. |
| `sheetName` | `sheet_name` | `String` | yes | — |
| `occurredAt` | `occurred_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `detail` | `detail` | `String` |  | Old name for a RENAME, or other operation-specific detail. |

### `ExcelMacroExecution` — `excel_macro_executions`

One macro run inside an `ExcelSession`. `allowed` records whether the macro was on the exam's/question's whitelist at execution time — a run that wasn't is still logged, not silently dropped, since a blocked-but-attempted run is itself the proctoring signal behind `ProctoringEventType.EXCEL_MACRO_RUN`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `excelSessionId` | `excel_session_id` | `Long` | yes | References `excel_sessions.id`. |
| `macroName` | `macro_name` | `String` | yes | — |
| `allowed` | `allowed` | `boolean` | yes | Defaults to `true`. |
| `inputSummary` | `input_summary` | `String` |  | — |
| `outputSummary` | `output_summary` | `String` |  | — |
| `durationMs` | `duration_ms` | `Integer` |  | — |
| `exitCode` | `exit_code` | `Integer` |  | — |
| `executedAt` | `executed_at` | `Instant` | yes | Defaults to `Instant.now()`. |


---

## Proctoring Sessions

### `DeviceSession` — `device_sessions`

One device connected to a proctoring session — normally the exam machine, optionally a phone providing a second camera angle. Many rows per session rather than columns on the session because a reconnect, a browser change or a second device each need their own environment record. Two overlapping rows with different fingerprints is a strong cheating signal.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `deviceFingerprint` | `device_fingerprint` | `String` | yes | — |
| `primary` | `is_primary` | `boolean` | yes | Defaults to `true`. |
| `deviceLabel` | `device_label` | `String` |  | — |
| `operatingSystem` | `operating_system` | `String` |  | — |
| `osVersion` | `os_version` | `String` |  | — |
| `browser` | `browser` | `String` |  | — |
| `browserVersion` | `browser_version` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `screenResolution` | `screen_resolution` | `String` |  | — |
| `monitorCount` | `monitor_count` | `Integer` |  | — |
| `cameraLabel` | `camera_label` | `String` |  | — |
| `microphoneLabel` | `microphone_label` | `String` |  | — |
| `ipAddress` | `ip_address` | `String` |  | — |
| `geoCountry` | `geo_country` | `String` |  | — |
| `geoCity` | `geo_city` | `String` |  | — |
| `timezoneOffsetMinutes` | `timezone_offset_minutes` | `Integer` |  | — |
| `vpnSuspected` | `vpn_suspected` | `boolean` | yes | Defaults to `false`. |
| `virtualMachineSuspected` | `virtual_machine_suspected` | `boolean` | yes | Defaults to `false`. |
| `connectedAt` | `connected_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `disconnectedAt` | `disconnected_at` | `Instant` |  | — |

### `DeviceTrustRecord` — `device_trust_records`

Running tally of one device fingerprint's sightings within one exam. A single device sat by several distinct candidates in the same exam window is a stronger integrity signal than anything one `DeviceSession` row alone can show — it only emerges by looking across attempts, which is exactly what a per-session table can't do.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `deviceFingerprint` | `device_fingerprint` | `String` | yes | — |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `firstSeenAt` | `first_seen_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `lastSeenAt` | `last_seen_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `sightingCount` | `sighting_count` | `int` | yes | Defaults to `1`. |
| `distinctCandidateCount` | `distinct_candidate_count` | `int` | yes | Defaults to `1`. |
| `distinctCandidateIds` | `distinct_candidate_ids` | `String` |  | JSON list of User.id values — an id, never a name or photo. |
| `trustLevel` | `trust_level` | `DeviceTrustLevel` | yes | One of `UNKNOWN`, `KNOWN`, `SHARED_SUSPECTED`, `SHARED_CONFIRMED`, `BLOCKED`. Defaults to `DeviceTrustLevel.UNKNOWN`. |
| `flaggedAt` | `flagged_at` | `Instant` |  | — |
| `flaggedReason` | `flagged_reason` | `String` |  | — |

### `EvidenceAccessLog` — `evidence_access_logs`

Who looked at which piece of evidence, when, and why. Webcam footage of a person's home is the most sensitive data this system holds, so every read is logged — not just every change.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `evidenceFileId` | `evidence_file_id` | `Long` | yes | References `evidence_files.id`. |
| `actorUserId` | `actor_user_id` | `Long` | yes | References `users.id`. |
| `action` | `action` | `EvidenceAccessAction` | yes | One of `VIEW`, `STREAM`, `DOWNLOAD`, `EXPORT`, `SHARE_LINK`, `PURGE`. |
| `accessedAt` | `accessed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `contextReference` | `context_reference` | `String` |  | The case or ticket the access was made under; unattributed access is a policy breach. |
| `purpose` | `purpose` | `String` |  | — |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `granted` | `was_granted` | `boolean` | yes | Defaults to `true`. |
| `deniedReason` | `denied_reason` | `String` |  | — |

### `EvidenceCustodyRecord` — `evidence_custody_records`

One step in an evidence file's chain of custody. `EvidenceFile#getChecksumSha256()` proves the bytes are unaltered at rest; this table proves they were unaltered at every step in between — captured, hashed, uploaded, verified — which a storage checksum alone can't show.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `evidenceFileId` | `evidence_file_id` | `Long` | yes | References `evidence_files.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `transition` | `transition` | `CustodyTransition` | yes | One of `CAPTURED`, `HASHED`, `UPLOADED`, `VERIFIED`, `ACCESSED`, `EXPORTED`, `PURGED`. |
| `actorUserId` | `actor_user_id` | `Long` |  | References `users.id`. |
| `actorApiClientId` | `actor_api_client_id` | `Long` |  | References `api_clients.id`. |
| `occurredAt` | `occurred_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `checksumBefore` | `checksum_before` | `String` |  | — |
| `checksumAfter` | `checksum_after` | `String` |  | Must equal checksumBefore for every transition except PURGED — a mismatch means tampering. |
| `storagePathBefore` | `storage_path_before` | `String` |  | — |
| `storagePathAfter` | `storage_path_after` | `String` |  | — |
| `note` | `note` | `String` |  | — |

### `EvidenceFile` — `evidence_files`

Metadata for a captured artefact — webcam frame, screen clip, audio, ID scan. Only the pointer and the checksum live in the database; bytes go to object storage.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `proctoringEventId` | `proctoring_event_id` | `Long` |  | References `proctoring_events.id`. |
| `kind` | `kind` | `EvidenceKind` | yes | One of `WEBCAM_SNAPSHOT`, `WEBCAM_CLIP`, `SCREEN_SNAPSHOT`, `SCREEN_CLIP`, `AUDIO_CLIP`, `ID_DOCUMENT`, `ENVIRONMENT_SCAN`, `KEYSTROKE_LOG`, `EVENT_LOG_BUNDLE`, `EXCEL_WORKBOOK_SNAPSHOT`, `EXCEL_FINAL_WORKBOOK`, `EXCEL_EDIT_LOG_BUNDLE`, `EXCEL_DIFF_REPORT`. |
| `storagePath` | `storage_path` | `String` | yes | — |
| `fileName` | `file_name` | `String` |  | — |
| `contentType` | `content_type` | `String` |  | — |
| `sizeBytes` | `size_bytes` | `Long` |  | — |
| `checksumSha256` | `checksum_sha256` | `String` |  | Tamper check — evidence that cannot be shown to be unaltered is not evidence. |
| `capturedAt` | `captured_at` | `Instant` | yes | — |
| `offsetMs` | `offset_ms` | `Long` |  | — |
| `durationMs` | `duration_ms` | `Long` |  | — |
| `widthPx` | `width_px` | `Integer` |  | — |
| `heightPx` | `height_px` | `Integer` |  | — |
| `uploadStatus` | `upload_status` | `EvidenceUploadStatus` | yes | One of `PENDING`, `UPLOADING`, `UPLOADED`, `FAILED`, `QUARANTINED`, `PURGED`. Defaults to `EvidenceUploadStatus.PENDING`. |
| `uploadedAt` | `uploaded_at` | `Instant` |  | — |
| `retentionUntil` | `retention_until` | `Instant` |  | — |
| `purgedAt` | `purged_at` | `Instant` |  | — |

### `LiveSessionStatus` — `live_session_status`

Current live state of one session, as the invigilator's wall of tiles reads it: camera, mic and screen health, connection quality, latest risk score, question the candidate is on.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `candidateStatus` | `candidate_status` | `CandidateLiveStatus` | yes | The candidate's own behavioral state — distinct from the stream/connection health below. Defaults to `CandidateLiveStatus.ACTIVE`. |
| `connectionStatus` | `connection_status` | `ConnectionStatus` | yes | One of `CONNECTED`, `UNSTABLE`, `RECONNECTING`, `DISCONNECTED`. Defaults to `ConnectionStatus.CONNECTED`. |
| `cameraStatus` | `camera_status` | `StreamStatus` | yes | One of `UNAVAILABLE`, `ACTIVE`, `DEGRADED`, `INTERRUPTED`, `STOPPED`. Defaults to `StreamStatus.UNAVAILABLE`. |
| `microphoneStatus` | `microphone_status` | `StreamStatus` | yes | One of `UNAVAILABLE`, `ACTIVE`, `DEGRADED`, `INTERRUPTED`, `STOPPED`. Defaults to `StreamStatus.UNAVAILABLE`. |
| `screenStatus` | `screen_status` | `StreamStatus` | yes | One of `UNAVAILABLE`, `ACTIVE`, `DEGRADED`, `INTERRUPTED`, `STOPPED`. Defaults to `StreamStatus.UNAVAILABLE`. |
| `fullscreen` | `is_fullscreen` | `boolean` | yes | Defaults to `false`. |
| `windowFocused` | `is_window_focused` | `boolean` | yes | Defaults to `true`. |
| `riskScore` | `risk_score` | `BigDecimal` |  | Mirrored from the latest RISK_ASSESSMENTS row so the monitor wall reads one table. |
| `riskLevel` | `risk_level` | `RiskLevel` |  | One of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `openAlertCount` | `open_alert_count` | `int` | yes | Defaults to `0`. |
| `currentQuestionNo` | `current_question_no` | `Integer` |  | — |
| `answeredCount` | `answered_count` | `int` | yes | Defaults to `0`. |
| `remainingSeconds` | `remaining_seconds` | `Integer` |  | — |
| `lastHeartbeatAt` | `last_heartbeat_at` | `Instant` |  | — |
| `lastEventAt` | `last_event_at` | `Instant` |  | — |
| `networkLatencyMs` | `network_latency_ms` | `Integer` |  | — |
| `websocketId` | `websocket_id` | `String` |  | — |
| `updatedByNode` | `updated_by_node` | `String` |  | — |

### `ProctorAction` — `proctor_actions`

A live human proctor's action on a session — warn, message, flag, pause, terminate — with the same audit standard a reviewer's decision already gets. Writing only a `ProctoringEvent` with `source = HUMAN_PROCTOR` would record *that* something happened but not the authority behind it; this table is where the reason, and — for a termination under a two-person-approval policy — the second signature, live.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `proctorUserId` | `proctor_user_id` | `Long` | yes | References `users.id`. |
| `proctorShiftId` | `proctor_shift_id` | `Long` |  | References `proctor_shifts.id`. |
| `actionType` | `action_type` | `ProctorActionType` | yes | One of `WARN`, `MESSAGE`, `FLAG`, `PAUSE`, `RESUME`, `TERMINATE`, `ESCALATE`, `NO_ACTION`. |
| `reason` | `reason` | `String` |  | Required for WARN, FLAG, PAUSE, TERMINATE — a silent action is an unauditable one. |
| `proctoringEventId` | `proctoring_event_id` | `Long` |  | References `proctoring_events.id`. |
| `occurredAt` | `occurred_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `candidateNotified` | `candidate_notified` | `boolean` | yes | Defaults to `false`. |
| `supervisorApprovalUserId` | `supervisor_approval_user_id` | `Long` |  | Required for TERMINATE when the exam's policy demands two-person approval. |
| `supervisorApprovedAt` | `supervisor_approved_at` | `Instant` |  | — |

### `ProctorShift` — `proctor_shifts`

One proctor's assignment window over one session, in LIVE_PROCTOR/HYBRID mode where coverage rotates across a shift change. `ProctoringSession.assignedProctorUserId` is a mirror of whichever shift is currently open, not the source of truth — this table is, since overwriting that single column on handover would lose who was watching when.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `proctorUserId` | `proctor_user_id` | `Long` | yes | References `users.id`. |
| `shiftStartAt` | `shift_start_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `shiftEndAt` | `shift_end_at` | `Instant` |  | Null while this proctor is the one currently on duty. |
| `handoverNote` | `handover_note` | `String` |  | What the outgoing proctor wants the incoming one to watch. Required on a handover. |
| `eventsDuringShift` | `events_during_shift` | `int` | yes | Defaults to `0`. |
| `flagsRaisedDuringShift` | `flags_raised_during_shift` | `int` | yes | Defaults to `0`. |
| `outcome` | `outcome` | `ProctorShiftOutcome` |  | One of `COMPLETED`, `HANDED_OVER`, `ESCALATED`, `ABANDONED`. |

### `ProctoringEvent` — `proctoring_events`

One observation during a session — a tab switch, a missed heartbeat, a proctor's manual flag, or the summary of an AI finding. Append-only and the highest-volume table in the model, hence the running counters on the session and the `(session, occurred_at)` index.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `deviceSessionId` | `device_session_id` | `Long` |  | Which device raised it, when more than one was connected. |
| `eventType` | `event_type` | `ProctoringEventType` | yes | One of `SESSION_STARTED`, `SESSION_ENDED`, `CONSENT_ACCEPTED`, `IDENTITY_CHECK_PASSED`, `IDENTITY_CHECK_FAILED`, `ENVIRONMENT_SCAN_COMPLETED`, `HEARTBEAT_MISSED`, `NETWORK_DROP`, `CAMERA_BLOCKED`, `CAMERA_DISCONNECTED`, `MICROPHONE_MUTED`, `SCREEN_SHARE_STARTED`, `SCREEN_SHARE_STOPPED`, `FULLSCREEN_ENTERED`, `FULLSCREEN_EXITED`, `TAB_SWITCHED`, `WINDOW_BLURRED`, `WINDOW_FOCUSED`, `BROWSER_HIDDEN`, `BROWSER_VISIBLE`, `COPY_ATTEMPT`, `PASTE_ATTEMPT`, `PRINT_ATTEMPT`, `RIGHT_CLICK_BLOCKED`, `KEYBOARD_SHORTCUT_BLOCKED`, `SUSPICIOUS_KEY_SEQUENCE`, `DEV_TOOLS_OPENED`, `VIRTUAL_MACHINE_SUSPECTED`, `MULTIPLE_DISPLAYS_DETECTED`, `DEVICE_CHANGED`, `NO_FACE_DETECTED`, `MULTIPLE_FACES_DETECTED`, `FACE_MISMATCH`, `GAZE_OFF_SCREEN`, `PROHIBITED_OBJECT_DETECTED`, `VOICE_DETECTED`, `ABNORMAL_BEHAVIOR`, `PROCTOR_MESSAGE`, `PROCTOR_WARNING`, `PROCTOR_MANUAL_FLAG`, `TIME_WARNING_ISSUED`, `ATTEMPT_SUBMITTED`, `ATTEMPT_AUTO_SUBMITTED`, `ATTEMPT_PAUSED`, `ATTEMPT_RESUMED`, `ATTEMPT_TERMINATED`, `EXCEL_CELL_EDIT`, `EXCEL_BULK_PASTE`, `EXCEL_SHEET_OP`, `EXCEL_MACRO_RUN`, `EXCEL_EXTERNAL_LINK`, `EXCEL_ADDIN_LOAD`, `EXCEL_FOCUS_LOST`, `EXCEL_IDLE_ANSWER_CELL`, `EXCEL_ENGINE_ERROR`, `EXCEL_INTEGRITY_FAIL`. |
| `severity` | `severity` | `EventSeverity` | yes | One of `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Defaults to `EventSeverity.INFO`. |
| `source` | `source` | `EventSource` | yes | One of `BROWSER_AGENT`, `AI_ENGINE`, `HUMAN_PROCTOR`, `SYSTEM`. Defaults to `EventSource.BROWSER_AGENT`. |
| `occurredAt` | `occurred_at` | `Instant` | yes | — |
| `receivedAt` | `received_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `offsetMs` | `offset_ms` | `Long` |  | Milliseconds from attempt start, for timeline playback. |
| `durationMs` | `duration_ms` | `Long` |  | — |
| `description` | `description` | `String` |  | — |
| `payload` | `payload` | `String` |  | Type-specific detail (tab URL host, key pressed, device ids…). Shape varies by event type. |
| `autoAction` | `auto_action` | `AutoAction` | yes | One of `NONE`, `LOG_ONLY`, `WARN_CANDIDATE`, `PAUSE_ATTEMPT`, `LOCK_SCREEN`, `NOTIFY_PROCTOR`, `TERMINATE_ATTEMPT`. Defaults to `AutoAction.NONE`. |
| `acknowledgedAt` | `acknowledged_at` | `Instant` |  | Set when a proctor or reviewer dismisses the event as benign. |
| `idempotencyKey` | `idempotency_key` | `String` |  | — |

### `ProctoringSession` — `proctoring_sessions`

The supervision envelope around one attempt: one session per attempt, created when the candidate passes the pre-flight checks and closed when the attempt ends. It owns the live-monitoring state (heartbeat, identity verification, assigned proctor); the observations themselves live in `ProctoringEvent`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `status` | `status` | `ProctoringSessionStatus` | yes | One of `PENDING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `TERMINATED`, `FAILED`. Defaults to `ProctoringSessionStatus.PENDING`. |
| `mode` | `mode` | `ProctoringMode` | yes | Copied from the exam policy at start; the policy may change afterwards, this may not. Defaults to `ProctoringMode.AI_ONLY`. |
| `assignedProctorUserId` | `assigned_proctor_user_id` | `Long` |  | Set only in LIVE_PROCTOR / HYBRID mode. |
| `startedAt` | `started_at` | `Instant` |  | — |
| `endedAt` | `ended_at` | `Instant` |  | — |
| `consentAcceptedAt` | `consent_accepted_at` | `Instant` |  | — |
| `identityVerified` | `identity_verified` | `boolean` | yes | Defaults to `false`. |
| `identityVerifiedAt` | `identity_verified_at` | `Instant` |  | — |
| `identityVerifiedByUserId` | `identity_verified_by_user_id` | `Long` |  | References `users.id`. |
| `environmentScanCompletedAt` | `environment_scan_completed_at` | `Instant` |  | — |
| `lastHeartbeatAt` | `last_heartbeat_at` | `Instant` |  | Last agent ping. A stale value is itself an integrity signal, so it is stored, not derived. |
| `heartbeatMissCount` | `heartbeat_miss_count` | `int` | yes | Defaults to `0`. |
| `eventCount` | `event_count` | `int` | yes | Running counters, maintained on event ingest to keep monitoring dashboards off aggregates. Defaults to `0`. |
| `criticalEventCount` | `critical_event_count` | `int` | yes | Defaults to `0`. |
| `warningIssuedCount` | `warning_issued_count` | `int` | yes | Defaults to `0`. |
| `terminatedAt` | `terminated_at` | `Instant` |  | — |
| `terminationReason` | `termination_reason` | `String` |  | — |


---

## AI Detection

### `AiDetection` — `ai_detections`

One inference produced by an AI model over one captured frame or time window. Holds everything common to every model — which model ran, how confident it was, what it analysed — while the findings themselves live in the subtype tables.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `detectionType` | `detection_type` | `DetectionType` | yes | Which detail table holds the findings for this row: FACE, OBJECT, BEHAVIOR or AUDIO. A stored column, so a reader knows where to look without probing four tables. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `proctoringEventId` | `proctoring_event_id` | `Long` |  | The event this inference raised, if it crossed the reporting threshold. Null for routine frames that were analysed and found unremarkable — those are kept for the baseline. |
| `evidenceFileId` | `evidence_file_id` | `Long` |  | The frame or clip the inference ran over, when it was retained. |
| `aiModelVersionId` | `ai_model_version_id` | `Long` |  | Registry entry for the model version that produced this row. Optional because a client-side model may report a version the registry has not seen; the name/version strings below are the authoritative snapshot either way. |
| `modelName` | `model_name` | `String` | yes | — |
| `modelVersion` | `model_version` | `String` | yes | — |
| `confidence` | `confidence` | `BigDecimal` | yes | 0.0000–1.0000. Below the model's threshold the row is advisory only. |
| `capturedAt` | `captured_at` | `Instant` | yes | — |
| `processedAt` | `processed_at` | `Instant` |  | — |
| `processingTimeMs` | `processing_time_ms` | `Integer` |  | — |
| `offsetMs` | `offset_ms` | `Long` |  | — |
| `anomaly` | `is_anomaly` | `boolean` | yes | True when this row is what a human should look at, as judged by the model. Defaults to `false`. |
| `rawOutput` | `raw_output` | `String` |  | Verbatim model output, kept for re-scoring and for defending a decision on appeal. |

### `AudioDetection` — `audio_detections`

Audio-model findings over a listening window: speech while alone in the room, a second voice, sustained background noise, or the microphone going silent when it should not.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiDetectionId` | `ai_detection_id` | `Long` | yes | The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. |
| `audioEventType` | `audio_event_type` | `AudioEventType` | yes | One of `SPEECH_DETECTED`, `MULTIPLE_VOICES`, `BACKGROUND_NOISE`, `AUDIO_INTERRUPTION`, `PROLONGED_SILENCE`, `SUSPICIOUS_AUDIO`. |
| `windowStartAt` | `window_start_at` | `Instant` | yes | — |
| `windowEndAt` | `window_end_at` | `Instant` | yes | — |
| `durationMs` | `duration_ms` | `Long` |  | — |
| `speakerCount` | `speaker_count` | `Integer` |  | Distinct voices heard. Greater than one is a strong impersonation or coaching signal. |
| `unknownSpeaker` | `unknown_speaker` | `boolean` | yes | True when a voice was heard that does not match the candidate's enrolled voiceprint. Defaults to `false`. |
| `peakDb` | `peak_db` | `BigDecimal` |  | — |
| `averageDb` | `average_db` | `BigDecimal` |  | — |
| `signalToNoiseRatio` | `signal_to_noise_ratio` | `BigDecimal` |  | — |
| `speechRatio` | `speech_ratio` | `BigDecimal` |  | — |
| `languageCode` | `language_code` | `String` |  | — |
| `transcriptExcerpt` | `transcript_excerpt` | `String` |  | Short excerpt retained only when the exam policy allows it. |

### `BehaviorDetection` — `behavior_detections`

Behavior-model findings over a time window rather than a single frame — looking away repeatedly, leaving the seat, talking, typing in bursts that do not match the candidate's own baseline.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiDetectionId` | `ai_detection_id` | `Long` | yes | The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. |
| `behaviorType` | `behavior_type` | `BehaviorType` | yes | One of `LOOKING_AWAY`, `LEAVING_SEAT`, `FACE_OUT_OF_FRAME`, `TALKING`, `BACKGROUND_VOICE`, `SUSPICIOUS_MOVEMENT`, `RAPID_TYPING_BURST`, `PROLONGED_IDLE`, `SCREEN_OCCLUSION`, `REPEATED_TAB_SWITCH`. |
| `windowStartAt` | `window_start_at` | `Instant` | yes | — |
| `windowEndAt` | `window_end_at` | `Instant` | yes | — |
| `durationMs` | `duration_ms` | `Long` |  | — |
| `occurrenceCount` | `occurrence_count` | `int` | yes | Defaults to `1`. |
| `intensityScore` | `intensity_score` | `BigDecimal` |  | How pronounced the behavior was within the window, 0.0000–1.0000. |
| `baselineDeviation` | `baseline_deviation` | `BigDecimal` |  | Standard deviations from this candidate's own calibration baseline. Absolute thresholds misjudge people who simply fidget, so the deviation is what the risk model consumes. |
| `audioRelated` | `audio_related` | `boolean` | yes | Defaults to `false`. |

### `BoundingBox` *(embedded value object)*

Region of interest inside an analysed frame, stored as normalised (0..1) coordinates so it survives a change of capture resolution.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `x` | `box_x` | `Double` |  | — |
| `y` | `box_y` | `Double` |  | — |
| `width` | `box_width` | `Double` |  | — |
| `height` | `box_height` | `Double` |  | — |

### `FaceDetection` — `face_detections`

Face-model findings for one frame: who is in shot, how many, where they are looking, and whether the face is live rather than a photo held to the camera.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiDetectionId` | `ai_detection_id` | `Long` | yes | The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. |
| `faceCount` | `face_count` | `int` | yes | 0 means the candidate left the frame; >1 means someone else is present. |
| `identityMatchScore` | `identity_match_score` | `BigDecimal` |  | Similarity against the enrolment photo, 0.0000–1.0000. |
| `identityMatched` | `identity_matched` | `Boolean` |  | Null when no comparison was possible (no face, or no enrolment photo). |
| `gazeDirection` | `gaze_direction` | `GazeDirection` |  | One of `ON_SCREEN`, `LEFT`, `RIGHT`, `UP`, `DOWN`, `AWAY`, `UNKNOWN`. Defaults to `GazeDirection.UNKNOWN`. |
| `gazeOffScreenMs` | `gaze_off_screen_ms` | `Long` |  | — |
| `headYaw` | `head_yaw` | `BigDecimal` |  | — |
| `headPitch` | `head_pitch` | `BigDecimal` |  | — |
| `headRoll` | `head_roll` | `BigDecimal` |  | — |
| `eyesClosed` | `eyes_closed` | `Boolean` |  | — |
| `maskOrOcclusionDetected` | `mask_or_occlusion_detected` | `boolean` | yes | Defaults to `false`. |
| `livenessScore` | `liveness_score` | `BigDecimal` |  | Anti-spoofing score: low values suggest a photo, mask or replayed video. |
| `spoofSuspected` | `spoof_suspected` | `boolean` | yes | Defaults to `false`. |
| `boundingBox` | `bounding_box` | `BoundingBox` |  | Embedded value object — see its own field table. |

### `ObjectDetection` — `object_detections`

Object-model findings for one frame: a prohibited item recognised in the candidate's environment. One row per detected instance, so a frame containing a phone and a book produces two rows and each can be weighted on its own.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiDetectionId` | `ai_detection_id` | `Long` | yes | The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. |
| `objectClass` | `object_class` | `DetectedObjectClass` | yes | One of `MOBILE_PHONE`, `BOOK`, `PAPER_NOTES`, `SECOND_LAPTOP`, `SECOND_MONITOR`, `EARPHONE`, `SMARTWATCH`, `CAMERA`, `ADDITIONAL_PERSON`, `UNKNOWN_OBJECT`. |
| `objectLabel` | `object_label` | `String` |  | Raw model label, kept because the taxonomy above is coarser than the model's vocabulary. |
| `objectCount` | `object_count` | `int` | yes | Defaults to `1`. |
| `prohibited` | `is_prohibited` | `boolean` | yes | Defaults to `true`. |
| `proximityScore` | `proximity_score` | `BigDecimal` |  | How close to the candidate's hands or face the item was — a phone in hand outranks one on a shelf. |
| `persistedFrames` | `persisted_frames` | `Integer` |  | Consecutive frames the item stayed visible; separates a glimpse from sustained use. |
| `boundingBox` | `bounding_box` | `BoundingBox` |  | Embedded value object — see its own field table. |

### `SuspiciousActivity` — `suspicious_activities`

A correlated alert: the layer above raw detections, where "looked away", "phone visible" and "voice heard" within the same thirty seconds become one incident a human can act on.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `proctoringSessionId` | `proctoring_session_id` | `Long` | yes | References `proctoring_sessions.id`. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `activityType` | `activity_type` | `SuspiciousActivityType` | yes | One of `REPEATED_LOOKING_AWAY`, `MULTIPLE_FACES`, `PHONE_DETECTED`, `NO_FACE`, `SUSPICIOUS_OBJECT`, `TAB_SWITCHING`, `FULLSCREEN_EXIT`, `AUDIO_EVENT`, `IMPERSONATION_SUSPECTED`, `COMBINED_BEHAVIOR`. |
| `severity` | `severity` | `EventSeverity` | yes | One of `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Defaults to `EventSeverity.MEDIUM`. |
| `confidence` | `confidence` | `BigDecimal` | yes | Combined confidence across the contributing signals, 0.0000–1.0000. |
| `firstSeenAt` | `first_seen_at` | `Instant` | yes | — |
| `lastSeenAt` | `last_seen_at` | `Instant` | yes | — |
| `durationMs` | `duration_ms` | `Long` |  | — |
| `occurrenceCount` | `occurrence_count` | `int` | yes | Repeats folded into this incident, rather than one alert per frame. Defaults to `1`. |
| `detectionCount` | `detection_count` | `int` | yes | Defaults to `0`. |
| `eventCount` | `event_count` | `int` | yes | Defaults to `0`. |
| `primaryDetectionId` | `primary_detection_id` | `Long` |  | The single most indicative detection, for the reviewer's first click. |
| `contributingSignals` | `contributing_signals` | `String` |  | Ids of every contributing detection and event — the correlation rule's working. |
| `ruleCode` | `rule_code` | `String` |  | — |
| `description` | `description` | `String` |  | — |
| `verdict` | `verdict` | `ActivityVerdict` | yes | One of `DETECTED`, `CONFIRMED`, `FALSE_POSITIVE`, `UNDER_REVIEW`, `DISMISSED`. Defaults to `ActivityVerdict.DETECTED`. |
| `adjudicatedByUserId` | `adjudicated_by_user_id` | `Long` |  | References `users.id`. |
| `adjudicatedAt` | `adjudicated_at` | `Instant` |  | — |
| `adjudicationNote` | `adjudication_note` | `String` |  | — |


---

## Risk Engine

### `RiskAssessment` — `risk_assessments`

Aggregated integrity score for one attempt: the point where thousands of events and detections collapse into a single number a human can act on.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` |  | Null for an attempt sat without supervision, where risk rests on delivery signals only. |
| `version` | `version` | `int` | yes | Defaults to `1`. |
| `latest` | `is_latest` | `boolean` | yes | Defaults to `true`. |
| `riskScore` | `risk_score` | `BigDecimal` | yes | 0–100. Bands are configured, not hardcoded, hence `thresholdVersion`. |
| `riskLevel` | `risk_level` | `RiskLevel` | yes | One of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Defaults to `RiskLevel.LOW`. |
| `recommendation` | `recommendation` | `RiskRecommendation` | yes | One of `ALLOW`, `MONITOR`, `FLAG_FOR_REVIEW`, `INVALIDATE_ATTEMPT`, `REQUIRE_RETAKE`. Defaults to `RiskRecommendation.ALLOW`. |
| `scoringModel` | `scoring_model` | `String` | yes | — |
| `scoringModelVersion` | `scoring_model_version` | `String` | yes | — |
| `thresholdVersion` | `threshold_version` | `String` |  | — |
| `computedAt` | `computed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `recomputeReason` | `recompute_reason` | `String` |  | — |
| `identityScore` | `identity_score` | `BigDecimal` |  | Component scores, so a reviewer can see which dimension drove the total. |
| `faceAnomalyScore` | `face_anomaly_score` | `BigDecimal` |  | — |
| `objectAnomalyScore` | `object_anomaly_score` | `BigDecimal` |  | — |
| `behaviorAnomalyScore` | `behavior_anomaly_score` | `BigDecimal` |  | — |
| `environmentScore` | `environment_score` | `BigDecimal` |  | — |
| `totalEventCount` | `total_event_count` | `int` | yes | Defaults to `0`. |
| `criticalEventCount` | `critical_event_count` | `int` | yes | Defaults to `0`. |
| `autoActionApplied` | `auto_action_applied` | `boolean` | yes | Defaults to `false`. |

### `RiskEvent` — `risk_events`

One line of the risk score's working: which signal contributed, with what weight, for how many points. Without this table a score is a bare number nobody can defend in an appeal; with it, "72, of which 30 came from a phone visible for 40 seconds" is reconstructable.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `riskAssessmentId` | `risk_assessment_id` | `Long` | yes | References `risk_assessments.id`. |
| `proctoringEventId` | `proctoring_event_id` | `Long` |  | The observation behind the contribution, when it came from a session event. |
| `aiDetectionId` | `ai_detection_id` | `Long` |  | The inference behind the contribution, when it came from a model rather than an event. |
| `factorCode` | `factor_code` | `String` | yes | Stable identifier of the scoring rule, e.g. PROHIBITED_OBJECT_SUSTAINED. |
| `factorLabel` | `factor_label` | `String` |  | — |
| `severity` | `severity` | `EventSeverity` | yes | One of `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `weight` | `weight` | `BigDecimal` | yes | Rule weight at scoring time, kept so a later reweighting cannot rewrite this explanation. |
| `occurrenceCount` | `occurrence_count` | `int` | yes | Defaults to `1`. |
| `contributedPoints` | `contributed_points` | `BigDecimal` | yes | Points this factor actually added to the total. |
| `occurredAt` | `occurred_at` | `Instant` |  | — |
| `note` | `note` | `String` |  | — |


---

## Review System

### `ReviewCase` — `review_cases`

A human investigation into one attempt, opened automatically when risk crosses the exam's review threshold or manually by a proctor. The case is the queue item and the audit record; the judgements inside it are `ReviewDecision` rows.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `caseNumber` | `case_number` | `String` | yes | Human-quotable reference, e.g. RC-2026-000481. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `riskAssessmentId` | `risk_assessment_id` | `Long` |  | References `risk_assessments.id`. |
| `status` | `status` | `ReviewCaseStatus` | yes | One of `OPEN`, `ASSIGNED`, `IN_REVIEW`, `PENDING_INFO`, `ESCALATED`, `RESOLVED`, `CLOSED`. Defaults to `ReviewCaseStatus.OPEN`. |
| `priority` | `priority` | `ReviewPriority` | yes | One of `LOW`, `NORMAL`, `HIGH`, `URGENT`. Defaults to `ReviewPriority.NORMAL`. |
| `openedAt` | `opened_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `openedByUserId` | `opened_by_user_id` | `Long` |  | Null when the platform opened the case automatically. |
| `openReason` | `open_reason` | `String` | yes | — |
| `assignedReviewerUserId` | `assigned_reviewer_user_id` | `Long` |  | References `users.id`. |
| `assignedAt` | `assigned_at` | `Instant` |  | — |
| `dueAt` | `due_at` | `Instant` |  | — |
| `slaBreached` | `sla_breached` | `boolean` | yes | Defaults to `false`. |
| `closedAt` | `closed_at` | `Instant` |  | — |
| `finalOutcome` | `final_outcome` | `ReviewOutcome` |  | Settled verdict, mirrored from the final `ReviewDecision` when the case closes. |
| `summary` | `summary` | `String` |  | — |
| `candidateNotified` | `candidate_notified` | `boolean` | yes | True once the candidate has been told the outcome. Defaults to `false`. |

### `ReviewDecision` — `review_decisions`

One judgement recorded against a case. Append-only and ordered: a case can pass through "request more info", "escalate" and "invalidate", and each step keeps its own author, timestamp and rationale. Exactly one row per case may be `finalDecision`.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `reviewCaseId` | `review_case_id` | `Long` | yes | References `review_cases.id`. |
| `reviewerUserId` | `reviewer_user_id` | `Long` | yes | References `users.id`. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `decisionType` | `decision_type` | `ReviewDecisionType` | yes | One of `CLEAR`, `WARN_CANDIDATE`, `ADJUST_SCORE`, `INVALIDATE_ATTEMPT`, `GRANT_RETAKE`, `ESCALATE`, `REQUEST_MORE_INFO`. |
| `decidedAt` | `decided_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `rationale` | `rationale` | `String` | yes | Required: a decision without stated reasoning cannot be defended on appeal. |
| `evidenceRefs` | `evidence_refs` | `String` |  | Evidence the reviewer relied on, as a list of EVIDENCE_FILES public ids. |
| `scoreAdjustment` | `score_adjustment` | `BigDecimal` |  | Signed adjustment applied to the result when decision type is ADJUST_SCORE. |
| `finalDecision` | `is_final` | `boolean` | yes | Defaults to `false`. |
| `supersedesDecisionId` | `supersedes_decision_id` | `Long` |  | The decision this one overturns, when a case is reopened or escalated. |

### `ReviewFinding` — `review_findings`

A reviewer's verdict on one flagged item inside a case — this event, this detection, this clip: valid, false positive, or needs investigation.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `reviewCaseId` | `review_case_id` | `Long` | yes | References `review_cases.id`. |
| `itemKind` | `item_kind` | `ReviewItemKind` | yes | One of `PROCTORING_EVENT`, `AI_DETECTION`, `SUSPICIOUS_ACTIVITY`, `EVIDENCE_FILE`, `RISK_FACTOR`. |
| `itemId` | `item_id` | `Long` | yes | — |
| `verdict` | `verdict` | `ItemVerdict` | yes | One of `VALID`, `FALSE_POSITIVE`, `NEEDS_INVESTIGATION`, `INCONCLUSIVE`. |
| `reviewerUserId` | `reviewer_user_id` | `Long` | yes | References `users.id`. |
| `reviewedAt` | `reviewed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `comment` | `comment` | `String` |  | — |
| `weightDisputed` | `weight_disputed` | `boolean` | yes | True when the reviewer disagrees with how the engine weighted this item. Defaults to `false`. |

### `ReviewNote` — `review_notes`

A comment on a case — the working conversation between reviewers, separate from the `ReviewDecision` rows that carry formal outcomes.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `reviewCaseId` | `review_case_id` | `Long` | yes | References `review_cases.id`. |
| `authorUserId` | `author_user_id` | `Long` | yes | References `users.id`. |
| `body` | `body` | `String` | yes | — |
| `candidateVisible` | `candidate_visible` | `boolean` | yes | Defaults to `false`. |
| `attachmentPath` | `attachment_path` | `String` |  | Attachment supporting the note (an external report, a screenshot annotation). |
| `editedAt` | `edited_at` | `Instant` |  | — |


---

## Exam Results

### `ExamResult` — `exam_results`

The published outcome of an attempt — one row per attempt. Separate from ExamAttempt because a result has its own lifecycle: it can be provisional, withheld pending an integrity review, adjusted by a reviewer, released, or voided, all while the attempt itself stays unchanged.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `examId` | `exam_id` | `Long` | yes | References `exams.id`. |
| `candidateUserId` | `candidate_user_id` | `Long` | yes | References `users.id`. |
| `rawScore` | `raw_score` | `BigDecimal` | yes | Defaults to `BigDecimal.ZERO`. |
| `scoreAdjustment` | `score_adjustment` | `BigDecimal` |  | — |
| `finalScore` | `final_score` | `BigDecimal` | yes | Defaults to `BigDecimal.ZERO`. |
| `maxScore` | `max_score` | `BigDecimal` | yes | — |
| `percentage` | `percentage` | `BigDecimal` |  | — |
| `grade` | `grade` | `String` |  | — |
| `passed` | `passed` | `Boolean` |  | Null while the result is provisional — pass/fail is only meaningful once final. |
| `status` | `status` | `ResultStatus` | yes | One of `PROVISIONAL`, `PENDING_REVIEW`, `WITHHELD`, `FINAL`, `VOID`. Defaults to `ResultStatus.PROVISIONAL`. |
| `integrityStatus` | `integrity_status` | `IntegrityStatus` | yes | One of `CLEAN`, `FLAGGED`, `UNDER_REVIEW`, `INVALIDATED`. Defaults to `IntegrityStatus.CLEAN`. |
| `riskScore` | `risk_score` | `BigDecimal` |  | Risk score as it stood when the result was finalised, copied from the assessment. Frozen rather than joined: a later recompute must not change a published result's stated basis. |
| `riskLevel` | `risk_level` | `RiskLevel` |  | One of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `reviewCaseId` | `review_case_id` | `Long` |  | The review that gated or adjusted this result, when there was one. |
| `gradingMode` | `grading_mode` | `GradingMode` | yes | One of `AUTO`, `MANUAL`, `HYBRID`. Defaults to `GradingMode.AUTO`. |
| `gradedAt` | `graded_at` | `Instant` |  | — |
| `gradedByUserId` | `graded_by_user_id` | `Long` |  | References `users.id`. |
| `publishedAt` | `published_at` | `Instant` |  | — |
| `releasedToCandidate` | `released_to_candidate` | `boolean` | yes | Defaults to `false`. |
| `correctCount` | `correct_count` | `int` | yes | Defaults to `0`. |
| `incorrectCount` | `incorrect_count` | `int` | yes | Defaults to `0`. |
| `unansweredCount` | `unanswered_count` | `int` | yes | Defaults to `0`. |
| `pendingManualCount` | `pending_manual_count` | `int` | yes | Defaults to `0`. |
| `timeSpentSeconds` | `time_spent_seconds` | `Long` |  | — |
| `certificateSerial` | `certificate_serial` | `String` |  | — |

### `ResultDetail` — `result_details`

Per-question line of a result: the frozen scoring breakdown behind the total.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examResultId` | `exam_result_id` | `Long` | yes | References `exam_results.id`. |
| `examQuestionId` | `exam_question_id` | `Long` | yes | References `exam_questions.id`. |
| `examSectionId` | `exam_section_id` | `Long` |  | Denormalised from the placement so section subtotals need no extra join. |
| `attemptAnswerId` | `attempt_answer_id` | `Long` |  | Null when the question was never answered — the row still records the points forgone. |
| `sequenceNo` | `sequence_no` | `int` | yes | — |
| `pointsAwarded` | `points_awarded` | `BigDecimal` | yes | Defaults to `BigDecimal.ZERO`. |
| `pointsPossible` | `points_possible` | `BigDecimal` | yes | — |
| `correct` | `is_correct` | `Boolean` |  | — |
| `answered` | `answered` | `boolean` | yes | Defaults to `false`. |
| `timeSpentSeconds` | `time_spent_seconds` | `Integer` |  | — |
| `scoringNote` | `scoring_note` | `String` |  | — |

### `ResultWithholding` — `result_withholdings`

Why one result is sitting at `ExamResult.status = WITHHELD`, for how long, and who eventually released it. Leaving this implicit in the status column is exactly the part most likely to be challenged on appeal — "why was my result held for three weeks?" needs an answer more specific than a status enum.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examResultId` | `exam_result_id` | `Long` | yes | References `exam_results.id`. |
| `reason` | `reason` | `WithholdingReason` | yes | One of `RISK_THRESHOLD`, `OPEN_REVIEW_CASE`, `PENDING_MANUAL_GRADING`, `APPEAL_FILED`, `EXTERNAL_HOLD`. |
| `reviewCaseId` | `review_case_id` | `Long` |  | References `review_cases.id`. |
| `riskAssessmentId` | `risk_assessment_id` | `Long` |  | The assessment version whose band triggered the withholding, when risk-driven. |
| `thresholdVersion` | `threshold_version` | `String` |  | — |
| `withheldAt` | `withheld_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `withheldByUserId` | `withheld_by_user_id` | `Long` |  | Null for automatic (threshold-driven) withholding. |
| `releasedAt` | `released_at` | `Instant` |  | — |
| `releasedByUserId` | `released_by_user_id` | `Long` |  | References `users.id`. |
| `releaseReason` | `release_reason` | `String` |  | Required whenever releasedByUserId is set — a release without a reason isn't defensible. |


---

## Proctoring Reports

### `AttemptTimeline` — `attempt_timelines`

A rendered, unified timeline for one attempt — proctoring events, answer revisions, risk contributions, and evidence captures, ordered by their own offsets/timestamps, so a reviewer doesn't join five tables by hand to scrub one sitting.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `version` | `version` | `int` | yes | Defaults to `1`. |
| `renderedAt` | `rendered_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `entryCount` | `entry_count` | `int` | yes | Defaults to `0`. |
| `timelineJson` | `timeline_json` | `String` |  | JSON array of {offsetMs, kind, refId, label, severity} — refId points back to its source row. |
| `checksumSha256` | `checksum_sha256` | `String` |  | — |
| `generatedByUserId` | `generated_by_user_id` | `Long` |  | Null for an automatically rendered timeline. |

### `ProctoringReport` — `proctoring_reports`

A rendered, immutable dossier for one attempt: candidate and exam details, the event timeline, the evidence index, the risk score and the reviewer's decision, as they stood when it was generated.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `publicId` | `public_id` | `UUID` | yes | Defaults to `UUID.randomUUID()`. |
| `examAttemptId` | `exam_attempt_id` | `Long` | yes | References `exam_attempts.id`. |
| `proctoringSessionId` | `proctoring_session_id` | `Long` |  | References `proctoring_sessions.id`. |
| `riskAssessmentId` | `risk_assessment_id` | `Long` |  | The assessment version the report was rendered from. |
| `reviewCaseId` | `review_case_id` | `Long` |  | References `review_cases.id`. |
| `version` | `version` | `int` | yes | Defaults to `1`. |
| `status` | `status` | `ReportStatus` | yes | One of `QUEUED`, `GENERATING`, `AVAILABLE`, `FAILED`, `EXPIRED`. Defaults to `ReportStatus.QUEUED`. |
| `format` | `format` | `ReportFormat` | yes | One of `PDF`, `HTML`, `JSON`, `ZIP_BUNDLE`. Defaults to `ReportFormat.PDF`. |
| `requestedByUserId` | `requested_by_user_id` | `Long` |  | References `users.id`. |
| `requestedAt` | `requested_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `generatedAt` | `generated_at` | `Instant` |  | — |
| `storagePath` | `storage_path` | `String` |  | — |
| `sizeBytes` | `size_bytes` | `Long` |  | — |
| `checksumSha256` | `checksum_sha256` | `String` |  | Integrity seal over the rendered file, so a copy in circulation can be proved genuine. |
| `summarySnapshot` | `summary_snapshot` | `String` |  | Figures as rendered: event counts by severity, risk breakdown, score, timings. |
| `riskScore` | `risk_score` | `BigDecimal` |  | — |
| `finalIntegrityStatus` | `final_integrity_status` | `IntegrityStatus` |  | One of `CLEAN`, `FLAGGED`, `UNDER_REVIEW`, `INVALIDATED`. |
| `includesEvidence` | `includes_evidence` | `boolean` | yes | Defaults to `false`. |
| `expiresAt` | `expires_at` | `Instant` |  | — |
| `failureReason` | `failure_reason` | `String` |  | — |


---

## Notifications

### `Notification` — `notifications`

One message to one recipient on one channel — the outbox. Rendered content is stored, not re-derived: the template it came from will be edited, and "what were they actually told, and when?" is a question that gets asked about exam notifications.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `recipientUserId` | `recipient_user_id` | `Long` | yes | References `users.id`. |
| `notificationType` | `notification_type` | `NotificationType` | yes | One of `ACCOUNT_VERIFICATION`, `PASSWORD_RESET`, `EXAM_INVITATION`, `EXAM_REMINDER`, `EXAM_STARTED`, `EXAM_SUBMITTED`, `ATTEMPT_TERMINATED`, `REVIEW_ASSIGNED`, `REVIEW_COMPLETED`, `RESULT_PUBLISHED`, `HIGH_RISK_ALERT`, `SYSTEM_ALERT`. |
| `channel` | `channel` | `NotificationChannel` | yes | One of `EMAIL`, `SMS`, `IN_APP`, `PUSH`, `WEBHOOK`. Defaults to `NotificationChannel.EMAIL`. |
| `status` | `status` | `NotificationStatus` | yes | One of `PENDING`, `QUEUED`, `SENT`, `DELIVERED`, `OPENED`, `FAILED`, `CANCELLED`. Defaults to `NotificationStatus.PENDING`. |
| `templateId` | `template_id` | `Long` |  | References `notification_templates.id`. |
| `sentTo` | `sent_to` | `String` | yes | Address actually used, kept because a profile change must not rewrite delivery history. |
| `subject` | `subject` | `String` |  | — |
| `body` | `body` | `String` | yes | — |
| `variables` | `variables` | `String` |  | Values interpolated into the template, for support and for a resend. |
| `relatedEntityType` | `related_entity_type` | `String` |  | — |
| `relatedEntityId` | `related_entity_id` | `Long` |  | — |
| `scheduledFor` | `scheduled_for` | `Instant` |  | — |
| `sentAt` | `sent_at` | `Instant` |  | — |
| `deliveredAt` | `delivered_at` | `Instant` |  | — |
| `openedAt` | `opened_at` | `Instant` |  | — |
| `retryCount` | `retry_count` | `int` | yes | Defaults to `0`. |
| `nextRetryAt` | `next_retry_at` | `Instant` |  | — |
| `failureReason` | `failure_reason` | `String` |  | — |
| `providerMessageId` | `provider_message_id` | `String` |  | Provider message id, for reconciling bounces against the outbox. |
| `idempotencyKey` | `idempotency_key` | `String` |  | Guards against duplicate sends when a trigger is retried. |

### `NotificationSuppression` — `notification_suppressions`

A rule that defers, throttles, or blocks a class of outgoing notification — quiet hours in the recipient's own time zone, an opt-out, a rate limit, or a duplicate-content window.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `recipientUserId` | `recipient_user_id` | `Long` |  | Null for a rule that applies to every recipient. |
| `notificationType` | `notification_type` | `NotificationType` |  | Null for a rule that isn't specific to one notification type. |
| `channel` | `channel` | `NotificationChannel` |  | Null for a rule that applies regardless of channel. |
| `suppressionKind` | `suppression_kind` | `SuppressionKind` | yes | One of `QUIET_HOURS`, `OPT_OUT`, `RATE_LIMIT`, `DUPLICATE_WINDOW`. |
| `windowStartLocal` | `window_start_local` | `LocalTime` |  | For QUIET_HOURS, in the recipient's own User.timeZone — never the server's. |
| `windowEndLocal` | `window_end_local` | `LocalTime` |  | — |
| `maxPerWindow` | `max_per_window` | `Integer` |  | — |
| `windowSeconds` | `window_seconds` | `Integer` |  | — |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |
| `createdByUserId` | `created_by_user_id` | `Long` |  | References `users.id`. |

### `NotificationTemplate` — `notification_templates`

Editable subject and body for one (type, channel, locale) combination. In the database rather than in resource bundles so an administrator can correct the wording of an exam reminder without a release — and, given the platform sends in more than one language, so the Khmer and English versions of a message stay side by side.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `notificationType` | `notification_type` | `NotificationType` | yes | One of `ACCOUNT_VERIFICATION`, `PASSWORD_RESET`, `EXAM_INVITATION`, `EXAM_REMINDER`, `EXAM_STARTED`, `EXAM_SUBMITTED`, `ATTEMPT_TERMINATED`, `REVIEW_ASSIGNED`, `REVIEW_COMPLETED`, `RESULT_PUBLISHED`, `HIGH_RISK_ALERT`, `SYSTEM_ALERT`. |
| `channel` | `channel` | `NotificationChannel` | yes | One of `EMAIL`, `SMS`, `IN_APP`, `PUSH`, `WEBHOOK`. Defaults to `NotificationChannel.EMAIL`. |
| `locale` | `locale` | `String` | yes | Defaults to `"en"`. |
| `subjectTemplate` | `subject_template` | `String` |  | — |
| `bodyTemplate` | `body_template` | `String` | yes | — |
| `expectedVariables` | `expected_variables` | `String` |  | Placeholders the template expects, e.g. `candidateName,examTitle,startsAt`. |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |
| `version` | `version` | `int` | yes | Defaults to `1`. |


---

## Audit

### `AuditLog` — `audit_logs`

Append-only record of every consequential action: who did what to which object, from where, and whether it succeeded. In an exam system the interesting rows are the administrative ones — a score overridden, an attempt reinstated, a permission granted, an exam's rules relaxed an hour before it opened.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `actorUserId` | `actor_user_id` | `Long` |  | Null for actions taken by the platform itself (schedulers, auto-submit, auto-termination). |
| `actorRole` | `actor_role` | `String` |  | Actor's role at the time — role grants change, and the row must still read correctly. |
| `action` | `action` | `AuditAction` | yes | One of `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `PUBLISH`, `ASSIGN`, `START_ATTEMPT`, `SUBMIT_ATTEMPT`, `GRADE`, `SCORE_OVERRIDE`, `TERMINATE_ATTEMPT`, `VIEW_EVIDENCE`, `EXPORT`, `CONFIG_CHANGE`, `PERMISSION_CHANGE`, `MODEL_CHANGE`, `PAYMENT_METHOD_ADDED`, `PAYMENT_METHOD_REMOVED`, `PAYMENT_METHOD_SET_DEFAULT`, `PAYMENT_REFUNDED`, `PAYMENT_DISPUTED`. |
| `outcome` | `outcome` | `AuditOutcome` | yes | One of `SUCCESS`, `FAILURE`, `DENIED`. Defaults to `AuditOutcome.SUCCESS`. |
| `entityType` | `entity_type` | `String` | yes | — |
| `entityId` | `entity_id` | `Long` |  | — |
| `entityLabel` | `entity_label` | `String` |  | — |
| `occurredAt` | `occurred_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `beforeState` | `before_state` | `String` |  | — |
| `afterState` | `after_state` | `String` |  | — |
| `reason` | `reason` | `String` |  | Justification, required by policy for overrides and terminations. |
| `ipAddress` | `ip_address` | `String` |  | — |
| `userAgent` | `user_agent` | `String` |  | — |
| `requestId` | `request_id` | `String` |  | Correlation id of the originating request, to tie a row to application logs. |


---

## AI Model Registry

### `AiModel` — `ai_models`

Registry entry for one model the platform depends on — the face matcher, the object detector, the risk scorer. The registry exists so detections can name their producer by key rather than by a free-text string, and so a model can be swapped without touching detection code.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `code` | `code` | `String` | yes | — |
| `name` | `name` | `String` | yes | — |
| `purpose` | `purpose` | `ModelPurpose` | yes | One of `FACE_DETECTION`, `FACE_VERIFICATION`, `HEAD_POSE`, `GAZE_ESTIMATION`, `OBJECT_DETECTION`, `PERSON_DETECTION`, `SPEECH_DETECTION`, `VOICE_DIARIZATION`, `BEHAVIOR_ANALYSIS`, `RISK_SCORING`, `EXCEL_ANOMALY_DETECTION`. |
| `vendor` | `vendor` | `String` |  | — |
| `description` | `description` | `String` |  | — |
| `runsOnClient` | `runs_on_client` | `boolean` | yes | Where inference runs — matters for latency budgets and for data-residency questions. Defaults to `false`. |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |

### `AiModelVersion` — `ai_model_versions`

One deployable version of a model, with the thresholds it runs at. Thresholds live on the version, not on the model: raising the phone-detector's confidence floor from 0.60 to 0.80 changes what counts as evidence, and every detection scored under the old floor must remain interpretable.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiModelId` | `ai_model_id` | `Long` | yes | References `ai_models.id`. |
| `version` | `version` | `String` | yes | — |
| `status` | `status` | `ModelVersionStatus` | yes | One of `DRAFT`, `CANDIDATE`, `SHADOW`, `ACTIVE`, `DEPRECATED`, `RETIRED`. Defaults to `ModelVersionStatus.DRAFT`. |
| `artifactRef` | `artifact_ref` | `String` |  | Artefact reference — registry URI, image digest or bundle path. |
| `artifactChecksum` | `artifact_checksum` | `String` |  | — |
| `confidenceThreshold` | `confidence_threshold` | `BigDecimal` | yes | Confidence below which output is ignored entirely. |
| `detectionThreshold` | `detection_threshold` | `BigDecimal` | yes | Confidence at or above which a detection is raised as an event. |
| `configuration` | `configuration` | `String` |  | Remaining knobs — frame rate, window length, NMS, per-class overrides. |
| `activatedAt` | `activated_at` | `Instant` |  | — |
| `deprecatedAt` | `deprecated_at` | `Instant` |  | — |
| `activatedByUserId` | `activated_by_user_id` | `Long` |  | References `users.id`. |
| `releaseNotes` | `release_notes` | `String` |  | — |

### `ModelPerformanceMetric` — `model_performance_metrics`

One measured metric for one model version over one window. The ground truth comes from reviewers: every alert marked a false positive in REVIEW_FINDINGS is a labelled sample, which is what makes a real false-positive rate computable rather than estimated.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `aiModelVersionId` | `ai_model_version_id` | `Long` | yes | References `ai_model_versions.id`. |
| `metricType` | `metric_type` | `MetricType` | yes | One of `PRECISION`, `RECALL`, `F1_SCORE`, `ACCURACY`, `FALSE_POSITIVE_RATE`, `FALSE_NEGATIVE_RATE`, `LATENCY_P95_MS`, `THROUGHPUT_FPS`. |
| `metricValue` | `metric_value` | `BigDecimal` | yes | — |
| `windowStartAt` | `window_start_at` | `Instant` | yes | — |
| `windowEndAt` | `window_end_at` | `Instant` | yes | — |
| `sampleCount` | `sample_count` | `long` | yes | Detections in the window; a metric over forty samples deserves less trust than over forty thousand. |
| `truePositiveCount` | `true_positive_count` | `Long` |  | Reviewer-labelled counts behind the figure, where the metric derives from adjudications. |
| `falsePositiveCount` | `false_positive_count` | `Long` |  | — |
| `falseNegativeCount` | `false_negative_count` | `Long` |  | — |
| `computedAt` | `computed_at` | `Instant` | yes | Defaults to `Instant.now()`. |
| `note` | `note` | `String` |  | — |

### `ShadowEvaluation` — `shadow_evaluations`

A measured comparison of one SHADOW model version against the currently ACTIVE one over the same traffic — the difference between "the shadow looks fine" and knowing it. A shadow version never drives an auto-action and never grades itself: its false-positive rate here comes only from `ReviewFinding` verdicts on its own shadow-only detections, the same ground truth `ModelPerformanceMetric` already relies on for the active model.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `shadowVersionId` | `shadow_version_id` | `Long` | yes | References `ai_model_versions.id`. |
| `activeVersionId` | `active_version_id` | `Long` | yes | References `ai_model_versions.id`. |
| `windowStartAt` | `window_start_at` | `Instant` | yes | — |
| `windowEndAt` | `window_end_at` | `Instant` | yes | — |
| `agreementCount` | `agreement_count` | `long` | yes | Defaults to `0`. |
| `disagreementCount` | `disagreement_count` | `long` | yes | Defaults to `0`. |
| `shadowOnlyDetections` | `shadow_only_detections` | `long` | yes | Defaults to `0`. |
| `activeOnlyDetections` | `active_only_detections` | `long` | yes | Defaults to `0`. |
| `shadowFalsePositiveRate` | `shadow_false_positive_rate` | `BigDecimal` |  | — |
| `activeFalsePositiveRate` | `active_false_positive_rate` | `BigDecimal` |  | — |
| `recommendation` | `recommendation` | `ShadowRecommendation` |  | One of `PROMOTE`, `HOLD`, `RETIRE`. |
| `computedAt` | `computed_at` | `Instant` | yes | Defaults to `Instant.now()`. |


---

## System Configuration

### `RiskFactorConfig` — `risk_factor_configs`

The risk engine's rule book: one row per scoring factor, giving the weight a signal carries, how repeats accumulate, and how fast old contributions decay.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `factorCode` | `factor_code` | `String` | yes | — |
| `factorLabel` | `factor_label` | `String` | yes | — |
| `configVersion` | `config_version` | `String` | yes | — |
| `triggerKind` | `trigger_kind` | `RiskFactorTrigger` | yes | One of `PROCTORING_EVENT`, `AI_DETECTION`, `SUSPICIOUS_ACTIVITY`, `DEVICE_SIGNAL`, `IDENTITY_SIGNAL`. |
| `triggerCode` | `trigger_code` | `String` | yes | The enum constant this rule reacts to, e.g. `PROHIBITED_OBJECT_DETECTED`. |
| `severity` | `severity` | `EventSeverity` | yes | One of `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Defaults to `EventSeverity.MEDIUM`. |
| `baseWeight` | `base_weight` | `BigDecimal` | yes | Base points a single occurrence contributes. |
| `confidenceMultiplier` | `confidence_multiplier` | `BigDecimal` | yes | Multiplier applied to the model's confidence before weighting. Defaults to `BigDecimal.ONE`. |
| `frequencyIncrement` | `frequency_increment` | `BigDecimal` |  | Extra points per repeat, so sustained behavior outweighs a one-off. |
| `durationWeightPerSecond` | `duration_weight_per_second` | `BigDecimal` |  | Points per second of duration, for factors where dwell time is the signal. |
| `maxContribution` | `max_contribution` | `BigDecimal` |  | Ceiling on this factor's total, so one noisy signal cannot dominate the score. |
| `decayHalfLifeSeconds` | `decay_half_life_seconds` | `Integer` |  | Half-life in seconds for decay; a glance twenty minutes ago should not weigh as much as one now. |
| `graceOccurrences` | `grace_occurrences` | `int` | yes | Occurrences tolerated before the factor scores at all. Defaults to `0`. |
| `minConfidence` | `min_confidence` | `BigDecimal` |  | Minimum model confidence for the signal to be considered. |
| `immediateCritical` | `is_immediate_critical` | `boolean` | yes | True when a single occurrence should escalate straight to CRITICAL. Defaults to `false`. |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |
| `effectiveFrom` | `effective_from` | `Instant` | yes | Defaults to `Instant.now()`. |
| `effectiveTo` | `effective_to` | `Instant` |  | — |

### `RiskLevelThreshold` — `risk_level_thresholds`

The band boundaries that turn a numeric risk score into LOW / MEDIUM / HIGH / CRITICAL, and what each band does.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `configVersion` | `config_version` | `String` | yes | — |
| `riskLevel` | `risk_level` | `RiskLevel` | yes | One of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| `minScore` | `min_score` | `BigDecimal` | yes | Inclusive lower bound of the band, 0–100. |
| `maxScore` | `max_score` | `BigDecimal` |  | Exclusive upper bound; null for the topmost band. |
| `recommendation` | `recommendation` | `RiskRecommendation` | yes | What the engine advises for an attempt landing in this band. Defaults to `RiskRecommendation.ALLOW`. |
| `autoAction` | `auto_action` | `AutoAction` | yes | What the platform does on its own, with no human in the loop. Defaults to `AutoAction.NONE`. |
| `opensReviewCase` | `opens_review_case` | `boolean` | yes | Open a review case for anything reaching this band. Defaults to `false`. |
| `withholdsResult` | `withholds_result` | `boolean` | yes | Withhold the result until the review closes, rather than publishing provisionally. Defaults to `false`. |
| `alertsProctor` | `alerts_proctor` | `boolean` | yes | Raise the band on the live monitoring wall and notify the assigned proctor. Defaults to `false`. |
| `displayColour` | `display_colour` | `String` |  | Colour token for the dashboard, so the wall and the reports agree. |
| `active` | `is_active` | `boolean` | yes | Defaults to `true`. |
| `effectiveFrom` | `effective_from` | `Instant` | yes | Defaults to `Instant.now()`. |
| `effectiveTo` | `effective_to` | `Instant` |  | — |

### `SystemSetting` — `system_settings`

Runtime configuration an administrator can change without a deploy: storage limits, retention periods, notification defaults, AI frame rates, security policy.

| Field | Column | Type | Required | Note |
|---|---|---|---|---|
| `category` | `category` | `SettingCategory` | yes | One of `GENERAL`, `PROCTORING`, `RISK`, `EXAM`, `STORAGE`, `NOTIFICATION`, `AI`, `SECURITY`, `EXCEL`. Defaults to `SettingCategory.GENERAL`. |
| `settingKey` | `setting_key` | `String` | yes | — |
| `settingValue` | `setting_value` | `String` |  | — |
| `valueType` | `value_type` | `SettingValueType` | yes | One of `STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `JSON`, `DURATION`, `ENUM`. Defaults to `SettingValueType.STRING`. |
| `defaultValue` | `default_value` | `String` |  | Shipped default, so "reset to default" needs no code lookup. |
| `description` | `description` | `String` |  | — |
| `validationRule` | `validation_rule` | `String` |  | Allowed values or bounds, checked before a save. |
| `secret` | `is_secret` | `boolean` | yes | Defaults to `false`. |
| `requiresRestart` | `requires_restart` | `boolean` | yes | True when the change only takes effect after a restart — worth telling the operator. Defaults to `false`. |
| `editable` | `is_editable` | `boolean` | yes | Defaults to `true`. |
| `updatedByUserId` | `updated_by_user_id` | `Long` |  | References `users.id`. |
| `lastChangedAt` | `last_changed_at` | `Instant` |  | — |


---

## Enum reference

Every enum type referenced above, grouped by module.


### Authentication & Access

**`ApiClientStatus`** — Lifecycle of a machine credential.  
`ACTIVE`, `SUSPENDED`, `REVOKED`, `EXPIRED`

**`LoginOutcome`** — Result of one authentication attempt.  
`SUCCESS`, `BAD_CREDENTIALS`, `ACCOUNT_LOCKED`, `ACCOUNT_DISABLED`, `EMAIL_NOT_VERIFIED`, `TOKEN_EXPIRED`, `MFA_REQUIRED`, `MFA_FAILED`

**`MfaMethod`** — Second factor a user has enrolled in, when MFA is turned on.  
`TOTP`, `SMS`, `EMAIL`

**`TokenPurpose`** — What a one-time security token authorises.  
`EMAIL_VERIFICATION`, `PASSWORD_RESET`, `ACCOUNT_INVITATION`, `EMAIL_CHANGE`

**`UserStatus`** — Lifecycle of a platform account.  
`PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DISABLED`


### Payment

**`CardBrand`** — Card network, as reported by the payment processor.  
`VISA`, `MASTERCARD`, `AMEX`, `DISCOVER`, `JCB`, `DINERS_CLUB`, `UNIONPAY`, `MAESTRO`, `ELO`, `OTHER`

**`PaymentCardFunding`** — How a card draws funds.  
`CREDIT`, `DEBIT`, `PREPAID`, `CHARGE`, `UNKNOWN`

**`PaymentCardholderVerification`** — Result of a CVV or AVS check.  
`NOT_ATTEMPTED`, `PASSED`, `FAILED`, `UNAVAILABLE`, `UNRECOGNIZED`

**`PaymentCardStatus`** — Lifecycle of one stored payment card.  
`ACTIVE`, `EXPIRED`, `PENDING_VERIFICATION`, `SUSPENDED`, `REVOKED`

**`PaymentProvider`** — Which PCI-compliant processor tokenized a stored card or moved money.  
`STRIPE`, `ADYEN`, `BRAINTREE`, `SQUARE`, `PAYPAL`, `MANUAL`

**`PaymentTransactionStatus`** — Lifecycle of one payment transaction.  
`INITIATED`, `PENDING`, `AUTHORIZED`, `CAPTURED`, `SETTLED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `DISPUTED`, `CHARGEBACK`

**`PaymentTransactionType`** — What kind of money movement a transaction represents.  
`AUTHORIZATION`, `CAPTURE`, `SALE`, `REFUND`, `VOID`, `CHARGEBACK`, `PAYOUT`


### Student Groups

**`GroupType`** — Kind of student grouping an exam can be assigned to.  
`CLASS`, `COHORT`, `DEPARTMENT`, `PROGRAM`, `CUSTOM`


### Exam Definition

**`AssignmentStatus`** — State of an exam invitation issued to a candidate.  
`ASSIGNED`, `NOTIFIED`, `STARTED`, `SUBMITTED`, `EXPIRED`, `CANCELLED`

**`ExamStatus`** — Publication lifecycle of an exam definition.  
`DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `CLOSED`, `ARCHIVED`

**`ExcelRecalcMode`** — How the Excel runtime recalculates formulas for an exam.  
`AUTOMATIC`, `MANUAL`, `ITERATIVE`

**`ExcelUiActionPolicy`** — How an exam's Excel runtime treats one candidate-initiated UI action (copy/paste, cut/drag-fill).  
`ALLOW`, `BLOCK`, `LOG`

**`GradingMode`** — How an exam is scored: engine, human, or both.  
`AUTO`, `MANUAL`, `HYBRID`

**`InvitationChannel`** — Delivery channel of an exam invitation.  
`EMAIL`, `SMS`, `IN_APP`

**`InvitationStatus`** — Delivery and acceptance state of an exam invitation.  
`PENDING`, `SENT`, `DELIVERED`, `OPENED`, `ACCEPTED`, `EXPIRED`, `CANCELLED`, `FAILED`

**`ProctoringMode`** — Level of supervision required while sitting an exam.  
`NONE`, `AI_ONLY`, `LIVE_PROCTOR`, `RECORD_AND_REVIEW`, `HYBRID`


### Question Bank

**`CalibrationAction`** — What a human should do about a question's calibration result. Never applied automatically.  
`KEEP`, `REVIEW`, `RETIRE`, `REWEIGHT`

**`CalibrationFlag`** — What a calibration run found wrong with a question, if anything.  
`TOO_EASY`, `TOO_HARD`, `NEGATIVE_DISCRIMINATION`, `MIS_KEY_SUSPECTED`, `NONE`

**`ExcelAnswerKind`** — What kind of check one `ExcelCellBinding` represents, and — reused on `ExcelGradeResult` — what kind of check was actually applied to grade it.  
`VALUE`, `FORMULA`, `RANGE`, `CHART`, `PIVOT_TABLE`, `CONDITIONAL_FORMATTING`, `NAMED_RANGE`, `MACRO_OUTPUT`, `MANUAL`

**`ExcelMacroPolicy`** — Whether a SPREADSHEET question's macros may run, and under what constraint.  
`OFF`, `SANDBOXED`, `ALLOWED_WHITELIST`

**`ExcelWorkbookFileType`** — File format of an uploaded SPREADSHEET question's workbook.  
`XLSX`, `XLSM`, `XLSB`, `CSV`, `ODS`

**`ProgrammingLanguage`** — Language a CODE question is written and graded in.  
`PYTHON`, `JAVA`, `JAVASCRIPT`, `TYPESCRIPT`, `CPP`, `C`, `CSHARP`, `GO`, `RUST`, `SQL`

**`QuestionDifficulty`** — Author-declared difficulty, used for random section draws.  
`EASY`, `MEDIUM`, `HARD`, `EXPERT`

**`QuestionStatus`** — Bank lifecycle. Retired questions stay readable for old attempts.  
`DRAFT`, `ACTIVE`, `RETIRED`

**`QuestionType`** — Shape of the expected answer; drives rendering and auto-grading.  
`SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_ANSWER`, `ESSAY`, `NUMERIC`, `MATCHING`, `ORDERING`, `FILL_IN_BLANK`, `CODE`, `SPREADSHEET`

**`TestCaseVisibility`** — Whether a code test case is shown to the candidate before grading or held back.  
`SAMPLE`, `HIDDEN`


### Pre-Exam System Check

**`CheckResult`** — Outcome of one pre-flight check item.  
`NOT_RUN`, `PASSED`, `WARNING`, `FAILED`, `SKIPPED`

**`SystemCheckStatus`** — Overall verdict of a pre-flight check run.  
`IN_PROGRESS`, `PASSED`, `PASSED_WITH_WARNINGS`, `FAILED`, `EXPIRED`

**`SystemCheckType`** — Individual pre-flight check run before an attempt may start.  
`BROWSER_COMPATIBILITY`, `CAMERA`, `MICROPHONE`, `SPEAKER`, `NETWORK_BANDWIDTH`, `SCREEN_SHARE_PERMISSION`, `FULLSCREEN`, `ENVIRONMENT_SCAN`, `OS_COMPATIBILITY`, `SECOND_SCREEN`, `EXCEL_RUNTIME`, `FORMULA_ENGINE`, `WORKBOOK_LOAD`


### Identity Verification

**`VerificationMethod`** — How a candidate's identity was established.  
`FACE_MATCH`, `ID_DOCUMENT`, `MANUAL_PROCTOR`, `KNOWLEDGE_CHALLENGE`, `SECOND_FACTOR`

**`VerificationStatus`** — State of an identity verification attempt.  
`PENDING`, `IN_PROGRESS`, `PASSED`, `FAILED`, `MANUAL_OVERRIDE`, `EXPIRED`


### Exam Session & Attempts

**`AttemptStatus`** — State machine of a single sitting.  
`NOT_STARTED`, `IN_PROGRESS`, `PAUSED`, `SUBMITTED`, `AUTO_SUBMITTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `GRADED`

**`GradingStatus`** — Per-answer grading progress; manual types start at PENDING.  
`NOT_REQUIRED`, `PENDING`, `IN_PROGRESS`, `GRADED`, `REGRADED`

**`PauseRequestStatus`** — State of one request to pause a live attempt.  
`PENDING`, `APPROVED`, `DENIED`, `AUTO_APPROVED`

**`QuestionStateType`** — Navigation state of one question within one attempt.  
`UNSEEN`, `VIEWED`, `ANSWERED`, `FLAGGED`, `SKIPPED`, `LOCKED`

**`ResumptionReason`** — Why a candidate's session had to be re-established mid-attempt.  
`BROWSER_CRASH`, `NETWORK_DROP`, `DEVICE_REBOOT`, `POWER_LOSS`, `PROCTOR_INITIATED`

**`TimingAnomalyType`** — Shape of an implausible answer-timing pattern flagged after grading.  
`TOO_FAST_CORRECT`, `TOO_FAST_HIGH_SCORE`, `ZERO_TIME_CORRECT`, `BURST_SUBMIT`


### Excel Runtime

**`ExcelIntegrityStatus`** — Result of checking a submitted workbook's final hash against what the session actually produced.  
`VALID`, `TAMPERED`, `INCONCLUSIVE`

**`ExcelRuntimeEngine`** — Which spreadsheet runtime hosted one Excel session.  
`LIBREOFFICE`, `ONLYOFFICE`, `SHEETJS_HYPERFORMULA`, `OFFICE_SCRIPTS`

**`ExcelSessionStatus`** — Lifecycle of one candidate's Excel runtime session for an attempt.  
`PENDING`, `ACTIVE`, `SUBMITTED`, `CRASHED`, `RECOVERED`

**`ExcelSheetOperationType`** — A structural change to a sheet within an Excel session, as opposed to a cell-value edit.  
`INSERT`, `DELETE`, `RENAME`, `HIDE`, `UNHIDE`, `PROTECT`, `UNPROTECT`


### Proctoring Sessions

**`AutoAction`** — Automated response the platform applied when the event fired.  
`NONE`, `LOG_ONLY`, `WARN_CANDIDATE`, `PAUSE_ATTEMPT`, `LOCK_SCREEN`, `NOTIFY_PROCTOR`, `TERMINATE_ATTEMPT`

**`CandidateLiveStatus`** — The candidate's own behavioral state, distinct from stream/connection health.  
`ACTIVE`, `IDLE`, `SUSPENDED`

**`ConnectionStatus`** — Websocket/transport state of the candidate's client.  
`CONNECTED`, `UNSTABLE`, `RECONNECTING`, `DISCONNECTED`

**`CustodyTransition`** — One step in an evidence file's chain of custody, from capture to purge.  
`CAPTURED`, `HASHED`, `UPLOADED`, `VERIFIED`, `ACCESSED`, `EXPORTED`, `PURGED`

**`DeviceTrustLevel`** — Standing of one device fingerprint against one exam, built up over sightings.  
`UNKNOWN`, `KNOWN`, `SHARED_SUSPECTED`, `SHARED_CONFIRMED`, `BLOCKED`

**`EventSeverity`** — Severity band of a proctoring event; feeds risk weighting.  
`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

**`EventSource`** — Who or what reported the event.  
`BROWSER_AGENT`, `AI_ENGINE`, `HUMAN_PROCTOR`, `SYSTEM`

**`EvidenceAccessAction`** — Operation performed against an evidence file.  
`VIEW`, `STREAM`, `DOWNLOAD`, `EXPORT`, `SHARE_LINK`, `PURGE`

**`EvidenceKind`** — Type of captured artefact backing an event.  
`WEBCAM_SNAPSHOT`, `WEBCAM_CLIP`, `SCREEN_SNAPSHOT`, `SCREEN_CLIP`, `AUDIO_CLIP`, `ID_DOCUMENT`, `ENVIRONMENT_SCAN`, `KEYSTROKE_LOG`, `EVENT_LOG_BUNDLE`, `EXCEL_WORKBOOK_SNAPSHOT`, `EXCEL_FINAL_WORKBOOK`, `EXCEL_EDIT_LOG_BUNDLE`, `EXCEL_DIFF_REPORT`

**`EvidenceUploadStatus`** — Upload and retention state of an evidence file.  
`PENDING`, `UPLOADING`, `UPLOADED`, `FAILED`, `QUARANTINED`, `PURGED`

**`ProctorActionType`** — What a live human proctor did in response to something observed.  
`WARN`, `MESSAGE`, `FLAG`, `PAUSE`, `RESUME`, `TERMINATE`, `ESCALATE`, `NO_ACTION`

**`ProctorShiftOutcome`** — How a live proctor's shift on one session ended.  
`COMPLETED`, `HANDED_OVER`, `ESCALATED`, `ABANDONED`

**`ProctoringEventType`** — Catalogue of everything worth recording during a session.  
`SESSION_STARTED`, `SESSION_ENDED`, `CONSENT_ACCEPTED`, `IDENTITY_CHECK_PASSED`, `IDENTITY_CHECK_FAILED`, `ENVIRONMENT_SCAN_COMPLETED`, `HEARTBEAT_MISSED`, `NETWORK_DROP`, `CAMERA_BLOCKED`, `CAMERA_DISCONNECTED`, `MICROPHONE_MUTED`, `SCREEN_SHARE_STARTED`, `SCREEN_SHARE_STOPPED`, `FULLSCREEN_ENTERED`, `FULLSCREEN_EXITED`, `TAB_SWITCHED`, `WINDOW_BLURRED`, `WINDOW_FOCUSED`, `BROWSER_HIDDEN`, `BROWSER_VISIBLE`, `COPY_ATTEMPT`, `PASTE_ATTEMPT`, `PRINT_ATTEMPT`, `RIGHT_CLICK_BLOCKED`, `KEYBOARD_SHORTCUT_BLOCKED`, `SUSPICIOUS_KEY_SEQUENCE`, `DEV_TOOLS_OPENED`, `VIRTUAL_MACHINE_SUSPECTED`, `MULTIPLE_DISPLAYS_DETECTED`, `DEVICE_CHANGED`, `NO_FACE_DETECTED`, `MULTIPLE_FACES_DETECTED`, `FACE_MISMATCH`, `GAZE_OFF_SCREEN`, `PROHIBITED_OBJECT_DETECTED`, `VOICE_DETECTED`, `ABNORMAL_BEHAVIOR`, `PROCTOR_MESSAGE`, `PROCTOR_WARNING`, `PROCTOR_MANUAL_FLAG`, `TIME_WARNING_ISSUED`, `ATTEMPT_SUBMITTED`, `ATTEMPT_AUTO_SUBMITTED`, `ATTEMPT_PAUSED`, `ATTEMPT_RESUMED`, `ATTEMPT_TERMINATED`, `EXCEL_CELL_EDIT`, `EXCEL_BULK_PASTE`, `EXCEL_SHEET_OP`, `EXCEL_MACRO_RUN`, `EXCEL_EXTERNAL_LINK`, `EXCEL_ADDIN_LOAD`, `EXCEL_FOCUS_LOST`, `EXCEL_IDLE_ANSWER_CELL`, `EXCEL_ENGINE_ERROR`, `EXCEL_INTEGRITY_FAIL`

**`ProctoringSessionStatus`** — State of the supervision session wrapping an attempt.  
`PENDING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `TERMINATED`, `FAILED`

**`StreamStatus`** — Health of one media stream during live monitoring.  
`UNAVAILABLE`, `ACTIVE`, `DEGRADED`, `INTERRUPTED`, `STOPPED`


### AI Detection

**`ActivityVerdict`** — Human adjudication of a raised alert; feeds model performance.  
`DETECTED`, `CONFIRMED`, `FALSE_POSITIVE`, `UNDER_REVIEW`, `DISMISSED`

**`AudioEventType`** — What the audio model heard in the analysed window.  
`SPEECH_DETECTED`, `MULTIPLE_VOICES`, `BACKGROUND_NOISE`, `AUDIO_INTERRUPTION`, `PROLONGED_SILENCE`, `SUSPICIOUS_AUDIO`

**`BehaviorType`** — Time-window behavior patterns recognised by the behavior model.  
`LOOKING_AWAY`, `LEAVING_SEAT`, `FACE_OUT_OF_FRAME`, `TALKING`, `BACKGROUND_VOICE`, `SUSPICIOUS_MOVEMENT`, `RAPID_TYPING_BURST`, `PROLONGED_IDLE`, `SCREEN_OCCLUSION`, `REPEATED_TAB_SWITCH`

**`DetectedObjectClass`** — Prohibited-item taxonomy recognised by the object model.  
`MOBILE_PHONE`, `BOOK`, `PAPER_NOTES`, `SECOND_LAPTOP`, `SECOND_MONITOR`, `EARPHONE`, `SMARTWATCH`, `CAMERA`, `ADDITIONAL_PERSON`, `UNKNOWN_OBJECT`

**`DetectionType`** — Discriminator for the AI_DETECTIONS specialisation.  
`FACE`, `OBJECT`, `BEHAVIOR`, `AUDIO`

**`GazeDirection`** — Where the candidate was looking in the analysed frame.  
`ON_SCREEN`, `LEFT`, `RIGHT`, `UP`, `DOWN`, `AWAY`, `UNKNOWN`

**`SuspiciousActivityType`** — Alert class raised by the suspicious-activity engine.  
`REPEATED_LOOKING_AWAY`, `MULTIPLE_FACES`, `PHONE_DETECTED`, `NO_FACE`, `SUSPICIOUS_OBJECT`, `TAB_SWITCHING`, `FULLSCREEN_EXIT`, `AUDIO_EVENT`, `IMPERSONATION_SUSPECTED`, `COMBINED_BEHAVIOR`


### Risk Engine

**`RiskLevel`** — Band derived from the numeric risk score.  
`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

**`RiskRecommendation`** — What the scoring model advises; humans may override in review.  
`ALLOW`, `MONITOR`, `FLAG_FOR_REVIEW`, `INVALIDATE_ATTEMPT`, `REQUIRE_RETAKE`


### Review System

**`ItemVerdict`** — Reviewer's judgement on one flagged item.  
`VALID`, `FALSE_POSITIVE`, `NEEDS_INVESTIGATION`, `INCONCLUSIVE`

**`ReviewCaseStatus`** — Workflow state of a human integrity review.  
`OPEN`, `ASSIGNED`, `IN_REVIEW`, `PENDING_INFO`, `ESCALATED`, `RESOLVED`, `CLOSED`

**`ReviewDecisionType`** — Action taken by a reviewer at one step of a case.  
`CLEAR`, `WARN_CANDIDATE`, `ADJUST_SCORE`, `INVALIDATE_ATTEMPT`, `GRANT_RETAKE`, `ESCALATE`, `REQUEST_MORE_INFO`

**`ReviewItemKind`** — What kind of artefact a review finding is about.  
`PROCTORING_EVENT`, `AI_DETECTION`, `SUSPICIOUS_ACTIVITY`, `EVIDENCE_FILE`, `RISK_FACTOR`

**`ReviewOutcome`** — Settled result of a closed case.  
`CLEARED`, `WARNING_ISSUED`, `SCORE_ADJUSTED`, `ATTEMPT_INVALIDATED`, `RETAKE_GRANTED`, `ESCALATED`, `NO_ACTION`

**`ReviewPriority`** — Queue priority of a review case.  
`LOW`, `NORMAL`, `HIGH`, `URGENT`


### Exam Results

**`IntegrityStatus`** — Integrity verdict attached to a released result.  
`CLEAN`, `FLAGGED`, `UNDER_REVIEW`, `INVALIDATED`

**`ResultStatus`** — Release state of a score.  
`PROVISIONAL`, `PENDING_REVIEW`, `WITHHELD`, `FINAL`, `VOID`

**`WithholdingReason`** — Why a result is not yet releasable to the candidate.  
`RISK_THRESHOLD`, `OPEN_REVIEW_CASE`, `PENDING_MANUAL_GRADING`, `APPEAL_FILED`, `EXTERNAL_HOLD`


### Proctoring Reports

**`ReportFormat`** — Rendered format of a proctoring report.  
`PDF`, `HTML`, `JSON`, `ZIP_BUNDLE`

**`ReportStatus`** — Generation state of a proctoring report.  
`QUEUED`, `GENERATING`, `AVAILABLE`, `FAILED`, `EXPIRED`


### Notifications

**`NotificationChannel`** — Transport used to deliver a notification.  
`EMAIL`, `SMS`, `IN_APP`, `PUSH`, `WEBHOOK`

**`NotificationStatus`** — Delivery state of a single notification.  
`PENDING`, `QUEUED`, `SENT`, `DELIVERED`, `OPENED`, `FAILED`, `CANCELLED`

**`NotificationType`** — Business trigger behind a notification.  
`ACCOUNT_VERIFICATION`, `PASSWORD_RESET`, `EXAM_INVITATION`, `EXAM_REMINDER`, `EXAM_STARTED`, `EXAM_SUBMITTED`, `ATTEMPT_TERMINATED`, `REVIEW_ASSIGNED`, `REVIEW_COMPLETED`, `RESULT_PUBLISHED`, `HIGH_RISK_ALERT`, `SYSTEM_ALERT`

**`SuppressionKind`** — The mechanism by which a notification rule holds back or blocks a send.  
`QUIET_HOURS`, `OPT_OUT`, `RATE_LIMIT`, `DUPLICATE_WINDOW`


### Audit

**`AuditAction`** — Auditable operation class.  
`CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `PUBLISH`, `ASSIGN`, `START_ATTEMPT`, `SUBMIT_ATTEMPT`, `GRADE`, `SCORE_OVERRIDE`, `TERMINATE_ATTEMPT`, `VIEW_EVIDENCE`, `EXPORT`, `CONFIG_CHANGE`, `PERMISSION_CHANGE`, `MODEL_CHANGE`, `PAYMENT_METHOD_ADDED`, `PAYMENT_METHOD_REMOVED`, `PAYMENT_METHOD_SET_DEFAULT`, `PAYMENT_REFUNDED`, `PAYMENT_DISPUTED`

**`AuditOutcome`** — Whether the audited operation was carried out.  
`SUCCESS`, `FAILURE`, `DENIED`


### AI Model Registry

**`MetricType`** — Measured model performance metric.  
`PRECISION`, `RECALL`, `F1_SCORE`, `ACCURACY`, `FALSE_POSITIVE_RATE`, `FALSE_NEGATIVE_RATE`, `LATENCY_P95_MS`, `THROUGHPUT_FPS`

**`ModelPurpose`** — What an AI model is responsible for.  
`FACE_DETECTION`, `FACE_VERIFICATION`, `HEAD_POSE`, `GAZE_ESTIMATION`, `OBJECT_DETECTION`, `PERSON_DETECTION`, `SPEECH_DETECTION`, `VOICE_DIARIZATION`, `BEHAVIOR_ANALYSIS`, `RISK_SCORING`, `EXCEL_ANOMALY_DETECTION`

**`ModelVersionStatus`** — Rollout state of one model version.  
`DRAFT`, `CANDIDATE`, `SHADOW`, `ACTIVE`, `DEPRECATED`, `RETIRED`

**`ShadowRecommendation`** — What a shadow-vs-active comparison recommends for the shadow version.  
`PROMOTE`, `HOLD`, `RETIRE`


### System Configuration

**`RiskFactorTrigger`** — What kind of signal a risk rule reacts to.  
`PROCTORING_EVENT`, `AI_DETECTION`, `SUSPICIOUS_ACTIVITY`, `DEVICE_SIGNAL`, `IDENTITY_SIGNAL`

**`SettingCategory`** — Configuration area a setting belongs to.  
`GENERAL`, `PROCTORING`, `RISK`, `EXAM`, `STORAGE`, `NOTIFICATION`, `AI`, `SECURITY`, `EXCEL`

**`SettingValueType`** — How a setting's stored string is to be parsed.  
`STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `JSON`, `DURATION`, `ENUM`

