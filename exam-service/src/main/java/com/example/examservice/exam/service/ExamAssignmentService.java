package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.AssignToGroupRequest;
import com.example.examservice.exam.dto.AssignToStudentRequest;
import com.example.examservice.exam.dto.ExamAssignmentResponse;
import com.example.examservice.exam.dto.GroupAssignmentResponse;

public interface ExamAssignmentService {

    ExamAssignmentResponse assignToStudent(Long examId, AssignToStudentRequest request, Long callerUserId);

    GroupAssignmentResponse assignToGroup(Long examId, AssignToGroupRequest request, Long callerUserId);

    GroupAssignmentResponse assignToClass(Long examId, AssignToGroupRequest request, Long callerUserId);

    ExamAssignmentResponse cancelAssignment(Long assignmentId, String reason, Long callerUserId);

    /**
     * Triggered when a candidate joins a group with a live {@code autoEnrollNewMembers} group
     * assignment (see {@code identity-service}'s {@code GroupAutoEnrollPort}). Assigns every exam
     * whose group assignment opts in, using the group assignment's own window and the assigner who
     * originally created it; a candidate who fails a prerequisite is skipped, not failed.
     */
    void autoEnrollNewMember(Long studentGroupId, Long candidateUserId);
}
