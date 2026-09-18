package com.example.examservice.common.security;

/** Permission codes checked via {@link RoleService#checkAccess}. Matches the {@code exam:*} /
 * {@code question:*} vocabulary used throughout the Exam Management spec. */
public final class Permissions {

    public static final String EXAM_CREATE = "exam:create";
    public static final String EXAM_UPDATE = "exam:update";
    public static final String EXAM_DELETE = "exam:delete";
    public static final String EXAM_PUBLISH = "exam:publish";
    public static final String EXAM_ACTIVATE = "exam:activate";
    public static final String EXAM_CLOSE = "exam:close";
    public static final String EXAM_ARCHIVE = "exam:archive";

    public static final String QUESTION_CREATE = "question:create";
    public static final String QUESTION_UPDATE = "question:update";

    public static final String EXAM_ASSIGN = "exam:assign";
    public static final String EXAM_INVITE = "exam:invite";
    public static final String EXAM_GENERATE_ACCESS_CODE = "exam:generate_access_code";
    public static final String EXAM_GRANT_EXTRA_ATTEMPTS = "exam:grant_extra_attempts";
    public static final String PREREQUISITE_MANAGE = "prerequisite:manage";

    private Permissions() {
    }
}
