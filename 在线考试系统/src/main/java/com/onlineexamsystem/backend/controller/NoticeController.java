package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.NoticeService;
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
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(noticeService.listNotices(request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(noticeService.createNotice(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "发布成功");
    }

    @PutMapping("/{noticeId}")
    public ApiResponse<?> update(@PathVariable Long noticeId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(noticeService.updateNotice(noticeId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{noticeId}")
    public ApiResponse<?> delete(@PathVariable Long noticeId) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(noticeService.deleteNotice(noticeId), "删除成功");
    }
}
