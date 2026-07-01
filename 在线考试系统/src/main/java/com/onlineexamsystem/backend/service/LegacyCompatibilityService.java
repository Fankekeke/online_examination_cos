package com.onlineexamsystem.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LegacyCompatibilityService {

    private final JdbcTemplate jdbcTemplate;

    public LegacyCompatibilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long nextSafeQuestionId() {
        long maxQuestionId = queryLong("SELECT COALESCE(MAX(id), 0) FROM questions");
        long maxLegacyId = maxQuestionId;
        for (String tableName : List.of("submission_answers", "wrong_questions")) {
            if (tableExists(tableName)) {
                long tableMax = queryLong("SELECT COALESCE(MAX(question_id), 0) FROM " + tableName);
                maxLegacyId = Math.max(maxLegacyId, tableMax);
            }
        }
        return maxLegacyId > maxQuestionId ? maxLegacyId + 1 : null;
    }

    public void cleanupLegacyQuestionLinks(List<Long> questionIds) {
        List<Long> ids = questionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        for (String tableName : List.of("submission_answers", "wrong_questions")) {
            if (tableExists(tableName)) {
                jdbcTemplate.update("DELETE FROM " + tableName + " WHERE question_id IN (" + placeholders(ids.size()) + ")", ids.toArray());
            }
        }
    }

    public boolean hasCurrentQuestionReferences(List<Long> questionIds) {
        List<Long> ids = questionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return false;
        }
        return countByQuestionIds("exam_questions", ids) > 0 || countByQuestionIds("attempt_answers", ids) > 0;
    }

    public boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private long countByQuestionIds(String tableName, List<Long> questionIds) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName + " WHERE question_id IN (" + placeholders(questionIds.size()) + ")",
            Long.class,
            questionIds.toArray()
        );
        return count == null ? 0L : count;
    }

    private long queryLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String placeholders(int size) {
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < size; i++) {
            joiner.add("?");
        }
        return joiner.toString();
    }
}
