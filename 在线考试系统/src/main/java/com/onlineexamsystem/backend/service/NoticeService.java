package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.NoticeEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.NoticeMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public NoticeService(NoticeMapper noticeMapper,
                         ReferenceLookupService referenceLookupService,
                         ViewAssembler viewAssembler) {
        this.noticeMapper = noticeMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.NoticeItemDto> listNotices(HttpServletRequest request) {
        String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword").trim();
        Page<NoticeEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        if (!keyword.isEmpty()) {
            wrapper.like(NoticeEntity::getTitle, keyword);
        }
        wrapper.orderByDesc(NoticeEntity::getIsPinned).orderByDesc(NoticeEntity::getPublishedAt);
        Page<NoticeEntity> result = noticeMapper.selectPage(page, wrapper);
        Map<Long, UserEntity> publisherMap = referenceLookupService.loadUsers(
            result.getRecords().stream().map(NoticeEntity::getPublisherId).filter(Objects::nonNull).toList()
        );
        List<ApiDtos.NoticeItemDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toNotice(item, publisherMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.NoticeItemDto createNotice(UserEntity currentUser, Map<String, Object> payload) {
        String title = trim(payload.get("title"));
        String content = trim(payload.get("content"));
        if (title.isEmpty() || content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "公告标题和内容不能为空");
        }
        NoticeEntity notice = new NoticeEntity();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setIsPinned(PayloadUtils.booleanValue(payload.get("isPinned"), false));
        notice.setPublisherId(currentUser.getId());
        notice.setPublishedAt(DateTimeUtils.utcNow());
        noticeMapper.insert(notice);
        return toNotice(noticeMapper.selectById(notice.getId()));
    }

    public ApiDtos.NoticeItemDto updateNotice(Long noticeId, Map<String, Object> payload) {
        NoticeEntity notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "公告不存在");
        }
        String title = trim(payload.getOrDefault("title", notice.getTitle()));
        String content = trim(payload.getOrDefault("content", notice.getContent()));
        if (title.isEmpty() || content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "公告标题和内容不能为空");
        }
        notice.setTitle(title);
        notice.setContent(content);
        notice.setIsPinned(PayloadUtils.booleanValue(payload.get("isPinned"), Boolean.TRUE.equals(notice.getIsPinned())));
        noticeMapper.updateById(notice);
        return toNotice(noticeMapper.selectById(noticeId));
    }

    public boolean deleteNotice(Long noticeId) {
        NoticeEntity notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "公告不存在");
        }
        noticeMapper.deleteById(noticeId);
        return true;
    }

    private ApiDtos.NoticeItemDto toNotice(NoticeEntity notice) {
        Map<Long, UserEntity> publisherMap = referenceLookupService.loadUsers(List.of(notice.getPublisherId()));
        return viewAssembler.toNotice(notice, publisherMap);
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
