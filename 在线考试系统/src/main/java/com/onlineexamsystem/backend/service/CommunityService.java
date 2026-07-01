package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.CommunityCommentEntity;
import com.onlineexamsystem.backend.entity.CommunityPostEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.CommunityCommentMapper;
import com.onlineexamsystem.backend.mapper.CommunityPostMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CommunityService {

    private final CommunityPostMapper communityPostMapper;
    private final CommunityCommentMapper communityCommentMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public CommunityService(CommunityPostMapper communityPostMapper,
                            CommunityCommentMapper communityCommentMapper,
                            ReferenceLookupService referenceLookupService,
                            ViewAssembler viewAssembler) {
        this.communityPostMapper = communityPostMapper;
        this.communityCommentMapper = communityCommentMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.CommunityPostDto> listPosts(HttpServletRequest request) {
        String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword").trim();
        Page<CommunityPostEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<CommunityPostEntity> wrapper = new LambdaQueryWrapper<>();
        if (!keyword.isEmpty()) {
            wrapper.like(CommunityPostEntity::getContent, keyword);
        }
        wrapper.orderByDesc(CommunityPostEntity::getId);
        Page<CommunityPostEntity> result = communityPostMapper.selectPage(page, wrapper);
        return buildPostPage(result.getRecords(), result.getTotal());
    }

    public ApiDtos.CommunityPostDto createPost(UserEntity currentUser, Map<String, Object> payload) {
        String content = payload.get("content") == null ? "" : String.valueOf(payload.get("content")).trim();
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "留言内容不能为空");
        }
        CommunityPostEntity post = new CommunityPostEntity();
        post.setUserId(currentUser.getId());
        post.setContent(content);
        post.setLikes(0);
        communityPostMapper.insert(post);
        return buildPostPage(List.of(communityPostMapper.selectById(post.getId())), 1).getItems().get(0);
    }

    public boolean createComment(UserEntity currentUser, Long postId, Map<String, Object> payload) {
        CommunityPostEntity post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在");
        }
        String content = payload.get("content") == null ? "" : String.valueOf(payload.get("content")).trim();
        if (content.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        CommunityCommentEntity comment = new CommunityCommentEntity();
        comment.setPostId(postId);
        comment.setUserId(currentUser.getId());
        comment.setContent(content);
        communityCommentMapper.insert(comment);
        return true;
    }

    public boolean likePost(Long postId) {
        CommunityPostEntity post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在");
        }
        post.setLikes((post.getLikes() == null ? 0 : post.getLikes()) + 1);
        communityPostMapper.updateById(post);
        return true;
    }

    private PageData<ApiDtos.CommunityPostDto> buildPostPage(List<CommunityPostEntity> posts, long total) {
        List<Long> postIds = posts.stream().map(CommunityPostEntity::getId).toList();
        Map<Long, List<CommunityCommentEntity>> commentMap = referenceLookupService.loadCommentsByPostIds(postIds);
        List<Long> userIds = new ArrayList<>(posts.stream().map(CommunityPostEntity::getUserId).filter(Objects::nonNull).toList());
        commentMap.values().forEach(comments -> comments.forEach(comment -> userIds.add(comment.getUserId())));
        Map<Long, UserEntity> userMap = referenceLookupService.loadUsers(userIds);
        List<ApiDtos.CommunityPostDto> items = posts.stream()
            .map(item -> viewAssembler.toCommunityPost(item, userMap, commentMap))
            .toList();
        return new PageData<>(items, total);
    }
}
