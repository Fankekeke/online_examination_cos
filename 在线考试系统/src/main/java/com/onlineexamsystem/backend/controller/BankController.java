package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.BankService;
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
@RequestMapping("/api/banks")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.listBanks(request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.createBank(SecurityUtils.currentUser(), payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{bankId}")
    public ApiResponse<?> update(@PathVariable Long bankId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.updateBank(bankId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{bankId}")
    public ApiResponse<?> delete(@PathVariable Long bankId) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.deleteBank(bankId), "删除成功");
    }

    @GetMapping("/{bankId}/questions")
    public ApiResponse<?> listQuestions(@PathVariable Long bankId, HttpServletRequest request) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.listQuestions(bankId, request));
    }

    @PostMapping("/{bankId}/questions")
    public ApiResponse<?> createQuestion(@PathVariable Long bankId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.createQuestion(SecurityUtils.currentUser(), bankId, payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{bankId}/questions/{questionId}")
    public ApiResponse<?> updateQuestion(@PathVariable Long bankId,
                                         @PathVariable Long questionId,
                                         @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.updateQuestion(bankId, questionId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @PostMapping("/{bankId}/questions/import")
    public ApiResponse<?> importQuestions(@PathVariable Long bankId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.importQuestions(SecurityUtils.currentUser(), bankId, payload == null ? Collections.emptyMap() : payload), "批量导入完成");
    }

    @DeleteMapping("/{bankId}/questions/{questionId}")
    public ApiResponse<?> deleteQuestion(@PathVariable Long bankId, @PathVariable Long questionId) {
        SecurityUtils.requireAnyRole("admin", "teacher");
        return ApiResponse.success(bankService.deleteQuestion(bankId, questionId), "删除成功");
    }
}
