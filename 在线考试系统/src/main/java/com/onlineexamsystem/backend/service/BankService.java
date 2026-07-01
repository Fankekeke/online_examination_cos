package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.JsonUtils;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.QuestionBankEntity;
import com.onlineexamsystem.backend.entity.QuestionEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.QuestionBankMapper;
import com.onlineexamsystem.backend.mapper.QuestionMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {

    private static final Set<String> QUESTION_TYPES = Set.of("single", "multi", "judge", "text");

    private final QuestionBankMapper questionBankMapper;
    private final QuestionMapper questionMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;
    private final LegacyCompatibilityService legacyCompatibilityService;

    public BankService(QuestionBankMapper questionBankMapper,
                       QuestionMapper questionMapper,
                       ReferenceLookupService referenceLookupService,
                       ViewAssembler viewAssembler,
                       LegacyCompatibilityService legacyCompatibilityService) {
        this.questionBankMapper = questionBankMapper;
        this.questionMapper = questionMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
        this.legacyCompatibilityService = legacyCompatibilityService;
    }

    public PageData<ApiDtos.QuestionBankDto> listBanks(HttpServletRequest request) {
        String keyword = trim(request.getParameter("keyword"));
        String subject = trim(request.getParameter("subject"));
        Page<QuestionBankEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<QuestionBankEntity> wrapper = new LambdaQueryWrapper<>();
        if (!keyword.isEmpty()) {
            wrapper.like(QuestionBankEntity::getName, keyword);
        }
        if (!subject.isEmpty()) {
            wrapper.like(QuestionBankEntity::getSubject, subject);
        }
        wrapper.orderByDesc(QuestionBankEntity::getId);

        Page<QuestionBankEntity> result = questionBankMapper.selectPage(page, wrapper);
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(
            result.getRecords().stream().map(QuestionBankEntity::getCreatorId).filter(Objects::nonNull).toList()
        );
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByBankIds(
            result.getRecords().stream().map(QuestionBankEntity::getId).toList()
        );
        List<ApiDtos.QuestionBankDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toQuestionBank(item, creatorMap, questionCountMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.QuestionBankDto createBank(UserEntity currentUser, Map<String, Object> payload) {
        String name = trim(payload.get("name"));
        String subject = trim(payload.get("subject"));
        if (name.isEmpty() || subject.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题库名称和学科不能为空");
        }
        QuestionBankEntity bank = new QuestionBankEntity();
        bank.setName(name);
        bank.setSubject(subject);
        bank.setDifficulty(trim(payload.getOrDefault("difficulty", "中等")));
        bank.setDescription(orEmpty(payload.get("description")));
        bank.setCreatorId(currentUser.getId());
        questionBankMapper.insert(bank);
        return toQuestionBank(questionBankMapper.selectById(bank.getId()));
    }

    public ApiDtos.QuestionBankDto updateBank(Long bankId, Map<String, Object> payload) {
        QuestionBankEntity bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "题库不存在");
        }
        String name = trim(payload.getOrDefault("name", bank.getName()));
        String subject = trim(payload.getOrDefault("subject", bank.getSubject()));
        bank.setName(name.isEmpty() ? bank.getName() : name);
        bank.setSubject(subject.isEmpty() ? bank.getSubject() : subject);
        String difficulty = trim(payload.getOrDefault("difficulty", bank.getDifficulty()));
        bank.setDifficulty(difficulty.isEmpty() ? bank.getDifficulty() : difficulty);
        bank.setDescription(orEmpty(payload.get("description")));
        questionBankMapper.updateById(bank);
        return toQuestionBank(questionBankMapper.selectById(bank.getId()));
    }

    @Transactional
    public boolean deleteBank(Long bankId) {
        QuestionBankEntity bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "题库不存在");
        }
        List<QuestionEntity> questions = questionMapper.selectList(new LambdaQueryWrapper<QuestionEntity>().eq(QuestionEntity::getBankId, bankId));
        List<Long> questionIds = questions.stream().map(QuestionEntity::getId).toList();
        if (legacyCompatibilityService.hasCurrentQuestionReferences(questionIds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题库存在关联作答或考试记录，暂无法删除，请先清理关联数据");
        }
        legacyCompatibilityService.cleanupLegacyQuestionLinks(questionIds);
        if (!questionIds.isEmpty()) {
            questionMapper.delete(new LambdaQueryWrapper<QuestionEntity>().eq(QuestionEntity::getBankId, bankId));
        }
        questionBankMapper.deleteById(bankId);
        return true;
    }

    public PageData<ApiDtos.QuestionItemDto> listQuestions(Long bankId, HttpServletRequest request) {
        ensureBankExists(bankId);
        String keyword = trim(request.getParameter("keyword"));
        Page<QuestionEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<QuestionEntity> wrapper = new LambdaQueryWrapper<QuestionEntity>()
            .eq(QuestionEntity::getBankId, bankId)
            .orderByDesc(QuestionEntity::getId);
        if (!keyword.isEmpty()) {
            wrapper.like(QuestionEntity::getStem, keyword);
        }
        Page<QuestionEntity> result = questionMapper.selectPage(page, wrapper);
        List<ApiDtos.QuestionItemDto> items = result.getRecords().stream().map(viewAssembler::toQuestionItem).toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.QuestionItemDto createQuestion(UserEntity currentUser, Long bankId, Map<String, Object> payload) {
        ensureBankExists(bankId);
        NormalizedQuestion normalized = normalizeQuestionPayload(payload, null);
        QuestionEntity question = new QuestionEntity();
        question.setBankId(bankId);
        question.setType(normalized.type());
        question.setStem(normalized.stem());
        question.setOptionsJson(JsonUtils.toJson(normalized.options()));
        question.setAnswerJson(JsonUtils.toJson(normalized.answer()));
        question.setScore(normalized.score());
        question.setAnalysis(normalized.analysis());
        question.setCreatorId(currentUser.getId());
        Long safeQuestionId = legacyCompatibilityService.nextSafeQuestionId();
        if (safeQuestionId != null) {
            question.setId(safeQuestionId);
        }
        questionMapper.insert(question);
        return viewAssembler.toQuestionItem(questionMapper.selectById(question.getId()));
    }

    public ApiDtos.QuestionItemDto updateQuestion(Long bankId, Long questionId, Map<String, Object> payload) {
        QuestionEntity question = questionMapper.selectOne(new LambdaQueryWrapper<QuestionEntity>()
            .eq(QuestionEntity::getId, questionId)
            .eq(QuestionEntity::getBankId, bankId));
        if (question == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "题目不存在");
        }
        NormalizedQuestion normalized = normalizeQuestionPayload(payload, question);
        question.setType(normalized.type());
        question.setStem(normalized.stem());
        question.setOptionsJson(JsonUtils.toJson(normalized.options()));
        question.setAnswerJson(JsonUtils.toJson(normalized.answer()));
        question.setScore(normalized.score());
        question.setAnalysis(normalized.analysis());
        questionMapper.updateById(question);
        return viewAssembler.toQuestionItem(questionMapper.selectById(questionId));
    }

    @Transactional
    public ApiDtos.QuestionImportResultDto importQuestions(UserEntity currentUser, Long bankId, Map<String, Object> payload) {
        ensureBankExists(bankId);
        Object rawItems = payload.get("items");
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请提供待导入题目列表");
        }

        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        Long nextSafeQuestionId = legacyCompatibilityService.nextSafeQuestionId();

        for (int index = 0; index < items.size(); index++) {
            Object rawItem = items.get(index);
            if (!(rawItem instanceof Map<?, ?> map)) {
                skippedCount++;
                errors.add("第" + (index + 1) + "题格式错误：需为对象");
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) map;
                NormalizedQuestion normalized = normalizeQuestionPayload(item, null);
                QuestionEntity question = new QuestionEntity();
                question.setBankId(bankId);
                question.setType(normalized.type());
                question.setStem(normalized.stem());
                question.setOptionsJson(JsonUtils.toJson(normalized.options()));
                question.setAnswerJson(JsonUtils.toJson(normalized.answer()));
                question.setScore(normalized.score());
                question.setAnalysis(normalized.analysis());
                question.setCreatorId(currentUser.getId());
                if (nextSafeQuestionId != null) {
                    question.setId(nextSafeQuestionId);
                    nextSafeQuestionId++;
                }
                questionMapper.insert(question);
                importedCount++;
            } catch (ApiException ex) {
                skippedCount++;
                errors.add("第" + (index + 1) + "题导入失败：" + ex.getMessage());
            }
        }

        if (importedCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "没有可导入的有效题目，请检查导入内容");
        }
        return new ApiDtos.QuestionImportResultDto(importedCount, skippedCount, errors.stream().limit(20).toList());
    }

    @Transactional
    public boolean deleteQuestion(Long bankId, Long questionId) {
        QuestionEntity question = questionMapper.selectOne(new LambdaQueryWrapper<QuestionEntity>()
            .eq(QuestionEntity::getId, questionId)
            .eq(QuestionEntity::getBankId, bankId));
        if (question == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "题目不存在");
        }
        if (legacyCompatibilityService.hasCurrentQuestionReferences(List.of(questionId))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题目存在关联作答或考试记录，暂无法删除，请先清理关联数据");
        }
        legacyCompatibilityService.cleanupLegacyQuestionLinks(List.of(questionId));
        questionMapper.deleteById(questionId);
        return true;
    }

    private QuestionBankEntity ensureBankExists(Long bankId) {
        QuestionBankEntity bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "题库不存在");
        }
        return bank;
    }

    private ApiDtos.QuestionBankDto toQuestionBank(QuestionBankEntity bank) {
        Map<Long, UserEntity> creatorMap = referenceLookupService.loadUsers(List.of(bank.getCreatorId()));
        Map<Long, Long> questionCountMap = referenceLookupService.loadQuestionCountsByBankIds(List.of(bank.getId()));
        return viewAssembler.toQuestionBank(bank, creatorMap, questionCountMap);
    }

    private NormalizedQuestion normalizeQuestionPayload(Map<String, Object> payload, QuestionEntity fallback) {
        String defaultType = fallback == null ? "single" : fallback.getType();
        String type = trim(payload.getOrDefault("type", defaultType));
        if (!QUESTION_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题型不正确");
        }

        String defaultStem = fallback == null ? "" : fallback.getStem();
        String stem = trim(payload.getOrDefault("stem", defaultStem));
        if (stem.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题干不能为空");
        }

        List<String> baseOptions = fallback == null ? List.of() : JsonUtils.parseStringList(fallback.getOptionsJson());
        List<String> baseAnswer = fallback == null ? List.of() : JsonUtils.parseStringList(fallback.getAnswerJson());

        List<String> options = PayloadUtils.stringList(payload.getOrDefault("options", baseOptions));
        List<String> answer = PayloadUtils.stringList(payload.getOrDefault("answer", baseAnswer));

        if (answer.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写标准答案");
        }
        if (Set.of("single", "judge").contains(type) && answer.size() != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "单选题和判断题必须且只能有1个标准答案");
        }
        if (Set.of("single", "multi").contains(type) && options.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "选择题至少需要2个选项");
        }
        if ("judge".equals(type) && options.isEmpty()) {
            options = List.of("正确", "错误");
        }
        if ("text".equals(type)) {
            options = List.of();
        }

        int defaultScore = fallback == null ? 5 : fallback.getScore();
        Integer score = PayloadUtils.integerValue(payload.getOrDefault("score", defaultScore), defaultScore);
        if (score == null || score <= 0 || score > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "题目分值需在1-100之间");
        }
        String analysis = payload.get("analysis") == null
            ? (fallback == null ? "" : fallback.getAnalysis())
            : String.valueOf(payload.get("analysis"));

        return new NormalizedQuestion(type, stem, options, answer, score, analysis);
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String orEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record NormalizedQuestion(
        String type,
        String stem,
        List<String> options,
        List<String> answer,
        Integer score,
        String analysis
    ) {
    }
}
