package com.example.identityservice.user.port;

/**
 * The one call this module makes into {@code exam-service} when a student joins a group: let it
 * work out whether that group has any {@code ExamGroupAssignment} with
 * {@code autoEnrollNewMembers = true} and, if so, create the matching {@code ExamAssignment} for
 * this student. {@code ExamGroupAssignment} data lives in {@code exam-service}'s own schema, not
 * here, so the whole "for each auto-enroll assignment, assign" loop described in
 * {@code StudentGroupService.addMember} happens on the other side of this call. See
 * {@link NoOpGroupAutoEnrollPort} for the stub used until a real client is wired in.
 */
public interface GroupAutoEnrollPort {

    void enrollNewGroupMember(Long studentGroupId, Long candidateUserId);
}
