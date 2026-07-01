package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.ScoreUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.ClassRoomEntity;
import com.onlineexamsystem.backend.entity.ExamAttemptEntity;
import com.onlineexamsystem.backend.entity.ExamEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.ClassRoomMapper;
import com.onlineexamsystem.backend.mapper.ExamAttemptMapper;
import com.onlineexamsystem.backend.mapper.ExamMapper;
import com.onlineexamsystem.backend.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

    private final ExamAttemptMapper examAttemptMapper;
    private final UserMapper userMapper;
    private final ExamMapper examMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ViewAssembler viewAssembler;

    public ScoreService(ExamAttemptMapper examAttemptMapper,
                        UserMapper userMapper,
                        ExamMapper examMapper,
                        ClassRoomMapper classRoomMapper,
                        ViewAssembler viewAssembler) {
        this.examAttemptMapper = examAttemptMapper;
        this.userMapper = userMapper;
        this.examMapper = examMapper;
        this.classRoomMapper = classRoomMapper;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.ScoreRecordDto> listScores(UserEntity currentUser, HttpServletRequest request) {
        List<Long> scopedStudentIds = scopedStudentIds(currentUser);
        if ("teacher".equals(currentUser.getRole()) && scopedStudentIds.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0);
        }

        Page<ExamAttemptEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<ExamAttemptEntity> wrapper = new LambdaQueryWrapper<>();
        if ("teacher".equals(currentUser.getRole())) {
            wrapper.in(ExamAttemptEntity::getStudentId, scopedStudentIds);
        }

        String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword").trim();
        if (!keyword.isEmpty()) {
            List<Long> matchedStudentIds = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .and(group -> group.like(UserEntity::getName, keyword).or().like(UserEntity::getUsername, keyword)))
                .stream().map(UserEntity::getId).toList();
            if (matchedStudentIds.isEmpty()) {
                return new PageData<>(Collections.emptyList(), 0);
            }
            wrapper.in(ExamAttemptEntity::getStudentId, matchedStudentIds);
        }

        String examId = request.getParameter("examId");
        if (examId != null && !examId.isBlank()) {
            wrapper.eq(ExamAttemptEntity::getExamId, Long.parseLong(examId));
        }
        String studentId = request.getParameter("studentId");
        if (studentId != null && !studentId.isBlank()) {
            wrapper.eq(ExamAttemptEntity::getStudentId, Long.parseLong(studentId));
        }
        String status = request.getParameter("status");
        if (status != null && !status.isBlank()) {
            wrapper.eq(ExamAttemptEntity::getStatus, status.trim());
        }
        wrapper.orderByDesc(ExamAttemptEntity::getId);

        Page<ExamAttemptEntity> result = examAttemptMapper.selectPage(page, wrapper);
        return buildScorePage(result.getRecords(), result.getTotal());
    }

    public List<ApiDtos.SegmentItemDto> scoreSegment(UserEntity currentUser, HttpServletRequest request) {
        List<Long> scopedStudentIds = scopedStudentIds(currentUser);
        if ("teacher".equals(currentUser.getRole()) && scopedStudentIds.isEmpty()) {
            return List.of(
                new ApiDtos.SegmentItemDto("0-59", 0),
                new ApiDtos.SegmentItemDto("60-69", 0),
                new ApiDtos.SegmentItemDto("70-79", 0),
                new ApiDtos.SegmentItemDto("80-89", 0),
                new ApiDtos.SegmentItemDto("90-100", 0)
            );
        }

        LambdaQueryWrapper<ExamAttemptEntity> wrapper = new LambdaQueryWrapper<>();
        if ("teacher".equals(currentUser.getRole())) {
            wrapper.in(ExamAttemptEntity::getStudentId, scopedStudentIds);
        }
        String examId = request.getParameter("examId");
        if (examId != null && !examId.isBlank()) {
            wrapper.eq(ExamAttemptEntity::getExamId, Long.parseLong(examId));
        }
        wrapper.eq(ExamAttemptEntity::getStatus, "submitted");

        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-59", 0L);
        buckets.put("60-69", 0L);
        buckets.put("70-79", 0L);
        buckets.put("80-89", 0L);
        buckets.put("90-100", 0L);

        for (ExamAttemptEntity attempt : examAttemptMapper.selectList(wrapper)) {
            String range = ScoreUtils.scoreSegment(attempt.getScore() == null ? 0 : attempt.getScore());
            buckets.put(range, buckets.get(range) + 1);
        }

        return buckets.entrySet().stream()
            .map(entry -> new ApiDtos.SegmentItemDto(entry.getKey(), entry.getValue()))
            .toList();
    }

    public PageData<ApiDtos.ScoreRecordDto> myScores(UserEntity currentUser, HttpServletRequest request) {
        Page<ExamAttemptEntity> page = PageUtils.createPage(request);
        Page<ExamAttemptEntity> result = examAttemptMapper.selectPage(page, new LambdaQueryWrapper<ExamAttemptEntity>()
            .eq(ExamAttemptEntity::getStudentId, currentUser.getId())
            .orderByDesc(ExamAttemptEntity::getId));
        return buildScorePage(result.getRecords(), result.getTotal());
    }

    private PageData<ApiDtos.ScoreRecordDto> buildScorePage(List<ExamAttemptEntity> attempts, long total) {
        if (attempts.isEmpty()) {
            return new PageData<>(Collections.emptyList(), total);
        }

        List<Long> examIds = attempts.stream().map(ExamAttemptEntity::getExamId).toList();
        Map<Long, ExamEntity> examMap = examIds.isEmpty()
            ? Map.of()
            : examMapper.selectBatchIds(examIds).stream()
                .collect(java.util.stream.Collectors.toMap(ExamEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Long> studentIds = attempts.stream().map(ExamAttemptEntity::getStudentId).toList();
        Map<Long, UserEntity> studentMap = studentIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(studentIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Long> classIds = studentMap.values().stream()
            .map(UserEntity::getClassId)
            .filter(Objects::nonNull)
            .toList();
        Map<Long, ClassRoomEntity> classMap = classIds.isEmpty()
            ? Map.of()
            : classRoomMapper.selectBatchIds(classIds).stream()
                .collect(java.util.stream.Collectors.toMap(ClassRoomEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<ApiDtos.ScoreRecordDto> items = attempts.stream()
            .map(item -> viewAssembler.toScoreRecord(item, examMap, studentMap, classMap))
            .toList();
        return new PageData<>(items, total);
    }

    private List<Long> scopedStudentIds(UserEntity currentUser) {
        if (!"teacher".equals(currentUser.getRole())) {
            return List.of();
        }
        List<Long> classIds = classRoomMapper.selectList(new LambdaQueryWrapper<ClassRoomEntity>()
                .eq(ClassRoomEntity::getTeacherId, currentUser.getId()))
            .stream()
            .map(ClassRoomEntity::getId)
            .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRole, "student")
                .in(UserEntity::getClassId, classIds))
            .stream()
            .map(UserEntity::getId)
            .toList();
    }
}
