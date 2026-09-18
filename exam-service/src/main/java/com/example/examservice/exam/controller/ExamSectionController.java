package com.example.examservice.exam.controller;

import com.example.examservice.common.security.CallerUserIdResolver;
import com.example.examservice.exam.dto.CreateSectionRequest;
import com.example.examservice.exam.dto.ReorderSectionsRequest;
import com.example.examservice.exam.dto.SectionResponse;
import com.example.examservice.exam.dto.SetSectionTimeLimitRequest;
import com.example.examservice.exam.service.ExamSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExamSectionController {

    private final ExamSectionService examSectionService;

    @PostMapping("/api/exams/{examId}/sections")
    public ResponseEntity<SectionResponse> createSection(@PathVariable Long examId,
                                                           @Valid @RequestBody CreateSectionRequest request) {
        SectionResponse response = examSectionService.createSection(examId, request, CallerUserIdResolver.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/exams/{examId}/sections/reorder")
    public List<SectionResponse> reorderSections(@PathVariable Long examId,
                                                  @Valid @RequestBody ReorderSectionsRequest request) {
        return examSectionService.reorderSections(examId, request, CallerUserIdResolver.require());
    }

    @PutMapping("/api/exam-sections/{sectionId}/time-limit")
    public SectionResponse setTimeLimit(@PathVariable Long sectionId,
                                         @RequestBody SetSectionTimeLimitRequest request) {
        return examSectionService.setTimeLimit(sectionId, request, CallerUserIdResolver.require());
    }
}
