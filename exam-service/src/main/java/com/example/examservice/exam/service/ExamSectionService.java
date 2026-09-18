package com.example.examservice.exam.service;

import com.example.examservice.exam.dto.CreateSectionRequest;
import com.example.examservice.exam.dto.ReorderSectionsRequest;
import com.example.examservice.exam.dto.SectionResponse;
import com.example.examservice.exam.dto.SetSectionTimeLimitRequest;

import java.util.List;

public interface ExamSectionService {

    SectionResponse createSection(Long examId, CreateSectionRequest request, Long callerUserId);

    List<SectionResponse> reorderSections(Long examId, ReorderSectionsRequest request, Long callerUserId);

    SectionResponse setTimeLimit(Long sectionId, SetSectionTimeLimitRequest request, Long callerUserId);
}
