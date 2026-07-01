package com.onlineexamsystem.backend.dto;

import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record UserProfileDto(
        Long id,
        String username,
        String name,
        String role,
        String status,
        String phone,
        String email,
        String avatar,
        String bio,
        Long classId,
        String className
    ) {
    }

    public record ClassInfoDto(
        Long id,
        String name,
        String grade,
        String description,
        Long teacherId,
        String teacherName,
        long studentCount
    ) {
    }

    public record QuestionBankDto(
        Long id,
        String name,
        String subject,
        String difficulty,
        String description,
        long questionCount,
        String creatorName
    ) {
    }

    public record QuestionItemDto(
        Long id,
        Long bankId,
        String type,
        String stem,
        List<String> options,
        List<String> answer,
        Integer score,
        String analysis
    ) {
    }

    public record ExamPaperQuestionDto(
        Long id,
        String type,
        String stem,
        List<String> options,
        Integer score
    ) {
    }

    public record ExamDto(
        Long id,
        String title,
        String description,
        String type,
        String startTime,
        String endTime,
        Integer durationMinutes,
        Integer totalScore,
        Integer passScore,
        Integer attemptLimit,
        Boolean shuffleQuestions,
        String status,
        long questionCount,
        String creatorName,
        Integer submittedAttempts,
        Integer remainingAttempts,
        Boolean hasInProgress,
        Boolean canStart,
        String availabilityText
    ) {
    }

    public record ScoreRecordDto(
        Long id,
        Long examId,
        String examTitle,
        Long studentId,
        String studentName,
        String className,
        Integer score,
        String status,
        String submittedAt
    ) {
    }

    public record NoticeItemDto(
        Long id,
        String title,
        String content,
        Boolean isPinned,
        String publishedAt,
        String publisherName
    ) {
    }

    public record CarouselItemDto(
        Long id,
        String title,
        String imageUrl,
        String link,
        Integer sortOrder,
        Boolean isActive
    ) {
    }

    public record CommunityCommentDto(
        Long id,
        Long userId,
        String userName,
        String content,
        String createdAt
    ) {
    }

    public record CommunityPostDto(
        Long id,
        Long userId,
        String userName,
        String content,
        String createdAt,
        Integer likes,
        List<CommunityCommentDto> comments
    ) {
    }

    public record SegmentItemDto(
        String range,
        long count
    ) {
    }

    public record DateValueDto(
        String date,
        double value
    ) {
    }

    public record LabelValueDto(
        String label,
        long value
    ) {
    }

    public record DashboardCardDto(
        String key,
        String label,
        long value,
        Integer trend
    ) {
    }

    public record DashboardPayloadDto(
        List<DashboardCardDto> cards,
        List<DateValueDto> scoreTrend,
        List<LabelValueDto> examStat,
        List<SegmentItemDto> passDistribution,
        List<NoticeItemDto> latestNotices
    ) {
    }

    public record PassRateDto(
        String className,
        double passRate
    ) {
    }

    public record AvgScoreTrendDto(
        String date,
        double avgScore
    ) {
    }

    public record ExamHeatDto(
        String examTitle,
        long participants
    ) {
    }

    public record RoleCountDto(
        String role,
        long count
    ) {
    }

    public record BigScreenDataDto(
        List<DashboardCardDto> overview,
        List<PassRateDto> passRateByClass,
        List<AvgScoreTrendDto> scoreTrend,
        List<ExamHeatDto> examHeat,
        List<RoleCountDto> onlineStatus,
        String serverTime
    ) {
    }

    public record QuestionImportResultDto(
        int importedCount,
        int skippedCount,
        List<String> errors
    ) {
    }

    public record StartExamDataDto(
        Long attemptId,
        ExamDto exam,
        List<ExamPaperQuestionDto> questions,
        String startedAt,
        Integer submittedAttempts,
        Integer remainingAttempts
    ) {
    }

    public record SubmitExamResultDto(
        Integer score,
        Boolean pass,
        Integer submittedAttempts,
        Integer remainingAttempts
    ) {
    }
}
