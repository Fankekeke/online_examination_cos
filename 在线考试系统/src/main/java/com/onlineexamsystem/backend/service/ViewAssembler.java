package com.onlineexamsystem.backend.service;

import com.onlineexamsystem.backend.common.util.DateTimeUtils;
import com.onlineexamsystem.backend.common.util.JsonUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.CarouselEntity;
import com.onlineexamsystem.backend.entity.ClassRoomEntity;
import com.onlineexamsystem.backend.entity.CommunityCommentEntity;
import com.onlineexamsystem.backend.entity.CommunityPostEntity;
import com.onlineexamsystem.backend.entity.ExamAttemptEntity;
import com.onlineexamsystem.backend.entity.ExamEntity;
import com.onlineexamsystem.backend.entity.NoticeEntity;
import com.onlineexamsystem.backend.entity.QuestionBankEntity;
import com.onlineexamsystem.backend.entity.QuestionEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ViewAssembler {

    public ApiDtos.UserProfileDto toUserProfile(UserEntity user, Map<Long, ClassRoomEntity> classMap) {
        ClassRoomEntity classRoom = user.getClassId() == null ? null : classMap.get(user.getClassId());
        return new ApiDtos.UserProfileDto(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getRole(),
            user.getStatus(),
            user.getPhone(),
            user.getEmail(),
            user.getAvatar(),
            user.getBio(),
            user.getClassId(),
            classRoom == null ? null : classRoom.getName()
        );
    }

    public ApiDtos.ClassInfoDto toClassInfo(ClassRoomEntity classRoom,
                                            Map<Long, UserEntity> teacherMap,
                                            Map<Long, Long> studentCountMap) {
        UserEntity teacher = classRoom.getTeacherId() == null ? null : teacherMap.get(classRoom.getTeacherId());
        return new ApiDtos.ClassInfoDto(
            classRoom.getId(),
            classRoom.getName(),
            classRoom.getGrade(),
            classRoom.getDescription(),
            classRoom.getTeacherId(),
            teacher == null ? null : teacher.getName(),
            studentCountMap.getOrDefault(classRoom.getId(), 0L)
        );
    }

    public ApiDtos.QuestionBankDto toQuestionBank(QuestionBankEntity bank,
                                                  Map<Long, UserEntity> creatorMap,
                                                  Map<Long, Long> questionCountMap) {
        UserEntity creator = creatorMap.get(bank.getCreatorId());
        return new ApiDtos.QuestionBankDto(
            bank.getId(),
            bank.getName(),
            bank.getSubject(),
            bank.getDifficulty(),
            bank.getDescription(),
            questionCountMap.getOrDefault(bank.getId(), 0L),
            creator == null ? "" : creator.getName()
        );
    }

    public ApiDtos.QuestionItemDto toQuestionItem(QuestionEntity question) {
        return new ApiDtos.QuestionItemDto(
            question.getId(),
            question.getBankId(),
            question.getType(),
            question.getStem(),
            JsonUtils.parseStringList(question.getOptionsJson()),
            JsonUtils.parseStringList(question.getAnswerJson()),
            question.getScore(),
            question.getAnalysis()
        );
    }

    public ApiDtos.ExamPaperQuestionDto toExamPaperQuestion(QuestionEntity question) {
        return new ApiDtos.ExamPaperQuestionDto(
            question.getId(),
            question.getType(),
            question.getStem(),
            JsonUtils.parseStringList(question.getOptionsJson()),
            question.getScore()
        );
    }

    public ApiDtos.ExamDto toExam(ExamEntity exam,
                                  Map<Long, UserEntity> creatorMap,
                                  Map<Long, Long> questionCountMap) {
        UserEntity creator = creatorMap.get(exam.getCreatorId());
        return new ApiDtos.ExamDto(
            exam.getId(),
            exam.getTitle(),
            exam.getDescription(),
            exam.getType(),
            DateTimeUtils.formatDateTime(exam.getStartTime()),
            DateTimeUtils.formatDateTime(exam.getEndTime()),
            exam.getDurationMinutes(),
            exam.getTotalScore(),
            exam.getPassScore(),
            exam.getAttemptLimit(),
            Boolean.TRUE.equals(exam.getShuffleQuestions()),
            exam.getStatus(),
            questionCountMap.getOrDefault(exam.getId(), 0L),
            creator == null ? "" : creator.getName(),
            null,
            null,
            null,
            null,
            null
        );
    }

    public ApiDtos.ExamDto toExamWithStudentState(ExamEntity exam,
                                                  Map<Long, UserEntity> creatorMap,
                                                  Map<Long, Long> questionCountMap,
                                                  int submittedAttempts,
                                                  int remainingAttempts,
                                                  boolean hasInProgress,
                                                  boolean canStart,
                                                  String availabilityText) {
        ApiDtos.ExamDto base = toExam(exam, creatorMap, questionCountMap);
        return new ApiDtos.ExamDto(
            base.id(),
            base.title(),
            base.description(),
            base.type(),
            base.startTime(),
            base.endTime(),
            base.durationMinutes(),
            base.totalScore(),
            base.passScore(),
            base.attemptLimit(),
            base.shuffleQuestions(),
            base.status(),
            base.questionCount(),
            base.creatorName(),
            submittedAttempts,
            remainingAttempts,
            hasInProgress,
            canStart,
            availabilityText
        );
    }

    public ApiDtos.ScoreRecordDto toScoreRecord(ExamAttemptEntity attempt,
                                                Map<Long, ExamEntity> examMap,
                                                Map<Long, UserEntity> studentMap,
                                                Map<Long, ClassRoomEntity> classMap) {
        ExamEntity exam = examMap.get(attempt.getExamId());
        UserEntity student = studentMap.get(attempt.getStudentId());
        ClassRoomEntity classRoom = student != null && student.getClassId() != null ? classMap.get(student.getClassId()) : null;
        return new ApiDtos.ScoreRecordDto(
            attempt.getId(),
            attempt.getExamId(),
            exam == null ? "" : exam.getTitle(),
            attempt.getStudentId(),
            student == null ? "" : student.getName(),
            classRoom == null ? null : classRoom.getName(),
            attempt.getScore() == null ? 0 : attempt.getScore().intValue(),
            attempt.getStatus(),
            DateTimeUtils.formatDateTime(attempt.getSubmitTime())
        );
    }

    public ApiDtos.NoticeItemDto toNotice(NoticeEntity notice, Map<Long, UserEntity> publisherMap) {
        UserEntity publisher = publisherMap.get(notice.getPublisherId());
        return new ApiDtos.NoticeItemDto(
            notice.getId(),
            notice.getTitle(),
            notice.getContent(),
            Boolean.TRUE.equals(notice.getIsPinned()),
            DateTimeUtils.formatDateTime(notice.getPublishedAt()),
            publisher == null ? "" : publisher.getName()
        );
    }

    public ApiDtos.CarouselItemDto toCarousel(CarouselEntity carousel) {
        return new ApiDtos.CarouselItemDto(
            carousel.getId(),
            carousel.getTitle(),
            carousel.getImageUrl(),
            carousel.getLink(),
            carousel.getSortOrder(),
            Boolean.TRUE.equals(carousel.getIsActive())
        );
    }

    public ApiDtos.CommunityCommentDto toCommunityComment(CommunityCommentEntity comment, Map<Long, UserEntity> userMap) {
        UserEntity user = userMap.get(comment.getUserId());
        return new ApiDtos.CommunityCommentDto(
            comment.getId(),
            comment.getUserId(),
            user == null ? "" : user.getName(),
            comment.getContent(),
            DateTimeUtils.formatDateTime(comment.getCreatedAt())
        );
    }

    public ApiDtos.CommunityPostDto toCommunityPost(CommunityPostEntity post,
                                                    Map<Long, UserEntity> userMap,
                                                    Map<Long, List<CommunityCommentEntity>> commentMap) {
        UserEntity user = userMap.get(post.getUserId());
        List<ApiDtos.CommunityCommentDto> comments = commentMap.getOrDefault(post.getId(), Collections.emptyList())
            .stream()
            .map(item -> toCommunityComment(item, userMap))
            .toList();
        return new ApiDtos.CommunityPostDto(
            post.getId(),
            post.getUserId(),
            user == null ? "" : user.getName(),
            post.getContent(),
            DateTimeUtils.formatDateTime(post.getCreatedAt()),
            post.getLikes(),
            comments
        );
    }
}
