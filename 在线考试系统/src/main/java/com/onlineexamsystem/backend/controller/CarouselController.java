package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.security.SecurityUtils;
import com.onlineexamsystem.backend.service.CarouselService;
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
@RequestMapping("/api/carousels")
public class CarouselController {

    private final CarouselService carouselService;

    public CarouselController(CarouselService carouselService) {
        this.carouselService = carouselService;
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request) {
        return ApiResponse.success(carouselService.listCarousels(request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(carouselService.createCarousel(payload == null ? Collections.emptyMap() : payload), "新增成功");
    }

    @PutMapping("/{carouselId}")
    public ApiResponse<?> update(@PathVariable Long carouselId, @RequestBody(required = false) Map<String, Object> payload) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(carouselService.updateCarousel(carouselId, payload == null ? Collections.emptyMap() : payload), "更新成功");
    }

    @DeleteMapping("/{carouselId}")
    public ApiResponse<?> delete(@PathVariable Long carouselId) {
        SecurityUtils.requireAnyRole("admin");
        return ApiResponse.success(carouselService.deleteCarousel(carouselId), "删除成功");
    }
}
