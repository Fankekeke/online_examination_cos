package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{role}")
    public ApiResponse<?> roleDashboard(@PathVariable String role) {
        SecurityUtils.requireAnyRole("admin", "teacher", "student");
        return ApiResponse.success(dashboardService.roleDashboard(SecurityUtils.currentUser(), role));
    }

    @GetMapping("/big-screen")
    public ApiResponse<?> bigScreen() {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(dashboardService.bigScreen());
    }
}
