package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.ClassService;
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
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(classService.listClasses(SecurityUtils.currentUser(), request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(classService.createClass(payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{classId}")
    public ApiResponse<?> update(@PathVariable Long classId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(classService.updateClass(classId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{classId}")
    public ApiResponse<?> delete(@PathVariable Long classId) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(classService.deleteClass(classId), "删除成功");
    }

    @PutMapping("/{classId}/teacher")
    public ApiResponse<?> assignTeacher(@PathVariable Long classId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(classService.assignTeacher(classId, payload == null ? Collections.emptyMap() : payload), "分配成功");
    }

    @PostMapping("/transfer-student")
    public ApiResponse<?> transferStudent(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(classService.transferStudent(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "学生调班成功");
    }

    @GetMapping("/my/students")
    public ApiResponse<?> myStudents(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("teacher");
        return ApiResponse.success(classService.myClassStudents(SecurityUtils.currentUser(), request));
    }
}
