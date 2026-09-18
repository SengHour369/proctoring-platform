# Full Rule Logic of Every Function

This is the complete, formal rule logic — the **decision procedure** each function executes,
expressed as invariants, preconditions, postconditions, and the exact order of checks. Every
rule cites the entity, field, or enum it depends on. Nothing here is a method signature; it is
the **logic** the method must implement.

Like [`service-layer-plan.md`](service-layer-plan.md), **nothing here is implemented** — this
repo is entities only, by design. This document is one level more formal than that plan: where
the plan lists functions and one-line rules, this spells out the exact precondition/step/
postcondition sequence, so an implementer isn't left inferring order from prose.

Every entity name, field name, and enum constant below was checked against the actual source —
see the note at the end for the handful of corrections that check turned up.

---

## How to Read This

For each function:

- **Preconditions** — what must be true before the call is valid. If false, the call is rejected.
- **Invariants** — what must remain true throughout. If violated, the transaction rolls back.
- **Steps** — the exact order of operations.
- **Postconditions** — what must be true after a successful call.
- **Rejection reasons** — every reason the call can fail, and what is written when it does.

The order matters. A precondition checked after a write is a bug. An audit row written after the
business row is a bug. A token hashed before it is generated is a bug.

---

# Module 1 — Authentication & Access

## 1.1 `AuthService.login(email, password)`

### Preconditions
- `email` is non-null, normalized (lowercase, trimmed).
- `password` is non-null.
- The caller is unauthenticated — no session is required.

### Invariants
- No response reveals whether `email` matches a `User` row.
- Every call writes **exactly one** `LoginAttempt` row, regardless of outcome.
- The plaintext `password` is never logged, never stored, never returned.

### Steps

**Step 1 — Normalize and look up.**
- Normalize `email`.
- Query `User` by `email`.
- If no row → write `LoginAttempt { userId: null, email, outcome: BAD_CREDENTIALS, attemptedAt: now, ipAddress, userAgent, deviceFingerprint, geoCountry }`. Return generic failure. **Stop.**
- If row exists → hold `user`.

**Step 2 — Status gate (fail closed).**
- If `user.status = PENDING_VERIFICATION` → write `LoginAttempt { userId, outcome: EMAIL_NOT_VERIFIED }`. Return. **Stop.**
- If `user.status = SUSPENDED` → write `LoginAttempt { userId, outcome: ACCOUNT_DISABLED }`. Return. **Stop.**
- If `user.status = DISABLED` → write `LoginAttempt { userId, outcome: ACCOUNT_DISABLED }`. Return. **Stop.**
- If `user.status ≠ ACTIVE` → write `LoginAttempt { userId, outcome: ACCOUNT_DISABLED }`. Return. **Stop.**

**Step 3 — Lock gate.**
- If `user.lockedUntil != null` AND `user.lockedUntil > now` → write `LoginAttempt { userId, outcome: ACCOUNT_LOCKED }`. Return. **Stop.**

**Step 4 — Password verification.**
- Compute `hash(password)` using the same algorithm and salt parameters as `user.passwordHash`.
- If mismatch:
  - `user.failedLoginCount = user.failedLoginCount + 1`.
  - If `user.failedLoginCount >= configuredThreshold`:
    - `user.lockedUntil = now + configuredLockDuration`.
    - `user.failedLoginCount = 0` (reset on lock, so the next window starts clean).
  - Write `LoginAttempt { userId, outcome: BAD_CREDENTIALS, failureDetail: 'password_mismatch' }`.
  - Return generic failure. **Stop.**

**Step 5 — MFA gate.**
- If `user.mfaEnabled = true`:
  - Write `LoginAttempt { userId, outcome: MFA_REQUIRED }`.
  - Generate a short-lived MFA challenge (held in-memory/cache, not a database row — see the
    correction note at the end on why this isn't a `SecurityToken`).
  - Return `{ mfaRequired: true, challengeToken }`. **Stop.** No `UserSession` is created yet.

**Step 6 — Session issuance (MFA off, or MFA already passed).**
- Reset `user.failedLoginCount = 0`.
- Set `user.lastLoginAt = now`.
- Generate a cryptographically random refresh token (≥ 256 bits).
- Compute `refreshTokenHash = hash(refreshToken)`.
- Insert `UserSession { userId, refreshTokenHash, ipAddress, userAgent, deviceFingerprint, issuedAt: now, expiresAt: now + configuredTTL, lastSeenAt: now }`.
- Generate a short-lived access token (JWT or opaque), scoped to `user.id`.
- Write `LoginAttempt { userId, outcome: SUCCESS }`.
- Return `{ accessToken, refreshToken, sessionId }`.

### Postconditions
- Exactly one `LoginAttempt` row written.
- On success: exactly one `UserSession` row written with a **hashed** refresh token.
- On failure: no `UserSession` row written.
- `user.failedLoginCount` is 0 on success, incremented on `BAD_CREDENTIALS`, reset to 0 on lock.

### Rejection reasons
| Condition | `LoginOutcome` written |
|---|---|
| No `User` row | `BAD_CREDENTIALS` (user null) |
| Status not `ACTIVE` | `EMAIL_NOT_VERIFIED` / `ACCOUNT_DISABLED` |
| `lockedUntil` in future | `ACCOUNT_LOCKED` |
| Password mismatch | `BAD_CREDENTIALS` |
| MFA enabled, first factor passed | `MFA_REQUIRED` |

---

## 1.2 `AuthService.verifyMfaCode(userId, code)`

### Preconditions
- `userId` refers to a `User` with `mfaEnabled = true`.
- `code` is non-null.
- A valid MFA challenge is in flight for this `userId` (from step 5 of login).

### Invariants
- The MFA code is single-use.
- The plaintext TOTP seed is never returned.

### Steps

**Step 1 — Load and gate.**
- Load `User`. If `mfaEnabled = false` → reject with `MFA_FAILED`. **Stop.**
- If `mfaMethod = null` → reject with `MFA_FAILED`. **Stop.**

**Step 2 — Method dispatch.**
- If `mfaMethod = TOTP`:
  - Decrypt `user.mfaSecretEncrypted`.
  - Verify `code` against the current TOTP window (± 1 step tolerance).
- If `mfaMethod = SMS` or `EMAIL`:
  - Load the one-time code issued to `user.phoneNumber` / `user.email`.
  - Verify `code` matches and is not expired.

**Step 3 — Outcome.**
- On success:
  - Reset `user.failedLoginCount = 0`.
  - Set `user.lastLoginAt = now`.
  - Issue `UserSession` exactly as in login step 6.
  - Write `LoginAttempt { userId, outcome: SUCCESS }`.
  - Return `{ accessToken, refreshToken, sessionId }`.
- On failure:
  - Increment `user.failedLoginCount`.
  - If threshold crossed → set `user.lockedUntil`.
  - Write `LoginAttempt { userId, outcome: MFA_FAILED }`.
  - Return failure. **Stop.**

### Postconditions
- On success: exactly one `UserSession` row, one `LoginAttempt { outcome: SUCCESS }`.
- On failure: one `LoginAttempt { outcome: MFA_FAILED }`, no session.

---

## 1.3 `AuthService.logout(sessionId)`

### Preconditions
- `sessionId` refers to an existing `UserSession`.

### Invariants
- The session row is **never deleted** — only revoked.

### Steps
1. Load `UserSession` by `sessionId`.
2. If already `revokedAt != null` → idempotent no-op, return success.
3. Set `revokedAt = now`, `revokedReason = 'user_logout'`.
4. Write `AuditLog { action: LOGOUT, actorUserId: session.user, entityType: 'UserSession', entityId: sessionId, occurredAt: now }`.

### Postconditions
- `UserSession.revokedAt` is set.
- One `AuditLog` row.

---

## 1.4 `AuthService.refreshToken(refreshToken)`

### Preconditions
- `refreshToken` is non-null.

### Invariants
- The plaintext refresh token is never stored or queried back.
- A revoked session cannot be refreshed.

### Steps
1. Compute `hash(refreshToken)`.
2. Query `UserSession` by `refreshTokenHash`.
3. If no row → reject. **Stop.**
4. If `revokedAt != null` → reject. **Stop.**
5. If `expiresAt <= now` → reject. **Stop.**
6. Load `User` by `session.user`.
7. If `user.status ≠ ACTIVE` → reject. **Stop.**
8. Generate a new access token.
9. **Optional rotation:** generate a new refresh token, set `session.refreshTokenHash = hash(new)`, `session.expiresAt = now + TTL`. Return both.
10. Set `session.lastSeenAt = now`.

### Postconditions
- On success: new access token returned; if rotation enabled, `refreshTokenHash` updated.
- On failure: no change.

---

## 1.5 `AuthService.forgotPassword(email)`

### Preconditions
- `email` is non-null, normalized.

### Invariants
- The response is **identical** whether or not the email matches a `User`.
- The plaintext reset token is never stored.
- A new token does not invalidate a previously issued one unless policy says so (default: both remain valid until used or expired).

### Steps
1. Normalize `email`.
2. Query `User` by `email`.
3. **Always** return the same generic success response.
4. If a `User` row exists:
   - Generate a random reset token (≥ 256 bits).
   - Compute `tokenHash = hash(token)`.
   - Insert `SecurityToken { userId, purpose: PASSWORD_RESET, tokenHash, issuedAt: now, expiresAt: now + configuredTTL }`.
   - Dispatch the reset email with the **plaintext** token in the link.
   - Write `AuditLog { action: UPDATE, actorUserId: null, entityType: 'SecurityToken', entityId: token.id, reason: 'password_reset_requested' }`.

### Postconditions
- On success: possibly one `SecurityToken` row, one email dispatched.
- The caller cannot distinguish "user exists" from "user does not exist".

---

## 1.6 `AuthService.resetPassword(token, newPassword)`

### Preconditions
- `token` is non-null.
- `newPassword` passes the password policy.

### Invariants
- The token is single-use.
- The token row is **never deleted** after use.
- The token's `purpose` must match `PASSWORD_RESET`.

### Steps
1. Compute `tokenHash = hash(token)`.
2. Query `SecurityToken` by `tokenHash`.
3. If no row → reject. **Stop.**
4. If `purpose ≠ PASSWORD_RESET` → reject. **Stop.**
5. If `usedAt != null` → reject (single-use). **Stop.**
6. If `invalidatedAt != null` → reject. **Stop.**
7. If `expiresAt <= now` → reject. **Stop.**
8. Validate `newPassword` against policy (length, complexity, not in breach list).
9. Load `User` by `token.user`.
10. Set `user.passwordHash = hash(newPassword)`.
11. Set `token.usedAt = now`, `token.redeemedIp = request IP`.
12. **Do not delete** the token row.
13. Revoke all existing `UserSession` rows for `user.id` (set `revokedAt`, `revokedReason = 'password_reset'`).
14. Write `AuditLog { action: UPDATE, actorUserId: user, entityType: 'User', entityId: user.id, reason: 'password_reset' }`.

### Postconditions
- `user.passwordHash` updated.
- `token.usedAt` set.
- All `UserSession` rows for the user revoked.
- One `AuditLog` row.

### Rejection reasons
| Condition | Result |
|---|---|
| Token not found | reject |
| Wrong purpose | reject |
| Already used | reject |
| Invalidated | reject |
| Expired | reject |
| Weak password | reject |

---

## 1.7 `AuthService.verifyEmail(token)`

### Preconditions
- `token` is non-null.

### Invariants
- This is the **only** path from `PENDING_VERIFICATION` to `ACTIVE`.
- The token is single-use.

### Steps
1. Compute `tokenHash = hash(token)`.
2. Query `SecurityToken` by `tokenHash`.
3. If no row → reject. **Stop.**
4. If `purpose ≠ EMAIL_VERIFICATION` → reject. **Stop.**
5. If `usedAt != null` → reject. **Stop.**
6. If `expiresAt <= now` → reject. **Stop.**
7. Load `User` by `token.user`.
8. Set `user.status = ACTIVE`.
9. Set `user.emailVerifiedAt = now`.
10. Set `token.usedAt = now`, `token.redeemedIp = request IP`.
11. Write `AuditLog { action: UPDATE, actorUserId: user.id, entityType: 'User', entityId: user.id, beforeState: { status: PENDING_VERIFICATION }, afterState: { status: ACTIVE } }`.

### Postconditions
- `user.status = ACTIVE`, `user.emailVerifiedAt` set.
- `token.usedAt` set.
- One `AuditLog` row.

---

## 1.8 `AuthService.enrollMfa(userId, method)`

### Preconditions
- `userId` refers to an `ACTIVE` `User`.
- `method` ∈ `MfaMethod`.

### Invariants
- The plaintext TOTP seed is returned exactly once.
- SMS/EMAIL methods reuse `phoneNumber`/`email` — no separate address column.

### Steps
1. Load `User`.
2. If `method = TOTP`:
   - Generate a random TOTP seed.
   - Encrypt with the platform key → `mfaSecretEncrypted`.
   - Return the plaintext seed (for QR) **once**.
3. If `method = SMS` or `EMAIL`:
   - Verify `user.phoneNumber` / `user.email` is present and verified.
   - No secret stored — `mfaSecretEncrypted` stays null.
4. Set `user.mfaMethod = method`, `user.mfaEnabled = true`, `user.mfaEnrolledAt = now`.
5. Write `AuditLog { action: UPDATE, actorUserId: user, entityType: 'User', entityId: user.id, reason: 'mfa_enrolled' }`.

### Postconditions
- `user.mfaEnabled = true`, `user.mfaMethod` set.
- For TOTP: `user.mfaSecretEncrypted` set.

---

## 1.9 `RoleService.deleteRole(roleId)`

### Preconditions
- `roleId` refers to an existing `Role`.

### Invariants
- A `Role` with `system = true` cannot be deleted.
- A `Role` with `system = true` cannot have its `code` changed.

### Steps
1. Load `Role`.
2. If `role.system = true` → reject. **Stop.**
3. Check for `UserRole` rows referencing `roleId`.
4. If any exist → either reject, or (per policy) close them by setting `expiresAt = now` rather
   than deleting — a `UserRole` grant is deactivated the same way every other grant in this model
   is, by expiry, not by row removal.
5. Check for `RolePermission` rows referencing `roleId` → delete them (they are pure grants with
   no independent lifecycle of their own — see the correction note on why this is fine here but
   not for `UserRole`).
6. Delete the `Role` row.
7. Write `AuditLog { action: PERMISSION_CHANGE, actorUserId, entityType: 'Role', entityId: roleId, beforeState: {...}, afterState: null, reason: required }`.

### Postconditions
- `Role` row deleted (unless a soft-delete policy is chosen instead).
- `RolePermission` rows deleted.
- One `AuditLog` row with `reason`.

### Rejection reasons
- `system = true`.
- Referenced by `UserRole` (if policy blocks deletion).

---

## 1.10 `RoleService.grantRole(userId, roleId, grantedBy, expiresAt)`

### Preconditions
- `userId` refers to an existing `User`.
- `roleId` refers to an existing `Role`.
- `grantedBy` refers to an existing `User` (or null for system grants).

### Invariants
- A grant is an entity, not a bare pair.
- `expiresAt` may be null (permanent), but if set, it must be in the future.

### Steps
1. Validate `User` exists, `Role` exists.
2. If `expiresAt != null` AND `expiresAt <= now` → reject. **Stop.**
3. Check for an existing `UserRole` row for `(userId, roleId)` that is not expired.
4. If exists → either update `expiresAt`, or reject as duplicate (per policy). Default: update.
5. Insert `UserRole { userId, roleId, grantedByUserId, grantedAt: now, expiresAt }`.
6. Write `AuditLog {action: PERMISSION_CHANGE, actorUserId: grantedBy, entityType: 'UserRole', entityId: newRow.id, after: {...}, reason: required }`.

### Postconditions
- One `UserRole` row.
- One `AuditLog` row with `reason`.

---

## 1.11 `RoleService.checkAccess(userId, permissionCode)`

### Preconditions
- `userId` refers to an existing `User`.
- `permissionCode` is a non-null string.

### Invariants
- An expired grant is inert — no cleanup job is needed.
- The result is a pure function of the current `UserRole` + `RolePermission` + `Permission` state.

### Steps
1. Query all `UserRole` rows for `userId`.
2. Filter out rows where `expiresAt != null AND expiresAt <= now`.
3. For each surviving `UserRole`:
   - Query `RolePermission` rows for `roleId`.
   - Query `Permission` rows for each `permissionId`.
   - Collect `Permission.code` values.
4. Union the collected codes.
5. Return `permissionCode ∈ union`.
6. (Optional) cache keyed by `(userId, permissionCode)` with TTL ≤ the nearest `UserRole.expiresAt`.

### Postconditions
- Returns boolean. No write. No audit (read-only).

---

## 1.12 `ApiClientService.registerClient(name, allowedScopes)`

### Preconditions
- `name` is non-null.
- `allowedScopes` is a space-separated string of valid permission codes.

### Invariants
- The plaintext secret is returned **exactly once**.
- Only `clientSecretHash` is stored.

### Steps
1. Validate `allowedScopes` against the known permission vocabulary.
2. Generate `clientId` (random, URL-safe).
3. Generate a random plaintext `secret` (≥ 256 bits).
4. Compute `clientSecretHash = hash(secret)`.
5. Insert `ApiClient { clientId, name, clientSecretHash, allowedScopes, status: ACTIVE, rateLimitPerMinute, allowedIpRanges, createdAt: now }`.
6. Write `AuditLog { action: CREATE, actorUserId, entityType: 'ApiClient', entityId: newRow.id }`.
7. Return `{ clientId, secret }` — the secret is **never** returned again.

### Postconditions
- One `ApiClient` row with a hashed secret.
- The plaintext secret is in the response and nowhere else.

---

## 1.13 `ApiClientService.authenticate(clientId, secret)`

### Preconditions
- `clientId` and `secret` are non-null.
- The request carries an IP address.

### Invariants
- A request outside `allowedIpRanges` or over `rateLimitPerMinute` is rejected **before** any business logic.
- The plaintext secret is never logged.

### Steps
1. Load `ApiClient` by `clientId`.
2. If no row → reject. **Stop.**
3. If `status ≠ ACTIVE` → reject. **Stop.**
4. If `expiresAt != null AND expiresAt <= now` → reject. **Stop.**
5. If `revokedAt != null` → reject. **Stop.**
6. Compute `hash(secret)`. If ≠ `clientSecretHash` → reject. **Stop.**
7. If `allowedIpRanges != null`:
   - Parse the request IP.
   - If IP not in any CIDR range → reject. **Stop.**
8. If `rateLimitPerMinute != null`:
   - Count requests for this `clientId` in the last 60 seconds.
   - If count ≥ `rateLimitPerMinute` → reject. **Stop.**
9. Set `lastUsedAt = now`, `lastUsedIp = request IP`.
10. Return `{ clientId, allowedScopes }`.

### Postconditions
- On success: `lastUsedAt` / `lastUsedIp` updated.
- On failure: no write, no business logic reached.

---

# Module 2 — Student Groups

## 2.1 `StudentGroupService.createGroup(name, groupType, parentGroupId?)`

### Preconditions
- `name` non-null.
- `groupType` ∈ `GroupType`.
- If `parentGroupId` set, it refers to an existing `StudentGroup`.

### Invariants
- No cycle in the parent chain.

### Steps
1. If `parentGroupId != null`:
   - Load parent.
   - Walk the parent chain upward.
   - If `groupId` would appear as its own ancestor → reject (cycle). **Stop.**
2. Insert `StudentGroup { code, name, description, groupType, parentGroupId, ownerUserId, academicTerm, active: true }`.
3. Write `AuditLog { action: CREATE, entityType: 'StudentGroup', entityId: newRow.id }`.

### Postconditions
- One `StudentGroup` row.
- No cycle.

---

## 2.2 `StudentGroupService.addMember(groupId, userId)`

### Preconditions
- `groupId` and `userId` refer to existing rows.
- The `StudentGroup.active = true`.

### Invariants
- A membership is never duplicated.

### Steps
1. Load `StudentGroup`. If `active = false` → reject. **Stop.**
2. Query `GroupMembership` for `(groupId, userId)` where `active = true`.
3. If exists → idempotent no-op, return existing. **Stop.**
4. Insert `GroupMembership { studentGroupId, userId, addedByUserId, joinedAt: now, active: true }`.
5. If `StudentGroup` has any `ExamGroupAssignment` with `autoEnrollNewMembers = true`:
   - For each such assignment, call `ExamAssignmentService.assignToStudent(examId, userId)`.
6. Write `AuditLog { action: ASSIGN, entityType: 'GroupMembership', entityId: newRow.id }`.

### Postconditions
- One `GroupMembership` row.
- If auto-enroll, one `ExamAssignment` per active `ExamGroupAssignment`.
- One `AuditLog` row.

---

## 2.3 `StudentGroupService.removeMember(groupId, userId)`

### Preconditions
- A `GroupMembership` row exists for `(groupId, userId)` with `active = true`.

### Invariants
- The row is **never deleted**.
- An exam assigned while the member was active must stay explicable.

### Steps
1. Load the `GroupMembership` row.
2. If `active = false` → idempotent no-op. **Stop.**
3. Set `leftAt = now`, `active = false`.
4. Write `AuditLog { action: UPDATE, entityType: 'GroupMembership', entityId: row.id }`.

### Postconditions
- `GroupMembership.leftAt` set, `active = false`.
- No `ExamAssignment` is cancelled — the historical entitlement stays.

---

# Module 3 — Exam Definition

## 3.1 `ExamService.createExam(...)`

### Preconditions
- `createdByUserId` refers to a `User` with `exam:create` permission.
- `title` non-null.
- `closesAt > opensAt` (if both set).
- `maxAttempts >= 1`.

### Invariants
- A new exam starts in `DRAFT`.

### Steps
1. Check `checkAccess(createdByUserId, 'exam:create')`. If false → reject. **Stop.**
2. Validate `closesAt > opensAt`. If not → reject. **Stop.**
3. Validate `maxAttempts >= 1`. If not → reject. **Stop.**
4. Insert `Exam { publicId: UUID, code, title, description, instructions, createdByUserId, status: DRAFT, version: 1, durationMinutes, opensAt, closesAt, maxAttempts, totalPoints, passingScore, gradingMode, shuffleSections, holdResultsForReview, showResultImmediately, proctoringPolicy }`.
5. Write `AuditLog { action: CREATE, entityType: 'Exam', entityId: newRow.id }`.

### Postconditions
- One `Exam` row in `DRAFT`.
- One `AuditLog` row.

---

## 3.2 `ExamService.publishExam(examId)`

### Preconditions
- `Exam` exists and `status ∈ {DRAFT, SCHEDULED}`.
- At least one `ExamSection` exists for the exam.
- At least one `ExamQuestion` exists across all sections.
- `closesAt > opensAt`.
- `maxAttempts >= 1`.

### Invariants
- Structure freezes at publish — subsequent structural edits bump `Exam.version`.

### Steps
1. Load `Exam`.
2. If `status ∉ {DRAFT, SCHEDULED}` → reject. **Stop.**
3. Count `ExamSection` rows where `examId` matches. If 0 → reject. **Stop.**
4. Count `ExamQuestion` rows across all of the exam's sections. If 0 → reject. **Stop.**
5. Validate `closesAt > opensAt`. If not → reject. **Stop.**
6. Validate `maxAttempts >= 1`. If not → reject. **Stop.**
7. Validate each section:
   - If `questionsToDraw != null`, ensure `questionsToDraw <= count(ExamQuestion for that section)`.
   - If `timeLimitMinutes != null`, ensure sum of section time limits ≤ exam duration (if duration set).
8. Validate each question placement:
   - `points` non-null.
   - `sequenceNo` unique per section.
9. Set `Exam.status = PUBLISHED`.
10. Write `AuditLog { action: PUBLISH, entityType: 'Exam', entityId: examId }`.

### Postconditions
- `Exam.status = PUBLISHED`.
- One `AuditLog` row.
- Structure is frozen.

### Rejection reasons
- No sections.
- No questions.
- `closesAt <= opensAt`.
- `maxAttempts < 1`.
- `questionsToDraw > available`.

---

## 3.3 `ExamService.updateExam(examId, changes)`

### Preconditions
- `Exam` exists.

### Invariants
- A structural edit after publish bumps `Exam.version` rather than mutating.
- An attempt in progress keeps referencing `ExamAttempt.examVersion`.

### Steps
1. Load `Exam`.
2. Classify `changes`:
   - **Non-structural:** `title`, `description`, `instructions`, `showResultImmediately`.
   - **Structural:** add/remove section, add/remove question, change `points`, change `sequenceNo`, change `shuffleQuestions`, change `questionsToDraw`, change `proctoringPolicy`.
3. If `status = DRAFT`:
   - Apply all changes in place.
4. If `status` past `DRAFT`:
   - If non-structural → apply in place.
   - If structural:
     - **Do not mutate** the live row's structure.
     - Create a new `Exam` row with `version = old.version + 1`, copying all fields (a full
       re-insert of the sections/questions/policy under the new exam id — this model has no
       association to "clone" through, so the copy is an explicit, ordered set of inserts).
     - Apply the structural change to the new row.
     - Set the old row's status per policy (e.g. `ARCHIVED` if replaced, or keep both with
       `version` discriminating).
     - Existing `ExamAttempt` rows keep `examVersion = old.version` and their `examId` pointing
       at the old row — they are not migrated to the new version.
5. Write `AuditLog { action: UPDATE, entityType: 'Exam', entityId: examId, beforeState: {...}, afterState: {...} }`.

### Postconditions
- Non-structural: `Exam` updated in place.
- Structural: new `Exam` version row; old row preserved.
- `ExamAttempt.examVersion` unchanged for in-progress attempts.

---

## 3.4 `ExamService.activateExam(examId)` / `closeExam(examId)` / `archiveExam(examId)`

### Preconditions
- `activateExam`: `status = PUBLISHED`.
- `closeExam`: `status = ACTIVE`.
- `archiveExam`: `status ∈ {CLOSED, PUBLISHED}`.

### Steps
1. Load `Exam`.
2. Validate the transition:
   - `PUBLISHED → ACTIVE`.
   - `ACTIVE → CLOSED`.
   - `CLOSED → ARCHIVED` (or `PUBLISHED → ARCHIVED`).
3. Set `status` to the target.
4. Write `AuditLog`.

### Postconditions
- `Exam.status` transitioned.
- One `AuditLog` row.

---

## 3.5 `ExamPrerequisiteService.addRule(examId, requiredExamId?, minScore?, courseReference?)`

### Preconditions
- `examId` refers to an existing `Exam`.
- If `requiredExamId` set, it refers to an existing `Exam` ≠ `examId`.
- If `courseReference` set, `requiredExamId` must be null (mutually exclusive).

### Invariants
- A prerequisite is 1:many, not embedded.
- `isActive` lets a rule retire without losing history.

### Steps
1. Validate `examId` exists.
2. If `requiredExamId != null`:
   - Validate it exists and ≠ `examId`.
   - If `courseReference != null` → reject (mutually exclusive). **Stop.**
3. If `courseReference != null`:
   - Validate non-empty.
4. Insert `ExamPrerequisite { examId, requiredExamId, minScore, courseReference, description, active: true }`.
5. Write `AuditLog`.

### Postconditions
- One `ExamPrerequisite` row.

---

## 3.6 `ExamPrerequisiteService.checkEligibility(examId, candidateId)`

### Preconditions
- `examId` and `candidateId` refer to existing rows.

### Invariants
- **All** active rules must pass (AND, not OR).
- An inactive rule is skipped, not evaluated as satisfied.

### Steps
1. Query `ExamPrerequisite` rows for `examId`.
2. Filter to `isActive = true`.
3. For each active rule:
   - If `requiredExamId != null`:
     - Query `ExamResult` rows for `(candidateId, requiredExamId)`.
     - Pick the best by `finalScore`.
     - If none, or `passed = false`, or `finalScore < minScore` → rule fails.
   - If `courseReference != null`:
     - Delegate to the external LMS integration.
     - If the integration is unavailable → rule fails closed (reject).
4. If **any** rule fails → return `{ eligible: false, failedRules: [...] }`.
5. If **all** rules pass → return `{ eligible: true }`.

### Postconditions
- Read-only. No write.

---

## 3.7 `ExamAssignmentService.assignToStudent(examId, candidateId)`

### Preconditions
- `examId` and `candidateId` refer to existing rows.
- The caller has `exam:assign` permission.

### Invariants
- `(examId, candidateUserId)` is unique.
- Prerequisite check runs **before** the row is created.

### Steps
1. Check `checkAccess(actorId, 'exam:assign')`. If false → reject. **Stop.**
2. Load `Exam`.
3. If `Exam.status ∉ {PUBLISHED, ACTIVE, SCHEDULED}` → reject. **Stop.**
4. Run `ExamPrerequisiteService.checkEligibility(examId, candidateId)`.
   - If not eligible → **block the assignment outright**. Do not create a row the candidate can never start. **Stop.**
5. Query existing `ExamAssignment` for `(examId, candidateId)`.
6. If exists:
   - Update the existing row (window, attempts, extra time).
   - Do **not** duplicate.
7. If new:
   - Validate `windowStartAt` / `windowEndAt`:
     - If both set, `windowStartAt < windowEndAt`.
     - If `Exam.opensAt` set, `windowStartAt >= Exam.opensAt`.
     - If `Exam.closesAt` set, `windowEndAt <= Exam.closesAt`.
     - Wider than the exam → reject. **Stop.**
   - Insert `ExamAssignment { examId, candidateUserId, assignedByUserId, status: ASSIGNED, assignedAt: now, windowStartAt, windowEndAt, dueAt, attemptsAllowed, extraTimeMinutes, accessCodeHash }`.
8. Write `AuditLog { action: ASSIGN, entityType: 'ExamAssignment', entityId: row.id }`.

### Postconditions
- One `ExamAssignment` row (created or updated).
- One `AuditLog` row.

### Rejection reasons
- No permission.
- Exam not in an assignable status.
- Prerequisite failed.
- Window wider than the exam.

---

## 3.8 `ExamAssignmentService.assignToGroup(examId, groupId)`

### Preconditions
- `examId` and `groupId` refer to existing rows.

### Invariants
- Group assignment **fans out** into per-candidate assignments.
- `ExamAssignment` remains the single source of truth.

### Steps
1. Check permission.
2. Load `Exam`, `StudentGroup`.
3. Validate `windowStartAt` / `windowEndAt` against `Exam` (same as 3.7).
4. Insert `ExamGroupAssignment { examId, studentGroupId, assignedByUserId, assignedAt: now, windowStartAt, windowEndAt, dueAt, autoEnrollNewMembers, expandedCount: 0 }`.
5. Query active `GroupMembership` rows for `groupId`.
6. For each member:
   - Call `assignToStudent(examId, member.userId)`.
   - Increment `expandedCount`.
7. Set `expandedAt = now`.
8. Write `AuditLog`.

### Postconditions
- One `ExamGroupAssignment` row.
- One `ExamAssignment` per active member.
- One `AuditLog` row.

---

## 3.9 `ExamAssignmentService.cancelAssignment(assignmentId, reason)`

### Preconditions
- `ExamAssignment` exists and `status ∉ {SUBMITTED, EXPIRED}`.

### Invariants
- Cancellation is a status change, not a deletion.

### Steps
1. Load `ExamAssignment`.
2. If `status = SUBMITTED` → reject (already sat). **Stop.**
3. Set `status = CANCELLED`, `cancelledAt = now`, `cancelReason = reason`.
4. Write `AuditLog { action: UPDATE, entityType: 'ExamAssignment', entityId: assignmentId, reason }`.

### Postconditions
- `ExamAssignment.status = CANCELLED`.
- One `AuditLog` row.

---

## 3.10 `ExamInvitationService.sendInvitation(assignmentId)`

### Preconditions
- `ExamAssignment` exists and `status ∈ {ASSIGNED, NOTIFIED}`.

### Invariants
- An invitation is separate from the assignment.
- The plaintext token is never stored.

### Steps
1. Load `ExamAssignment`, `Exam`, `User`.
2. Generate a random invitation token.
3. Compute `tokenHash = hash(token)`.
4. Insert `ExamInvitation { examAssignmentId, channel, status: PENDING, sentTo, tokenHash, sequenceNo: 1, reminder: false, expiresAt: now + TTL }`.
5. Dispatch via the channel.
6. Update `status = SENT`, `sentAt = now`.
7. Set `ExamAssignment.status = NOTIFIED`, `notifiedAt = now`.
8. Write `AuditLog`.

### Postconditions
- One `ExamInvitation` row.
- `ExamAssignment.status = NOTIFIED`.

---

## 3.11 `ExamInvitationService.resend(assignmentId)`

### Preconditions
- `ExamAssignment` exists.

### Invariants
- A resend creates a **new** row — never reuses or extends the previous token.

### Steps
1. Query max `sequenceNo` for the assignment's invitations.
2. Generate a fresh random token.
3. Compute `tokenHash = hash(token)`.
4. Insert `ExamInvitation { examAssignmentId, channel, status: PENDING, sentTo, tokenHash, sequenceNo: prev + 1, reminder: true, expiresAt: now + TTL }`.
5. Dispatch.
6. Update `status = SENT`, `sentAt = now`.
7. Write `AuditLog`.

### Postconditions
- New `ExamInvitation` row.
- Previous invitation(s) unchanged.

---

## 3.12 `ExamInvitationService.recordOpen(invitationId)` / `recordAccept(invitationId)`

### Preconditions
- `ExamInvitation` exists.
- The presented token hashes to `tokenHash`.

### Steps
1. Load `ExamInvitation`.
2. Verify the token hash matches.
3. If `expiresAt <= now` → reject. **Stop.**
4. Set `openedAt = now` (or `acceptedAt = now`).
5. Update `status = OPENED` (or `ACCEPTED`).
6. Write `AuditLog`.

### Postconditions
- `ExamInvitation` state updated.

---

## 3.13 `RetakeGrantService.grantRetake(assignmentId, reviewDecisionId, additionalAttempts, reason)`

### Preconditions
- `assignmentId` refers to an existing `ExamAssignment`.
- `reviewDecisionId` refers to a `ReviewDecision` with `decisionType = GRANT_RETAKE`.
- `reason` non-null.

### Invariants
- A grant cannot exist without the decision behind it.
- `additionalAttempts` never mutates `ExamAssignment.attemptsAllowed` or `Exam.maxAttempts`.

### Steps
1. Load `ExamAssignment`; load `ReviewDecision`.
2. If `ReviewDecision.decisionType ≠ GRANT_RETAKE` → reject. **Stop.**
3. Insert `RetakeGrant { examAssignmentId, candidateUserId, reviewDecisionId, grantedByUserId, grantedAt: now, additionalAttempts, expiresAt, reason }`.
4. Write `AuditLog { action: PERMISSION_CHANGE, reason }`.

### Postconditions
- One `RetakeGrant` row, unconsumed.

## 3.14 `RetakeGrantService.consume(grantId, attemptId)`

### Preconditions
- `RetakeGrant.consumedAt = null`.
- `RetakeGrant.expiresAt = null OR expiresAt > now`.

### Steps
1. Load `RetakeGrant`. If already consumed → reject. **Stop.** If expired → reject. **Stop.**
2. Set `consumedAt = now`, `consumedByAttemptId = attemptId` **atomically with the `ExamAttempt` insert** in `ExamAttemptService.startAttempt` — the same transaction, not a follow-up call.

### Postconditions
- `RetakeGrant.consumedAt` set exactly once.

## 3.15 `RetakeGrantService.revoke(grantId, reason)`

### Steps
1. Load `RetakeGrant`.
2. Insert a new `RetakeGrant` row with `additionalAttempts: 0`, `supersedesGrantId: grantId`, `reason`.
3. The original row is never edited — its `additionalAttempts` still reads as granted, but
   `ExamAttemptService.startAttempt`'s cap computation must walk the `supersedesGrantId` chain
   and count only the row at the end of it.
4. Write `AuditLog { reason }`.

### Postconditions
- A new row supersedes the old one; the old one is untouched.

---

## 3.16 `ExamWindowOverrideService.grantOverride(assignmentId, newWindow, reason, justificationRef)`

### Preconditions
- `assignmentId` refers to an existing `ExamAssignment`.
- `justificationRef` non-null.

### Invariants
- Widens `ExamAssignment`'s window only — `Exam.opensAt`/`closesAt` untouched.

### Steps
1. Load `ExamAssignment`.
2. Validate `justificationRef` non-empty. If empty → reject. **Stop.**
3. Insert `ExamWindowOverride { examAssignmentId, overriddenWindowStartAt, overriddenWindowEndAt, reason, justificationRef, grantedByUserId, grantedAt: now, expiresAt }`.
4. Write `AuditLog { action: CONFIG_CHANGE, reason }`.

### Postconditions
- One `ExamWindowOverride` row.
- `ExamAssignmentService.assignToStudent`'s window check (3.7) must additionally check for an
  active, unexpired `ExamWindowOverride` before rejecting a window outside the exam's own range.

## 3.17 `ExamWindowOverrideService.revoke(overrideId, reason)`

Same supersede pattern as `RetakeGrantService.revoke`: insert a new row with
`supersedesOverrideId` pointing back; never edit the original.

---

# Module 4 — Question Bank

## 4.1 `QuestionService.createQuestion(...)`

### Preconditions
- Caller has `question:create` permission.
- `stem` non-null.
- `questionType` ∈ `QuestionType`.

### Invariants
- A new question starts in `DRAFT`.

### Steps
1. Check permission.
2. Validate `questionType`.
3. Insert `Question { code, questionType, stem, explanation, answerKey, numericTolerance, mediaPath, difficulty, status: DRAFT, defaultPoints, topic, questionCategoryId, expectedSeconds, createdByUserId, version: 1, programmingLanguage, starterCode, executionTimeLimitMs, executionMemoryLimitMb }`.
4. Write `AuditLog`.

### Postconditions
- One `Question` row in `DRAFT`.

---

## 4.2 `QuestionService.addOption(questionId, option)`

### Preconditions
- `Question` exists with `questionType ∈ {SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, MATCHING, ORDERING}`.

### Invariants
- `SINGLE_CHOICE` / `TRUE_FALSE` require exactly one correct option.
- `MULTIPLE_CHOICE` requires at least one correct option.

### Steps
1. Load `Question`.
2. If `questionType = SINGLE_CHOICE` or `TRUE_FALSE`:
   - If `option.correct = true`:
     - Query existing `QuestionOption` rows with `correct = true`.
     - If any exist → reject (exactly one). **Stop.**
3. Insert `QuestionOption { questionId, label, content, mediaPath, correct, sequenceNo, optionWeight, feedback, matchKey }`.
4. Write `AuditLog`.

### Postconditions
- One `QuestionOption` row.

### Rejection reasons
- Second correct option on a single-answer question.

---

## 4.3 `QuestionService.publishQuestion(questionId)`

### Preconditions
- `Question.status = DRAFT`.

### Invariants
- Type-specific completeness is enforced at publish.

### Steps
1. Load `Question`.
2. Validate by type:
   - `SINGLE_CHOICE` / `TRUE_FALSE`: exactly one `QuestionOption.correct = true`.
   - `MULTIPLE_CHOICE`: at least one `QuestionOption.correct = true`.
   - `SHORT_ANSWER` / `ESSAY`: `answerKey` may be null (manual grading).
   - `NUMERIC`: `answerKey` non-null, `numericTolerance` non-null.
   - `CODE`: at least one `CodeTestCase` with `visibility = HIDDEN`; `programmingLanguage` non-null.
   - `MATCHING` / `ORDERING` / `FILL_IN_BLANK`: type-specific option requirements.
3. If any validation fails → reject. **Stop.**
4. Set `Question.status = ACTIVE`.
5. Write `AuditLog { action: PUBLISH }`.

### Postconditions
- `Question.status = ACTIVE`.

---

## 4.4 `QuestionService.retireQuestion(questionId)`

### Preconditions
- `Question` exists.

### Invariants
- Questions are **never hard-deleted**.
- A retired question stays readable by past `AttemptAnswer` / `ResultDetail`.

### Steps
1. Load `Question`.
2. Set `status = RETIRED`.
3. Do **not** delete.
4. Ensure `searchBank` filters `status = ACTIVE` so it is excluded from new pool draws.
5. Write `AuditLog { action: UPDATE }`.

### Postconditions
- `Question.status = RETIRED`.
- Row preserved.

---

## 4.5 `QuestionService.addTestCase(questionId, testCase)`

### Preconditions
- `Question.questionType = CODE`.

### Invariants
- A `CODE` question requires at least one `HIDDEN` test case at publish.

### Steps
1. Load `Question`; if not `CODE` → reject. **Stop.**
2. Insert `CodeTestCase { questionId, sequenceNo, inputData, expectedOutput, visibility, points }`.
3. Write `AuditLog`.

### Postconditions
- One `CodeTestCase` row.

---

## 4.6 `QuestionBankService.createCategory(...)` / `moveCategory(categoryId, newParentId)`

### Preconditions
- `categoryId` exists (for move).
- `newParentId` exists (for move) or is null.

### Invariants
- No cycle in the tree.
- The materialised `path` is correct for the entire subtree.

### Steps for `moveCategory`:
1. Load the category.
2. If `newParentId != null`:
   - Load the new parent.
   - Walk the parent chain upward.
   - If `categoryId` appears as its own ancestor → reject (cycle). **Stop.**
3. Compute new `path` and `depth` for the moved node.
4. Query **all descendants** by `path LIKE oldPath + '%'`.
5. For each descendant:
   - Rewrite `path` by replacing the old prefix with the new prefix.
   - Recompute `depth`.
6. Set the moved node's `parent`, `path`, `depth`.
7. Write `AuditLog`.

### Postconditions
- Entire subtree's `path` / `depth` consistent.
- No cycle.

---

## 4.7 `QuestionBankService.tagQuestion(questionId, tagName)` / `untagQuestion(...)`

### Steps for `tagQuestion`:
1. Query `QuestionTag` by `name`.
2. If not found → insert `QuestionTag { name, description, usageCount: 0 }`.
3. Insert `QuestionTagLink { questionId, questionTagId }`.
4. Increment `QuestionTag.usageCount`.
5. Write `AuditLog`.

### Steps for `untagQuestion`:
1. Load `QuestionTagLink`.
2. Delete it (a pure join row, no independent lifecycle).
3. Decrement `QuestionTag.usageCount`.
4. Write `AuditLog`.

### Postconditions
- `QuestionTagLink` state consistent.
- `usageCount` accurate.

---

## 4.8 `QuestionCalibrationService.computeCalibration(questionId, window)`

### Preconditions
- `questionId` refers to an existing `Question`.

### Invariants
- Computed only from `ExamResult.status = FINAL`.
- Never auto-retires a question.

### Steps
1. Query `ResultDetail` rows for `questionId`'s `ExamQuestion` placements where the parent
   `ExamResult.status = FINAL` and `gradedAt` falls in the window.
2. `responseCount` = count; `correctCount` = count where `correct = true`.
3. `difficultyIndex = correctCount / responseCount`.
4. `discriminationIndex` = point-biserial correlation between `ResultDetail.correct` and the
   attempt's `ExamResult.percentage`.
5. `averageTimeSeconds` = mean of `ResultDetail.timeSpentSeconds`.
6. Determine `flaggedReason`:
   - `difficultyIndex >= 0.95` → `TOO_EASY`.
   - `difficultyIndex <= 0.05` → `TOO_HARD` (or `MIS_KEY_SUSPECTED` if `discriminationIndex < 0`).
   - `discriminationIndex < 0` → `NEGATIVE_DISCRIMINATION` (implies `MIS_KEY_SUSPECTED`).
   - Else → `NONE`.
7. Determine `recommendedAction` from `flaggedReason` (`TOO_EASY`/`TOO_HARD` → `REVIEW`,
   `NEGATIVE_DISCRIMINATION`/`MIS_KEY_SUSPECTED` → `RETIRE`, `NONE` → `KEEP`).
8. Insert `QuestionCalibration { questionId, windowStartAt, windowEndAt, responseCount, correctCount, difficultyIndex, discriminationIndex, averageTimeSeconds, flaggedReason, recommendedAction, computedAt: now }`.

### Postconditions
- One `QuestionCalibration` row.
- No change to `Question.status` — `recommendedAction = RETIRE` is a suggestion, not an action.

---

## 4.9 `QuestionService.createNextVersion(questionId, changes)`

### Preconditions
- `questionId` refers to an existing `Question` (the parent).
- `changes` is a partial set of field overrides — everything not named in it is inherited from
  the parent as-is.

### Invariants
- The parent row is never edited by this call.
- The new row always starts at `status = DRAFT`, regardless of the parent's status.
- `code` is never copied verbatim — `uk_questions_code` is unique, so two versions can't share
  one.

### Steps
1. Load the parent `Question`.
2. Construct the new row by copying every field from the parent (`questionType`, `stem`,
   `explanation`, `answerKey`, `numericTolerance`, `mediaPath`, `difficulty`, `defaultPoints`,
   `topic`, `questionCategoryId`, `expectedSeconds`, `programmingLanguage`, `starterCode`,
   `executionTimeLimitMs`, `executionMemoryLimitMb`), then applying `changes` on top.
3. Set `status = DRAFT` unconditionally.
4. Set `version = parent.version + 1`.
5. Set `parentQuestionId = parent.id`.
6. Set `createdByUserId` to the caller.
7. If `code` wasn't explicitly supplied in `changes` → leave it null (or apply the caller's
   own versioning convention) rather than copying the parent's — inserting the parent's `code`
   unchanged would violate `uk_questions_code`.
8. If the parent question type is `SINGLE_CHOICE`/`MULTIPLE_CHOICE`/`TRUE_FALSE`/`MATCHING`/
   `ORDERING`, copy its `QuestionOption` rows onto the new question (new `QuestionOption` rows,
   not shared ones — options belong to one question, per `QuestionOption.questionId`).
9. If the parent question type is `CODE`, copy its `CodeTestCase` rows the same way.
10. Insert the new `Question` row.
11. Write `AuditLog { action: CREATE, entityType: 'Question', entityId: newRow.id, reason: 'versioned from ' + questionId }`.

### Postconditions
- One new `Question` row, `status = DRAFT`, `parentQuestionId` set, `version = parent.version + 1`.
- The parent `Question` row is completely unchanged — its own `status`, `version`, and every
  field are exactly as they were before this call.
- Copied `QuestionOption`/`CodeTestCase` rows exist on the new question, independent of the
  parent's.
- Every existing `ExamQuestion` still points at the parent's `id` — nothing about the parent's
  placement in past or current exams changes until a caller explicitly places the new version
  into a (new or future) exam via `ExamService`/`ExamSection` placement.

### Rejection reasons
| Condition | Result |
|---|---|
| Parent `questionId` not found | reject |

---

# Module 5 — Pre-Exam System Check

## 5.1 `SystemCheckService.startCheck(candidateId, examId)`

### Preconditions
- `candidateId` and `examId` refer to existing rows.
- `Exam.status ∈ {PUBLISHED, ACTIVE}`.

### Invariants
- A `SystemCheck` run exists **before** any attempt.

### Steps
1. Load `Exam`, `ProctoringPolicy`.
2. Determine required `SystemCheckType` values from the policy.
3. Insert `SystemCheck { candidateUserId, examId, status: IN_PROGRESS, attemptNo, startedAt: now, deviceFingerprint, userAgent, ipAddress }`.
4. Return the list of required checks to run.

### Postconditions
- One `SystemCheck` row in `IN_PROGRESS`.

---

## 5.2 `SystemCheckService.runCheckItem(checkType)`

### Preconditions
- An `IN_PROGRESS` `SystemCheck` exists for the candidate+exam.

### Invariants
- Each item keeps its own measurement.
- A required item failing blocks the run.

### Steps
1. Load the `SystemCheck`.
2. Run the check for `checkType` (BROWSER_COMPATIBILITY, CAMERA, MICROPHONE, SPEAKER, NETWORK_BANDWIDTH, SCREEN_SHARE_PERMISSION, FULLSCREEN, ENVIRONMENT_SCAN, OS_COMPATIBILITY, SECOND_SCREEN).
3. Determine `result` ∈ `{NOT_RUN, PASSED, WARNING, FAILED, SKIPPED}`.
4. Determine `isRequired` from the policy.
5. Insert `SystemCheckItem { systemCheckId, checkType, result, required, checkedAt: now, message, measurement, retryCount }`.
6. If `result = FAILED` AND `isRequired = true`:
   - Increment `SystemCheck.failedCheckCount`.
7. After all required items:
   - If all required `PASSED` → `SystemCheck.status = PASSED`.
   - If warnings only on optional items → `PASSED_WITH_WARNINGS`.
   - If any required `FAILED` → `FAILED`.
8. Set `completedAt = now`, `validUntil = now + configuredTTL`.

### Postconditions
- One `SystemCheckItem` per check.
- `SystemCheck.status` set.

---

## 5.3 `SystemCheckService.overrideFailure(proctorId, reason)`

### Preconditions
- `SystemCheck.status = FAILED`.
- `reason` non-null.

### Invariants
- The override is recorded, not silent.

### Steps
1. Load `SystemCheck`.
2. Set `overridden = true`, `overriddenBy: proctorId`, `overrideReason = reason`.
3. Per policy, either:
   - Keep `status = FAILED` but mark overridden, or
   - Set `status = PASSED_WITH_WARNINGS`.
4. Write `AuditLog { action: UPDATE, actorUserId: proctorId, reason }`.

### Postconditions
- `SystemCheck.overridden = true`.
- One `AuditLog` row with `reason`.

---

# Module 6 — Identity Verification

## 6.1 `IdentityVerificationService.verifyFace(candidateId, attemptId, capture)`

### Preconditions
- `candidateId` refers to a `User` with `enrolmentPhotoPath != null`.
- `attemptId` refers to an existing `ExamAttempt`.

### Invariants
- The `matchThreshold` is copied at verification time.
- Many rows per attempt — a failure followed by an override is two rows.

### Steps
1. Load `User`. If `enrolmentPhotoPath = null` → reject. **Stop.**
2. Load the active face-match model version; read `confidenceThreshold`, `detectionThreshold`.
3. Run the model over `capture` against `User.enrolmentPhotoPath`.
4. Compute `matchScore`, `livenessScore`.
5. Determine `status`:
   - If `matchScore >= matchThreshold` AND `livenessScore >= livenessThreshold` → `PASSED`.
   - Else → `FAILED`.
6. Compute `sequenceNo = previous max + 1`.
7. Insert `IdentityVerification { candidateUserId, examAttemptId, proctoringSessionId, method: FACE_MATCH, status, sequenceNo, matchScore, matchThreshold, livenessScore, referencePhotoPath, capturedEvidenceId, verifiedAt: now }`.
8. Write `AuditLog`.

### Postconditions
- One `IdentityVerification` row.
- `matchThreshold` frozen on the row.

---

## 6.2 `IdentityVerificationService.manualOverride(attemptId, proctorId, reason)`

### Preconditions
- A previous `IdentityVerification` exists for the attempt with `status = FAILED`.
- `reason` non-null.

### Invariants
- The original `FAILED` row stays.

### Steps
1. Load the latest `IdentityVerification` for the attempt.
2. Compute `sequenceNo = previous max + 1`.
3. Insert a **new** `IdentityVerification { method: MANUAL_PROCTOR, status: MANUAL_OVERRIDE, verifiedByUserId: proctorId, verifiedAt: now, overrideReason: reason, sequenceNo }`.
4. Write `AuditLog { action: UPDATE, reason }`.

### Postconditions
- Two `IdentityVerification` rows: the original failure and the override.

---

# Module 7 — Consent & Privacy

## 7.1 `ConsentService.recordConsent(attemptId, noticeId)`

### Preconditions
- `attemptId` refers to an existing `ExamAttempt`.
- `noticeId` refers to a `PrivacyNotice` with `isActive = true`.

### Invariants
- Required before `ExamAttemptService.startAttempt` proceeds whenever `ProctoringPolicy.mode ≠ NONE`.
- `noticeVersion` is copied at consent time.

### Steps
1. Load `ExamAttempt`, `PrivacyNotice`.
2. Insert `ConsentRecord { examAttemptId, privacyNoticeId: noticeId, noticeVersion: notice.version, consentedAt: now, ipAddress, userAgent }`.

### Postconditions
- One `ConsentRecord` row.

## 7.2 `ConsentService.withdrawConsent(consentId)`

### Steps
1. Load `ConsentRecord`.
2. Set `withdrawnAt = now`. Never delete.

### Postconditions
- `ConsentRecord.withdrawnAt` set; row preserved.

## 7.3 `PrivacyNoticeService.publishNotice(noticeCode, version, locale, body)`

### Steps
1. Query the currently active `PrivacyNotice` for `(noticeCode, locale)`.
2. If found, set its `effectiveTo = now`, `isActive = false` — never edited in place.
3. Insert the new `PrivacyNotice { noticeCode, version, locale, bodyMarkdown: body, effectiveFrom: now, active: true }`.

### Postconditions
- Old notice closed by date; new one active.

---

# Module 8 — Exam Session & Attempts

## 8.1 `ExamAttemptService.startAttempt(assignmentId)` — the heaviest function

### Preconditions
- `assignmentId` refers to an existing `ExamAssignment`.
- `ExamAssignment.status ∈ {ASSIGNED, NOTIFIED}`.

### Invariants
- All checks run **before** any row is created.
- `expiresAt` is computed once.
- `sessionTokenHash` is scoped to one attempt.

### Steps

**Step 1 — Load and gate.**
- Load `ExamAssignment`.
- If `status ∉ {ASSIGNED, NOTIFIED}` → reject. **Stop.**
- Load `Exam`, `User`, `ProctoringPolicy`.

**Step 2 — Window gate.**
- If `ExamAssignment.windowStartAt != null` AND `now < windowStartAt` → check for an active,
  unexpired `ExamWindowOverride` (Idea 7) covering `now`; if none → reject. **Stop.**
- If `ExamAssignment.windowEndAt != null` AND `now > windowEndAt` → same override check; if none
  → reject. **Stop.**

**Step 3 — Attempt count gate.**
- Effective cap = `ExamAssignment.attemptsAllowed ?? Exam.maxAttempts`.
- Count existing `ExamAttempt` rows for `(examId, candidateId)`.
- Walk each `RetakeGrant` for the assignment to its terminal row via `supersedesGrantId`; add
  `additionalAttempts` for each unconsumed, unexpired terminal grant.
- If count ≥ effective cap → reject. **Stop.**

**Step 4 — Concurrent attempt gate.**
- Query `ExamAttempt` for `(examId, candidateId)` where `status ∈ {IN_PROGRESS, PAUSED}`.
- If any exists → reject. **Stop.** (Route to `AttemptResumptionService.resume` instead — Idea 1.)

**Step 5 — Prerequisite gate.**
- Run `ExamPrerequisiteService.checkEligibility(examId, candidateId)`.
- If not eligible → reject. **Stop.**

**Step 6 — System check gate.**
- Query the latest `SystemCheck` for `(candidateId, examId)`.
- If none, or `status ∉ {PASSED, PASSED_WITH_WARNINGS}`, or `overridden = false` with `failedCheckCount > 0` → reject. **Stop.**
- If `validUntil <= now` → reject (stale). **Stop.**

**Step 7 — Consent gate.**
- If `ProctoringPolicy.mode ≠ NONE`:
  - Query `ConsentRecord` for the attempt (or the candidate's most recent one for this exam, if
    consent is captured before the attempt row exists).
  - If none, or `withdrawnAt != null`, or `noticeVersion` doesn't match the currently active
    `PrivacyNotice.version` → require `ConsentService.recordConsent` before proceeding. **Stop.**

**Step 8 — Identity gate.**
- If `ProctoringPolicy.requireIdentityCheck = true`:
  - Query `IdentityVerification` for the attempt.
  - If none with `status ∈ {PASSED, MANUAL_OVERRIDE}` → reject. **Stop.**

**Step 9 — Create the attempt.**
- Compute `attemptNo = previous max + 1`.
- Compute `expiresAt = now + Exam.durationMinutes + (ExamAssignment.extraTimeMinutes ?? 0)`.
- Generate a random session token (≥ 256 bits).
- Compute `sessionTokenHash = hash(sessionToken)`.
- Insert `ExamAttempt { publicId: UUID, examId, candidateUserId, examAssignmentId, attemptNo, status: IN_PROGRESS, startedAt: now, expiresAt, sessionTokenHash, examVersion: Exam.version, currentSectionId: firstSection, ipAddress, userAgent }`.

**Step 10 — Generate question states.**
- For each `ExamSection` (ordered by `sequenceNo`):
  - Determine the delivered set:
    - If `questionsToDraw != null` → randomly select `questionsToDraw` from the section's `ExamQuestion` rows.
    - Else → all.
  - If `shuffleQuestions` → shuffle the delivered set.
  - For each delivered `ExamQuestion`:
    - Compute `displayOrder`.
    - If `ExamQuestion.shuffleOptions` → compute `optionOrder`.
    - Insert `QuestionState { examAttemptId, examQuestionId, state: UNSEEN, displayOrder, optionOrder, viewCount: 0, timeOnQuestionSeconds: 0 }`.
- Compute `formHash` over `(examQuestionId, displayOrder, optionOrder)` and insert
  `QuestionFormFingerprint { examAttemptId, examId, formHash, questionCount, sectionCount }` (Idea 4).

**Step 11 — Consume retake grant (if any).**
- If a `RetakeGrant` was counted in Step 3, call `RetakeGrantService.consume` in the same
  transaction as Step 9's insert.

**Step 12 — Update assignment.**
- Set `ExamAssignment.status = STARTED`.

**Step 13 — Audit.**
- Write `AuditLog { action: START_ATTEMPT, entityType: 'ExamAttempt', entityId: newAttempt.id }`.

**Step 14 — Return.**
- Return `{ attemptId, sessionToken (plaintext, once), expiresAt }`.

### Postconditions
- One `ExamAttempt` row in `IN_PROGRESS`.
- One `QuestionState` per delivered question.
- One `QuestionFormFingerprint` row.
- `ExamAssignment.status = STARTED`.
- One `AuditLog` row.
- The plaintext session token is returned exactly once.

### Rejection reasons
| Condition | Result |
|---|---|
| Wrong assignment status | reject |
| Outside window (no override) | reject |
| Attempt cap reached | reject |
| Concurrent attempt exists | reject |
| Prerequisite failed | reject |
| System check failed/stale | reject |
| Consent missing/withdrawn/stale | reject |
| Identity not verified | reject |

---

## 8.2 `ExamAttemptService.heartbeat(attemptId)`

### Preconditions
- `ExamAttempt.status ∈ {IN_PROGRESS, PAUSED}`.

### Invariants
- The heartbeat touches `LiveSessionStatus` only, never `ProctoringEvent`, except when the miss
  threshold is crossed.

### Steps
1. Load `ExamAttempt`.
2. If `status ∉ {IN_PROGRESS, PAUSED}` → reject. **Stop.**
3. Set `lastHeartbeatAt = now`, `lastActivityAt = now`.
4. Update `ProctoringSession.lastHeartbeatAt = now`.
5. Update `LiveSessionStatus.lastHeartbeatAt = now`.
6. If the interval since the previous heartbeat exceeds the configured threshold:
   - Increment `ProctoringSession.heartbeatMissCount`.
   - If `heartbeatMissCount >= configuredThreshold`:
     - Call `ProctoringEventService.ingestEvent(sessionId, HEARTBEAT_MISSED, ...)`.
     - Reset `heartbeatMissCount = 0`.

### Postconditions
- `lastHeartbeatAt` updated.
- If threshold crossed, one `ProctoringEvent`.

---

## 8.3 `ExamAttemptService.navigateTo(attemptId, examQuestionId)`

### Preconditions
- `ExamAttempt.status = IN_PROGRESS`.
- `QuestionState` exists for `(attemptId, examQuestionId)`.

### Invariants
- `lockOnExit` is respected.

### Steps
1. Load `ExamAttempt`, `QuestionState`, `ExamSection`.
2. If the current section has `lockOnExit = true` and the target is in a different section → reject. **Stop.**
3. Update the previous `QuestionState.lastViewedAt = now`, `timeOnQuestionSeconds += elapsed`.
4. Update the target `QuestionState`:
   - If `state = UNSEEN` → `state = VIEWED`.
   - `firstViewedAt = now` (if first time).
   - `lastViewedAt = now`.
   - `viewCount += 1`.
5. Update `ExamAttempt.currentSectionId` if the target is in a different section.
6. Update `LiveSessionStatus.currentQuestionNo = target.displayOrder`.

### Postconditions
- `QuestionState` navigation state updated.

---

## 8.4 `ExamAttemptService.markForReview(attemptId, examQuestionId)`

### Steps
1. Load `QuestionState`.
2. If `state = FLAGGED` → toggle off to `ANSWERED` (or `VIEWED`).
3. Else → set `state = FLAGGED`.
4. Do **not** touch `AttemptAnswer.flaggedByCandidate` — that is a different flag.

### Postconditions
- `QuestionState.state` toggled.

---

## 8.5 `ExamAttemptService.submit(attemptId)`

### Preconditions
- `ExamAttempt.status ∈ {IN_PROGRESS, PAUSED}`.

### Invariants
- No `AttemptAnswer` write is accepted after this.

### Steps
1. Load `ExamAttempt`.
2. If `status ∉ {IN_PROGRESS, PAUSED}` → reject. **Stop.**
3. Set `status = SUBMITTED`, `submittedAt = now`.
4. Compute `timeSpentSeconds = (submittedAt - startedAt) - pausedSeconds`.
5. Update `ExamAssignment.status = SUBMITTED`.
6. Ingest `ProctoringEventType.ATTEMPT_SUBMITTED`.
7. Call `ExamResultService.computeRawScore(attemptId)`.
8. Write `AuditLog { action: SUBMIT_ATTEMPT }`.

### Postconditions
- `ExamAttempt.status = SUBMITTED`.
- `ExamResult` row created/updated with `status = PROVISIONAL`.
- One `ProctoringEvent`, one `AuditLog`.

---

## 8.6 `ExamAttemptService.autoSubmitOnTimeout` (scheduled sweep)

### Steps
1. Query `ExamAttempt` where `status ∈ {IN_PROGRESS, PAUSED}` AND `expiresAt < now`.
2. For each:
   - Set `status = AUTO_SUBMITTED`, `submittedAt = expiresAt` (not `now`).
   - Compute `timeSpentSeconds = (expiresAt - startedAt) - pausedSeconds`.
   - Ingest `ATTEMPT_AUTO_SUBMITTED`.
   - Call `ExamResultService.computeRawScore`.
   - Write `AuditLog { actorUserId: null }`.

### Postconditions
- All overdue attempts auto-submitted.

---

## 8.7 `AnswerService.saveAnswer(attemptId, examQuestionId, response)`

### Preconditions
- `ExamAttempt.status ∈ {IN_PROGRESS, PAUSED}`.
- `QuestionState` exists for `(attemptId, examQuestionId)`.

### Invariants
- No write after `SUBMITTED`.
- Every save appends an `AnswerRevision`.
- `AttemptAnswer` is overwritten in place.

### Steps
1. Load `ExamAttempt`.
2. If `status ∉ {IN_PROGRESS, PAUSED}` → reject. **Stop.**
3. Load or create `AttemptAnswer` for `(attemptId, examQuestionId)`.
4. Compute `selectedOptionIds` snapshot (JSON of `QuestionOption.id` list).
5. Overwrite `AttemptAnswer`:
   - `responseText`, `responseNumeric`, `attachmentPath`.
   - `answeredAt = now`.
   - `revisionCount += 1`.
6. Insert `AnswerRevision { attemptAnswerId, revisionNo: revisionCount, savedAt: now, autoSaved: false, responseText, responseNumeric, selectedOptionIds, clientTimestamp, ipAddress }`.
7. If `MULTIPLE_CHOICE` / `MATCHING` / `ORDERING`:
   - Delete existing `AttemptAnswerOption` rows for this answer.
   - Insert new rows for each selected `questionOptionId`, with `sequenceNo` for ordering.
8. Set `gradingStatus`:
   - For auto-gradable types → `NOT_REQUIRED` (if graded inline) or `PENDING`.
   - For `ESSAY` / `SHORT_ANSWER` / `CODE` → `PENDING`.
9. Update `QuestionState.state = ANSWERED`, `lastViewedAt = now`.
10. Update `LiveSessionStatus.answeredCount`.

### Postconditions
- `AttemptAnswer` overwritten.
- One `AnswerRevision` row.
- `AttemptAnswerOption` rows consistent.
- `QuestionState.state = ANSWERED`.

---

## 8.8 `AnswerService.autoSave(...)`

Same as 8.7 but with `AnswerRevision.autoSaved = true`. Never overwrites history.

---

## 8.9 `AnswerService.gradeAutomatically(answerId)`

### Preconditions
- `AttemptAnswer.gradingStatus ∈ {PENDING, IN_PROGRESS}`.

### Invariants
- `CODE` resolves automatically from `CodeExecutionResult`; `SPREADSHEET` resolves automatically
  from `ExcelGradeResult` (§20.10) whenever every `ExcelCellBinding` on the question graded as
  something other than `MANUAL` — the same "auto unless the grading kind says otherwise" rule
  `CODE` already follows implicitly (a CODE question is always fully automatic).
- `correct` stays null until graded.

### Steps
1. Load `AttemptAnswer`, `ExamQuestion`, `Question`.
2. If `questionType = CODE`:
   - For each `CodeTestCase` of the question:
     - Execute the candidate's code with `inputData`.
     - Compare `actualOutput` against `expectedOutput`.
     - Insert `CodeExecutionResult { attemptAnswerId, codeTestCaseId, passed, actualOutput, runtimeMs, memoryKb, errorMessage, executedAt: now }`.
   - Sum `pointsAwarded` = sum of `CodeTestCase.points` for passed cases (equal weighting if `points` null).
   - Set `correct = (pointsAwarded > 0)`.
3. Else if `questionType = SPREADSHEET`:
   - Call `ExcelGradingService.gradeAnswer` (§20.10), which writes one `ExcelGradeResult` per
     `ExcelCellBinding` and returns their summed `pointsAwarded`.
   - Set `pointsAwarded` to that sum.
   - Set `correct = (pointsAwarded > 0)`.
   - If any `ExcelGradeResult` for this answer has `graderType = MANUAL` → set
     `gradingStatus = PENDING` instead of `GRADED` at step 4, since a human still has to resolve
     that cell before the question's score is final.
4. Else (auto-gradable non-code, non-spreadsheet):
   - Compare against `answerKey` / `QuestionOption.correct` / `numericTolerance`.
   - Set `correct`, `pointsAwarded`.
5. Set `gradingStatus = GRADED`, unless step 3 set it to `PENDING`.
6. Set `gradedAt = now`.
7. If `ExamResult` exists, update aggregates.

### Postconditions
- `AttemptAnswer.gradingStatus = GRADED`, or `PENDING` for a SPREADSHEET answer with an
  unresolved MANUAL cell.
- For `CODE`: one `CodeExecutionResult` per test case. For `SPREADSHEET`: one `ExcelGradeResult`
  per `ExcelCellBinding`.

---

## 8.10 `AnswerTimingAnomalyService.checkTiming(answerId)`

### Preconditions
- `AttemptAnswer.gradingStatus = GRADED`.

### Invariants
- Evaluated only after grading.
- Writes a `RiskEvent`, never a `ProctoringEvent`.

### Steps
1. Load `AttemptAnswer`, `QuestionState`, `Question`.
2. If `AttemptAnswer.correct ≠ true` → **stop, no anomaly** (a fast wrong answer isn't suspicious).
3. `actualSeconds = QuestionState.timeOnQuestionSeconds`.
4. `expectedSeconds = Question.expectedSeconds` (copied onto the flag, not referenced live).
5. `ratio = actualSeconds / expectedSeconds` (skip if `expectedSeconds` is null).
6. Determine `anomalyType`:
   - `actualSeconds = 0` → `ZERO_TIME_CORRECT`.
   - `ratio` below the configured floor → `TOO_FAST_CORRECT` (or `TOO_FAST_HIGH_SCORE` if scored
     rather than binary-correct).
   - A burst of `AnswerRevision.savedAt` timestamps within a short window across many questions →
     `BURST_SUBMIT` (checked independently of a single answer's ratio).
7. If flagged, insert `AnswerTimingAnomaly { examAttemptId, attemptAnswerId, examQuestionId, expectedSeconds, actualSeconds, ratio, answerCorrect: true, revisionCount: AttemptAnswer.revisionCount, anomalyType, flaggedAt: now }`.
8. Write a `RiskEvent` with `factorCode = TIMING_ANOMALY` against the attempt's latest
   `RiskAssessment` — not a `ProctoringEvent`, since this is a derived signal, not an observation.

### Postconditions
- Zero or one `AnswerTimingAnomaly` row per check.
- A re-grade re-runs this check and inserts a new row; the old one is untouched.

---

## 8.11 `QuestionFormFingerprintService.checkCollisions(examId, formHash)`

### Steps
1. Query `QuestionFormFingerprint` rows for `examId` with a matching `formHash`, excluding the
   fingerprint's own `examAttemptId`.
2. Set `collisionCount` and `collisionAttemptIds` (JSON) on the fingerprint being checked.
3. If `collisionCount > 0`:
   - Insert `SuspiciousActivity { ..., activityType: COMBINED_BEHAVIOR, ruleCode: 'FORM_COLLISION', verdict: DETECTED }`, referencing both attempts' sessions.

### Postconditions
- Fingerprint's collision fields updated.
- A collision writes a signal, never a verdict.

---

## 8.12 `AttemptResumptionService.resume(attemptId, reason)`

### Preconditions
- `ExamAttempt.status ∈ {IN_PROGRESS, PAUSED}`.

### Invariants
- A resumption rotates the session token; it is never a second concurrent session.

### Steps
1. Load `ExamAttempt`. If `status ∉ {IN_PROGRESS, PAUSED}` → reject. **Stop.**
2. Compute `timeAwaySeconds = now - ExamAttempt.lastHeartbeatAt`.
3. Generate a new session token; compute `newSessionTokenHash = hash(newToken)`.
4. Set `ExamAttempt.sessionTokenHash = newSessionTokenHash` — the previous hash is invalidated
   in the same write, not a separate step.
5. `ExamAttempt.pausedSeconds += timeAwaySeconds`.
6. Determine `autoApproved`: `timeAwaySeconds <= policy grace window`.
7. If not auto-approved, require `approvedByUserId` before proceeding (a proctor decision, out of
   band from this call — the row is written `autoApproved: false` with `approvedByUserId` set
   once the decision comes back).
8. If `timeAwaySeconds` exceeds the grace window:
   - Compute a `RiskEvent` with `factorCode = ATTEMPT_GAP`; set `riskEventId` on the row.
9. Insert `AttemptResumption { examAttemptId, reason, previousSessionTokenHash: old hash, newSessionTokenHash, resumedAt: now, timeAwaySeconds, autoApproved, approvedByUserId, riskEventId }`.

### Postconditions
- `ExamAttempt.sessionTokenHash` rotated.
- `ExamAttempt.pausedSeconds` increased by `timeAwaySeconds`.
- One `AttemptResumption` row (append-only — a second crash produces a second row).

---

## 8.13 `AttemptPauseService.requestPause(attemptId, reason)`

### Preconditions
- `ExamAttempt.status = IN_PROGRESS`.

### Invariants
- Only one `PENDING` request per attempt.

### Steps
1. Load `ExamAttempt`.
2. If `status ≠ IN_PROGRESS` → reject. **Stop.**
3. Query `AttemptPauseRequest` for the attempt where `status = PENDING`.
4. If any exists → reject (do not queue). **Stop.**
5. Insert `AttemptPauseRequest { examAttemptId, requestedAt: now, requestedByUserId, reason, status: PENDING }`.
6. If policy auto-approves (e.g. system-initiated) → set `status = AUTO_APPROVED`, `decidedAt = now`.
7. Write `AuditLog`.

### Postconditions
- One `AttemptPauseRequest` row.

---

## 8.14 `AttemptPauseService.decide(requestId, approve/deny, note)`

### Preconditions
- `AttemptPauseRequest.status = PENDING`.

### Steps
1. Load the request.
2. If `status ≠ PENDING` → reject. **Stop.**
3. Set `status = APPROVED` or `DENIED`, `decidedByUserId`, `decidedAt = now`, `decisionNote`.
4. If `APPROVED`:
   - Set `ExamAttempt.status = PAUSED`.
   - Ingest `ProctoringEventType.ATTEMPT_PAUSED`.
5. Write `AuditLog`.

### Postconditions
- `AttemptPauseRequest.status` set.
- If approved, `ExamAttempt.status = PAUSED`.

---

## 8.15 `AttemptPauseService.resume(requestId)`

### Preconditions
- `AttemptPauseRequest.status ∈ {APPROVED, AUTO_APPROVED}`.

### Steps
1. Load the request.
2. If `status ∉ {APPROVED, AUTO_APPROVED}` → reject. **Stop.**
3. Set `resumedAt = now`.
4. Compute `pauseDuration = resumedAt - decidedAt`.
5. `ExamAttempt.pausedSeconds += pauseDuration`.
6. Set `ExamAttempt.status = IN_PROGRESS`.
7. Ingest `ProctoringEventType.ATTEMPT_RESUMED`.
8. Write `AuditLog`.

### Postconditions
- `resumedAt` set (distinct from `decidedAt`).
- `ExamAttempt.pausedSeconds` increased.
- `ExamAttempt.status = IN_PROGRESS`.

---

# Module 9 — Proctoring Sessions

## 9.1 `ProctoringSessionService.startSession(attemptId)`

### Preconditions
- `ExamAttempt.status = IN_PROGRESS`.
- No `ProctoringSession` exists for the attempt.

### Invariants
- `mode` is snapshotted at start.

### Steps
1. Load `ExamAttempt`, `Exam`, `ProctoringPolicy`.
2. Insert `ProctoringSession { publicId: UUID, examAttemptId, status: PENDING, mode: policy.mode, assignedProctorUserId, startedAt: now }`.
3. Update `status = ACTIVE`.
4. Insert `LiveSessionStatus { proctoringSessionId, candidateStatus: ACTIVE, connectionStatus: CONNECTED, cameraStatus: UNAVAILABLE, microphoneStatus: UNAVAILABLE, screenStatus: UNAVAILABLE, fullscreen: false, windowFocused: true, openAlertCount: 0, answeredCount: 0 }`.
5. Ingest `ProctoringEventType.SESSION_STARTED`.
6. Write `AuditLog`.

### Postconditions
- One `ProctoringSession` in `ACTIVE`.
- One `LiveSessionStatus`.
- One `ProctoringEvent`.

---

## 9.2 `ProctoringSessionService.recordHeartbeat(sessionId)`

### Steps
1. Load `LiveSessionStatus`.
2. Set `lastHeartbeatAt = now`.
3. Update `ProctoringSession.lastHeartbeatAt = now`.

### Postconditions
- Heartbeat timestamps updated.

---

## 9.3 `ProctoringSessionService.updateLiveStatus(sessionId, updates)`

### Steps
1. Load `LiveSessionStatus`.
2. Update the provided fields (`candidateStatus`, `connectionStatus`, `cameraStatus`, `microphoneStatus`, `screenStatus`, `isFullscreen`, `isWindowFocused`, `riskScore`, `riskLevel`, `openAlertCount`, `currentQuestionNo`, `answeredCount`, `remainingSeconds`, `networkLatencyMs`, `websocketId`, `updatedByNode`).

### Postconditions
- `LiveSessionStatus` updated in place.

---

## 9.4 `ProctoringSessionService.endSession(sessionId)`

### Steps
1. Load `ProctoringSession`.
2. Set `status = COMPLETED`, `endedAt = now`.
3. Ingest `ProctoringEventType.SESSION_ENDED`.
4. Write `AuditLog`.

### Postconditions
- `ProctoringSession.status = COMPLETED`.

---

## 9.5 `ProctoringEventService.ingestEvent(sessionId, type, severity, source, payload)`

### Preconditions
- `ProctoringSession` exists.

### Invariants
- `idempotencyKey` deduplicates retries.

### Steps
1. If `idempotencyKey` provided:
   - Query `ProctoringEvent` by `idempotencyKey`.
   - If found → return the existing row. **Stop.**
2. Insert `ProctoringEvent { proctoringSessionId, deviceSessionId, eventType, severity, source, occurredAt, receivedAt: now, offsetMs: occurredAt - ExamAttempt.startedAt, durationMs, description, payload, autoAction: NONE, idempotencyKey }`.
3. Increment `ProctoringSession.eventCount`.
4. If `severity ∈ {HIGH, CRITICAL}` → increment `ProctoringSession.criticalEventCount`.
5. Update `LiveSessionStatus.lastEventAt = now`, `openAlertCount += 1`.
6. Call `applyAutoAction(event)`.
7. If the event is severe enough → trigger `RiskScoringService.computeScore`.

### Postconditions
- One `ProctoringEvent` row (or the existing one).
- Counters incremented.

---

## 9.6 `ProctoringEventService.applyAutoAction(event)`

### Steps
1. Load the session's `ProctoringPolicy` and current `RiskAssessment`.
2. Consult `RiskFactorConfig.graceOccurrences`.
3. Decide `autoAction`:
   - Single occurrence within grace → `LOG_ONLY`.
   - Recurring past grace → escalate per the factor's `severity` and `isImmediateCritical`.
   - `WARN_CANDIDATE`, `PAUSE_ATTEMPT`, `LOCK_SCREEN`, `NOTIFY_PROCTOR`, `TERMINATE_ATTEMPT`.
4. Set `ProctoringEvent.autoAction`.
5. If `PAUSE_ATTEMPT` → create `AttemptPauseRequest { requestedByUserId: null, status: AUTO_APPROVED }`.
6. If `TERMINATE_ATTEMPT` → set `ExamAttempt.status = INVALIDATED`, `invalidatedAt`, `invalidationReason`.
7. Write `AuditLog` with `reason` required for `TERMINATE_ATTEMPT`.

### Postconditions
- `autoAction` recorded.
- If terminal, `ExamAttempt.status = INVALIDATED`.

---

## 9.7 `EvidenceService.captureEvidence(sessionId, kind, sourceEvent?)`

### Steps
1. Load `ProctoringSession`, `ProctoringPolicy`.
2. Capture the artefact.
3. Compute `checksumSha256`.
4. Compute `retentionUntil = capturedAt + ProctoringPolicy.evidenceRetentionDays` (**at capture time**).
5. Insert `EvidenceFile { publicId: UUID, proctoringSessionId, proctoringEventId, kind, storagePath, fileName, contentType, sizeBytes, checksumSha256, capturedAt, offsetMs, durationMs, widthPx, heightPx, uploadStatus: PENDING, retentionUntil }`.
6. Write `EvidenceCustodyRecord { evidenceFileId, sequenceNo: 1, transition: CAPTURED, checksumAfter: checksumSha256 }`.
7. Write `EvidenceCustodyRecord { sequenceNo: 2, transition: HASHED, checksumBefore: checksumSha256, checksumAfter: checksumSha256 }` — a no-op transition unless a mismatch is found, in
   which case the pipeline stops and the file is quarantined instead of continuing to upload.
8. Upload bytes to object storage, authenticated as an `ApiClient` (`actorApiClientId`, never a
   `User` — the uploader is a machine).
9. Update `uploadStatus = UPLOADED`, `uploadedAt = now`.
10. Write `EvidenceCustodyRecord { sequenceNo: 3, transition: UPLOADED, actorApiClientId, checksumBefore: checksumSha256, checksumAfter: recomputed post-upload checksum }` — reject if the
    two checksums differ.
11. Write `AuditLog`.

### Postconditions
- One `EvidenceFile` row.
- Three `EvidenceCustodyRecord` rows (CAPTURED, HASHED, UPLOADED).
- `retentionUntil` frozen at capture time.

---

## 9.8 `EvidenceService.viewEvidence(fileId, actorId, purpose)`

### Preconditions
- `actorId` has `evidence:download` permission.

### Invariants
- Every read writes an `EvidenceAccessLog` **first**.

### Steps
1. Check `checkAccess(actorId, 'evidence:download')`.
2. If denied:
   - Write `EvidenceAccessLog { evidenceFileId, actorUserId: actorId, action, accessedAt: now, contextReference, purpose, ipAddress, userAgent, granted: false, deniedReason }`.
   - Return 403. **Stop.**
3. Write `EvidenceAccessLog { granted: true, ... }`.
4. Write `EvidenceCustodyRecord { transition: ACCESSED, actorUserId: actorId }`.
5. Return the evidence (stream / signed URL / download).

### Postconditions
- One `EvidenceAccessLog` row on every call, granted or denied.
- One `EvidenceCustodyRecord` on granted access.

---

## 9.9 `EvidenceService.purgeExpired` (scheduled sweep)

### Steps
1. Query `EvidenceFile` where `retentionUntil < now` AND `uploadStatus ≠ PURGED`.
2. Delete bytes from object storage.
3. Set `uploadStatus = PURGED`, `purgedAt = now`.
4. Write `EvidenceAccessLog { action: PURGE }`.
5. Write `EvidenceCustodyRecord { transition: PURGED }` — the terminal transition; the file
   cannot be re-uploaded under the same `evidenceFileId`.
6. Write `AuditLog`.

### Postconditions
- Expired evidence purged.
- Access log and custody record written.

---

## 9.10 `DeviceTrustService.recordSighting(examId, deviceFingerprint, candidateId)`

### Steps
1. Query `DeviceTrustRecord` for `(deviceFingerprint, examId)`.
2. If none → insert `DeviceTrustRecord { deviceFingerprint, examId, firstSeenAt: now, lastSeenAt: now, sightingCount: 1, distinctCandidateCount: 1, distinctCandidateIds: [candidateId], trustLevel: UNKNOWN }`.
3. If exists:
   - `sightingCount += 1`, `lastSeenAt = now`.
   - If `candidateId` not already in `distinctCandidateIds` → append it, `distinctCandidateCount += 1`.
4. If `distinctCandidateCount` crosses the policy threshold AND `trustLevel = UNKNOWN`:
   - Set `trustLevel = SHARED_SUSPECTED`, `flaggedAt = now`, `flaggedReason`.
   - Insert `SuspiciousActivity { activityType: COMBINED_BEHAVIOR, ruleCode: 'SHARED_DEVICE', verdict: DETECTED }`.
5. Write `AuditLog` on every `trustLevel` transition (not on every sighting).

### Postconditions
- `DeviceTrustRecord` updated in place.
- A `SuspiciousActivity` row on first crossing the threshold, not on every subsequent sighting.

## 9.11 `DeviceTrustService.confirmShared(recordId, reviewFindingId)`

### Preconditions
- `ReviewFinding.verdict = VALID` for the referencing `SuspiciousActivity`.

### Steps
1. Load `DeviceTrustRecord`.
2. Set `trustLevel = SHARED_CONFIRMED`.
3. Write `AuditLog`.

### Postconditions
- `trustLevel = SHARED_CONFIRMED` only ever reached through this path, never automatically.

## 9.12 `DeviceTrustService.block(recordId, reason)`

### Steps
1. Set `trustLevel = BLOCKED`.
2. `ExamAttemptService.startAttempt` must check for a `BLOCKED` `DeviceTrustRecord` matching the
   incoming device fingerprint and reject new attempts — existing attempts are untouched.

---

## 9.13 `ProctorShiftService.startShift(sessionId, proctorId)`

### Preconditions
- No `ProctorShift` for the session has `shiftEndAt = null`.

### Steps
1. Insert `ProctorShift { proctoringSessionId, proctorUserId: proctorId, shiftStartAt: now }`.
2. Set `ProctoringSession.assignedProctorUserId = proctorId`.

### Postconditions
- One open `ProctorShift`; `ProctoringSession.assignedProctorUserId` mirrors it.

## 9.14 `ProctorShiftService.handOver(shiftId, note)`

### Preconditions
- `note` non-null (required on handover).

### Steps
1. Load the open `ProctorShift`.
2. Set `shiftEndAt = now`, `handoverNote = note`, `outcome = HANDED_OVER`.
3. Call `startShift` for the incoming proctor.

### Postconditions
- Old shift closed; new shift open; `assignedProctorUserId` updated.

## 9.15 `ProctorShiftService.endShift(shiftId, outcome)`

### Steps
1. Load the open `ProctorShift`.
2. Set `shiftEndAt = now`, `outcome`.
3. `ProctoringSession.assignedProctorUserId` is left as-is (or cleared, per policy) — no new
   shift is opened.

---

## 9.16 `ProctorActionService.recordAction(sessionId, proctorId, actionType, reason?)`

### Preconditions
- `reason` non-null when `actionType ∈ {WARN, FLAG, PAUSE, TERMINATE}`.
- `supervisorApprovalUserId` set when `actionType = TERMINATE` and the exam's policy requires
  two-person approval.

### Steps
1. Load `ProctoringSession`; load the currently open `ProctorShift` for the session.
2. If `reason` required and null → reject. **Stop.**
3. If `actionType = TERMINATE` and policy requires two-person approval and
   `supervisorApprovalUserId = null` → reject. **Stop.**
4. Insert `ProctorAction { proctoringSessionId, proctorUserId: proctorId, proctorShiftId, actionType, reason, occurredAt: now, supervisorApprovalUserId, supervisorApprovedAt }`.
5. Ingest a `ProctoringEvent` with `source = HUMAN_PROCTOR` matching `actionType`.
6. If `actionType = TERMINATE` → set `ExamAttempt.status = INVALIDATED`.
7. If `actionType = PAUSE` → call `AttemptPauseService.requestPause` with `requestedByUserId = proctorId`.
8. Write `AuditLog { action: matching AuditAction, reason }`.
9. On delivery to the candidate (e.g. a UI toast, a forced message) → set `candidateNotified = true`.

### Postconditions
- One `ProctorAction` row.
- One `ProctoringEvent`, one `AuditLog`.
- `candidateNotified` set once actually delivered, not at write time.

---

# Module 10 — AI Detection

## 10.1 `AiDetectionIngestService.receiveInference(sessionId, modelVersionId, confidence, rawOutput)`

### Preconditions
- Caller is an authenticated `ApiClient` scoped for detection ingest.
- `ProctoringSession` exists.
- `AiModelVersion` exists.

### Invariants
- Below `confidenceThreshold` → advisory only, no event.
- At or above `detectionThreshold` → event raised.
- `rawOutput` stored verbatim.

### Steps
1. Authenticate the `ApiClient` (1.13).
2. Load `AiModelVersion`; read `confidenceThreshold`, `detectionThreshold`.
3. Load the `ProctoringPolicy`.
4. Insert `AiDetection { detectionType, proctoringSessionId, aiModelVersionId: modelVersionId, modelName, modelVersion, confidence, capturedAt, processedAt: now, processingTimeMs, offsetMs, anomaly: confidence >= detectionThreshold, rawOutput }`.
5. If `AiModelVersion.status = SHADOW` → set `anomaly = false` unconditionally and skip step 7 —
   a shadow version's findings never drive an event, regardless of confidence.
6. Insert the matching detail row:
   - `FaceDetection { faceCount, identityMatchScore, identityMatched, gazeDirection, gazeOffScreenMs, headYaw, headPitch, headRoll, eyesClosed, maskOrOcclusionDetected, livenessScore, spoofSuspected, boundingBox }`.
   - `ObjectDetection { objectClass, objectLabel, objectCount, prohibited, proximityScore, persistedFrames, boundingBox }`.
   - `BehaviorDetection { behaviorType, windowStartAt, windowEndAt, durationMs, occurrenceCount, intensityScore, baselineDeviation, audioRelated }`.
   - `AudioDetection { audioEventType, windowStartAt, windowEndAt, durationMs, speakerCount, unknownSpeaker, peakDb, averageDb, signalToNoiseRatio, speechRatio, languageCode, transcriptExcerpt }` — `transcriptExcerpt` only if `ProctoringPolicy.retainAudioTranscript = true`.
   - Each detail row's `aiDetectionId` points back to the parent inserted in step 4.
7. If `confidence < confidenceThreshold`:
   - No `ProctoringEvent`. Return the `AiDetection` id.
8. If `confidence >= detectionThreshold` (and not SHADOW):
   - Call `ProctoringEventService.ingestEvent(sessionId, matchingEventType, severity, AI_ENGINE, payload)`.
   - Set `AiDetection.evidenceFileId`/associated event reference via the event's own id (stored
     on the `ProctoringEvent` side, not a new column on `AiDetection` beyond what already exists).
9. Trigger `SuspiciousActivityCorrelationService.correlateWindow`.

### Postconditions
- One `AiDetection` row.
- One detail row, keyed to it by `aiDetectionId`.
- If threshold crossed and not SHADOW, one `ProctoringEvent`.

---

## 10.2 `SuspiciousActivityCorrelationService.correlateWindow(sessionId, windowStart, windowEnd)`

### Steps
1. Load all `AiDetection` and `ProctoringEvent` rows in the window.
2. For each configured `ruleCode` in the correlation catalogue:
   - Check whether the rule's signals co-occur.
   - If yes:
     - Compute `severity` (max of contributing severities).
     - Compute `confidence` (combined).
     - Compute `firstSeenAt`, `lastSeenAt`, `durationMs`.
     - Compute `occurrenceCount`, `detectionCount`, `eventCount`.
     - Pick `primaryDetectionId` (most indicative).
     - Build `contributingSignals` JSON.
     - Upsert `SuspiciousActivity { proctoringSessionId, examAttemptId, activityType, severity, confidence, firstSeenAt, lastSeenAt, durationMs, occurrenceCount, detectionCount, eventCount, primaryDetectionId, contributingSignals, ruleCode, description, verdict: DETECTED }`.
3. Trigger `RiskScoringService.computeScore`.

### Postconditions
- Zero or more `SuspiciousActivity` rows.
- `verdict` starts `DETECTED` — never moved by this service.

---

## 10.3 `ShadowEvaluationService.evaluate(shadowVersionId, activeVersionId, window)`

### Preconditions
- `shadowVersionId` refers to an `AiModelVersion` with `status = SHADOW`.
- `activeVersionId` refers to an `AiModelVersion` with `status = ACTIVE`, same `AiModel.purpose`.

### Steps
1. Query `AiDetection` rows for each version over the window (same frames, both models ran).
2. Compute `agreementCount`/`disagreementCount`/`shadowOnlyDetections`/`activeOnlyDetections`.
3. `shadowFalsePositiveRate`/`activeFalsePositiveRate` computed only from `ReviewFinding` verdicts
   on the respective version's detections — never self-reported.
4. Determine `recommendation`:
   - `PROMOTE` requires `shadowFalsePositiveRate <= activeFalsePositiveRate` AND a minimum sample
     count met.
   - Else `HOLD` (or `RETIRE` if materially worse).
5. Insert `ShadowEvaluation { shadowVersionId, activeVersionId, windowStartAt, windowEndAt, agreementCount, disagreementCount, shadowOnlyDetections, activeOnlyDetections, shadowFalsePositiveRate, activeFalsePositiveRate, recommendation, computedAt: now }`.
6. If `recommendation = PROMOTE` → write `AuditLog { action: MODEL_CHANGE }` **before** any
   actual promotion call — the measurement is on record first.

### Postconditions
- One `ShadowEvaluation` row.
- `AuditLog` written only for a `PROMOTE` recommendation.

---

# Module 11 — Risk Engine

## 11.1 `RiskScoringService.computeScore(attemptId)`

### Preconditions
- `ExamAttempt` exists.

### Invariants
- A recompute creates a new `RiskAssessment` version.
- Exactly one row per attempt has `isLatest = true`.
- The threshold version is copied.

### Steps

**Step 1 — Load context.**
- Load `ExamAttempt`, `ProctoringSession`, `Exam`, `ProctoringPolicy`.

**Step 2 — Load active factors.**
- Query `RiskFactorConfig` where `active = true` AND `effectiveFrom <= now` AND (`effectiveTo IS NULL OR effectiveTo > now`).

**Step 3 — For each factor, compute contribution.**
- Filter applicable `ProctoringEvent` / `AiDetection` / `SuspiciousActivity` rows by `triggerKind` and `triggerCode`.
- `occurrenceCount` = count of matching rows.
- `base = baseWeight × confidenceMultiplier`.
- `frequency = frequencyIncrement × max(0, occurrenceCount - graceOccurrences)`.
- `duration = durationWeightPerSecond × totalDurationSeconds` (where relevant).
- `raw = base + frequency + duration`.
- `decayed = raw × 0.5^(ageSeconds / decayHalfLifeSeconds)`.
- `capped = min(decayed, maxContribution)`.
- `contributedPoints = capped`.
- Insert `RiskEvent { riskAssessmentId, proctoringEventId, aiDetectionId, factorCode, factorLabel, severity, weight, occurrenceCount, contributedPoints, occurredAt, note }`.

**Step 4 — Sum.**
- `riskScore = clamp(sum(contributedPoints), 0, 100)`.

**Step 5 — Resolve level.**
- Query `RiskLevelThreshold` where `active = true` AND `effectiveFrom <= now` AND (`effectiveTo IS NULL OR effectiveTo > now`).
- Find the band where `minScore <= riskScore < maxScore`.
- Read `riskLevel`, `recommendation`, `autoAction`, `opensReviewCase`, `withholdsResult`, `alertsProctor`.

**Step 6 — Version flip.**
- Mark the previous `RiskAssessment` row `isLatest = false` (atomically).
- Insert new `RiskAssessment { examAttemptId, proctoringSessionId, version: prev + 1, latest: true, riskScore, riskLevel, recommendation, scoringModel, scoringModelVersion, thresholdVersion, computedAt: now, identityScore, faceAnomalyScore, objectAnomalyScore, behaviorAnomalyScore, environmentScore, totalEventCount, criticalEventCount, autoActionApplied }`.

**Step 7 — Apply threshold actions.**
- If `opensReviewCase` → `ReviewCaseService.openCase(attemptId, reason, riskAssessmentId)`.
- If `withholdsResult` → set `ExamResult.status = PENDING_REVIEW`; call
  `ResultWithholdingService.withhold(resultId, RISK_THRESHOLD, ...)` (Idea 9) — the withholding
  record, not just the status column, is what carries the "why" forward.
- If `alertsProctor` → update `LiveSessionStatus` and notify the assigned proctor.
- If `autoAction` → apply it.

**Step 8 — Mirror to live status.**
- Set `LiveSessionStatus.riskScore`, `.riskLevel`.

**Step 9 — Audit.**
- If `autoAction` applied → write `AuditLog`.

### Postconditions
- One new `RiskAssessment` with `isLatest = true`.
- Previous `isLatest = false`.
- One `RiskEvent` per contributing factor.
- Threshold actions applied.

---

# Module 12 — Review System

## 12.1 `ReviewCaseService.openCase(attemptId, reason, riskAssessmentId?)`

### Preconditions
- `ExamAttempt` exists.
- `reason` non-null.

### Invariants
- A case is **not unique** per attempt.

### Steps
1. Generate `caseNumber` (e.g. `RC-YYYY-NNNNNN`).
2. Determine `priority` from the risk band (or default `NORMAL`).
3. Compute `dueAt` from SLA.
4. Insert `ReviewCase { caseNumber, examAttemptId, riskAssessmentId, status: OPEN, priority, openedAt: now, openedByUserId, openReason: reason, dueAt }`.
5. Write `AuditLog`.
6. Notify reviewers.

### Postconditions
- One `ReviewCase` row in `OPEN`.

---

## 12.2 `ReviewCaseService.assignReviewer(caseId, reviewerUserId)`

### Steps
1. Load `ReviewCase`.
2. Set `assignedReviewer`, `assignedAt = now`, `status = ASSIGNED`.
3. Write `AuditLog`.

### Postconditions
- `ReviewCase.status = ASSIGNED`.

---

## 12.3 `ReviewCaseService.addFinding(caseId, itemKind, itemId, verdict)`

### Steps
1. Load `ReviewCase`.
2. Insert `ReviewFinding { reviewCaseId, itemKind, itemId, verdict, reviewerUserId, reviewedAt: now, comment, weightDisputed }`.
3. Do **not** change the case outcome.
4. If `verdict = FALSE_POSITIVE` AND `itemKind = AI_DETECTION` → this is ground truth for `ModelPerformanceMetric`.

### Postconditions
- One `ReviewFinding` row.

---

## 12.4 `ReviewCaseService.addNote(caseId, body, candidateVisible)`

### Steps
1. Insert `ReviewNote { reviewCaseId, authorUserId, body, candidateVisible, attachmentPath }`.

### Postconditions
- One `ReviewNote` row.

---

## 12.5 `ReviewCaseService.recordDecision(caseId, type, rationale)`

### Preconditions
- `rationale` non-null, non-empty.

### Invariants
- Append-only.

### Steps
1. Load `ReviewCase`.
2. Compute `sequenceNo = previous max + 1`.
3. Insert `ReviewDecision { reviewCaseId, reviewerUserId, sequenceNo, decisionType, decidedAt: now, rationale, evidenceRefs, scoreAdjustment, finalDecision: finalDecision, supersedesDecisionId }`.
4. If `finalDecision = true` → set `ReviewCase.finalOutcome`, `status = RESOLVED`.
5. If `decisionType = ADJUST_SCORE` → call `ExamResultService.applyReviewAdjustment`.
6. If `decisionType = GRANT_RETAKE` → this decision is what `RetakeGrantService.grantRetake`
   requires as its precondition — the grant is a separate call, not implicit in the decision.
7. Write `AuditLog`.

### Postconditions
- One `ReviewDecision` row.
- If final, `ReviewCase` resolved.

---

## 12.6 `ReviewCaseService.closeCase(caseId, finalOutcome)`

### Steps
1. Load `ReviewCase`.
2. If `status` terminal → reject. **Stop.**
3. Set `status = CLOSED`, `closedAt = now`, `finalOutcome`.
4. Notify the candidate; set `candidateNotified = true`.
5. If the result was `PENDING_REVIEW`/withheld → call `ResultWithholdingService.release` then
   `ExamResultService.finalize`.
6. Write `AuditLog`.

### Postconditions
- `ReviewCase.status = CLOSED`.
- Result finalised if held.

---

# Module 13 — Exam Results

## 13.1 `ExamResultService.computeRawScore(attemptId)`

### Steps
1. Load `ExamAttempt`.
2. For each delivered `ExamQuestion`:
   - Load `AttemptAnswer` (or null).
   - If `CODE` → sum `CodeExecutionResult.points` against `CodeTestCase.points`.
   - Else → use `AttemptAnswer.pointsAwarded`.
   - Insert `ResultDetail { examResultId, examQuestionId, examSectionId, attemptAnswerId, sequenceNo, pointsAwarded, pointsPossible, correct, answered, timeSpentSeconds, scoringNote }`.
3. Sum `pointsAwarded` → `rawScore`.
4. Sum `pointsPossible` → `maxScore`.
5. Count `correct`, `incorrect`, `unanswered`, `pendingManualCount`.
6. Upsert `ExamResult { examAttemptId, examId, candidateUserId, rawScore, finalScore: rawScore + scoreAdjustment, maxScore, percentage, status: PROVISIONAL, integrityStatus: CLEAN, gradingMode, correctCount, incorrectCount, unansweredCount, pendingManualCount, timeSpentSeconds }`.

### Postconditions
- One `ResultDetail` per delivered question.
- One `ExamResult` in `PROVISIONAL`.

---

## 13.2 `ExamResultService.applyReviewAdjustment(resultId, decision)`

### Preconditions
- `ReviewDecision.decisionType = ADJUST_SCORE`.

### Steps
1. Load `ExamResult` and `ReviewDecision`.
2. `scoreAdjustment += decision.scoreAdjustment`.
3. `finalScore = rawScore + scoreAdjustment`.
4. `passed = finalScore >= Exam.passingScore`.
5. Write `AuditLog { action: SCORE_OVERRIDE, reason: required }`.

### Postconditions
- `ExamResult.scoreAdjustment` / `finalScore` updated.
- Adjustment traces back to the decision.

---

## 13.3 `ExamResultService.finalize(resultId)`

### Preconditions
- `ExamResult.status ∈ {PROVISIONAL, PENDING_REVIEW}`.

### Invariants
- Blocked while any `ResultWithholding` on the result has `releasedAt = null`.
- `riskScore` / `riskLevel` frozen at finalisation.

### Steps
1. Load `ExamResult`, `Exam`.
2. Query `ResultWithholding` rows for the result where `releasedAt = null`. If any exist → **block**; leave `status = PENDING_REVIEW`. **Stop.**
3. Load the latest `RiskAssessment`.
4. Copy `riskScore`, `riskLevel`.
5. Load `ReviewCase` if one exists; copy `reviewCase`.
6. Set `status = FINAL`, `gradedAt = now`, `gradedBy`.
7. Compute `passed = finalScore >= Exam.passingScore`.
8. Write `AuditLog`.

### Postconditions
- `ExamResult.status = FINAL`.
- `riskScore` / `riskLevel` frozen.

---

## 13.4 `ExamResultService.publish(resultId)`

### Steps
1. Load `ExamResult`; confirm `status = FINAL`.
2. Set `publishedAt = now`, `releasedToCandidate = true`.
3. Call `NotificationService.send(RESULT_PUBLISHED, candidateUserId, ...)`.
4. Write `AuditLog`.

### Postconditions
- `ExamResult` published.
- One `Notification`.

---

## 13.5 `ResultWithholdingService.withhold(resultId, reason, reviewCaseId?)`

### Preconditions
- The resolved `RiskLevelThreshold.withholdsResult = true` OR a `ReviewCase` is open for the
  attempt.

### Steps
1. Load `ExamResult`.
2. Insert `ResultWithholding { examResultId, reason, reviewCaseId, riskAssessmentId, thresholdVersion, withheldAt: now, withheldByUserId }`.
3. Set `ExamResult.status = WITHHELD` (or leave `PENDING_REVIEW`, per `reason`).

### Postconditions
- One `ResultWithholding` row, `releasedAt = null`.

## 13.6 `ResultWithholdingService.release(withholdingId, releasedBy, reason)`

### Preconditions
- `reason` non-null.

### Steps
1. Load `ResultWithholding`. If `releasedAt != null` → reject (already released). **Stop.**
2. Set `releasedAt = now`, `releasedByUserId: releasedBy`, `releaseReason: reason`.
3. If no other unreleased `ResultWithholding` remains on the result → `ExamResultService.finalize`
   may now proceed.

### Postconditions
- `ResultWithholding.releasedAt` set.
- A later re-withholding is a new row, never a reopening of this one.

---

# Module 14 — Proctoring Reports

## 14.1 `ProctoringReportService.generateReport(attemptId)`

### Steps
1. Load `ExamAttempt`, `ProctoringSession`, `RiskAssessment`, `ReviewCase`.
2. Render `summarySnapshot` JSON.
3. Insert `ProctoringReport { publicId: UUID, examAttemptId, proctoringSessionId, riskAssessmentId, reviewCaseId, version: 1, status: QUEUED, format, requestedByUserId, requestedAt: now, generatedAt: now, storagePath, sizeBytes, checksumSha256, summarySnapshot, riskScore, finalIntegrityStatus, includesEvidence, expiresAt }`.
4. Update `status = AVAILABLE`.
5. Write `AuditLog`.

### Postconditions
- One `ProctoringReport` row, immutable.

---

## 14.2 `ProctoringReportService.regenerate(reportId)`

### Steps
1. Load the existing report.
2. Do **not** edit version 1.
3. Generate a new report with `version = old + 1`.
4. Old version stays.

### Postconditions
- New report version; old preserved.

---

## 14.3 `AttemptTimelineService.renderTimeline(attemptId)`

### Steps
1. Query `ProctoringEvent`, `AnswerRevision`, `RiskEvent`, `EvidenceFile` rows for the attempt,
   each contributing `{offsetMs or savedAt, kind, refId, label, severity}` entries.
2. Merge and sort by offset/timestamp into `timelineJson`.
3. Determine `version = previous max for this attempt + 1`.
4. Compute `checksumSha256` over the rendered JSON.
5. If `ProctoringPolicy.retainAudioTranscript` was false at capture time for a given
   `AudioDetection`, its transcript is omitted from the rendered entry regardless of the policy
   in force at render time.
6. Insert `AttemptTimeline { examAttemptId, version, renderedAt: now, entryCount, timelineJson, checksumSha256, generatedByUserId }`.

### Postconditions
- One new `AttemptTimeline` version; previous versions untouched.
- Every entry's `refId` resolves to a real row in its source table — no content is duplicated
  into the timeline itself beyond the rendering fields.

---

# Module 15 — Notifications

## 15.1 `NotificationService.send(type, recipientId, variables)`

### Preconditions
- `recipientId` refers to an existing `User`.

### Invariants
- `idempotencyKey` prevents duplicate sends.
- Rendered content is stored.

### Steps
1. If `idempotencyKey` provided:
   - Query `Notification` by `idempotencyKey`.
   - If found → return the existing row. **Stop.**
2. If `type ∉ {HIGH_RISK_ALERT, SYSTEM_ALERT}`:
   - Check `NotificationSuppressionService.shouldSuppress(recipientId, type, channel)`:
     - If suppressed by quiet hours → defer (`scheduledFor` set to the window's end, in the
       recipient's `User.timeZone`).
     - If suppressed by opt-out or a hard rule → write `Notification { status: CANCELLED, failureReason: 'SUPPRESSED_BY_RULE' }`. **Stop.**
     - If suppressed by rate limit → defer.
3. Load `User` for `locale`.
4. Find `NotificationTemplate` by `(type, channel, locale)` where `active = true`.
5. Render `subjectTemplate`, `bodyTemplate` with `variables`.
6. Insert `Notification { recipientUserId, notificationType, channel, status: PENDING, templateId, sentTo, subject, body, variables, relatedEntityType, relatedEntityId, scheduledFor, idempotencyKey }`.
7. Dispatch.
8. Update `status = SENT`, `sentAt = now`.
9. On delivery webhook → `DELIVERED`, `deliveredAt`.
10. On open → `OPENED`, `openedAt`.
11. On failure → `retryCount += 1`, `nextRetryAt = now + backoff`. If `retryCount > bound` → `status = FAILED`.
12. Store `providerMessageId`.

### Postconditions
- One `Notification` row.
- Rendered content stored.

---

## 15.2 `NotificationService.scheduleReminder(examId, offsetBeforeStart)`

### Steps
1. Query `ExamAssignment` rows for the exam.
2. For each, insert `Notification { scheduledFor: Exam.opensAt - offsetBeforeStart, ... }`.
3. Scheduler dispatches when `scheduledFor <= now`.

### Postconditions
- One `Notification` per assignment, scheduled.

---

## 15.3 `NotificationSuppressionService.shouldSuppress(recipientId, type, channel)`

### Preconditions
- Consulted by `NotificationService.send` before every dispatch except for
  `HIGH_RISK_ALERT`/`SYSTEM_ALERT`, which never call this at all.

### Steps
1. Query `NotificationSuppression` rows matching `(recipientUserId = recipientId OR null)` AND
   `(notificationType = type OR null)` AND `(channel = channel OR null)` AND `isActive = true`.
2. For `QUIET_HOURS` rows: convert `now` to the recipient's `User.timeZone`; if within
   `[windowStartLocal, windowEndLocal)` → suppress, deferred to `windowEndLocal`.
3. For `OPT_OUT` rows: suppress, cancelled.
4. For `RATE_LIMIT` rows: count `Notification` rows for the recipient in the last
   `windowSeconds`; if `>= maxPerWindow` → suppress, deferred.
5. For `DUPLICATE_WINDOW` rows: count near-identical `Notification` rows (same type, same
   rendered body hash) for the recipient in the last `windowSeconds`; if any → suppress,
   cancelled. This check is independent of `idempotencyKey`, which only catches a retried
   trigger, not a fresh trigger producing a near-identical message.

### Postconditions
- Read-only; returns a suppression decision consumed by `send`.

---

# Module 16 — Audit

## 16.1 `AuditService.record(action, actorId, entityType, entityId, before, after, reason?)`

### Preconditions
- `action` ∈ `AuditAction`.

### Invariants
- Append-only.
- `reason` required for specific actions.

### Steps
1. Resolve `actorRole` from the actor's current `UserRole` grants **at the time**.
2. If `action ∈ {SCORE_OVERRIDE, PERMISSION_CHANGE, CONFIG_CHANGE, TERMINATE_ATTEMPT}` AND `reason = null` → reject the call. **Stop.**
3. Insert `AuditLog { actorUserId: actorId, actorRole, action, outcome, entityType, entityId, entityLabel, occurredAt: now, beforeState, afterState, reason, ipAddress, userAgent, requestId }`.
4. Never update or delete.

### Postconditions
- One `AuditLog` row.

---

# Module 17 — AI Model Registry

## 17.1 `AiModelRegistryService.registerVersion(modelId, thresholds, config)`

### Steps
1. Load `AiModel`; confirm `active = true`.
2. Insert `AiModelVersion { aiModelId, version, status: DRAFT, artifactRef, artifactChecksum, confidenceThreshold, detectionThreshold, configuration, releaseNotes }`.
3. Write `AuditLog { action: MODEL_CHANGE }`.

### Postconditions
- One `AiModelVersion` in `DRAFT`.

---

## 17.2 `AiModelRegistryService.promote(versionId, newStatus)`

### Steps
1. Load `AiModelVersion`.
2. Validate transition (`DRAFT → CANDIDATE → SHADOW → ACTIVE → DEPRECATED → RETIRED`).
3. If `newStatus = ACTIVE`:
   - Deprecate the current `ACTIVE` version for the same model.
   - Set `activatedAt = now`, `activatedBy`.
4. If `newStatus = SHADOW`:
   - The version scores in parallel but never drives `autoAction` (enforced in
     `AiDetectionIngestService.receiveInference` step 5).
5. Write `AuditLog { action: MODEL_CHANGE }`.

### Postconditions
- Status transitioned; previous `ACTIVE` deprecated.

---

## 17.3 `AiModelRegistryService.recordMetric(versionId, metricType, window)`

### Steps
1. Load `AiModelVersion`.
2. Compute the metric from `ReviewFinding` verdicts.
3. Insert `ModelPerformanceMetric { aiModelVersionId, metricType, metricValue, windowStartAt, windowEndAt, sampleCount, truePositiveCount, falsePositiveCount, falseNegativeCount, computedAt: now, note }`.

### Postconditions
- One `ModelPerformanceMetric` row.

---

# Module 18 — System Configuration

## 18.1 `SystemSettingService.set(key, value)`

### Preconditions
- `key` refers to an existing `SystemSetting`.

### Invariants
- `editable = false` rejects writes.
- `secret = true` is never returned in plaintext.

### Steps
1. Load `SystemSetting` by `key`.
2. If `editable = false` → reject. **Stop.**
3. Validate `value` against `valueType` and `validationRule`.
4. Set `settingValue`, `updatedBy`, `lastChangedAt = now`.
5. If `requiresRestart = true` → warn.
6. Write `AuditLog { action: CONFIG_CHANGE, reason: required }`.

### Postconditions
- `SystemSetting.settingValue` updated.
- One `AuditLog` with `reason`.

---

## 18.2 `RiskFactorConfigService.updateFactor(factorCode, weights)`

### Steps
1. Load the currently active `RiskFactorConfig`.
2. Do **not** edit in place.
3. Set the old row's `effectiveTo = now`, `active = false`.
4. Insert a new row with `configVersion` bumped, `effectiveFrom = now`, `effectiveTo = null`, `active = true`.
5. Write `AuditLog { action: CONFIG_CHANGE, reason: required }`.

### Postconditions
- Old row effective-dated closed; new row active.
- `RiskAssessment` computed under the old version keeps its `thresholdVersion`.

---

## 18.3 `RiskLevelThresholdService.updateThresholdBand(level, range)`

### Steps
1. Load the currently active `RiskLevelThreshold`.
2. Do **not** edit in place.
3. Set the old row's `effectiveTo = now`, `active = false`.
4. Insert a new row with `configVersion` bumped, `effectiveFrom = now`, `effectiveTo = null`, `active = true`.
5. Write `AuditLog { action: CONFIG_CHANGE, reason: required }`.

### Postconditions
- Old row effective-dated closed; new row active.

---

# Module 19 — Payment

## 19.1 `PaymentCardService.addCard(paymentCustomerId, providerToken, cardDetails)`

### Preconditions
- `paymentCustomerId` refers to an existing `PaymentCustomer`.
- `providerToken` is the opaque payment-method reference returned by the processor's own
  tokenization step (client-side, before this call — the raw card number never reaches this
  service).

### Invariants
- The raw card number and CVV are never received, logged, or stored by this call or any other
  in this system.
- `(provider, providerPaymentMethodId)` is unique — the same processor token can't be stored
  twice.

### Steps
1. Validate `paymentCustomerId` exists; read its `userId` and `provider`.
2. Query `PaymentCard` for `(provider, providerPaymentMethodId = providerToken)`.
3. If found → reject as a duplicate. **Stop.**
4. Extract `brand`, `funding`, `last4`, `bin`, `expiryMonth`, `expiryYear`, `issuer`,
   `issuerCountry`, `fingerprint` from the processor's response to the tokenization call (the
   processor, not this system, is what saw the actual card number).
5. Insert `PaymentCard { paymentCustomerId, userId, provider, providerPaymentMethodId: providerToken, providerCardToken, brand, funding, last4, bin, expiryMonth, expiryYear, cardholderName, issuer, issuerCountry, billingCountry, billingPostalCode, fingerprint, status: PENDING_VERIFICATION }`.
6. Write `AuditLog { action: PAYMENT_METHOD_ADDED, entityType: 'PaymentCard', entityId: newRow.id }`.

### Postconditions
- One `PaymentCard` row, `status = PENDING_VERIFICATION` — not chargeable yet; see `verifyCard`
  (§19.5).

### Rejection reasons
| Condition | Result |
|---|---|
| `paymentCustomerId` not found | reject |
| Token already stored | reject |

---

## 19.2 `PaymentCardService.setDefault(cardId)`

### Preconditions
- `PaymentCard.status = ACTIVE`.

### Invariants
- At most one `ACTIVE` card per `paymentCustomerId` has `defaultCard = true`.

### Steps
1. Load the card; if `status ≠ ACTIVE` → reject. **Stop.**
2. Set `defaultCard = false` on every other `PaymentCard` row for the same `paymentCustomerId`.
3. Set `defaultCard = true` on this row.
4. Set `PaymentCustomer.defaultPaymentCardId = cardId` for the owning customer.
5. Write `AuditLog { action: PAYMENT_METHOD_SET_DEFAULT, entityType: 'PaymentCard', entityId: cardId }`.

### Postconditions
- Exactly one default card for the customer (or none, if every card was just revoked).

---

## 19.3 `PaymentCardService.removeCard(cardId, reason)`

### Preconditions
- `PaymentCard` exists and `status ≠ REVOKED`.

### Invariants
- The row is never deleted — past charges must keep a valid reference to the card that was
  used at the time.

### Steps
1. Load the card.
2. Set `status = REVOKED`, `revokedAt = now`, `revokedReason = reason`.
3. If it was the default, clear `defaultCard` and, if `PaymentCustomer.defaultPaymentCardId =
   cardId`, clear that too — do **not** silently promote another card to default; the
   caller/candidate chooses the next one explicitly.
4. Write `AuditLog { action: PAYMENT_METHOD_REMOVED, entityType: 'PaymentCard', entityId: cardId, reason }`.

### Postconditions
- `PaymentCard.status = REVOKED`, row preserved.

---

## 19.4 `PaymentCardService.sweepExpiredCards` (scheduled sweep)

### Steps
1. Query `PaymentCard` where `status = ACTIVE` AND `(expiryYear, expiryMonth)` is in the past
   relative to now.
2. Set `status = EXPIRED` on each.

### Postconditions
- No `ACTIVE` card is ever actually past its own expiry date.

---

## 19.5 `PaymentCardService.verifyCard(cardId, verified, cvvCheck, avsLine1Check, avsPostalCodeCheck, threeDsEnrolled)`

### Preconditions
- `PaymentCard.status = PENDING_VERIFICATION`.

### Invariants
- A card is never chargeable before this call resolves it one way or the other.

### Steps
1. Load the card; if `status ≠ PENDING_VERIFICATION` → reject. **Stop.**
2. Set `cvvCheck`, `avsLine1Check`, `avsPostalCodeCheck`, `threeDsEnrolled` from the processor's
   verification response.
3. If `verified` → set `status = ACTIVE`.
4. Else → set `status = REVOKED`, `revokedAt = now`, `revokedReason: 'Verification failed'`.

### Postconditions
- `PaymentCard.status` is `ACTIVE` or `REVOKED` — never left `PENDING_VERIFICATION`.

---

## 19.6 `PaymentCardService.suspendCard(cardId, reason)` / `reinstateCard(cardId)`

### Preconditions
- `suspendCard`: `PaymentCard.status = ACTIVE`.
- `reinstateCard`: `PaymentCard.status = SUSPENDED`.

### Steps
1. `suspendCard`: set `status = SUSPENDED`. Write `AuditLog { action: CONFIG_CHANGE, entityType: 'PaymentCard', entityId: cardId, reason }`.
2. `reinstateCard`: set `status = ACTIVE`.

### Postconditions
- `SUSPENDED` is the only non-terminal state reachable from, and returning to, `ACTIVE` — it
  never silently expires or auto-clears.

---

## 19.7 `PaymentCustomerService.createCustomer(userId, provider, providerCustomerId, billingDetails)`

### Preconditions
- `userId` refers to an existing `User`.
- `providerCustomerId` is the processor's own customer id, created by the caller against the
  processor's API before this call (this service never talks to the processor directly).

### Invariants
- `(userId, provider)` is unique — a user has at most one billing profile per processor.
- `(provider, providerCustomerId)` is unique.

### Steps
1. Validate `userId` exists.
2. Query `PaymentCustomer` for `(userId, provider)`; if found → reject as a duplicate. **Stop.**
3. Insert `PaymentCustomer { userId, provider, providerCustomerId, billingEmail, billingName, billingPhone, billingAddressLine1, billingAddressLine2, billingCity, billingState, billingPostalCode, billingCountry, taxId, preferredCurrency, delinquent: false }`.

### Postconditions
- One `PaymentCustomer` row, ready to have cards added against it via `addCard` (§19.1).

### Rejection reasons
| Condition | Result |
|---|---|
| `userId` not found | reject |
| `(userId, provider)` already exists | reject |

---

## 19.8 `PaymentTransactionService.chargeCard(paymentCardId, amountMinor, currency, type, reference)`

### Preconditions
- `PaymentCard.status = ACTIVE`.
- `type` is `AUTHORIZATION` or `SALE`.

### Steps
1. Load the card; if `status ≠ ACTIVE` → reject. **Stop.**
2. Insert `PaymentTransaction { paymentCustomerId: card.paymentCustomerId, paymentCardId, provider: card.provider, currency, amountMinor, amountRefundedMinor: 0, type, status: INITIATED, reference }`.
3. Submit the charge to the processor using `card.providerPaymentMethodId`.
4. On processor acceptance → set `providerTransactionId`, `providerIntentId`, `status: AUTHORIZED` (if `type = AUTHORIZATION`) or `status: CAPTURED` (if `type = SALE`), `capturedAt: now` (SALE only), `threeDsAuthenticated` from the response.
5. On processor decline → set `status = FAILED`, `failureCode`, `failureMessage`.
6. Set `PaymentCard.lastUsedAt = now` regardless of outcome.

### Postconditions
- One `PaymentTransaction` row reflecting the processor's actual response — never left
  `INITIATED`.

### Rejection reasons
| Condition | Result |
|---|---|
| Card not `ACTIVE` | reject |

---

## 19.9 `PaymentTransactionService.refundTransaction(transactionId, amountMinor)`

### Preconditions
- The parent `PaymentTransaction.status` is `CAPTURED` or `SETTLED`.
- `amountMinor ≤ parent.amountMinor − parent.amountRefundedMinor`.

### Steps
1. Load the parent transaction; validate the precondition. **Stop** if it fails.
2. Insert `PaymentTransaction { paymentCustomerId: parent.paymentCustomerId, paymentCardId: parent.paymentCardId, parentTransactionId: transactionId, provider: parent.provider, currency: parent.currency, amountMinor, type: REFUND, status: INITIATED }`.
3. Submit the refund to the processor.
4. On success → set the new row's `status = SETTLED`, `providerTransactionId`; set
   `parent.amountRefundedMinor += amountMinor`; set `parent.status = REFUNDED` if fully refunded,
   else `PARTIALLY_REFUNDED`.
5. Write `AuditLog { action: PAYMENT_REFUNDED, entityType: 'PaymentTransaction', entityId: transactionId }`.

### Postconditions
- `parent.amountRefundedMinor` never exceeds `parent.amountMinor`.

---

## 19.10 `PaymentTransactionService.recordChargeback(transactionId, chargebackDetails)`

Driven by a processor webhook, not a candidate or admin action.

### Steps
1. Load the disputed transaction.
2. Insert `PaymentTransaction { paymentCustomerId: parent.paymentCustomerId, paymentCardId: parent.paymentCardId, parentTransactionId: transactionId, provider: parent.provider, currency: parent.currency, amountMinor: chargebackDetails.amountMinor, type: CHARGEBACK, status: DISPUTED }`.
3. Set `parent.status = DISPUTED`.
4. Write `AuditLog { action: PAYMENT_DISPUTED, entityType: 'PaymentTransaction', entityId: transactionId }`.
5. On final resolution from the processor (a later webhook, not modeled as a separate function
   here) → set both rows' `status = CHARGEBACK` if the merchant lost, or back to `SETTLED`/
   `CAPTURED` if the dispute was won. **(gap — resolution webhook not specified.)**

### Postconditions
- A disputed transaction is never silently dropped; it always has a `DISPUTED` or `CHARGEBACK`
  row.

---

## 19.11 `ExamPaymentService.setRequirement(examId, amountMinor, currency, description)`

Same shape as `ExamPrerequisite.addRule` (§3.x): one row per exam, no versioning needed because
`ExamPaymentCharge` freezes the amount at charge-creation time regardless of later edits here.

### Steps
1. If an `ExamPaymentRequirement` already exists for `examId` → update `amountMinor`, `currency`,
   `description` in place.
2. Else → insert `ExamPaymentRequirement { examId, amountMinor, currency, description, active: true }`.

### Postconditions
- At most one active `ExamPaymentRequirement` per exam.

---

## 19.12 `ExamPaymentService.createCharge(examId, candidateUserId, examAssignmentId)`

### Preconditions
- An `active` `ExamPaymentRequirement` exists for `examId`.
- No existing `ExamPaymentCharge` for the same `examAssignmentId` is `INITIATED`/`PENDING`/
  `AUTHORIZED`/`CAPTURED`/`SETTLED` (no duplicate live charge for the same assignment).

### Steps
1. Load the exam's `ExamPaymentRequirement`; if none `active` → reject. **Stop.**
2. Insert `ExamPaymentCharge { examId, examAssignmentId, examPaymentRequirementId: requirement.id, candidateUserId, currency: requirement.currency, amountMinor: requirement.amountMinor, status: INITIATED }`.

### Postconditions
- `ExamPaymentCharge.amountMinor`/`currency` are frozen at insert time — a later `setRequirement`
  call never reprices this charge.

### Rejection reasons
| Condition | Result |
|---|---|
| No active requirement for the exam | reject |
| A live charge already exists for the assignment | reject |

---

## 19.13 `ExamPaymentService.payCharge(chargeId, paymentCardId)`

### Preconditions
- `ExamPaymentCharge.status = INITIATED`.
- `PaymentCard.status = ACTIVE`.

### Steps
1. Load the charge; if `status ≠ INITIATED` → reject. **Stop.**
2. Call `chargeCard` (§19.8) with `type: SALE`, `amountMinor: charge.amountMinor`, `currency:
   charge.currency`, `reference: charge.publicId`.
3. Set `charge.paymentTransactionId = transaction.id`.
4. If the transaction's resulting `status = CAPTURED` → set `charge.status = CAPTURED`,
   `charge.paidAt = now`. This is what satisfies `ExamAttemptService.startAttempt`'s payment-gate
   precondition for exams carrying an active `ExamPaymentRequirement`. **(gap —
   `startAttempt`'s precondition list in Module 8 doesn't yet name this check.)**
5. If the transaction's resulting `status = FAILED` → set `charge.status = FAILED`.

### Postconditions
- `ExamPaymentCharge.status` always ends at `CAPTURED` or `FAILED` — never left `INITIATED`.

### Rejection reasons
| Condition | Result |
|---|---|
| Charge not `INITIATED` | reject |
| Card not `ACTIVE` | reject |

---

## 19.14 `ExamPaymentService.refundCharge(chargeId, reason)`

### Preconditions
- `ExamPaymentCharge.status` is `CAPTURED` or `SETTLED`.

### Steps
1. Load the charge; validate the precondition. **Stop** if it fails.
2. Call `refundTransaction` (§19.9) with `transactionId: charge.paymentTransactionId,
   amountMinor: charge.amountMinor`.
3. Set `charge.status = REFUNDED`, `charge.refundedAt = now`.

### Postconditions
- `ExamPaymentCharge.status = REFUNDED`, row preserved — the attempt it gated is unaffected by
  this call; any consequence for the attempt is a separate, unspecified admin decision. **(gap.)**

---

# Module 20 — Excel Runtime

## 20.1 `ExcelSessionService.createSession(examAttemptId)`

### Preconditions
- `examAttemptId` refers to an existing `ExamAttempt` whose exam has at least one `SPREADSHEET`
  question placed in it.
- No `ExcelSession` already exists for this `examAttemptId`.

### Steps
1. Validate the preconditions. **Stop** if either fails.
2. Insert `ExcelSession { examAttemptId, proctoringSessionId: the attempt's ProctoringSession.id if one exists, status: PENDING }`.

### Postconditions
- One `ExcelSession` row, `status = PENDING` — not yet running.

### Rejection reasons
| Condition | Result |
|---|---|
| Exam has no SPREADSHEET question | reject |
| `ExcelSession` already exists for this attempt | reject |

---

## 20.2 `ExcelSessionService.startSession(sessionId)`

### Preconditions
- `ExcelSession.status = PENDING`.

### Steps
1. Load the session; if `status ≠ PENDING` → reject. **Stop.**
2. Allocate a sandboxed container; set `sandboxContainerId`.
3. Select `engineUsed` per the exam's `ExcelPolicy.runtimeRequired`/global `SystemSetting` engine
   selector, falling back to the configured fallback engine if the primary fails to start.
4. Load the question's workbook (`Question.workbookStoragePath`) into the runtime, applying the
   exam's `ExcelPolicy.recalcMode` and seeding `RAND`/`RANDBETWEEN` deterministically per session
   when `volatileFunctionsPinned` is true.
5. Set `status = ACTIVE`, `startedAt = now`.

### Postconditions
- `ExcelSession.status = ACTIVE`, workbook loaded and ready for edits.

---

## 20.3 `ExcelSessionService.recordCellEdit(sessionId, sheetName, cellRef, newValue, newFormula)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED}`.

### Steps
1. Load the session; validate the precondition. **Stop** if it fails.
2. Read the cell's current value/formula as `oldValue`/`oldFormula`.
3. Insert `ExcelCellEdit { excelSessionId, sequenceNo: next in sequence, sheetName, cellRef, oldValue, newValue, oldFormula, newFormula, editedAt: now }`.
4. If this call is part of a paste covering an implausibly large range in one action → write
   `ProctoringEvent { eventType: EXCEL_BULK_PASTE, proctoringSessionId: session.proctoringSessionId, severity: MEDIUM }`. **(gap — the size threshold that counts as "implausibly large" isn't specified.)**

### Postconditions
- One new `ExcelCellEdit` row, strictly increasing `sequenceNo` for the session.

---

## 20.4 `ExcelSessionService.recordSheetOperation(sessionId, operationType, sheetName, detail)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED}`.

### Steps
1. Load the session; validate the precondition. **Stop** if it fails.
2. Insert `ExcelSheetOperation { excelSessionId, operationType, sheetName, occurredAt: now, detail }`.
3. Write `ProctoringEvent { eventType: EXCEL_SHEET_OP, proctoringSessionId: session.proctoringSessionId, severity: INFO }`.

### Postconditions
- One new `ExcelSheetOperation` row.

---

## 20.5 `ExcelSessionService.runMacro(sessionId, macroName, inputSummary)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED}`.

### Invariants
- A macro run is always logged, whether it was actually allowed to execute or not — a
  blocked-but-attempted run is itself the proctoring signal.

### Steps
1. Load the session and its question's `excelMacroPolicy`, and the exam's `ExcelPolicy.macrosAllowed`.
2. Compute `allowed = macrosAllowed AND excelMacroPolicy ≠ OFF AND (excelMacroPolicy = SANDBOXED OR macroName is on the configured whitelist)`.
3. If `allowed` → execute the macro in the sandbox; capture `outputSummary`, `durationMs`, `exitCode`.
4. Insert `ExcelMacroExecution { excelSessionId, macroName, allowed, inputSummary, outputSummary, durationMs, exitCode, executedAt: now }` — `outputSummary`/`durationMs`/`exitCode` stay null when `allowed = false`, since nothing ran.
5. Write `ProctoringEvent { eventType: EXCEL_MACRO_RUN, proctoringSessionId: session.proctoringSessionId, severity: allowed ? INFO : HIGH, autoAction: allowed ? NONE : WARN_CANDIDATE }`.

### Postconditions
- One `ExcelMacroExecution` row always exists for the attempt; it only actually ran when `allowed = true`.

---

## 20.6 `ExcelSessionService.takeSnapshot(sessionId, snapshotType)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED, SUBMITTED}`.
- `ExcelSession.proctoringSessionId` is not null — evidence capture requires a proctoring session
  to attach to, the same constraint every other `EvidenceFile` already has.

### Steps
1. Load the session; validate both preconditions. **Stop** if either fails.
2. Serialize the current in-memory workbook state; compute its SHA-256.
3. Insert `EvidenceFile { proctoringSessionId: session.proctoringSessionId, kind: snapshotType = FINAL ? EXCEL_FINAL_WORKBOOK : EXCEL_WORKBOOK_SNAPSHOT, storagePath, checksumSha256, capturedAt: now, uploadStatus: PENDING }`.

### Postconditions
- One `EvidenceFile` row capturing this point in the session.

### Rejection reasons
| Condition | Result |
|---|---|
| Session not ACTIVE/RECOVERED/SUBMITTED | reject |
| Exam's proctoring mode is NONE (no `proctoringSessionId`) | reject — **(gap: an unproctored exam with a SPREADSHEET question currently has no evidence trail for its workbook at all.)** |

---

## 20.7 `ExcelSessionService.submitSession(sessionId)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED}`.

### Steps
1. Load the session; validate the precondition. **Stop** if it fails.
2. Compute `finalChecksumSha256` from the current workbook state.
3. Call `takeSnapshot` (§20.6) with `snapshotType: FINAL`, when `proctoringSessionId` is present.
4. Replay every `ExcelCellEdit` for the session, from the most recent prior snapshot forward, and
   recompute the resulting workbook's hash.
   - If the replayed hash matches `finalChecksumSha256` → `integrityStatus = VALID`.
   - If it doesn't → `integrityStatus = TAMPERED`.
   - If the replay itself fails to complete → `integrityStatus = INCONCLUSIVE`.
5. Set `status = SUBMITTED`, `submittedAt = now`.

### Postconditions
- `ExcelSession.status = SUBMITTED`, `integrityStatus` always set to one of the three values —
  never left null once submitted.

---

## 20.8 `ExcelSessionService.markCrashed(sessionId)`

### Preconditions
- `ExcelSession.status ∈ {ACTIVE, RECOVERED}`.

### Steps
1. Set `status = CRASHED`.
2. Write `ProctoringEvent { eventType: EXCEL_ENGINE_ERROR, proctoringSessionId: session.proctoringSessionId, severity: HIGH }`.

### Postconditions
- `ExcelSession.status = CRASHED`.

---

## 20.9 `ExcelSessionService.recoverSession(sessionId)`

### Preconditions
- `ExcelSession.status = CRASHED`.

### Steps
1. Load the most recent `EvidenceFile` with `kind = EXCEL_WORKBOOK_SNAPSHOT` for this session.
2. Restore the workbook from that snapshot; replay every `ExcelCellEdit` recorded after the
   snapshot's `capturedAt`.
3. Re-launch the runtime engine against the replayed state.
4. Set `status = RECOVERED`.

### Postconditions
- `ExcelSession.status = RECOVERED` — further edits (§20.3), sheet operations (§20.4), macro runs
  (§20.5) and snapshots (§20.6) all accept `RECOVERED` the same as `ACTIVE`; the distinct status
  value exists purely so the audit trail shows this session survived a crash.

---

## 20.10 `ExcelGradingService.gradeAnswer(attemptAnswerId)`

Called from `AnswerService.gradeAutomatically` (§8.9) whenever `questionType = SPREADSHEET`.

### Preconditions
- `AttemptAnswer.gradingStatus ∈ {PENDING, IN_PROGRESS}`.
- The owning `ExcelSession.status = SUBMITTED`.

### Steps
1. Load the `AttemptAnswer`'s `Question` and the submitted workbook.
2. For each `ExcelCellBinding` of the question, ordered by `sequenceNo`:
   - Read the submitted workbook's value/formula at `cellRef`/`rangeRef`.
   - Dispatch by `answerKind`:
     - `VALUE` → compare against `expectedValue`, within `tolerance` for a numeric value.
     - `FORMULA` → compare the normalized formula string against `expectedFormula`.
     - `RANGE` → compare `rangeRef` cell-by-cell against the values encoded in `expectedValue`.
     - `CHART` / `PIVOT_TABLE` / `CONDITIONAL_FORMATTING` / `NAMED_RANGE` → run the structural
       comparison; if the comparison itself can't reach a verdict, fall back to `graderType =
       MANUAL` for this result rather than guessing.
     - `MACRO_OUTPUT` → re-run the bound macro against a hidden test workbook and compare its output.
     - `MANUAL` → no automated check; leave `pointsAwarded` null and `correct = false`, awaiting
       a reviewer.
   - Insert or update `ExcelGradeResult { attemptAnswerId, excelCellBindingId, graderType, correct, pointsAwarded, gradedValue, gradedFormula, graderNote, gradedAt: now }`.
3. Return the sum of `pointsAwarded` across all `ExcelGradeResult` rows for this answer (treating
   a null `pointsAwarded` as 0) back to the caller.

### Postconditions
- Exactly one `ExcelGradeResult` row per `ExcelCellBinding` of the question.

---

# The Rule Logic in One Sentence Each

| Function | Rule logic in one sentence |
|---|---|
| `login` | Every attempt writes a `LoginAttempt`; fail closed on status or lock; MFA defers session issuance. |
| `verifyMfaCode` | Verify against the enrolled method; on success issue the session exactly as login does. |
| `logout` | Revoke server-side; never delete the session row. |
| `refreshToken` | Hash, look up, check revoked/expired/status, rotate optionally. |
| `forgotPassword` | Always return the same response; store only the token hash. |
| `resetPassword` | Single-use, purpose-matched, row kept after redemption. |
| `verifyEmail` | The only path from `PENDING_VERIFICATION` to `ACTIVE`. |
| `enrollMfa` | Return the TOTP seed once; SMS/EMAIL reuse phone/email. |
| `deleteRole` | `system = true` blocks deletion; audit with reason. |
| `grantRole` | A grant is an entity; expired grants are inert. |
| `checkAccess` | Union permissions across non-expired `UserRole` grants. |
| `registerClient` | Return the plaintext secret exactly once. |
| `authenticate` | IP and rate checks run before any business logic. |
| `createGroup` | Reject cycles in the parent chain. |
| `addMember` | Idempotent; auto-enroll fans out. |
| `removeMember` | Set `leftAt`; never delete. |
| `createExam` | Validate window and attempts; start in `DRAFT`. |
| `publishExam` | Require sections and questions; freeze structure. |
| `updateExam` | Structural edits bump `version` rather than mutate. |
| `activate/close/archiveExam` | Validate the transition; audit. |
| `addRule` | `requiredExamId` and `courseReference` are mutually exclusive. |
| `checkEligibility` | All active rules pass (AND); inactive skipped. |
| `assignToStudent` | Prerequisite runs first; `(exam, candidate)` unique; window narrower than exam (or covered by an override). |
| `assignToGroup` | Fan out into per-candidate assignments; auto-enroll live. |
| `cancelAssignment` | Status change, not deletion. |
| `sendInvitation` | Separate from assignment; token hashed. |
| `resend` | New row, new token, incremented `sequenceNo`. |
| `recordOpen/Accept` | Token hash match; expiry gate. |
| `grantRetake` | Cannot exist without a `GRANT_RETAKE` decision; never mutates the exam's own caps. |
| `consume` (retake) | Set exactly once, atomically with the new attempt. |
| `revoke` (retake/window) | Append-only via `supersedes`, never edited in place. |
| `grantOverride` | Widens the assignment's window only, never the exam's. |
| `createQuestion` | Start in `DRAFT`. |
| `addOption` | Single-answer types enforce exactly one correct. |
| `publishQuestion` | Type-specific completeness validated. |
| `retireQuestion` | Never hard-delete. |
| `addTestCase` | `CODE` only; hidden test case required at publish. |
| `create/moveCategory` | No cycle; rewrite `path` for the whole subtree. |
| `tag/untagQuestion` | Real table; `usageCount` accurate. |
| `computeCalibration` | Only `FINAL` results feed it; recommends, never auto-retires. |
| `createNextVersion` | Copies the parent instead of editing it; new row starts `DRAFT` with `parentQuestionId` set. |
| `startCheck` | A run exists before any attempt. |
| `runCheckItem` | Each item keeps its own measurement; required items block. |
| `overrideFailure` | Recorded, not silent. |
| `recordConsent` | Required before an attempt starts under any proctored mode; notice version copied. |
| `withdrawConsent` | Sets `withdrawnAt`; never deletes. |
| `verifyFace` | `matchThreshold` copied at verification time. |
| `manualOverride` | New row; original failure preserved. |
| `startAttempt` | All gates run before any write; `expiresAt` computed once; token scoped. |
| `heartbeat` | Touch `LiveSessionStatus` only. |
| `navigateTo` | Respect `lockOnExit`. |
| `markForReview` | Toggle `QuestionState.state`, not `AttemptAnswer.flaggedByCandidate`. |
| `submit` | No writes after; compute score. |
| `autoSubmitOnTimeout` | `submittedAt = expiresAt`, not `now`. |
| `saveAnswer` | Append `AnswerRevision` every save; overwrite `AttemptAnswer` in place. |
| `gradeAutomatically` | `CODE` and `SPREADSHEET` resolve automatically; a SPREADSHEET answer with a MANUAL cell stays `PENDING`. |
| `checkTiming` | Only correct/high-scoring answers evaluated; writes a `RiskEvent`, not a `ProctoringEvent`. |
| `checkCollisions` | Signal, not verdict; scoped to one exam. |
| `resume` (attempt) | Token rotation, not a second session; the gap itself can be a risk factor. |
| `requestPause` | One `PENDING` per attempt. |
| `decide` | Only from `PENDING`. |
| `resume` (pause) | Only from `APPROVED`/`AUTO_APPROVED`; `resumedAt` distinct from `decidedAt`. |
| `startSession` | Snapshot `mode`; one session per attempt. |
| `ingestEvent` | `idempotencyKey` deduplicates. |
| `applyAutoAction` | Escalates on accumulated risk, not a single event. |
| `captureEvidence` | `retentionUntil` computed at capture time; custody chain written alongside. |
| `viewEvidence` | Write `EvidenceAccessLog` first, every read; custody chain updated too. |
| `purgeExpired` | Access log + custody record written; terminal transition. |
| `recordSighting` | Running counter; a threshold crossing is the only thing that writes a signal. |
| `confirmShared` | Only reached through a human `ReviewFinding`, never automatically. |
| `recordAction` (proctor) | Reason required; two-person approval gates termination where policy demands it. |
| `receiveInference` | Below `confidenceThreshold` advisory only; at `detectionThreshold` event raised; SHADOW never raises one. |
| `correlateWindow` | Rules are data; `verdict` starts `DETECTED`. |
| `evaluate` (shadow) | Neither version grades itself; promotion needs a minimum sample. |
| `computeScore` | New version; `isLatest` flipped atomically; threshold version copied. |
| `openCase` | Not unique per attempt; `caseNumber` generated. |
| `assignReviewer` | Set assignment; audit. |
| `addFinding` | Per item, not per case; feeds model performance. |
| `addNote` | `candidateVisible` distinguishes internal from appealable. |
| `recordDecision` | Append-only; `rationale` required; `supersedes` points back. |
| `closeCase` | Terminal status; finalise held result. |
| `computeRawScore` | One `ResultDetail` per question; `PROVISIONAL`. |
| `applyReviewAdjustment` | Traces back to the `ReviewDecision`. |
| `finalize` | Blocked while any `ResultWithholding` is unreleased; risk frozen. |
| `publish` | `publishedAt` set; notify. |
| `withhold` | Opened only from a resolved threshold or an open case. |
| `release` | `releaseReason` required; a later hold is a new row. |
| `generateReport` | Rendered, immutable, checksummed. |
| `regenerate` | New version; old preserved. |
| `renderTimeline` | New version per render; entries reference, never duplicate, source rows. |
| `send` | Idempotency; rendered content stored; suppression respected (except safety alerts). |
| `scheduleReminder` | One `Notification` per assignment. |
| `shouldSuppress` | Quiet hours in the recipient's own time zone; safety alerts exempt. |
| `record` (audit) | Append-only; `reason` required for overrides. |
| `registerVersion` | Thresholds on the version, not the model. |
| `promote` | `SHADOW` never drives `autoAction`. |
| `recordMetric` | Computed from `ReviewFinding`, never self-reported. |
| `set` | `editable = false` rejects; `secret = true` never returned. |
| `updateFactor` | Effective-dated; never edited in place. |
| `updateThresholdBand` | Effective-dated; never edited in place. |
| `addCard` | Stores a processor token, never a card number; starts `PENDING_VERIFICATION`; duplicate tokens rejected. |
| `setDefault` | At most one default card per customer. |
| `removeCard` | Status change to `REVOKED`, never a delete — past charges keep their reference. |
| `sweepExpiredCards` | Keeps `ACTIVE` honest against the card's own expiry date. |
| `verifyCard` | The only path off `PENDING_VERIFICATION` — resolves to `ACTIVE` or `REVOKED`. |
| `suspendCard` / `reinstateCard` | `SUSPENDED` only ever comes from, and returns to, `ACTIVE`. |
| `createCustomer` | One `PaymentCustomer` per `(userId, provider)`; the processor customer must already exist. |
| `chargeCard` | Inserts `INITIATED`, then resolves to the processor's actual response — never left pending. |
| `refundTransaction` | Refunded total never exceeds the original amount; parent flips `REFUNDED`/`PARTIALLY_REFUNDED`. |
| `recordChargeback` | Processor-driven, not user-driven; always leaves a `DISPUTED` or `CHARGEBACK` row. |
| `setRequirement` | One row per exam, updated in place — no versioning needed since charges freeze their own amount. |
| `createCharge` | Rejects if no active requirement, or a live charge already exists for the assignment. |
| `payCharge` | Delegates to `chargeCard`; resolves to `CAPTURED` or `FAILED`, never left `INITIATED`. |
| `refundCharge` | Delegates to `refundTransaction`; sets `REFUNDED`. |
| `createSession` | Rejects a duplicate session, or an exam with no SPREADSHEET question. |
| `startSession` | Loads the workbook, seeds volatile functions per session, sets `ACTIVE`. |
| `recordCellEdit` | Append-only, strictly increasing `sequenceNo`. |
| `recordSheetOperation` | Logged separately from cell edits; always raises a proctoring event. |
| `runMacro` | Always logged, whether allowed to actually execute or not. |
| `takeSnapshot` | Requires a proctoring session to attach evidence to. |
| `submitSession` | Replays edits to verify the final hash; sets `VALID`/`TAMPERED`/`INCONCLUSIVE`. |
| `markCrashed` / `recoverSession` | `RECOVERED` behaves like `ACTIVE` for further edits. |
| `gradeAnswer` | One `ExcelGradeResult` per binding; an inconclusive structural check falls back to `MANUAL`. |

---

## Corrections Made Against the Actual Model

Every `EntityName { field: value, ... }` literal above was checked mechanically against the real
entity source (extracted field names, not recalled from memory), and every mismatch was corrected
in place. The single systematic issue: this project's standing constraint is that every
relationship is a plain `Long` column named `<role><Type>Id` (`examId`, `createdByUserId`,
`assignedReviewerUserId`, and so on) — there is no JPA association to write `entity.role` through.
The first draft of this document used the shorter, association-style names an ORM with real
relationships would use instead (`user`, `session`, `attempt`, `createdBy`, `reviewer`...). Every
one of those was renamed to its real column — for example: `LoginAttempt.user` → `.userId`,
`AuditLog.actor` → `.actorUserId`, `ReviewCase.attempt`/`.riskAssessment`/`.openedBy` →
`.examAttemptId`/`.riskAssessmentId`/`.openedByUserId`, `IdentityVerification.candidate`/`.attempt`/
`.session`/`.verifiedBy`/`.capturedEvidence` → `.candidateUserId`/`.examAttemptId`/
`.proctoringSessionId`/`.verifiedByUserId`/`.capturedEvidenceId`, `ProctoringReport.attempt`/
`.session`/`.riskAssessment`/`.reviewCase`/`.requestedBy` → `.examAttemptId`/`.proctoringSessionId`/
`.riskAssessmentId`/`.reviewCaseId`/`.requestedByUserId`, and about twenty more of the same shape
across `UserRole`, `StudentGroup`, `GroupMembership`, `Exam`, `Question`, `QuestionOption`,
`SystemCheck`/`SystemCheckItem`, `AnswerRevision`, `EvidenceFile`/`EvidenceAccessLog`,
`ProctoringEvent`, `AiDetection`, `RiskEvent`, `ReviewDecision`/`ReviewFinding`/`ReviewNote`,
`ExamResult`/`ResultDetail`, `AiModelVersion`, `ModelPerformanceMetric`, `Notification`, and
`SuspiciousActivity`. Every literal in the document above now uses the verified column name; see
[`field-reference.md`](field-reference.md) for the authoritative list per entity.

A second, smaller class of fix: several boolean fields were referenced by their Lombok-generated
getter name (`isActive`, `isFullscreen`, `isReminder`, `isProhibited`, `isRequired`, `isFinal`,
`isLatest`, `wasGranted`) rather than the actual field name Lombok generates that getter *from*
(`active`, `fullscreen`, `reminder`, `prohibited`, `required`, `finalDecision`, `latest`,
`granted`). All eight are corrected above.

Two further, non-mechanical corrections:

- **`TokenPurpose` has no MFA constant.** The draft's step 5 of `login` originally proposed
  reusing `SecurityToken` for an MFA challenge. `TokenPurpose` is `EMAIL_VERIFICATION,
  PASSWORD_RESET, ACCOUNT_INVITATION, EMAIL_CHANGE` — none fit, and adding a fifth constant just
  for this would be wrong besides: an MFA challenge is seconds-lived and never needs the audit
  trail (`usedAt`, `redeemedIp`, a permanent row) `SecurityToken` exists to provide. The text
  above now says explicitly to hold the challenge in-memory/cache instead.
- **`ExamService.createExam`'s insert was missing two real columns** — `Exam.totalPoints` and
  `Exam.passingScore` — present on the entity but absent from the original literal. Both are now
  included so the literal matches every column the entity actually declares.
