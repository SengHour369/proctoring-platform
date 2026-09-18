# Entity relationships

The entity classes declare foreign keys as plain `Long` columns and carry no JPA associations, so
the relationships below are **not** discoverable from the Java types. This file and
[`db/schema-foreign-keys.sql`](../legacy-monolith/src/main/resources/db/schema-foreign-keys.sql) are the
reference; the SQL script is what enforces them.

Generated from the 188 foreign keys in that script.

## auth

### `users`

_No outgoing references._

Referenced by: `ai_model_versions.activated_by_user_id`, `api_clients.created_by_user_id`, `attempt_answers.graded_by_user_id`, `attempt_pause_requests.decided_by_user_id`, `attempt_pause_requests.requested_by_user_id`, `attempt_resumptions.approved_by_user_id`, `attempt_timelines.generated_by_user_id`, `audit_logs.actor_user_id`, `evidence_access_logs.actor_user_id`, `evidence_custody_records.actor_user_id`, `exam_assignments.assigned_by_user_id`, `exam_assignments.candidate_user_id`, `exam_attempts.candidate_user_id`, `exam_group_assignments.assigned_by_user_id`, `exam_payment_charges.candidate_user_id`, `exam_results.candidate_user_id`, `exam_results.graded_by_user_id`, `exam_window_overrides.granted_by_user_id`, `exams.created_by_user_id`, `group_memberships.added_by_user_id`, `group_memberships.user_id`, `identity_verifications.candidate_user_id`, `identity_verifications.verified_by_user_id`, `login_attempts.user_id`, `notification_suppressions.created_by_user_id`, `notification_suppressions.recipient_user_id`, `notifications.recipient_user_id`, `payment_cards.user_id`, `payment_customers.user_id`, `payment_transactions.initiated_by_user_id`, `proctor_actions.proctor_user_id`, `proctor_actions.supervisor_approval_user_id`, `proctor_shifts.proctor_user_id`, `proctoring_reports.requested_by_user_id`, `proctoring_sessions.assigned_proctor_user_id`, `proctoring_sessions.identity_verified_by_user_id`, `questions.created_by_user_id`, `result_withholdings.released_by_user_id`, `result_withholdings.withheld_by_user_id`, `retake_grants.candidate_user_id`, `retake_grants.granted_by_user_id`, `review_cases.assigned_reviewer_user_id`, `review_cases.opened_by_user_id`, `review_decisions.reviewer_user_id`, `review_findings.reviewer_user_id`, `review_notes.author_user_id`, `role_permission.granted_by_user_id`, `security_tokens.user_id`, `student_groups.owner_user_id`, `suspicious_activities.adjudicated_by_user_id`, `system_checks.candidate_user_id`, `system_checks.overridden_by_user_id`, `system_settings.updated_by_user_id`, `user_role.granted_by_user_id`, `user_role.user_id`, `user_sessions.user_id`

### `roles`

_No outgoing references._

Referenced by: `role_permission.role_id`, `user_role.role_id`

### `permissions`

_No outgoing references._

Referenced by: `role_permission.permission_id`

### `user_role`

| column | references |
|---|---|
| `granted_by_user_id` | `users` |
| `role_id` | `roles` |
| `user_id` | `users` |

### `role_permission`

| column | references |
|---|---|
| `granted_by_user_id` | `users` |
| `permission_id` | `permissions` |
| `role_id` | `roles` |

### `user_sessions`

| column | references |
|---|---|
| `user_id` | `users` |

### `security_tokens`

| column | references |
|---|---|
| `user_id` | `users` |

### `login_attempts`

| column | references |
|---|---|
| `user_id` | `users` |

### `api_clients`

| column | references |
|---|---|
| `created_by_user_id` | `users` |

Referenced by: `evidence_custody_records.actor_api_client_id`


## payment

### `payment_customers`

| column | references |
|---|---|
| `default_payment_card_id` | `payment_cards` |
| `user_id` | `users` |

Referenced by: `payment_cards.payment_customer_id`, `payment_transactions.payment_customer_id`

### `payment_cards`

| column | references |
|---|---|
| `payment_customer_id` | `payment_customers` |
| `user_id` | `users` |

Referenced by: `payment_customers.default_payment_card_id`, `payment_transactions.payment_card_id`

### `payment_transactions`

| column | references |
|---|---|
| `initiated_by_user_id` | `users` |
| `parent_transaction_id` | `payment_transactions` |
| `payment_card_id` | `payment_cards` |
| `payment_customer_id` | `payment_customers` |

Referenced by: `exam_payment_charges.payment_transaction_id`, `payment_transactions.parent_transaction_id`

### `exam_payment_requirements`

| column | references |
|---|---|
| `exam_id` | `exams` |

Referenced by: `exam_payment_charges.exam_payment_requirement_id`

### `exam_payment_charges`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `exam_assignment_id` | `exam_assignments` |
| `exam_attempt_id` | `exam_attempts` |
| `exam_id` | `exams` |
| `exam_payment_requirement_id` | `exam_payment_requirements` |
| `payment_transaction_id` | `payment_transactions` |


## usergroup

### `student_groups`

| column | references |
|---|---|
| `owner_user_id` | `users` |
| `parent_group_id` | `student_groups` |

Referenced by: `exam_group_assignments.student_group_id`, `group_memberships.student_group_id`, `student_groups.parent_group_id`

### `group_memberships`

| column | references |
|---|---|
| `added_by_user_id` | `users` |
| `student_group_id` | `student_groups` |
| `user_id` | `users` |


## exam

### `exams`

| column | references |
|---|---|
| `created_by_user_id` | `users` |

Referenced by: `device_trust_records.exam_id`, `exam_assignments.exam_id`, `exam_attempts.exam_id`, `exam_group_assignments.exam_id`, `exam_payment_charges.exam_id`, `exam_payment_requirements.exam_id`, `exam_prerequisites.exam_id`, `exam_prerequisites.required_exam_id`, `exam_results.exam_id`, `exam_sections.exam_id`, `question_form_fingerprints.exam_id`, `system_checks.exam_id`

### `exam_sections`

| column | references |
|---|---|
| `exam_id` | `exams` |

Referenced by: `exam_attempts.current_section_id`, `exam_questions.exam_section_id`, `result_details.exam_section_id`

### `exam_questions`

| column | references |
|---|---|
| `exam_section_id` | `exam_sections` |
| `question_id` | `questions` |

Referenced by: `answer_timing_anomalies.exam_question_id`, `attempt_answers.exam_question_id`, `question_states.exam_question_id`, `result_details.exam_question_id`

### `exam_assignments`

| column | references |
|---|---|
| `assigned_by_user_id` | `users` |
| `candidate_user_id` | `users` |
| `exam_id` | `exams` |

Referenced by: `exam_attempts.exam_assignment_id`, `exam_invitations.exam_assignment_id`, `exam_payment_charges.exam_assignment_id`, `exam_window_overrides.exam_assignment_id`, `retake_grants.exam_assignment_id`

### `exam_group_assignments`

| column | references |
|---|---|
| `assigned_by_user_id` | `users` |
| `exam_id` | `exams` |
| `student_group_id` | `student_groups` |

### `exam_invitations`

| column | references |
|---|---|
| `exam_assignment_id` | `exam_assignments` |

### `exam_prerequisites`

| column | references |
|---|---|
| `exam_id` | `exams` |
| `required_exam_id` | `exams` |

### `retake_grants`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `consumed_by_attempt_id` | `exam_attempts` |
| `exam_assignment_id` | `exam_assignments` |
| `granted_by_user_id` | `users` |
| `review_decision_id` | `review_decisions` |
| `supersedes_grant_id` | `retake_grants` |

Referenced by: `retake_grants.supersedes_grant_id`

### `exam_window_overrides`

| column | references |
|---|---|
| `exam_assignment_id` | `exam_assignments` |
| `granted_by_user_id` | `users` |
| `supersedes_override_id` | `exam_window_overrides` |

Referenced by: `exam_window_overrides.supersedes_override_id`


## question

### `questions`

| column | references |
|---|---|
| `created_by_user_id` | `users` |
| `parent_question_id` | `questions` |
| `question_category_id` | `question_categories` |

Referenced by: `code_test_cases.question_id`, `exam_questions.question_id`, `excel_cell_bindings.question_id`, `question_calibrations.question_id`, `question_options.question_id`, `question_tag_link.question_id`, `questions.parent_question_id`

### `question_options`

| column | references |
|---|---|
| `question_id` | `questions` |

Referenced by: `attempt_answer_options.question_option_id`

### `question_categories`

| column | references |
|---|---|
| `parent_category_id` | `question_categories` |

Referenced by: `question_categories.parent_category_id`, `questions.question_category_id`

### `question_tags`

_No outgoing references._

Referenced by: `question_tag_link.question_tag_id`

### `question_tag_link`

| column | references |
|---|---|
| `question_id` | `questions` |
| `question_tag_id` | `question_tags` |

### `code_test_cases`

| column | references |
|---|---|
| `question_id` | `questions` |

Referenced by: `code_execution_results.code_test_case_id`

### `excel_cell_bindings`

| column | references |
|---|---|
| `question_id` | `questions` |

Referenced by: `excel_grade_results.excel_cell_binding_id`

### `question_calibrations`

| column | references |
|---|---|
| `question_id` | `questions` |


## precheck

### `system_checks`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `exam_id` | `exams` |
| `overridden_by_user_id` | `users` |

Referenced by: `system_check_items.system_check_id`

### `system_check_items`

| column | references |
|---|---|
| `system_check_id` | `system_checks` |


## identity

### `identity_verifications`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `captured_evidence_id` | `evidence_files` |
| `exam_attempt_id` | `exam_attempts` |
| `proctoring_session_id` | `proctoring_sessions` |
| `verified_by_user_id` | `users` |


## consent

### `privacy_notices`

_No outgoing references._

Referenced by: `consent_records.privacy_notice_id`

### `consent_records`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `privacy_notice_id` | `privacy_notices` |


## attempt

### `exam_attempts`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `current_section_id` | `exam_sections` |
| `exam_assignment_id` | `exam_assignments` |
| `exam_id` | `exams` |

Referenced by: `answer_timing_anomalies.exam_attempt_id`, `attempt_answers.exam_attempt_id`, `attempt_pause_requests.exam_attempt_id`, `attempt_resumptions.exam_attempt_id`, `attempt_timelines.exam_attempt_id`, `consent_records.exam_attempt_id`, `exam_payment_charges.exam_attempt_id`, `exam_results.exam_attempt_id`, `excel_sessions.exam_attempt_id`, `identity_verifications.exam_attempt_id`, `proctoring_reports.exam_attempt_id`, `proctoring_sessions.exam_attempt_id`, `question_form_fingerprints.exam_attempt_id`, `question_states.exam_attempt_id`, `retake_grants.consumed_by_attempt_id`, `review_cases.exam_attempt_id`, `risk_assessments.exam_attempt_id`, `suspicious_activities.exam_attempt_id`, `system_checks.exam_attempt_id`

### `attempt_answers`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `exam_question_id` | `exam_questions` |
| `graded_by_user_id` | `users` |

Referenced by: `answer_revisions.attempt_answer_id`, `answer_timing_anomalies.attempt_answer_id`, `attempt_answer_options.attempt_answer_id`, `code_execution_results.attempt_answer_id`, `excel_grade_results.attempt_answer_id`, `result_details.attempt_answer_id`

### `attempt_answer_options`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |
| `question_option_id` | `question_options` |

### `answer_revisions`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |

### `question_states`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `exam_question_id` | `exam_questions` |

### `attempt_pause_requests`

| column | references |
|---|---|
| `decided_by_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `requested_by_user_id` | `users` |

### `code_execution_results`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |
| `code_test_case_id` | `code_test_cases` |

### `excel_grade_results`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |
| `excel_cell_binding_id` | `excel_cell_bindings` |

### `attempt_resumptions`

| column | references |
|---|---|
| `approved_by_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `risk_event_id` | `risk_events` |

### `question_form_fingerprints`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `exam_id` | `exams` |

### `answer_timing_anomalies`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |
| `exam_attempt_id` | `exam_attempts` |
| `exam_question_id` | `exam_questions` |


## excel

### `excel_sessions`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `excel_cell_edits.excel_session_id`, `excel_macro_executions.excel_session_id`, `excel_sheet_operations.excel_session_id`

### `excel_cell_edits`

| column | references |
|---|---|
| `excel_session_id` | `excel_sessions` |

### `excel_sheet_operations`

| column | references |
|---|---|
| `excel_session_id` | `excel_sessions` |

### `excel_macro_executions`

| column | references |
|---|---|
| `excel_session_id` | `excel_sessions` |


## proctoring

### `proctoring_sessions`

| column | references |
|---|---|
| `assigned_proctor_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `identity_verified_by_user_id` | `users` |

Referenced by: `ai_detections.proctoring_session_id`, `device_sessions.proctoring_session_id`, `evidence_files.proctoring_session_id`, `excel_sessions.proctoring_session_id`, `identity_verifications.proctoring_session_id`, `live_session_status.proctoring_session_id`, `proctor_actions.proctoring_session_id`, `proctor_shifts.proctoring_session_id`, `proctoring_events.proctoring_session_id`, `proctoring_reports.proctoring_session_id`, `risk_assessments.proctoring_session_id`, `suspicious_activities.proctoring_session_id`

### `proctoring_events`

| column | references |
|---|---|
| `device_session_id` | `device_sessions` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `ai_detections.proctoring_event_id`, `evidence_files.proctoring_event_id`, `proctor_actions.proctoring_event_id`, `risk_events.proctoring_event_id`

### `device_sessions`

| column | references |
|---|---|
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `proctoring_events.device_session_id`

### `evidence_files`

| column | references |
|---|---|
| `proctoring_event_id` | `proctoring_events` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `ai_detections.evidence_file_id`, `evidence_access_logs.evidence_file_id`, `evidence_custody_records.evidence_file_id`, `identity_verifications.captured_evidence_id`

### `evidence_access_logs`

| column | references |
|---|---|
| `actor_user_id` | `users` |
| `evidence_file_id` | `evidence_files` |

### `live_session_status`

| column | references |
|---|---|
| `proctoring_session_id` | `proctoring_sessions` |

### `device_trust_records`

| column | references |
|---|---|
| `exam_id` | `exams` |

### `evidence_custody_records`

| column | references |
|---|---|
| `actor_api_client_id` | `api_clients` |
| `actor_user_id` | `users` |
| `evidence_file_id` | `evidence_files` |

### `proctor_shifts`

| column | references |
|---|---|
| `proctor_user_id` | `users` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `proctor_actions.proctor_shift_id`

### `proctor_actions`

| column | references |
|---|---|
| `proctor_shift_id` | `proctor_shifts` |
| `proctor_user_id` | `users` |
| `proctoring_event_id` | `proctoring_events` |
| `proctoring_session_id` | `proctoring_sessions` |
| `supervisor_approval_user_id` | `users` |


## detection

### `ai_detections`

| column | references |
|---|---|
| `ai_model_version_id` | `ai_model_versions` |
| `evidence_file_id` | `evidence_files` |
| `proctoring_event_id` | `proctoring_events` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `audio_detections.ai_detection_id`, `behavior_detections.ai_detection_id`, `face_detections.ai_detection_id`, `object_detections.ai_detection_id`, `risk_events.ai_detection_id`, `suspicious_activities.primary_detection_id`

### `face_detections`

| column | references |
|---|---|
| `ai_detection_id` | `ai_detections` |

### `object_detections`

| column | references |
|---|---|
| `ai_detection_id` | `ai_detections` |

### `behavior_detections`

| column | references |
|---|---|
| `ai_detection_id` | `ai_detections` |

### `audio_detections`

| column | references |
|---|---|
| `ai_detection_id` | `ai_detections` |

### `suspicious_activities`

| column | references |
|---|---|
| `adjudicated_by_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `primary_detection_id` | `ai_detections` |
| `proctoring_session_id` | `proctoring_sessions` |


## risk

### `risk_assessments`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `proctoring_session_id` | `proctoring_sessions` |

Referenced by: `proctoring_reports.risk_assessment_id`, `result_withholdings.risk_assessment_id`, `review_cases.risk_assessment_id`, `risk_events.risk_assessment_id`

### `risk_events`

| column | references |
|---|---|
| `ai_detection_id` | `ai_detections` |
| `proctoring_event_id` | `proctoring_events` |
| `risk_assessment_id` | `risk_assessments` |

Referenced by: `attempt_resumptions.risk_event_id`


## review

### `review_cases`

| column | references |
|---|---|
| `assigned_reviewer_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `opened_by_user_id` | `users` |
| `risk_assessment_id` | `risk_assessments` |

Referenced by: `exam_results.review_case_id`, `proctoring_reports.review_case_id`, `result_withholdings.review_case_id`, `review_decisions.review_case_id`, `review_findings.review_case_id`, `review_notes.review_case_id`

### `review_decisions`

| column | references |
|---|---|
| `review_case_id` | `review_cases` |
| `reviewer_user_id` | `users` |
| `supersedes_decision_id` | `review_decisions` |

Referenced by: `retake_grants.review_decision_id`, `review_decisions.supersedes_decision_id`

### `review_findings`

| column | references |
|---|---|
| `review_case_id` | `review_cases` |
| `reviewer_user_id` | `users` |

### `review_notes`

| column | references |
|---|---|
| `author_user_id` | `users` |
| `review_case_id` | `review_cases` |


## result

### `exam_results`

| column | references |
|---|---|
| `candidate_user_id` | `users` |
| `exam_attempt_id` | `exam_attempts` |
| `exam_id` | `exams` |
| `graded_by_user_id` | `users` |
| `review_case_id` | `review_cases` |

Referenced by: `result_details.exam_result_id`, `result_withholdings.exam_result_id`

### `result_details`

| column | references |
|---|---|
| `attempt_answer_id` | `attempt_answers` |
| `exam_question_id` | `exam_questions` |
| `exam_result_id` | `exam_results` |
| `exam_section_id` | `exam_sections` |

### `result_withholdings`

| column | references |
|---|---|
| `exam_result_id` | `exam_results` |
| `released_by_user_id` | `users` |
| `review_case_id` | `review_cases` |
| `risk_assessment_id` | `risk_assessments` |
| `withheld_by_user_id` | `users` |


## report

### `proctoring_reports`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `proctoring_session_id` | `proctoring_sessions` |
| `requested_by_user_id` | `users` |
| `review_case_id` | `review_cases` |
| `risk_assessment_id` | `risk_assessments` |

### `attempt_timelines`

| column | references |
|---|---|
| `exam_attempt_id` | `exam_attempts` |
| `generated_by_user_id` | `users` |


## notification

### `notifications`

| column | references |
|---|---|
| `recipient_user_id` | `users` |
| `template_id` | `notification_templates` |

### `notification_templates`

_No outgoing references._

Referenced by: `notifications.template_id`

### `notification_suppressions`

| column | references |
|---|---|
| `created_by_user_id` | `users` |
| `recipient_user_id` | `users` |


## audit

### `audit_logs`

| column | references |
|---|---|
| `actor_user_id` | `users` |


## aimodel

### `ai_models`

_No outgoing references._

Referenced by: `ai_model_versions.ai_model_id`

### `ai_model_versions`

| column | references |
|---|---|
| `activated_by_user_id` | `users` |
| `ai_model_id` | `ai_models` |

Referenced by: `ai_detections.ai_model_version_id`, `model_performance_metrics.ai_model_version_id`, `shadow_evaluations.active_version_id`, `shadow_evaluations.shadow_version_id`

### `model_performance_metrics`

| column | references |
|---|---|
| `ai_model_version_id` | `ai_model_versions` |

### `shadow_evaluations`

| column | references |
|---|---|
| `active_version_id` | `ai_model_versions` |
| `shadow_version_id` | `ai_model_versions` |


## config

### `system_settings`

| column | references |
|---|---|
| `updated_by_user_id` | `users` |

### `risk_factor_configs`

_No outgoing references._

### `risk_level_thresholds`

_No outgoing references._

