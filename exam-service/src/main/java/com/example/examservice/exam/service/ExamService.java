package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.CreateExamRequest;
import com.example.examservice.exam.dto.ExamResponse;
import com.example.examservice.exam.dto.UpdateExamRequest;

public interface ExamService {

    ExamResponse createExam(CreateExamRequest request, Long callerUserId);

    ExamResponse updateExam(Long examId, UpdateExamRequest request, Long callerUserId);

    void deleteExam(Long examId, String reason, Long callerUserId);

    ExamResponse publishExam(Long examId, Long callerUserId);

    ExamResponse activateExam(Long examId, Long callerUserId);

    ExamResponse closeExam(Long examId, String reason, Long callerUserId);

    ExamResponse archiveExam(Long examId, String reason, Long callerUserId);
}
