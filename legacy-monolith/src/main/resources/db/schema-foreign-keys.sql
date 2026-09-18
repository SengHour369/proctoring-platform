-- Referential integrity for the AI Online Exam Proctoring schema.
--
-- The entity model uses plain foreign-key columns (Long) and declares no JPA associations, so
-- Hibernate generates the tables but NOT the constraints below. Apply this script after the
-- tables exist; it is idempotent and safe to re-run.
--
--   psql -h localhost -p 5435 -U <user> -d <db> -f src/main/resources/db/schema-foreign-keys.sql
--
-- No ON DELETE clauses: deletes default to RESTRICT. With JPA cascades gone, deleting a parent is
-- now an explicit, ordered operation in service code — which is the safer default for a system
-- whose child rows (evidence, events, audit) are subject to retention rules rather than to the
-- lifecycle of their parent.
--
-- Every FK column also gets an index. PostgreSQL indexes the referenced side automatically but
-- never the referencing side, and every join in this model is an explicit id join.

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------- foreign keys (166)

alter table ai_detections drop constraint if exists fk_ai_detections_event;
alter table ai_detections add constraint fk_ai_detections_event
    foreign key (proctoring_event_id) references proctoring_events (id);
alter table ai_detections drop constraint if exists fk_ai_detections_evidence;
alter table ai_detections add constraint fk_ai_detections_evidence
    foreign key (evidence_file_id) references evidence_files (id);
alter table ai_detections drop constraint if exists fk_ai_detections_model_version;
alter table ai_detections add constraint fk_ai_detections_model_version
    foreign key (ai_model_version_id) references ai_model_versions (id);
alter table ai_detections drop constraint if exists fk_ai_detections_session;
alter table ai_detections add constraint fk_ai_detections_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table ai_model_versions drop constraint if exists fk_ai_model_versions_activated_by;
alter table ai_model_versions add constraint fk_ai_model_versions_activated_by
    foreign key (activated_by_user_id) references users (id);
alter table ai_model_versions drop constraint if exists fk_ai_model_versions_model;
alter table ai_model_versions add constraint fk_ai_model_versions_model
    foreign key (ai_model_id) references ai_models (id);

alter table answer_revisions drop constraint if exists fk_answer_revisions_answer;
alter table answer_revisions add constraint fk_answer_revisions_answer
    foreign key (attempt_answer_id) references attempt_answers (id);

alter table answer_timing_anomalies drop constraint if exists fk_answer_timing_anomalies_answer;
alter table answer_timing_anomalies add constraint fk_answer_timing_anomalies_answer
    foreign key (attempt_answer_id) references attempt_answers (id);
alter table answer_timing_anomalies drop constraint if exists fk_answer_timing_anomalies_attempt;
alter table answer_timing_anomalies add constraint fk_answer_timing_anomalies_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table answer_timing_anomalies drop constraint if exists fk_answer_timing_anomalies_question;
alter table answer_timing_anomalies add constraint fk_answer_timing_anomalies_question
    foreign key (exam_question_id) references exam_questions (id);

alter table api_clients drop constraint if exists fk_api_clients_created_by;
alter table api_clients add constraint fk_api_clients_created_by
    foreign key (created_by_user_id) references users (id);

alter table attempt_answer_options drop constraint if exists fk_answer_options_answer;
alter table attempt_answer_options add constraint fk_answer_options_answer
    foreign key (attempt_answer_id) references attempt_answers (id);
alter table attempt_answer_options drop constraint if exists fk_answer_options_option;
alter table attempt_answer_options add constraint fk_answer_options_option
    foreign key (question_option_id) references question_options (id);

alter table attempt_answers drop constraint if exists fk_attempt_answers_attempt;
alter table attempt_answers add constraint fk_attempt_answers_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table attempt_answers drop constraint if exists fk_attempt_answers_exam_question;
alter table attempt_answers add constraint fk_attempt_answers_exam_question
    foreign key (exam_question_id) references exam_questions (id);
alter table attempt_answers drop constraint if exists fk_attempt_answers_graded_by;
alter table attempt_answers add constraint fk_attempt_answers_graded_by
    foreign key (graded_by_user_id) references users (id);

alter table attempt_pause_requests drop constraint if exists fk_attempt_pause_requests_attempt;
alter table attempt_pause_requests add constraint fk_attempt_pause_requests_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table attempt_pause_requests drop constraint if exists fk_attempt_pause_requests_decided_by;
alter table attempt_pause_requests add constraint fk_attempt_pause_requests_decided_by
    foreign key (decided_by_user_id) references users (id);
alter table attempt_pause_requests drop constraint if exists fk_attempt_pause_requests_requested_by;
alter table attempt_pause_requests add constraint fk_attempt_pause_requests_requested_by
    foreign key (requested_by_user_id) references users (id);

alter table attempt_resumptions drop constraint if exists fk_attempt_resumptions_approved_by;
alter table attempt_resumptions add constraint fk_attempt_resumptions_approved_by
    foreign key (approved_by_user_id) references users (id);
alter table attempt_resumptions drop constraint if exists fk_attempt_resumptions_attempt;
alter table attempt_resumptions add constraint fk_attempt_resumptions_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table attempt_resumptions drop constraint if exists fk_attempt_resumptions_risk_event;
alter table attempt_resumptions add constraint fk_attempt_resumptions_risk_event
    foreign key (risk_event_id) references risk_events (id);

alter table attempt_timelines drop constraint if exists fk_attempt_timelines_attempt;
alter table attempt_timelines add constraint fk_attempt_timelines_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table attempt_timelines drop constraint if exists fk_attempt_timelines_generated_by;
alter table attempt_timelines add constraint fk_attempt_timelines_generated_by
    foreign key (generated_by_user_id) references users (id);

alter table audio_detections drop constraint if exists fk_audio_detections_detection;
alter table audio_detections add constraint fk_audio_detections_detection
    foreign key (ai_detection_id) references ai_detections (id);

alter table audit_logs drop constraint if exists fk_audit_logs_actor;
alter table audit_logs add constraint fk_audit_logs_actor
    foreign key (actor_user_id) references users (id);

alter table behavior_detections drop constraint if exists fk_behavior_detections_detection;
alter table behavior_detections add constraint fk_behavior_detections_detection
    foreign key (ai_detection_id) references ai_detections (id);

alter table code_execution_results drop constraint if exists fk_code_exec_results_answer;
alter table code_execution_results add constraint fk_code_exec_results_answer
    foreign key (attempt_answer_id) references attempt_answers (id);
alter table code_execution_results drop constraint if exists fk_code_exec_results_test_case;
alter table code_execution_results add constraint fk_code_exec_results_test_case
    foreign key (code_test_case_id) references code_test_cases (id);

alter table code_test_cases drop constraint if exists fk_code_test_cases_question;
alter table code_test_cases add constraint fk_code_test_cases_question
    foreign key (question_id) references questions (id);

alter table consent_records drop constraint if exists fk_consent_records_attempt;
alter table consent_records add constraint fk_consent_records_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table consent_records drop constraint if exists fk_consent_records_notice;
alter table consent_records add constraint fk_consent_records_notice
    foreign key (privacy_notice_id) references privacy_notices (id);

alter table device_sessions drop constraint if exists fk_device_sessions_session;
alter table device_sessions add constraint fk_device_sessions_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table device_trust_records drop constraint if exists fk_device_trust_records_exam;
alter table device_trust_records add constraint fk_device_trust_records_exam
    foreign key (exam_id) references exams (id);

alter table evidence_access_logs drop constraint if exists fk_evidence_access_actor;
alter table evidence_access_logs add constraint fk_evidence_access_actor
    foreign key (actor_user_id) references users (id);
alter table evidence_access_logs drop constraint if exists fk_evidence_access_file;
alter table evidence_access_logs add constraint fk_evidence_access_file
    foreign key (evidence_file_id) references evidence_files (id);

alter table evidence_custody_records drop constraint if exists fk_evidence_custody_records_actor;
alter table evidence_custody_records add constraint fk_evidence_custody_records_actor
    foreign key (actor_user_id) references users (id);
alter table evidence_custody_records drop constraint if exists fk_evidence_custody_records_client;
alter table evidence_custody_records add constraint fk_evidence_custody_records_client
    foreign key (actor_api_client_id) references api_clients (id);
alter table evidence_custody_records drop constraint if exists fk_evidence_custody_records_file;
alter table evidence_custody_records add constraint fk_evidence_custody_records_file
    foreign key (evidence_file_id) references evidence_files (id);

alter table evidence_files drop constraint if exists fk_evidence_files_event;
alter table evidence_files add constraint fk_evidence_files_event
    foreign key (proctoring_event_id) references proctoring_events (id);
alter table evidence_files drop constraint if exists fk_evidence_files_session;
alter table evidence_files add constraint fk_evidence_files_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table exam_assignments drop constraint if exists fk_exam_assignments_assigned_by;
alter table exam_assignments add constraint fk_exam_assignments_assigned_by
    foreign key (assigned_by_user_id) references users (id);
alter table exam_assignments drop constraint if exists fk_exam_assignments_candidate;
alter table exam_assignments add constraint fk_exam_assignments_candidate
    foreign key (candidate_user_id) references users (id);
alter table exam_assignments drop constraint if exists fk_exam_assignments_exam;
alter table exam_assignments add constraint fk_exam_assignments_exam
    foreign key (exam_id) references exams (id);

alter table exam_attempts drop constraint if exists fk_exam_attempts_assignment;
alter table exam_attempts add constraint fk_exam_attempts_assignment
    foreign key (exam_assignment_id) references exam_assignments (id);
alter table exam_attempts drop constraint if exists fk_exam_attempts_candidate;
alter table exam_attempts add constraint fk_exam_attempts_candidate
    foreign key (candidate_user_id) references users (id);
alter table exam_attempts drop constraint if exists fk_exam_attempts_current_section;
alter table exam_attempts add constraint fk_exam_attempts_current_section
    foreign key (current_section_id) references exam_sections (id);
alter table exam_attempts drop constraint if exists fk_exam_attempts_exam;
alter table exam_attempts add constraint fk_exam_attempts_exam
    foreign key (exam_id) references exams (id);

alter table exam_group_assignments drop constraint if exists fk_exam_group_assignments_assigned_by;
alter table exam_group_assignments add constraint fk_exam_group_assignments_assigned_by
    foreign key (assigned_by_user_id) references users (id);
alter table exam_group_assignments drop constraint if exists fk_exam_group_assignments_exam;
alter table exam_group_assignments add constraint fk_exam_group_assignments_exam
    foreign key (exam_id) references exams (id);
alter table exam_group_assignments drop constraint if exists fk_exam_group_assignments_group;
alter table exam_group_assignments add constraint fk_exam_group_assignments_group
    foreign key (student_group_id) references student_groups (id);

alter table exam_invitations drop constraint if exists fk_exam_invitations_assignment;
alter table exam_invitations add constraint fk_exam_invitations_assignment
    foreign key (exam_assignment_id) references exam_assignments (id);

alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_exam;
alter table exam_payment_charges add constraint fk_exam_payment_charges_exam
    foreign key (exam_id) references exams (id);
alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_attempt;
alter table exam_payment_charges add constraint fk_exam_payment_charges_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_assignment;
alter table exam_payment_charges add constraint fk_exam_payment_charges_assignment
    foreign key (exam_assignment_id) references exam_assignments (id);
alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_requirement;
alter table exam_payment_charges add constraint fk_exam_payment_charges_requirement
    foreign key (exam_payment_requirement_id) references exam_payment_requirements (id);
alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_transaction;
alter table exam_payment_charges add constraint fk_exam_payment_charges_transaction
    foreign key (payment_transaction_id) references payment_transactions (id);
alter table exam_payment_charges drop constraint if exists fk_exam_payment_charges_candidate;
alter table exam_payment_charges add constraint fk_exam_payment_charges_candidate
    foreign key (candidate_user_id) references users (id);

alter table exam_payment_requirements drop constraint if exists fk_exam_payment_requirements_exam;
alter table exam_payment_requirements add constraint fk_exam_payment_requirements_exam
    foreign key (exam_id) references exams (id);

alter table exam_prerequisites drop constraint if exists fk_exam_prerequisites_exam;
alter table exam_prerequisites add constraint fk_exam_prerequisites_exam
    foreign key (exam_id) references exams (id);
alter table exam_prerequisites drop constraint if exists fk_exam_prerequisites_required_exam;
alter table exam_prerequisites add constraint fk_exam_prerequisites_required_exam
    foreign key (required_exam_id) references exams (id);

alter table exam_questions drop constraint if exists fk_exam_questions_question;
alter table exam_questions add constraint fk_exam_questions_question
    foreign key (question_id) references questions (id);
alter table exam_questions drop constraint if exists fk_exam_questions_section;
alter table exam_questions add constraint fk_exam_questions_section
    foreign key (exam_section_id) references exam_sections (id);

alter table exam_results drop constraint if exists fk_exam_results_attempt;
alter table exam_results add constraint fk_exam_results_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table exam_results drop constraint if exists fk_exam_results_candidate;
alter table exam_results add constraint fk_exam_results_candidate
    foreign key (candidate_user_id) references users (id);
alter table exam_results drop constraint if exists fk_exam_results_exam;
alter table exam_results add constraint fk_exam_results_exam
    foreign key (exam_id) references exams (id);
alter table exam_results drop constraint if exists fk_exam_results_graded_by;
alter table exam_results add constraint fk_exam_results_graded_by
    foreign key (graded_by_user_id) references users (id);
alter table exam_results drop constraint if exists fk_exam_results_review_case;
alter table exam_results add constraint fk_exam_results_review_case
    foreign key (review_case_id) references review_cases (id);

alter table exam_sections drop constraint if exists fk_exam_sections_exam;
alter table exam_sections add constraint fk_exam_sections_exam
    foreign key (exam_id) references exams (id);

alter table exam_window_overrides drop constraint if exists fk_exam_window_overrides_assignment;
alter table exam_window_overrides add constraint fk_exam_window_overrides_assignment
    foreign key (exam_assignment_id) references exam_assignments (id);
alter table exam_window_overrides drop constraint if exists fk_exam_window_overrides_granted_by;
alter table exam_window_overrides add constraint fk_exam_window_overrides_granted_by
    foreign key (granted_by_user_id) references users (id);
alter table exam_window_overrides drop constraint if exists fk_exam_window_overrides_supersedes;
alter table exam_window_overrides add constraint fk_exam_window_overrides_supersedes
    foreign key (supersedes_override_id) references exam_window_overrides (id);

alter table exams drop constraint if exists fk_exams_created_by;
alter table exams add constraint fk_exams_created_by
    foreign key (created_by_user_id) references users (id);

alter table excel_cell_bindings drop constraint if exists fk_excel_cell_bindings_question;
alter table excel_cell_bindings add constraint fk_excel_cell_bindings_question
    foreign key (question_id) references questions (id);

alter table excel_cell_edits drop constraint if exists fk_excel_cell_edits_session;
alter table excel_cell_edits add constraint fk_excel_cell_edits_session
    foreign key (excel_session_id) references excel_sessions (id);

alter table excel_grade_results drop constraint if exists fk_excel_grade_results_answer;
alter table excel_grade_results add constraint fk_excel_grade_results_answer
    foreign key (attempt_answer_id) references attempt_answers (id);
alter table excel_grade_results drop constraint if exists fk_excel_grade_results_binding;
alter table excel_grade_results add constraint fk_excel_grade_results_binding
    foreign key (excel_cell_binding_id) references excel_cell_bindings (id);

alter table excel_macro_executions drop constraint if exists fk_excel_macro_executions_session;
alter table excel_macro_executions add constraint fk_excel_macro_executions_session
    foreign key (excel_session_id) references excel_sessions (id);

alter table excel_sessions drop constraint if exists fk_excel_sessions_attempt;
alter table excel_sessions add constraint fk_excel_sessions_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table excel_sessions drop constraint if exists fk_excel_sessions_proctoring_session;
alter table excel_sessions add constraint fk_excel_sessions_proctoring_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table excel_sheet_operations drop constraint if exists fk_excel_sheet_operations_session;
alter table excel_sheet_operations add constraint fk_excel_sheet_operations_session
    foreign key (excel_session_id) references excel_sessions (id);

alter table face_detections drop constraint if exists fk_face_detections_detection;
alter table face_detections add constraint fk_face_detections_detection
    foreign key (ai_detection_id) references ai_detections (id);

alter table group_memberships drop constraint if exists fk_group_memberships_added_by;
alter table group_memberships add constraint fk_group_memberships_added_by
    foreign key (added_by_user_id) references users (id);
alter table group_memberships drop constraint if exists fk_group_memberships_group;
alter table group_memberships add constraint fk_group_memberships_group
    foreign key (student_group_id) references student_groups (id);
alter table group_memberships drop constraint if exists fk_group_memberships_user;
alter table group_memberships add constraint fk_group_memberships_user
    foreign key (user_id) references users (id);

alter table identity_verifications drop constraint if exists fk_identity_verifications_attempt;
alter table identity_verifications add constraint fk_identity_verifications_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table identity_verifications drop constraint if exists fk_identity_verifications_candidate;
alter table identity_verifications add constraint fk_identity_verifications_candidate
    foreign key (candidate_user_id) references users (id);
alter table identity_verifications drop constraint if exists fk_identity_verifications_evidence;
alter table identity_verifications add constraint fk_identity_verifications_evidence
    foreign key (captured_evidence_id) references evidence_files (id);
alter table identity_verifications drop constraint if exists fk_identity_verifications_session;
alter table identity_verifications add constraint fk_identity_verifications_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);
alter table identity_verifications drop constraint if exists fk_identity_verifications_verified_by;
alter table identity_verifications add constraint fk_identity_verifications_verified_by
    foreign key (verified_by_user_id) references users (id);

alter table live_session_status drop constraint if exists fk_live_session_status_session;
alter table live_session_status add constraint fk_live_session_status_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table login_attempts drop constraint if exists fk_login_attempts_user;
alter table login_attempts add constraint fk_login_attempts_user
    foreign key (user_id) references users (id);

alter table model_performance_metrics drop constraint if exists fk_model_performance_version;
alter table model_performance_metrics add constraint fk_model_performance_version
    foreign key (ai_model_version_id) references ai_model_versions (id);

alter table notification_suppressions drop constraint if exists fk_notification_suppressions_created_by;
alter table notification_suppressions add constraint fk_notification_suppressions_created_by
    foreign key (created_by_user_id) references users (id);
alter table notification_suppressions drop constraint if exists fk_notification_suppressions_recipient;
alter table notification_suppressions add constraint fk_notification_suppressions_recipient
    foreign key (recipient_user_id) references users (id);

alter table notifications drop constraint if exists fk_notifications_recipient;
alter table notifications add constraint fk_notifications_recipient
    foreign key (recipient_user_id) references users (id);
alter table notifications drop constraint if exists fk_notifications_template;
alter table notifications add constraint fk_notifications_template
    foreign key (template_id) references notification_templates (id);

alter table object_detections drop constraint if exists fk_object_detections_detection;
alter table object_detections add constraint fk_object_detections_detection
    foreign key (ai_detection_id) references ai_detections (id);

alter table payment_cards drop constraint if exists fk_payment_cards_customer;
alter table payment_cards add constraint fk_payment_cards_customer
    foreign key (payment_customer_id) references payment_customers (id);
alter table payment_cards drop constraint if exists fk_payment_cards_user;
alter table payment_cards add constraint fk_payment_cards_user
    foreign key (user_id) references users (id);

alter table payment_customers drop constraint if exists fk_payment_customers_user;
alter table payment_customers add constraint fk_payment_customers_user
    foreign key (user_id) references users (id);
alter table payment_customers drop constraint if exists fk_payment_customers_default_card;
alter table payment_customers add constraint fk_payment_customers_default_card
    foreign key (default_payment_card_id) references payment_cards (id);

alter table payment_transactions drop constraint if exists fk_payment_transactions_customer;
alter table payment_transactions add constraint fk_payment_transactions_customer
    foreign key (payment_customer_id) references payment_customers (id);
alter table payment_transactions drop constraint if exists fk_payment_transactions_card;
alter table payment_transactions add constraint fk_payment_transactions_card
    foreign key (payment_card_id) references payment_cards (id);
alter table payment_transactions drop constraint if exists fk_payment_transactions_parent;
alter table payment_transactions add constraint fk_payment_transactions_parent
    foreign key (parent_transaction_id) references payment_transactions (id);
alter table payment_transactions drop constraint if exists fk_payment_transactions_initiated_by;
alter table payment_transactions add constraint fk_payment_transactions_initiated_by
    foreign key (initiated_by_user_id) references users (id);

alter table proctor_actions drop constraint if exists fk_proctor_actions_event;
alter table proctor_actions add constraint fk_proctor_actions_event
    foreign key (proctoring_event_id) references proctoring_events (id);
alter table proctor_actions drop constraint if exists fk_proctor_actions_proctor;
alter table proctor_actions add constraint fk_proctor_actions_proctor
    foreign key (proctor_user_id) references users (id);
alter table proctor_actions drop constraint if exists fk_proctor_actions_session;
alter table proctor_actions add constraint fk_proctor_actions_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);
alter table proctor_actions drop constraint if exists fk_proctor_actions_shift;
alter table proctor_actions add constraint fk_proctor_actions_shift
    foreign key (proctor_shift_id) references proctor_shifts (id);
alter table proctor_actions drop constraint if exists fk_proctor_actions_supervisor;
alter table proctor_actions add constraint fk_proctor_actions_supervisor
    foreign key (supervisor_approval_user_id) references users (id);

alter table proctor_shifts drop constraint if exists fk_proctor_shifts_proctor;
alter table proctor_shifts add constraint fk_proctor_shifts_proctor
    foreign key (proctor_user_id) references users (id);
alter table proctor_shifts drop constraint if exists fk_proctor_shifts_session;
alter table proctor_shifts add constraint fk_proctor_shifts_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table proctoring_events drop constraint if exists fk_proctoring_events_device;
alter table proctoring_events add constraint fk_proctoring_events_device
    foreign key (device_session_id) references device_sessions (id);
alter table proctoring_events drop constraint if exists fk_proctoring_events_session;
alter table proctoring_events add constraint fk_proctoring_events_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table proctoring_reports drop constraint if exists fk_proctoring_reports_attempt;
alter table proctoring_reports add constraint fk_proctoring_reports_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table proctoring_reports drop constraint if exists fk_proctoring_reports_case;
alter table proctoring_reports add constraint fk_proctoring_reports_case
    foreign key (review_case_id) references review_cases (id);
alter table proctoring_reports drop constraint if exists fk_proctoring_reports_requested_by;
alter table proctoring_reports add constraint fk_proctoring_reports_requested_by
    foreign key (requested_by_user_id) references users (id);
alter table proctoring_reports drop constraint if exists fk_proctoring_reports_risk;
alter table proctoring_reports add constraint fk_proctoring_reports_risk
    foreign key (risk_assessment_id) references risk_assessments (id);
alter table proctoring_reports drop constraint if exists fk_proctoring_reports_session;
alter table proctoring_reports add constraint fk_proctoring_reports_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table proctoring_sessions drop constraint if exists fk_proctoring_sessions_attempt;
alter table proctoring_sessions add constraint fk_proctoring_sessions_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table proctoring_sessions drop constraint if exists fk_proctoring_sessions_identity_by;
alter table proctoring_sessions add constraint fk_proctoring_sessions_identity_by
    foreign key (identity_verified_by_user_id) references users (id);
alter table proctoring_sessions drop constraint if exists fk_proctoring_sessions_proctor;
alter table proctoring_sessions add constraint fk_proctoring_sessions_proctor
    foreign key (assigned_proctor_user_id) references users (id);

alter table question_calibrations drop constraint if exists fk_question_calibrations_question;
alter table question_calibrations add constraint fk_question_calibrations_question
    foreign key (question_id) references questions (id);

alter table question_categories drop constraint if exists fk_question_categories_parent;
alter table question_categories add constraint fk_question_categories_parent
    foreign key (parent_category_id) references question_categories (id);

alter table question_form_fingerprints drop constraint if exists fk_question_form_fingerprints_attempt;
alter table question_form_fingerprints add constraint fk_question_form_fingerprints_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table question_form_fingerprints drop constraint if exists fk_question_form_fingerprints_exam;
alter table question_form_fingerprints add constraint fk_question_form_fingerprints_exam
    foreign key (exam_id) references exams (id);

alter table question_options drop constraint if exists fk_question_options_question;
alter table question_options add constraint fk_question_options_question
    foreign key (question_id) references questions (id);

alter table question_states drop constraint if exists fk_question_states_attempt;
alter table question_states add constraint fk_question_states_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table question_states drop constraint if exists fk_question_states_exam_question;
alter table question_states add constraint fk_question_states_exam_question
    foreign key (exam_question_id) references exam_questions (id);

alter table question_tag_link drop constraint if exists fk_question_tag_link_question;
alter table question_tag_link add constraint fk_question_tag_link_question
    foreign key (question_id) references questions (id);
alter table question_tag_link drop constraint if exists fk_question_tag_link_tag;
alter table question_tag_link add constraint fk_question_tag_link_tag
    foreign key (question_tag_id) references question_tags (id);

alter table questions drop constraint if exists fk_questions_category;
alter table questions add constraint fk_questions_category
    foreign key (question_category_id) references question_categories (id);
alter table questions drop constraint if exists fk_questions_created_by;
alter table questions add constraint fk_questions_created_by
    foreign key (created_by_user_id) references users (id);
alter table questions drop constraint if exists fk_questions_parent;
alter table questions add constraint fk_questions_parent
    foreign key (parent_question_id) references questions (id);

alter table result_details drop constraint if exists fk_result_details_answer;
alter table result_details add constraint fk_result_details_answer
    foreign key (attempt_answer_id) references attempt_answers (id);
alter table result_details drop constraint if exists fk_result_details_exam_question;
alter table result_details add constraint fk_result_details_exam_question
    foreign key (exam_question_id) references exam_questions (id);
alter table result_details drop constraint if exists fk_result_details_result;
alter table result_details add constraint fk_result_details_result
    foreign key (exam_result_id) references exam_results (id);
alter table result_details drop constraint if exists fk_result_details_section;
alter table result_details add constraint fk_result_details_section
    foreign key (exam_section_id) references exam_sections (id);

alter table result_withholdings drop constraint if exists fk_result_withholdings_case;
alter table result_withholdings add constraint fk_result_withholdings_case
    foreign key (review_case_id) references review_cases (id);
alter table result_withholdings drop constraint if exists fk_result_withholdings_released_by;
alter table result_withholdings add constraint fk_result_withholdings_released_by
    foreign key (released_by_user_id) references users (id);
alter table result_withholdings drop constraint if exists fk_result_withholdings_result;
alter table result_withholdings add constraint fk_result_withholdings_result
    foreign key (exam_result_id) references exam_results (id);
alter table result_withholdings drop constraint if exists fk_result_withholdings_risk;
alter table result_withholdings add constraint fk_result_withholdings_risk
    foreign key (risk_assessment_id) references risk_assessments (id);
alter table result_withholdings drop constraint if exists fk_result_withholdings_withheld_by;
alter table result_withholdings add constraint fk_result_withholdings_withheld_by
    foreign key (withheld_by_user_id) references users (id);

alter table retake_grants drop constraint if exists fk_retake_grants_assignment;
alter table retake_grants add constraint fk_retake_grants_assignment
    foreign key (exam_assignment_id) references exam_assignments (id);
alter table retake_grants drop constraint if exists fk_retake_grants_candidate;
alter table retake_grants add constraint fk_retake_grants_candidate
    foreign key (candidate_user_id) references users (id);
alter table retake_grants drop constraint if exists fk_retake_grants_consumed_by;
alter table retake_grants add constraint fk_retake_grants_consumed_by
    foreign key (consumed_by_attempt_id) references exam_attempts (id);
alter table retake_grants drop constraint if exists fk_retake_grants_decision;
alter table retake_grants add constraint fk_retake_grants_decision
    foreign key (review_decision_id) references review_decisions (id);
alter table retake_grants drop constraint if exists fk_retake_grants_granted_by;
alter table retake_grants add constraint fk_retake_grants_granted_by
    foreign key (granted_by_user_id) references users (id);
alter table retake_grants drop constraint if exists fk_retake_grants_supersedes;
alter table retake_grants add constraint fk_retake_grants_supersedes
    foreign key (supersedes_grant_id) references retake_grants (id);

alter table review_cases drop constraint if exists fk_review_cases_attempt;
alter table review_cases add constraint fk_review_cases_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table review_cases drop constraint if exists fk_review_cases_opened_by;
alter table review_cases add constraint fk_review_cases_opened_by
    foreign key (opened_by_user_id) references users (id);
alter table review_cases drop constraint if exists fk_review_cases_reviewer;
alter table review_cases add constraint fk_review_cases_reviewer
    foreign key (assigned_reviewer_user_id) references users (id);
alter table review_cases drop constraint if exists fk_review_cases_risk_assessment;
alter table review_cases add constraint fk_review_cases_risk_assessment
    foreign key (risk_assessment_id) references risk_assessments (id);

alter table review_decisions drop constraint if exists fk_review_decisions_case;
alter table review_decisions add constraint fk_review_decisions_case
    foreign key (review_case_id) references review_cases (id);
alter table review_decisions drop constraint if exists fk_review_decisions_reviewer;
alter table review_decisions add constraint fk_review_decisions_reviewer
    foreign key (reviewer_user_id) references users (id);
alter table review_decisions drop constraint if exists fk_review_decisions_supersedes;
alter table review_decisions add constraint fk_review_decisions_supersedes
    foreign key (supersedes_decision_id) references review_decisions (id);

alter table review_findings drop constraint if exists fk_review_findings_case;
alter table review_findings add constraint fk_review_findings_case
    foreign key (review_case_id) references review_cases (id);
alter table review_findings drop constraint if exists fk_review_findings_reviewer;
alter table review_findings add constraint fk_review_findings_reviewer
    foreign key (reviewer_user_id) references users (id);

alter table review_notes drop constraint if exists fk_review_notes_author;
alter table review_notes add constraint fk_review_notes_author
    foreign key (author_user_id) references users (id);
alter table review_notes drop constraint if exists fk_review_notes_case;
alter table review_notes add constraint fk_review_notes_case
    foreign key (review_case_id) references review_cases (id);

alter table risk_assessments drop constraint if exists fk_risk_assessments_attempt;
alter table risk_assessments add constraint fk_risk_assessments_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table risk_assessments drop constraint if exists fk_risk_assessments_session;
alter table risk_assessments add constraint fk_risk_assessments_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table risk_events drop constraint if exists fk_risk_events_assessment;
alter table risk_events add constraint fk_risk_events_assessment
    foreign key (risk_assessment_id) references risk_assessments (id);
alter table risk_events drop constraint if exists fk_risk_events_detection;
alter table risk_events add constraint fk_risk_events_detection
    foreign key (ai_detection_id) references ai_detections (id);
alter table risk_events drop constraint if exists fk_risk_events_proctoring_event;
alter table risk_events add constraint fk_risk_events_proctoring_event
    foreign key (proctoring_event_id) references proctoring_events (id);

alter table role_permission drop constraint if exists fk_role_permission_granted_by;
alter table role_permission add constraint fk_role_permission_granted_by
    foreign key (granted_by_user_id) references users (id);
alter table role_permission drop constraint if exists fk_role_permission_permission;
alter table role_permission add constraint fk_role_permission_permission
    foreign key (permission_id) references permissions (id);
alter table role_permission drop constraint if exists fk_role_permission_role;
alter table role_permission add constraint fk_role_permission_role
    foreign key (role_id) references roles (id);

alter table security_tokens drop constraint if exists fk_security_tokens_user;
alter table security_tokens add constraint fk_security_tokens_user
    foreign key (user_id) references users (id);

alter table shadow_evaluations drop constraint if exists fk_shadow_evaluations_active;
alter table shadow_evaluations add constraint fk_shadow_evaluations_active
    foreign key (active_version_id) references ai_model_versions (id);
alter table shadow_evaluations drop constraint if exists fk_shadow_evaluations_shadow;
alter table shadow_evaluations add constraint fk_shadow_evaluations_shadow
    foreign key (shadow_version_id) references ai_model_versions (id);

alter table student_groups drop constraint if exists fk_student_groups_owner;
alter table student_groups add constraint fk_student_groups_owner
    foreign key (owner_user_id) references users (id);
alter table student_groups drop constraint if exists fk_student_groups_parent;
alter table student_groups add constraint fk_student_groups_parent
    foreign key (parent_group_id) references student_groups (id);

alter table suspicious_activities drop constraint if exists fk_suspicious_activities_adjudicated_by;
alter table suspicious_activities add constraint fk_suspicious_activities_adjudicated_by
    foreign key (adjudicated_by_user_id) references users (id);
alter table suspicious_activities drop constraint if exists fk_suspicious_activities_attempt;
alter table suspicious_activities add constraint fk_suspicious_activities_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table suspicious_activities drop constraint if exists fk_suspicious_activities_detection;
alter table suspicious_activities add constraint fk_suspicious_activities_detection
    foreign key (primary_detection_id) references ai_detections (id);
alter table suspicious_activities drop constraint if exists fk_suspicious_activities_session;
alter table suspicious_activities add constraint fk_suspicious_activities_session
    foreign key (proctoring_session_id) references proctoring_sessions (id);

alter table system_check_items drop constraint if exists fk_system_check_items_run;
alter table system_check_items add constraint fk_system_check_items_run
    foreign key (system_check_id) references system_checks (id);

alter table system_checks drop constraint if exists fk_system_checks_attempt;
alter table system_checks add constraint fk_system_checks_attempt
    foreign key (exam_attempt_id) references exam_attempts (id);
alter table system_checks drop constraint if exists fk_system_checks_candidate;
alter table system_checks add constraint fk_system_checks_candidate
    foreign key (candidate_user_id) references users (id);
alter table system_checks drop constraint if exists fk_system_checks_exam;
alter table system_checks add constraint fk_system_checks_exam
    foreign key (exam_id) references exams (id);
alter table system_checks drop constraint if exists fk_system_checks_overridden_by;
alter table system_checks add constraint fk_system_checks_overridden_by
    foreign key (overridden_by_user_id) references users (id);

alter table system_settings drop constraint if exists fk_system_settings_updated_by;
alter table system_settings add constraint fk_system_settings_updated_by
    foreign key (updated_by_user_id) references users (id);

alter table user_role drop constraint if exists fk_user_role_granted_by;
alter table user_role add constraint fk_user_role_granted_by
    foreign key (granted_by_user_id) references users (id);
alter table user_role drop constraint if exists fk_user_role_role;
alter table user_role add constraint fk_user_role_role
    foreign key (role_id) references roles (id);
alter table user_role drop constraint if exists fk_user_role_user;
alter table user_role add constraint fk_user_role_user
    foreign key (user_id) references users (id);

alter table user_sessions drop constraint if exists fk_user_sessions_user;
alter table user_sessions add constraint fk_user_sessions_user
    foreign key (user_id) references users (id);

-- ---------------------------------------------------------------- fk indexes (77)

create index if not exists ix_ai_detections_evidence_file_id on ai_detections (evidence_file_id);
create index if not exists ix_ai_detections_ai_model_version_id on ai_detections (ai_model_version_id);
create index if not exists ix_ai_model_versions_activated_by_user_id on ai_model_versions (activated_by_user_id);
create index if not exists ix_answer_timing_anomalies_exam_question_id on answer_timing_anomalies (exam_question_id);
create index if not exists ix_api_clients_created_by_user_id on api_clients (created_by_user_id);
create index if not exists ix_attempt_answers_exam_question_id on attempt_answers (exam_question_id);
create index if not exists ix_attempt_answers_graded_by_user_id on attempt_answers (graded_by_user_id);
create index if not exists ix_attempt_pause_requests_decided_by_user_id on attempt_pause_requests (decided_by_user_id);
create index if not exists ix_attempt_pause_requests_requested_by_user_id on attempt_pause_requests (requested_by_user_id);
create index if not exists ix_attempt_resumptions_approved_by_user_id on attempt_resumptions (approved_by_user_id);
create index if not exists ix_attempt_resumptions_risk_event_id on attempt_resumptions (risk_event_id);
create index if not exists ix_attempt_timelines_generated_by_user_id on attempt_timelines (generated_by_user_id);
create index if not exists ix_code_execution_results_code_test_case_id on code_execution_results (code_test_case_id);
create index if not exists ix_consent_records_privacy_notice_id on consent_records (privacy_notice_id);
create index if not exists ix_device_trust_records_exam_id on device_trust_records (exam_id);
create index if not exists ix_evidence_custody_records_actor_user_id on evidence_custody_records (actor_user_id);
create index if not exists ix_evidence_custody_records_actor_api_client_id on evidence_custody_records (actor_api_client_id);
create index if not exists ix_exam_assignments_assigned_by_user_id on exam_assignments (assigned_by_user_id);
create index if not exists ix_exam_attempts_exam_assignment_id on exam_attempts (exam_assignment_id);
create index if not exists ix_exam_attempts_current_section_id on exam_attempts (current_section_id);
create index if not exists ix_exam_group_assignments_assigned_by_user_id on exam_group_assignments (assigned_by_user_id);
create index if not exists ix_exam_questions_question_id on exam_questions (question_id);
create index if not exists ix_exam_results_graded_by_user_id on exam_results (graded_by_user_id);
create index if not exists ix_exam_results_review_case_id on exam_results (review_case_id);
create index if not exists ix_exam_window_overrides_granted_by_user_id on exam_window_overrides (granted_by_user_id);
create index if not exists ix_exam_window_overrides_supersedes_override_id on exam_window_overrides (supersedes_override_id);
create index if not exists ix_exams_created_by_user_id on exams (created_by_user_id);
create index if not exists ix_group_memberships_added_by_user_id on group_memberships (added_by_user_id);
create index if not exists ix_identity_verifications_captured_evidence_id on identity_verifications (captured_evidence_id);
create index if not exists ix_identity_verifications_proctoring_session_id on identity_verifications (proctoring_session_id);
create index if not exists ix_identity_verifications_verified_by_user_id on identity_verifications (verified_by_user_id);
create index if not exists ix_notification_suppressions_created_by_user_id on notification_suppressions (created_by_user_id);
create index if not exists ix_notifications_template_id on notifications (template_id);
create index if not exists ix_proctor_actions_proctoring_event_id on proctor_actions (proctoring_event_id);
create index if not exists ix_proctor_actions_proctor_user_id on proctor_actions (proctor_user_id);
create index if not exists ix_proctor_actions_supervisor_approval_user_id on proctor_actions (supervisor_approval_user_id);
create index if not exists ix_proctor_shifts_proctor_user_id on proctor_shifts (proctor_user_id);
create index if not exists ix_proctoring_events_device_session_id on proctoring_events (device_session_id);
create index if not exists ix_proctoring_reports_review_case_id on proctoring_reports (review_case_id);
create index if not exists ix_proctoring_reports_requested_by_user_id on proctoring_reports (requested_by_user_id);
create index if not exists ix_proctoring_reports_risk_assessment_id on proctoring_reports (risk_assessment_id);
create index if not exists ix_proctoring_reports_proctoring_session_id on proctoring_reports (proctoring_session_id);
create index if not exists ix_proctoring_sessions_identity_verified_by_user_id on proctoring_sessions (identity_verified_by_user_id);
create index if not exists ix_question_states_exam_question_id on question_states (exam_question_id);
create index if not exists ix_questions_question_category_id on questions (question_category_id);
create index if not exists ix_questions_created_by_user_id on questions (created_by_user_id);
create index if not exists ix_result_details_attempt_answer_id on result_details (attempt_answer_id);
create index if not exists ix_result_details_exam_question_id on result_details (exam_question_id);
create index if not exists ix_result_details_exam_section_id on result_details (exam_section_id);
create index if not exists ix_result_withholdings_review_case_id on result_withholdings (review_case_id);
create index if not exists ix_result_withholdings_released_by_user_id on result_withholdings (released_by_user_id);
create index if not exists ix_result_withholdings_risk_assessment_id on result_withholdings (risk_assessment_id);
create index if not exists ix_result_withholdings_withheld_by_user_id on result_withholdings (withheld_by_user_id);
create index if not exists ix_retake_grants_consumed_by_attempt_id on retake_grants (consumed_by_attempt_id);
create index if not exists ix_retake_grants_review_decision_id on retake_grants (review_decision_id);
create index if not exists ix_retake_grants_granted_by_user_id on retake_grants (granted_by_user_id);
create index if not exists ix_retake_grants_supersedes_grant_id on retake_grants (supersedes_grant_id);
create index if not exists ix_review_cases_opened_by_user_id on review_cases (opened_by_user_id);
create index if not exists ix_review_cases_risk_assessment_id on review_cases (risk_assessment_id);
create index if not exists ix_review_decisions_reviewer_user_id on review_decisions (reviewer_user_id);
create index if not exists ix_review_decisions_supersedes_decision_id on review_decisions (supersedes_decision_id);
create index if not exists ix_review_findings_reviewer_user_id on review_findings (reviewer_user_id);
create index if not exists ix_review_notes_author_user_id on review_notes (author_user_id);
create index if not exists ix_risk_assessments_proctoring_session_id on risk_assessments (proctoring_session_id);
create index if not exists ix_risk_events_ai_detection_id on risk_events (ai_detection_id);
create index if not exists ix_risk_events_proctoring_event_id on risk_events (proctoring_event_id);
create index if not exists ix_role_permission_granted_by_user_id on role_permission (granted_by_user_id);
create index if not exists ix_role_permission_permission_id on role_permission (permission_id);
create index if not exists ix_shadow_evaluations_active_version_id on shadow_evaluations (active_version_id);
create index if not exists ix_student_groups_owner_user_id on student_groups (owner_user_id);
create index if not exists ix_student_groups_parent_group_id on student_groups (parent_group_id);
create index if not exists ix_suspicious_activities_adjudicated_by_user_id on suspicious_activities (adjudicated_by_user_id);
create index if not exists ix_suspicious_activities_primary_detection_id on suspicious_activities (primary_detection_id);
create index if not exists ix_system_checks_overridden_by_user_id on system_checks (overridden_by_user_id);
create index if not exists ix_system_settings_updated_by_user_id on system_settings (updated_by_user_id);
create index if not exists ix_user_role_granted_by_user_id on user_role (granted_by_user_id);
create index if not exists ix_user_role_role_id on user_role (role_id);
