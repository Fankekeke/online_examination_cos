package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.ExamAttemptEntity;
import com.onlineexamsystem.backend.entity.ExamEntity;
import com.onlineexamsystem.backend.entity.ExamQuestionEntity;
import com.onlineexamsystem.backend.entity.QuestionEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.ExamAttemptMapper;
import com.onlineexamsystem.backend.mapper.ExamMapper;
import com.onlineexamsystem.backend.mapper.ExamQuestionMapper;
import com.onlineexamsystem.backend.mapper.QuestionMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAttemptMapper examAttemptMapper;
    private final QuestionMapper questionMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public ExamService(ExamMapper examMapper,
                       ExamQuestionMapper examQuestionMapper,
                       ExamAttemptMapper examAttemptMapper,
                       QuestionMapper questionMapper,
                       ReferenceLookupService referenceLookupService,
                       ViewAssembler viewAssembler) {
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examAttemptMapper = examAttemptMapper;
        this.questionMapper = questionMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.ExamDto> listExams(HttpServletRequest request) {
        String keyword = trim(request.getParameter("keyword"));
        String examType = trim(request.getParameter("type"));
        String status = trim(request.getParameter("status"));
        Page<ExamEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<ExamEntity> wrapper = new LambdaQueryWrapper<>();
        if (!keyword.isEmpty()) {
            wrapper.like(ExamEntity::getTitle, keyword);
        }
        if (!examType.isEmpty()) {
            wrapper.eq(ExamEntity::getType, examType);
        }
        if (!status.isEmpty()) {
            wrapper.eq(ExamEntity::getStatus, status);
        }
        wrapper.orderByDesc(ExamEntity::getId);

        Page<ExamEntity> result = examMapper.selectPage(page, wrapper);
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(
            result.getRecords().stream().map(ExamEntity::getCreatorId).filter(Objects::nonNull).toList()
        );
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByExamIds(
            result.getRecords().stream().map(ExamEntity::getId).toList()
        );
        List<ApiDtos.ExamDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toExam(item, creatorMap, questionCountMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.ExamDto createExam(UserEntity currentUser, Map<String, Object> payload) {
        String title = trim(payload.get("title"));
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "考试名称不能为空");
        }

        ExamEntity exam = new ExamEntity();
        exam.setTitle(title);
        exam.setDescription(orEmpty(payload.get("description")));
        exam.setType(trim(payload.getOrDefault("type", "formal")));
        exam.setDurationMinutes(PayloadUtils.integerValue(payload.get("durationMinutes"), 120));
        exam.setTotalScore(PayloadUtils.integerValue(payload.get("totalScore"), 100));
        exam.setPassScore(PayloadUtils.integerValue(payload.get("passScore"), 60));
        exam.setAttemptLimit(parseAttemptLimit(payload.get("attemptLimit"), 1));
        exam.setShuffleQuestions(parseBool(payload.get("shuffleQuestions"), false));
        exam.setStatus(trim(payload.getOrDefault("status", "draft")));
        exam.setStartTime(parseDateTime(payload.get("startTime")));
        exam.setEndTime(parseDateTime(payload.get("endTime")));
        exam.setCreatorId(currentUser.getId());
        examMapper.insert(exam);
        return toExam(examMapper.selectById(exam.getId()));
    }

    public ApiDtos.ExamDto updateExam(Long examId, Map<String, Object> payload) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }

        String title = trim(payload.getOrDefault("title", exam.getTitle()));
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "考试名称不能为空");
        }

        exam.setTitle(title);
        exam.setDescription(orEmpty(payload.get("description")));
        exam.setType(trim(payload.getOrDefault("type", exam.getType())));
        exam.setDurationMinutes(PayloadUtils.integerValue(payload.get("durationMinutes"), exam.getDurationMinutes()));
        exam.setTotalScore(PayloadUtils.integerValue(payload.get("totalScore"), exam.getTotalScore()));
        exam.setPassScore(PayloadUtils.integerValue(payload.get("passScore"), exam.getPassScore()));
        exam.setAttemptLimit(parseAttemptLimit(payload.get("attemptLimit"), exam.getAttemptLimit()));
        exam.setShuffleQuestions(parseBool(payload.get("shuffleQuestions"), Boolean.TRUE.equals(exam.getShuffleQuestions())));
        exam.setStatus(trim(payload.getOrDefault("status", exam.getStatus())));
        exam.setStartTime(parseDateTime(payload.get("startTime")));
        exam.setEndTime(parseDateTime(payload.get("endTime")));
        examMapper.updateById(exam);
        return toExam(examMapper.selectById(examId));
    }

    @Transactional
    public boolean deleteExam(Long examId) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }
        if (examAttemptMapper.selectCount(new LambdaQueryWrapper<ExamAttemptEntity>().eq(ExamAttemptEntity::getExamId, examId)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "已有作答记录，不能删除");
        }
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestionEntity>().eq(ExamQuestionEntity::getExamId, examId));
        examMapper.deleteById(examId);
        return true;
    }

    public boolean publishExam(Long examId) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }
        if (examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestionEntity>().eq(ExamQuestionEntity::getExamId, examId)) == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先组卷后再发布");
        }
        exam.setStatus("published");
        examMapper.updateById(exam);
        return true;
    }

    public Map<String, Object> examPaper(Long examId, UserEntity currentUser) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }
        List<ExamQuestionEntity> rows = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestionEntity>()
            .eq(ExamQuestionEntity::getExamId, examId)
            .orderByAsc(ExamQuestionEntity::getSortOrder));
        List<Long> questionIds = rows.stream().map(ExamQuestionEntity::getQuestionId).toList();
        Map<Long, QuestionEntity> questionMap = questionIds.isEmpty()
            ? Map.of()
            : questionMapper.selectBatchIds(questionIds).stream()
                .collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<?> questions = rows.stream()
            .map(row -> questionMap.get(row.getQuestionId()))
            .filter(Objects::nonNull)
            .map(question -> "student".equals(currentUser.getRole())
                ? viewAssembler.toExamPaperQuestion(question)
                : viewAssembler.toQuestionItem(question))
            .toList();
        return Map.of(
            "exam", toExam(exam),
            "questions", questions
        );
    }

    @Transactional
    public boolean bindQuestions(Long examId, Map<String, Object> payload) {
        ExamEntity exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "考试不存在");
        }
        Object rawQuestionIds = payload.get("questionIds");
        if (!(rawQuestionIds instanceof List<?> list)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数格式不正确");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Object item : list) {
            Long questionId = PayloadUtils.longValue(item, null);
            if (questionId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "题目ID格式不正确");
            }
            uniqueIds.add(questionId);
        }
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestionEntity>().eq(ExamQuestionEntity::getExamId, examId));
        int index = 1;
        for (Long questionId : uniqueIds) {
            QuestionEntity question = questionMapper.selectById(questionId);
            if (question != null) {
                ExamQuestionEntity row = new ExamQuestionEntity();
                row.setExamId(examId);
                row.setQuestionId(question.getId());
                row.setSortOrder(index++);
                examQuestionMapper.insert(row);
            }
        }
        return true;
    }

    private ApiDtos.ExamDto toExam(ExamEntity exam) {
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(List.of(exam.getCreatorId()));
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByExamIds(List.of(exam.getId()));
        return viewAssembler.toExam(exam, creatorMap, questionCountMap);
    }

    private Integer parseAttemptLimit(Object rawValue, Integer fallback) {
        Integer value = PayloadUtils.integerValue(rawValue, fallback);
        if (value == null || value < 1 || value > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "可考次数需在1-20之间");
        }
        return value;
    }

    private Boolean parseBool(Object rawValue, boolean fallback) {
        return PayloadUtils.booleanValue(rawValue, fallback);
    }

    private LocalDateTime parseDateTime(Object rawValue) {
        try {
            return DateTimeUtils.parseDateTime(rawValue == null ? null : String.valueOf(rawValue));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String orEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
