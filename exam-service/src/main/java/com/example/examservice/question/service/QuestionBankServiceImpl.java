package com.example.examservice.question.service;

import com.example.examservice.common.audit.AuditService;
import com.example.examservice.common.enums.AuditAction;
import com.example.examservice.common.exception.AccessDeniedException;
import com.example.examservice.common.exception.EntityNotFoundException;
import com.example.examservice.common.exception.InvalidRequestException;
import com.example.examservice.common.security.Permissions;
import com.example.examservice.common.security.RoleService;
import com.example.examservice.question.dto.CategoryResponse;
import com.example.examservice.question.dto.CreateCategoryRequest;
import com.example.examservice.question.entity.Question;
import com.example.examservice.question.entity.QuestionCategory;
import com.example.examservice.question.entity.QuestionTag;
import com.example.examservice.question.entity.QuestionTagLink;
import com.example.examservice.question.repository.QuestionCategoryRepository;
import com.example.examservice.question.repository.QuestionRepository;
import com.example.examservice.question.repository.QuestionTagLinkRepository;
import com.example.examservice.question.repository.QuestionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionCategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTagRepository tagRepository;
    private final QuestionTagLinkRepository tagLinkRepository;
    private final RoleService roleService;
    private final AuditService auditService;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request, Long callerUserId) {
        requirePermission(callerUserId, null);

        if (categoryRepository.existsByCode(request.code())) {
            throw new InvalidRequestException("code already in use");
        }

        QuestionCategory category = new QuestionCategory();
        category.setCode(request.code());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setParentCategoryId(request.parentCategoryId());
        category.setActive(true);

        if (request.parentCategoryId() != null) {
            QuestionCategory parent = categoryRepository.findById(request.parentCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found: " + request.parentCategoryId()));
            category.setPath(parent.getPath() + request.code() + "/");
            category.setDepth(parent.getDepth() + 1);
        } else {
            category.setPath("/" + request.code() + "/");
            category.setDepth(0);
        }

        category = categoryRepository.save(category);
        auditService.recordSuccess(AuditAction.CREATE, "QuestionCategory", category.getId(), callerUserId,
                null, Map.of("code", category.getCode(), "path", category.getPath()), null);
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CategoryResponse moveCategory(Long categoryId, Long newParentCategoryId, Long callerUserId) {
        requirePermission(callerUserId, categoryId);

        QuestionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("QuestionCategory not found: " + categoryId));

        String oldPath = category.getPath();
        String newPath;
        int newDepth;

        if (newParentCategoryId != null) {
            QuestionCategory newParent = categoryRepository.findById(newParentCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("QuestionCategory not found: " + newParentCategoryId));
            assertNoCycle(categoryId, newParent);
            newPath = newParent.getPath() + category.getCode() + "/";
            newDepth = newParent.getDepth() + 1;
        } else {
            newPath = "/" + category.getCode() + "/";
            newDepth = 0;
        }

        List<QuestionCategory> descendants = categoryRepository.findByPathStartingWith(oldPath);
        for (QuestionCategory descendant : descendants) {
            if (descendant.getId().equals(categoryId)) {
                continue;
            }
            String rewritten = newPath + descendant.getPath().substring(oldPath.length());
            int depthDelta = newDepth - category.getDepth();
            descendant.setPath(rewritten);
            descendant.setDepth(descendant.getDepth() + depthDelta);
            categoryRepository.save(descendant);
        }

        category.setParentCategoryId(newParentCategoryId);
        category.setPath(newPath);
        category.setDepth(newDepth);
        category = categoryRepository.save(category);

        auditService.recordSuccess(AuditAction.UPDATE, "QuestionCategory", categoryId, callerUserId,
                Map.of("path", oldPath), Map.of("path", newPath), "category_move");
        return CategoryResponse.from(category);
    }

    private void assertNoCycle(Long categoryId, QuestionCategory newParent) {
        QuestionCategory cursor = newParent;
        while (cursor != null) {
            if (cursor.getId().equals(categoryId)) {
                throw new InvalidRequestException("move would create a cycle in the category tree");
            }
            cursor = cursor.getParentCategoryId() == null
                    ? null
                    : categoryRepository.findById(cursor.getParentCategoryId()).orElse(null);
        }
    }

    @Override
    @Transactional
    public void tagQuestion(Long questionId, String tagName, String tagDescription, Long callerUserId) {
        requirePermission(callerUserId, questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        QuestionTag tag = tagRepository.findByName(tagName).orElseGet(() -> {
            QuestionTag created = new QuestionTag();
            created.setName(tagName);
            created.setDescription(tagDescription);
            created.setUsageCount(0);
            return tagRepository.save(created);
        });

        QuestionTagLink link = new QuestionTagLink();
        link.setQuestionId(question.getId());
        link.setQuestionTagId(tag.getId());
        tagLinkRepository.save(link);

        tag.setUsageCount(tag.getUsageCount() + 1);
        tagRepository.save(tag);

        auditService.recordSuccess(AuditAction.CREATE, "QuestionTagLink", link.getId(), callerUserId,
                null, Map.of("questionId", questionId, "tagName", tagName), null);
    }

    @Override
    @Transactional
    public void untagQuestion(Long questionId, Long tagId, Long callerUserId) {
        requirePermission(callerUserId, questionId);

        QuestionTagLink link = tagLinkRepository.findByQuestionIdAndQuestionTagId(questionId, tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag link not found"));
        tagLinkRepository.delete(link);

        tagRepository.findById(tagId).ifPresent(tag -> {
            tag.setUsageCount(Math.max(0, tag.getUsageCount() - 1));
            tagRepository.save(tag);
        });

        auditService.recordSuccess(AuditAction.DELETE, "QuestionTagLink", link.getId(), callerUserId,
                Map.of("questionId", questionId, "tagId", tagId), null, null);
    }

    private void requirePermission(Long callerUserId, Long entityId) {
        if (!roleService.checkAccess(callerUserId, Permissions.QUESTION_CREATE)) {
            auditService.recordDenied(AuditAction.CREATE, "QuestionCategory", entityId, callerUserId,
                    "missing " + Permissions.QUESTION_CREATE);
            throw new AccessDeniedException("missing permission: " + Permissions.QUESTION_CREATE);
        }
    }
}
