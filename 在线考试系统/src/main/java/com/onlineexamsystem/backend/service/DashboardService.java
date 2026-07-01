package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import com.onlineexamsystem.backend.common.util.ScoreUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.ClassRoomEntity;
import com.onlineexamsystem.backend.entity.ExamAttemptEntity;
import com.onlineexamsystem.backend.entity.ExamEntity;
import com.onlineexamsystem.backend.entity.NoticeEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.ClassRoomMapper;
import com.onlineexamsystem.backend.mapper.ExamAttemptMapper;
import com.onlineexamsystem.backend.mapper.ExamMapper;
import com.onlineexamsystem.backend.mapper.NoticeMapper;
import com.onlineexamsystem.backend.mapper.UserMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ExamMapper examMapper;
    private final ExamAttemptMapper examAttemptMapper;
    private final UserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public DashboardService(ExamMapper examMapper,
                            ExamAttemptMapper examAttemptMapper,
                            UserMapper userMapper,
                            NoticeMapper noticeMapper,
                            ClassRoomMapper classRoomMapper,
                            ReferenceLookupService referenceLookupService,
                            ViewAssembler viewAssembler) {
        this.examMapper = examMapper;
        this.examAttemptMapper = examAttemptMapper;
        this.userMapper = userMapper;
        this.noticeMapper = noticeMapper;
        this.classRoomMapper = classRoomMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public ApiDtos.DashboardPayloadDto roleDashboard(UserEntity currentUser, String role) {
        if (!isRoleDashboardAllowed(currentUser, role)) {
            return new ApiDtos.DashboardPayloadDto(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<ExamAttemptEntity> attemptScope;
        List<ApiDtos.DashboardCardDto> cards;
        List<ApiDtos.LabelValueDto> examStat;

        if ("admin".equals(role)) {
            attemptScope = examAttemptMapper.selectList(null);
            cards = List.of(
                new ApiDtos.DashboardCardDto("exam_total", "考试总量", examMapper.selectCount(null), 8),
                new ApiDtos.DashboardCardDto("exam_published", "发布中考试", examMapper.selectCount(new LambdaQueryWrapper<ExamEntity>().eq(ExamEntity::getStatus, "published")), 5),
                new ApiDtos.DashboardCardDto("user_students", "学生人数", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "student")), 3),
                new ApiDtos.DashboardCardDto("attempt_total", "已交卷次数", countSubmitted(attemptScope), 9)
            );
            examStat = examStatusStat(examMapper.selectList(null));
        } else if ("teacher".equals(role)) {
            List<Long> classIds = managedClassIds(currentUser.getId());
            List<Long> studentIds = studentIdsByClassIds(classIds);
            long studentCount = classIds.isEmpty()
                ? 0
                : userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "student").in(UserEntity::getClassId, classIds));
            List<ExamEntity> examScope = examMapper.selectList(new LambdaQueryWrapper<ExamEntity>().eq(ExamEntity::getCreatorId, currentUser.getId()));
            attemptScope = studentIds.isEmpty()
                ? List.of()
                : examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>().in(ExamAttemptEntity::getStudentId, studentIds));
            cards = List.of(
                new ApiDtos.DashboardCardDto("my_exam", "我创建的考试", examScope.size(), 6),
                new ApiDtos.DashboardCardDto("my_publish", "已发布考试", examScope.stream().filter(item -> "published".equals(item.getStatus())).count(), 4),
                new ApiDtos.DashboardCardDto("my_students", "班级学生数", studentCount, 2),
                new ApiDtos.DashboardCardDto("my_attempt", "班级作答次数", countSubmitted(attemptScope), 7)
            );
            examStat = examStatusStat(examScope);
        } else {
            attemptScope = examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>().eq(ExamAttemptEntity::getStudentId, currentUser.getId()));
            List<ExamAttemptEntity> submitted = attemptScope.stream().filter(item -> "submitted".equals(item.getStatus())).toList();
            long submittedCount = submitted.size();
            double avgScore = submittedCount == 0 ? 0 : submitted.stream().mapToDouble(item -> item.getScore() == null ? 0 : item.getScore()).average().orElse(0);
            Map<Long, ExamEntity> examMap = examMapper.selectBatchIds(submitted.stream().map(ExamAttemptEntity::getExamId).toList())
                .stream().collect(java.util.stream.Collectors.toMap(ExamEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
            long passCount = submitted.stream().filter(item -> {
                ExamEntity exam = examMap.get(item.getExamId());
                return exam != null && item.getScore() != null && item.getScore() >= exam.getPassScore();
            }).count();
            cards = List.of(
                new ApiDtos.DashboardCardDto("my_submit", "已完成考试", submittedCount, 5),
                new ApiDtos.DashboardCardDto("avg_score", "平均成绩", (long) avgScore, 4),
                new ApiDtos.DashboardCardDto("pass_count", "通过次数", passCount, 6),
                new ApiDtos.DashboardCardDto("pending", "进行中考试", attemptScope.stream().filter(item -> "in_progress".equals(item.getStatus())).count(), -1)
            );
            examStat = List.of(
                new ApiDtos.LabelValueDto("正式考试", examMapper.selectCount(new LambdaQueryWrapper<ExamEntity>().eq(ExamEntity::getType, "formal").eq(ExamEntity::getStatus, "published"))),
                new ApiDtos.LabelValueDto("模拟练习", examMapper.selectCount(new LambdaQueryWrapper<ExamEntity>().eq(ExamEntity::getType, "practice").eq(ExamEntity::getStatus, "published")))
            );
        }

        return new ApiDtos.DashboardPayloadDto(
            cards,
            scoreTrend(attemptScope, 7),
            examStat,
            segmentStats(attemptScope),
            latestNotices(5)
        );
    }

    public ApiDtos.BigScreenDataDto bigScreen() {
        List<ApiDtos.DashboardCardDto> overview = List.of(
            new ApiDtos.DashboardCardDto("student_total", "学生总数", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "student")), 3),
            new ApiDtos.DashboardCardDto("teacher_total", "教师总数", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "teacher")), 1),
            new ApiDtos.DashboardCardDto("exam_total", "考试总数", examMapper.selectCount(null), 5),
            new ApiDtos.DashboardCardDto("submitted_total", "累计交卷", examAttemptMapper.selectCount(new LambdaQueryWrapper<ExamAttemptEntity>().eq(ExamAttemptEntity::getStatus, "submitted")), 7)
        );

        List<ClassRoomEntity> classes = classRoomMapper.selectList(null);
        Map<Long, ExamEntity> examMap = examMapper.selectBatchIds(new LinkedHashSet<>(examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>()
                .eq(ExamAttemptEntity::getStatus, "submitted")).stream().map(ExamAttemptEntity::getExamId).toList()))
            .stream().collect(java.util.stream.Collectors.toMap(ExamEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<ApiDtos.PassRateDto> passRateByClass = new ArrayList<>();
        for (ClassRoomEntity classRoom : classes) {
            List<Long> studentIds = studentIdsByClassIds(List.of(classRoom.getId()));
            List<ExamAttemptEntity> attempts = studentIds.isEmpty()
                ? List.of()
                : examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>()
                    .in(ExamAttemptEntity::getStudentId, studentIds)
                    .eq(ExamAttemptEntity::getStatus, "submitted"));
            double passRate = 0;
            if (!attempts.isEmpty()) {
                long passCount = attempts.stream().filter(item -> {
                    ExamEntity exam = examMap.get(item.getExamId());
                    return exam != null && item.getScore() != null && item.getScore() >= exam.getPassScore();
                }).count();
                passRate = Math.round(passCount * 10000D / attempts.size()) / 100D;
            }
            passRateByClass.add(new ApiDtos.PassRateDto(classRoom.getName(), passRate));
        }

        List<ApiDtos.AvgScoreTrendDto> trend = scoreTrendForBigScreen();
        List<ApiDtos.ExamHeatDto> examHeat = examHeat();
        List<ApiDtos.RoleCountDto> onlineStatus = List.of(
            new ApiDtos.RoleCountDto("管理员", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "admin").eq(UserEntity::getStatus, "active"))),
            new ApiDtos.RoleCountDto("教师", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "teacher").eq(UserEntity::getStatus, "active"))),
            new ApiDtos.RoleCountDto("学生", userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "student").eq(UserEntity::getStatus, "active")))
        );

        return new ApiDtos.BigScreenDataDto(
            overview,
            passRateByClass,
            trend,
            examHeat,
            onlineStatus,
            DateTimeUtils.formatDateTime(DateTimeUtils.utcNow())
        );
    }

    private boolean isRoleDashboardAllowed(UserEntity currentUser, String role) {
        if (Objects.equals(role, currentUser.getRole())) {
            return true;
        }
        if ("admin".equals(currentUser.getRole()) && "teacher".equals(role)) {
            return true;
        }
        return "teacher".equals(currentUser.getRole()) && "teacher".equals(role);
    }

    private List<ApiDtos.DateValueDto> scoreTrend(List<ExamAttemptEntity> attempts, int days) {
        Map<String, List<Double>> dateMap = new LinkedHashMap<>();
        for (LocalDate date : DateTimeUtils.recentDates(days)) {
            dateMap.put(date.format(DateTimeUtils.MONTH_DAY_FORMATTER), new ArrayList<>());
        }
        for (ExamAttemptEntity attempt : attempts) {
            if (!"submitted".equals(attempt.getStatus()) || attempt.getCreatedAt() == null) {
                continue;
            }
            String label = attempt.getCreatedAt().format(DateTimeUtils.MONTH_DAY_FORMATTER);
            if (dateMap.containsKey(label)) {
                dateMap.get(label).add(attempt.getScore() == null ? 0 : attempt.getScore());
            }
        }
        return dateMap.entrySet().stream()
            .map(entry -> new ApiDtos.DateValueDto(entry.getKey(), entry.getValue().isEmpty()
                ? 0
                : Math.round(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100D) / 100D))
            .toList();
    }

    private List<ApiDtos.LabelValueDto> examStatusStat(List<ExamEntity> exams) {
        Map<String, String> labels = Map.of("draft", "草稿", "published", "已发布", "closed", "已结束");
        List<ApiDtos.LabelValueDto> data = new ArrayList<>();
        labels.forEach((key, label) -> data.add(new ApiDtos.LabelValueDto(label, exams.stream().filter(item -> key.equals(item.getStatus())).count())));
        return data;
    }

    private List<ApiDtos.SegmentItemDto> segmentStats(List<ExamAttemptEntity> attempts) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-59", 0L);
        buckets.put("60-69", 0L);
        buckets.put("70-79", 0L);
        buckets.put("80-89", 0L);
        buckets.put("90-100", 0L);
        for (ExamAttemptEntity attempt : attempts) {
            if (!"submitted".equals(attempt.getStatus())) {
                continue;
            }
            String range = ScoreUtils.scoreSegment(attempt.getScore() == null ? 0 : attempt.getScore());
            buckets.put(range, buckets.get(range) + 1);
        }
        return buckets.entrySet().stream()
            .map(entry -> new ApiDtos.SegmentItemDto(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<ApiDtos.NoticeItemDto> latestNotices(int limit) {
        List<NoticeEntity> notices = noticeMapper.selectList(new LambdaQueryWrapper<NoticeEntity>()
            .orderByDesc(NoticeEntity::getIsPinned)
            .orderByDesc(NoticeEntity::getPublishedAt)
            .last("LIMIT " + limit));
        Map<Long, UserEntity> publisherMap = referenceLookupService.loadUsers(notices.stream().map(NoticeEntity::getPublisherId).filter(Objects::nonNull).toList());
        return notices.stream().map(item -> viewAssembler.toNotice(item, publisherMap)).toList();
    }

    private long countSubmitted(List<ExamAttemptEntity> attempts) {
        return attempts.stream().filter(item -> "submitted".equals(item.getStatus())).count();
    }

    private List<Long> managedClassIds(Long teacherId) {
        return classRoomMapper.selectList(new LambdaQueryWrapper<ClassRoomEntity>().eq(ClassRoomEntity::getTeacherId, teacherId))
            .stream()
            .map(ClassRoomEntity::getId)
            .toList();
    }

    private List<Long> studentIdsByClassIds(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRole, "student").in(UserEntity::getClassId, classIds))
            .stream().map(UserEntity::getId).toList();
    }

    private List<ApiDtos.AvgScoreTrendDto> scoreTrendForBigScreen() {
        Map<String, List<Double>> dateMap = new LinkedHashMap<>();
        for (LocalDate date : DateTimeUtils.recentDates(10)) {
            dateMap.put(date.format(DateTimeUtils.MONTH_DAY_FORMATTER), new ArrayList<>());
        }
        for (ExamAttemptEntity attempt : examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>().eq(ExamAttemptEntity::getStatus, "submitted"))) {
            if (attempt.getCreatedAt() == null) {
                continue;
            }
            String label = attempt.getCreatedAt().format(DateTimeUtils.MONTH_DAY_FORMATTER);
            if (dateMap.containsKey(label)) {
                dateMap.get(label).add(attempt.getScore() == null ? 0 : attempt.getScore());
            }
        }
        return dateMap.entrySet().stream()
            .map(entry -> new ApiDtos.AvgScoreTrendDto(entry.getKey(), entry.getValue().isEmpty()
                ? 0
                : Math.round(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100D) / 100D))
            .toList();
    }

    private List<ApiDtos.ExamHeatDto> examHeat() {
        Map<Long, Long> countMap = examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>().eq(ExamAttemptEntity::getStatus, "submitted"))
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(ExamAttemptEntity::getExamId, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<Long, ExamEntity> examMap = examMapper.selectBatchIds(countMap.keySet())
            .stream().collect(java.util.stream.Collectors.toMap(ExamEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        return countMap.entrySet().stream()
            .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
            .limit(8)
            .map(entry -> new ApiDtos.ExamHeatDto(examMap.get(entry.getKey()) == null ? "未知考试" : examMap.get(entry.getKey()).getTitle(), entry.getValue()))
            .toList();
    }
}
