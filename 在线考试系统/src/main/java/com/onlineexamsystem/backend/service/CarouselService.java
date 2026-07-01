package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.CarouselEntity;
import com.onlineexamsystem.backend.mapper.CarouselMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CarouselService {

    private final CarouselMapper carouselMapper;
    private final ViewAssembler viewAssembler;

    public CarouselService(CarouselMapper carouselMapper, ViewAssembler viewAssembler) {
        this.carouselMapper = carouselMapper;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.CarouselItemDto> listCarousels(HttpServletRequest request) {
        boolean activeOnly = "true".equalsIgnoreCase(request.getParameter("activeOnly"));
        Page<CarouselEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<CarouselEntity> wrapper = new LambdaQueryWrapper<>();
        if (activeOnly) {
            wrapper.eq(CarouselEntity::getIsActive, true);
        }
        wrapper.orderByAsc(CarouselEntity::getSortOrder).orderByDesc(CarouselEntity::getId);
        Page<CarouselEntity> result = carouselMapper.selectPage(page, wrapper);
        List<ApiDtos.CarouselItemDto> items = result.getRecords().stream().map(viewAssembler::toCarousel).toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.CarouselItemDto createCarousel(Map<String, Object> payload) {
        String title = trim(payload.get("title"));
        String imageUrl = trim(payload.get("imageUrl"));
        if (title.isEmpty() || imageUrl.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题和图片不能为空");
        }
        CarouselEntity carousel = new CarouselEntity();
        carousel.setTitle(title);
        carousel.setImageUrl(imageUrl);
        carousel.setLink(orEmpty(payload.get("link")));
        carousel.setSortOrder(PayloadUtils.integerValue(payload.get("sortOrder"), 1));
        carousel.setIsActive(PayloadUtils.booleanValue(payload.get("isActive"), true));
        carouselMapper.insert(carousel);
        return viewAssembler.toCarousel(carouselMapper.selectById(carousel.getId()));
    }

    public ApiDtos.CarouselItemDto updateCarousel(Long carouselId, Map<String, Object> payload) {
        CarouselEntity carousel = carouselMapper.selectById(carouselId);
        if (carousel == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "轮播图不存在");
        }
        String title = trim(payload.getOrDefault("title", carousel.getTitle()));
        String imageUrl = trim(payload.getOrDefault("imageUrl", carousel.getImageUrl()));
        if (title.isEmpty() || imageUrl.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题和图片不能为空");
        }
        carousel.setTitle(title);
        carousel.setImageUrl(imageUrl);
        carousel.setLink(orEmpty(payload.get("link")));
        carousel.setSortOrder(PayloadUtils.integerValue(payload.get("sortOrder"), 1));
        carousel.setIsActive(PayloadUtils.booleanValue(payload.get("isActive"), true));
        carouselMapper.updateById(carousel);
        return viewAssembler.toCarousel(carouselMapper.selectById(carouselId));
    }

    public boolean deleteCarousel(Long carouselId) {
        CarouselEntity carousel = carouselMapper.selectById(carouselId);
        if (carousel == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "轮播图不存在");
        }
        carouselMapper.deleteById(carouselId);
        return true;
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String orEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
