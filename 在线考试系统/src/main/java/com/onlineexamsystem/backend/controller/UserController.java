package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.UserService;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(userService.listUsers(SecurityUtils.currentUser(), request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(userService.createUser(payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{userId}")
    public ApiResponse<?> update(@PathVariable Long userId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(userService.updateUser(userId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<?> delete(@PathVariable Long userId) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(userService.deleteUser(userId), "删除成功");
    }

    @PutMapping("/self")
    public ApiResponse<?> updateSelf(@RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success(userService.updateSelf(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "保存成功");
    }

    @PutMapping("/password")
    public ApiResponse<?> changePassword(@RequestBody(required = false) Map<String, Object> payload) {
        return ApiResponse.success(userService.updatePassword(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "密码更新成功");
    }
}
