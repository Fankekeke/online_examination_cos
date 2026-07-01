package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineexamsystem.backend.entity.ClassRoomEntity;
import com.onlineexamsystem.backend.entity.CommunityCommentEntity;
import com.onlineexamsystem.backend.entity.ExamQuestionEntity;
import com.onlineexamsystem.backend.entity.QuestionEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.ClassRoomMapper;
import com.onlineexamsystem.backend.mapper.CommunityCommentMapper;
import com.onlineexamsystem.backend.mapper.ExamQuestionMapper;
import com.onlineexamsystem.backend.mapper.QuestionMapper;
import com.onlineexamsystem.backend.mapper.UserMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReferenceLookupService {

    private final UserMapper userMapper;
    private final ClassRoomMapper classRoomMapper;
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final CommunityCommentMapper communityCommentMapper;

    public ReferenceLookupService(UserMapper userMapper,
                                  ClassRoomMapper classRoomMapper,
                                  QuestionMapper questionMapper,
                                  ExamQuestionMapper examQuestionMapper,
                                  CommunityCommentMapper communityCommentMapper) {
        this.userMapper = userMapper;
        this.classRoomMapper = classRoomMapper;
        this.questionMapper = questionMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.communityCommentMapper = communityCommentMapper;
    }

    public Map<Long, UserEntity> loadUsers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectBatchIds(ids.stream().filter(Objects::nonNull).toList());
        return users.stream().collect(Collectors.toMap(UserEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public Map<Long, ClassRoomEntity> loadClasses(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ClassRoomEntity> classes = classRoomMapper.selectBatchIds(ids.stream().filter(Objects::nonNull).toList());
        return classes.stream().collect(Collectors.toMap(ClassRoomEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public Map<Long, Long> loadStudentCountsByClassIds(Collection<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> students = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getRole, "student")
            .in(UserEntity::getClassId, classIds));
        return students.stream()
            .filter(student -> student.getClassId() != null)
            .collect(Collectors.groupingBy(UserEntity::getClassId, LinkedHashMap::new, Collectors.counting()));
    }

    public Map<Long, Long> loadQuestionCountsByBankIds(Collection<Long> bankIds) {
        if (bankIds == null || bankIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionEntity> questions = questionMapper.selectList(new LambdaQueryWrapper<QuestionEntity>()
            .in(QuestionEntity::getBankId, bankIds));
        return questions.stream().collect(Collectors.groupingBy(QuestionEntity::getBankId, LinkedHashMap::new, Collectors.counting()));
    }

    public Map<Long, Long> loadQuestionCountsByExamIds(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExamQuestionEntity> rows = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestionEntity>()
            .in(ExamQuestionEntity::getExamId, examIds));
        return rows.stream().collect(Collectors.groupingBy(ExamQuestionEntity::getExamId, LinkedHashMap::new, Collectors.counting()));
    }

    public Map<Long, List<CommunityCommentEntity>> loadCommentsByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CommunityCommentEntity> comments = communityCommentMapper.selectList(new LambdaQueryWrapper<CommunityCommentEntity>()
            .in(CommunityCommentEntity::getPostId, postIds)
            .orderByAsc(CommunityCommentEntity::getId));
        return comments.stream().collect(Collectors.groupingBy(CommunityCommentEntity::getPostId, LinkedHashMap::new, Collectors.toList()));
    }
}
