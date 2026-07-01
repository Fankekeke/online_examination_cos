package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import com.onlineexamsystem.backend.common.util.JsonUtils;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.AttemptAnswerEntity;
import com.onlineexamsystem.backend.entity.ExamAttemptEntity;
import com.onlineexamsystem.backend.entity.ExamEntity;
import com.onlineexamsystem.backend.entity.ExamQuestionEntity;
import com.onlineexamsystem.backend.entity.QuestionEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.AttemptAnswerMapper;
import com.onlineexamsystem.backend.mapper.ExamAttemptMapper;
import com.onlineexamsystem.backend.mapper.ExamMapper;
import com.onlineexamsystem.backend.mapper.ExamQuestionMapper;
import com.onlineexamsystem.backend.mapper.QuestionMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAttemptMapper examAttemptMapper;
    private final AttemptAnswerMapper attemptAnswerMapper;
    private final QuestionMapper questionMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public StudentService(ExamMapper examMapper,
                          ExamQuestionMapper examQuestionMapper,
                          ExamAttemptMapper examAttemptMapper,
                          AttemptAnswerMapper attemptAnswerMapper,
                          QuestionMapper questionMapper,
                          ReferenceLookupService referenceLookupService,
                          ViewAssembler viewAssembler) {
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examAttemptMapper = examAttemptMapper;
        this.attemptAnswerMapper = attemptAnswerMapper;
        this.questionMapper = questionMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.ExamDto> availableExams(UserEntity currentUser, HttpServletRequest request) {
        return listStudentExams(currentUser, request, false);
    }

    public PageData<ApiDtos.ExamDto> practiceExams(UserEntity currentUser, HttpServletRequest request) {
        return listStudentExams(currentUser, request, true);
    }

    @Transactional
    public ApiDtos.StartExamDataDto startExam(UserEntity currentUser, Long examId) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }
        LocalDateTime now = DateTimeUtils.utcNow();
        Availability availability = checkAvailability(exam, now);
        if (!availability.available()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, availability.message());
        }

        AttemptStats stats = attemptStats(examId, currentUser.getId());
        ExamAttemptEntity attempt = stats.inProgressAttempt();
        if (attempt == null) {
            if (stats.submittedCount() >= defaultAttemptLimit(exam)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "该考试最多可作答" + defaultAttemptLimit(exam) + "次，已达上限");
            }
            attempt = new ExamAttemptEntity();
            attempt.setExamId(examId);
            attempt.setStudentId(currentUser.getId());
            attempt.setStartTime(now);
            attempt.setStatus("in_progress");
            attempt.setScore(0D);
            examAttemptMapper.insert(attempt);
        }

        List<ExamQuestionEntity> rows = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestionEntity>()
            .eq(ExamQuestionEntity::getExamId, examId)
            .orderByAsc(ExamQuestionEntity::getSortOrder));
        if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(rows, new Random(attempt.getId()));
        }
        Map<Long, QuestionEntity> questionMap = questionMapper.selectBatchIds(rows.stream().map(ExamQuestionEntity::getQuestionId).toList())
            .stream().collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<ApiDtos.ExamPaperQuestionDto> questions = rows.stream()
            .map(row -> questionMap.get(row.getQuestionId()))
            .filter(Objects::nonNull)
            .map(viewAssembler::toExamPaperQuestion)
            .toList();

        int remainingAttempts = Math.max(defaultAttemptLimit(exam) - stats.submittedCount(), 0);
        return new ApiDtos.StartExamDataDto(
            attempt.getId(),
            toExam(exam),
            questions,
            DateTimeUtils.formatDateTime(attempt.getStartTime()),
            stats.submittedCount(),
            remainingAttempts
        );
    }

    @Transactional
    public SubmitExamOutcome submitExam(UserEntity currentUser, Long attemptId, Map<String, Object> payload) {
        ExamAttemptEntity attempt = examAttemptMapper.selectById(attemptId);
        if (attempt == null || !Objects.equals(attempt.getStudentId(), currentUser.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "作答记录不存在");
        }
        ExamEntity exam = examMapper.selectById(attempt.getExamId());

        if ("submitted".equals(attempt.getStatus())) {
            int submittedCount = submittedAttemptCount(attempt.getExamId(), currentUser.getId());
            int remainingAttempts = Math.max(defaultAttemptLimit(exam) - submittedCount, 0);
            return new SubmitExamOutcome(
                new ApiDtos.SubmitExamResultDto(
                    attempt.getScore() == null ? 0 : attempt.getScore().intValue(),
                    attempt.getScore() != null && attempt.getScore() >= exam.getPassScore(),
                    submittedCount,
                    remainingAttempts
                ),
                "已提交"
            );
        }

        Object rawAnswers = payload.get("answers");
        if (!(rawAnswers instanceof List<?> answerList)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "答案格式错误");
        }

        Map<Long, List<String>> answerMap = new HashMap<>();
        for (Object item : answerList) {
            if (item instanceof Map<?, ?> map) {
                Long questionId = PayloadUtils.longValue(map.get("questionId"), null);
                if (questionId != null) {
                    answerMap.put(questionId, PayloadUtils.stringList(map.get("answer")));
                }
            }
        }

        attemptAnswerMapper.delete(new LambdaQueryWrapper<AttemptAnswerEntity>().eq(AttemptAnswerEntity::getAttemptId, attemptId));
        double totalScore = 0;
        List<ExamQuestionEntity> rows = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestionEntity>()
            .eq(ExamQuestionEntity::getExamId, attempt.getExamId()));
        Map<Long, QuestionEntity> questionMap = questionMapper.selectBatchIds(rows.stream().map(ExamQuestionEntity::getQuestionId).toList())
            .stream().collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (ExamQuestionEntity row : rows) {
            QuestionEntity question = questionMap.get(row.getQuestionId());
            if (question == null) {
                continue;
            }
            List<String> userAnswer = answerMap.getOrDefault(question.getId(), List.of());
            boolean correct = isCorrect(question, userAnswer);
            double questionScore = correct ? question.getScore() : 0;
            totalScore += questionScore;

            AttemptAnswerEntity answer = new AttemptAnswerEntity();
            answer.setAttemptId(attemptId);
            answer.setQuestionId(question.getId());
            answer.setAnswerJson(JsonUtils.toJson(userAnswer));
            answer.setIsCorrect(correct);
            answer.setScore(questionScore);
            attemptAnswerMapper.insert(answer);
        }

        attempt.setScore(totalScore);
        attempt.setStatus("submitted");
        attempt.setSubmitTime(DateTimeUtils.utcNow());
        examAttemptMapper.updateById(attempt);

        int submittedCount = submittedAttemptCount(attempt.getExamId(), currentUser.getId());
        int remainingAttempts = Math.max(defaultAttemptLimit(exam) - submittedCount, 0);
        return new SubmitExamOutcome(
            new ApiDtos.SubmitExamResultDto(
                (int) totalScore,
                totalScore >= exam.getPassScore(),
                submittedCount,
                remainingAttempts
            ),
            "提交成功"
        );
    }

    private PageData<ApiDtos.ExamDto> listStudentExams(UserEntity currentUser, HttpServletRequest request, boolean practiceOnly) {
        String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword").trim();
        String examType = request.getParameter("type") == null ? "" : request.getParameter("type").trim();
        Page<ExamEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<ExamEntity> wrapper = new LambdaQueryWrapper<ExamEntity>()
            .eq(ExamEntity::getStatus, "published");
        if (practiceOnly) {
            wrapper.eq(ExamEntity::getType, "practice");
        } else if (!examType.isEmpty()) {
            wrapper.eq(ExamEntity::getType, examType);
        }
        if (!keyword.isEmpty()) {
            wrapper.like(ExamEntity::getTitle, keyword);
        }
        wrapper.orderByDesc(ExamEntity::getId);

        Page<ExamEntity> result = examMapper.selectPage(page, wrapper);
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(
            result.getRecords().stream().map(ExamEntity::getCreatorId).filter(Objects::nonNull).toList()
        );
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByExamIds(
            result.getRecords().stream().map(ExamEntity::getId).toList()
        );
        Map<Long, List<ExamAttemptEntity>> attemptMap = attemptsByExamIds(result.getRecords().stream().map(ExamEntity::getId).toList(), currentUser.getId());

        List<ApiDtos.ExamDto> items = result.getRecords().stream()
            .map(exam -> {
                List<ExamAttemptEntity> attempts = attemptMap.getOrDefault(exam.getId(), List.of());
                int submittedCount = (int) attempts.stream().filter(item -> "submitted".equals(item.getStatus())).count();
                ExamAttemptEntity inProgress = attempts.stream().filter(item -> "in_progress".equals(item.getStatus())).findFirst().orElse(null);
                int remainingAttempts = Math.max(defaultAttemptLimit(exam) - submittedCount, 0);
                Availability availability = checkAvailability(exam, DateTimeUtils.utcNow());
                boolean canStart = inProgress != null || (availability.available() && remainingAttempts > 0);
                String availabilityText = availability.message();
                if (inProgress != null) {
                    availabilityText = "继续作答";
                } else if (availability.available() && remainingAttempts <= 0) {
                    availabilityText = "次数已用完";
                }
                return viewAssembler.toExamWithStudentState(
                    exam,
                    creatorMap,
                    questionCountMap,
                    submittedCount,
                    remainingAttempts,
                    inProgress != null,
                    canStart,
                    availabilityText
                );
            })
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    private Map<Long, List<ExamAttemptEntity>> attemptsByExamIds(List<Long> examIds, Long studentId) {
        if (examIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>()
                .eq(ExamAttemptEntity::getStudentId, studentId)
                .in(ExamAttemptEntity::getExamId, examIds))
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(ExamAttemptEntity::getExamId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
    }

    private AttemptStats attemptStats(Long examId, Long studentId) {
        List<ExamAttemptEntity> attempts = examAttemptMapper.selectList(new LambdaQueryWrapper<ExamAttemptEntity>()
            .eq(ExamAttemptEntity::getExamId, examId)
            .eq(ExamAttemptEntity::getStudentId, studentId));
        int submittedCount = (int) attempts.stream().filter(item -> "submitted".equals(item.getStatus())).count();
        ExamAttemptEntity inProgress = attempts.stream().filter(item -> "in_progress".equals(item.getStatus())).findFirst().orElse(null);
        return new AttemptStats(submittedCount, inProgress);
    }

    private int submittedAttemptCount(Long examId, Long studentId) {
        return examAttemptMapper.selectCount(new LambdaQueryWrapper<ExamAttemptEntity>()
            .eq(ExamAttemptEntity::getExamId, examId)
            .eq(ExamAttemptEntity::getStudentId, studentId)
            .eq(ExamAttemptEntity::getStatus, "submitted")).intValue();
    }

    private Availability checkAvailability(ExamEntity exam, LocalDateTime now) {
        if (!"published".equals(exam.getStatus())) {
            return new Availability(false, "考试未发布");
        }
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return new Availability(false, "未到开考时间");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            return new Availability(false, "考试已结束");
        }
        return new Availability(true, "可作答");
    }

    private boolean isCorrect(QuestionEntity question, List<String> answerList) {
        List<String> standard = JsonUtils.parseStringList(question.getAnswerJson()).stream().map(String::trim).toList();
        if ("text".equals(question.getType())) {
            if (answerList.isEmpty() || standard.isEmpty()) {
                return false;
            }
            String userAnswer = answerList.get(0);
            String target = standard.get(0);
            return target.contains(userAnswer) || userAnswer.contains(target);
        }
        List<String> normalizedUser = new ArrayList<>(answerList.stream().map(String::trim).toList());
        List<String> normalizedStandard = new ArrayList<>(standard);
        Collections.sort(normalizedUser);
        Collections.sort(normalizedStandard);
        return normalizedStandard.equals(normalizedUser);
    }

    private int defaultAttemptLimit(ExamEntity exam) {
        return exam.getAttemptLimit() == null ? 1 : exam.getAttemptLimit();
    }

    private ApiDtos.ExamDto toExam(ExamEntity exam) {
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(List.of(exam.getCreatorId()));
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByExamIds(List.of(exam.getId()));
        return viewAssembler.toExam(exam, creatorMap, questionCountMap);
    }

    private record Availability(boolean available, String message) {
    }

    private record AttemptStats(int submittedCount, ExamAttemptEntity inProgressAttempt) {
    }

    public record SubmitExamOutcome(ApiDtos.SubmitExamResultDto result, String message) {
    }
}
