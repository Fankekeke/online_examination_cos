package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.CommunityService;
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
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public ApiResponse<?> listPosts(HttpServletRequest request) {
        return ApiResponse.success(communityService.listPosts(request));
    }

    @PostMapping("/posts")
    public ApiResponse<?> createPost(@RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success(communityService.createPost(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "发布成功");
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<?> createComment(@PathVariable Long postId, @RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success(communityService.createComment(SecurityUtils.currentUser(), postId, payload == null ? Collections.emptyMap() : payload), "评论成功");
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<?> like(@PathVariable Long postId) {
        return ApiResponse.success(communityService.likePost(postId), "点赞成功");
    }
}
