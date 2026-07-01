package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.ScoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(scoreService.listScores(SecurityUtils.currentUser(), request));
    }

    @GetMapping("/segment")
    public ApiResponse<?> segment(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(scoreService.scoreSegment(SecurityUtils.currentUser(), request));
    }
}
