package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.listExams(request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.createExam(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{examId}")
    public ApiResponse<?> update(@PathVariable Long examId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.updateExam(examId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<?> delete(@PathVariable Long examId) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.deleteExam(examId), "删除成功");
    }

    @PostMapping("/{examId}/publish")
    public ApiResponse<?> publish(@PathVariable Long examId) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.publishExam(examId), "发布成功");
    }

    @GetMapping("/{examId}/paper")
    public ApiResponse<?> paper(@PathVariable Long examId) {
        SecurityUtils.requireAnyRole("admin", "teacher", "student");
        return ApiResponse.success(examService.examPaper(examId, SecurityUtils.currentUser()));
    }

    @PutMapping("/{examId}/questions")
    public ApiResponse<?> bindQuestions(@PathVariable Long examId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(examService.bindQuestions(examId, payload == null ? Collections.emptyMap() : payload), "组卷成功");
    }
}
