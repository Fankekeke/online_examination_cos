package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.ScoreService;
import com.onlineexamsystem.backend.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;
    private final ScoreService scoreService;

    public StudentController(StudentService studentService, ScoreService scoreService) {
        this.studentService = studentService;
        this.scoreService = scoreService;
    }

    @GetMapping("/exams")
    public ApiResponse<?> exams(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("student");
        return ApiResponse.success(studentService.availableExams(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/practice")
    public ApiResponse<?> practice(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("student");
        return ApiResponse.success(studentService.practiceExams(SecurityUtils.currentUser(), request));
    }

    @PostMapping("/exams/{examId}/start")
    public ApiResponse<?> start(@PathVariable Long examId) {
        SecurityUtils.requireAnyRole("student");
        return ApiResponse.success(studentService.startExam(SecurityUtils.currentUser(), examId));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<?> submit(@PathVariable Long attemptId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("student");
        if (payload == null) {
            payload = Collections.emptyMap();
        }
        var outcome = studentService.submitExam(SecurityUtils.currentUser(), attemptId, payload);
        return ApiResponse.success(outcome.result(), outcome.message());
    }

    @GetMapping("/scores")
    public ApiResponse<?> scores(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("student");
        return ApiResponse.success(scoreService.myScores(SecurityUtils.currentUser(), request));
    }
}
