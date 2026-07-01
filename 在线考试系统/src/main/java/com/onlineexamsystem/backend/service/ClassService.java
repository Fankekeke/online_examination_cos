package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.model.PageData;
import com.onlineexamsystem.backend.common.util.PageUtils;
import com.onlineexamsystem.backend.common.util.PayloadUtils;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.ClassRoomEntity;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.ClassRoomMapper;
import com.onlineexamsystem.backend.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassService {

    private final ClassRoomMapper classRoomMapper;
    private final UserMapper userMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public ClassService(ClassRoomMapper classRoomMapper,
                        UserMapper userMapper,
                        ReferenceLookupService referenceLookupService,
                        ViewAssembler viewAssembler) {
        this.classRoomMapper = classRoomMapper;
        this.userMapper = userMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.ClassInfoDto> listClasses(UserEntity currentUser, HttpServletRequest request) {
        String keyword = trim(request.getParameter("keyword"));
        Page<ClassRoomEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<ClassRoomEntity> wrapper = new LambdaQueryWrapper<>();
        if ("teacher".equals(currentUser.getRole())) {
            wrapper.eq(ClassRoomEntity::getTeacherId, currentUser.getId());
        }
        if (!keyword.isEmpty()) {
            wrapper.like(ClassRoomEntity::getName, keyword);
        }
        wrapper.orderByDesc(ClassRoomEntity::getId);

        Page<ClassRoomEntity> result = classRoomMapper.selectPage(page, wrapper);
        Map<Long, UserEntity> teacherMap = referenceLookupService.loadUsers(
            result.getRecords().stream().map(ClassRoomEntity::getTeacherId).filter(Objects::nonNull).toList()
        );
        Map<Long, Long> studentCountMap = referenceLookupService.loadStudentCountsByClassIds(
            result.getRecords().stream().map(ClassRoomEntity::getId).toList()
        );
        List<ApiDtos.ClassInfoDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toClassInfo(item, teacherMap, studentCountMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.ClassInfoDto createClass(Map<String, Object> payload) {
        String name = trim(payload.get("name"));
        String grade = trim(payload.get("grade"));
        if (name.isEmpty() || grade.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "班级名称和年级不能为空");
        }
        if (classRoomMapper.selectCount(new LambdaQueryWrapper<ClassRoomEntity>().eq(ClassRoomEntity::getName, name)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "班级名称已存在");
        }
        ClassRoomEntity classRoom = new ClassRoomEntity();
        classRoom.setName(name);
        classRoom.setGrade(grade);
        classRoom.setTeacherId(PayloadUtils.longValue(payload.get("teacherId"), null));
        classRoom.setDescription(orEmpty(payload.get("description")));
        classRoomMapper.insert(classRoom);
        return toClassInfo(classRoomMapper.selectById(classRoom.getId()));
    }

    public ApiDtos.ClassInfoDto updateClass(Long classId, Map<String, Object> payload) {
        ClassRoomEntity classRoom = classRoomMapper.selectById(classId);
        if (classRoom == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "班级不存在");
        }
        String name = trim(payload.getOrDefault("name", classRoom.getName()));
        String grade = trim(payload.getOrDefault("grade", classRoom.getGrade()));
        if (name.isEmpty() || grade.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "班级名称和年级不能为空");
        }
        classRoom.setName(name);
        classRoom.setGrade(grade);
        classRoom.setTeacherId(PayloadUtils.longValue(payload.get("teacherId"), null));
        classRoom.setDescription(orEmpty(payload.get("description")));
        classRoomMapper.updateById(classRoom);
        return toClassInfo(classRoomMapper.selectById(classRoom.getId()));
    }

    @Transactional
    public boolean deleteClass(Long classId) {
        ClassRoomEntity classRoom = classRoomMapper.selectById(classId);
        if (classRoom == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "班级不存在");
        }
        List<UserEntity> students = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getClassId, classId));
        for (UserEntity student : students) {
            student.setClassId(null);
            userMapper.updateById(student);
        }
        classRoomMapper.deleteById(classId);
        return true;
    }

    public boolean assignTeacher(Long classId, Map<String, Object> payload) {
        ClassRoomEntity classRoom = classRoomMapper.selectById(classId);
        if (classRoom == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "班级不存在");
        }
        Long teacherId = PayloadUtils.longValue(payload.get("teacherId"), null);
        if (teacherId != null) {
            UserEntity teacher = userMapper.selectById(teacherId);
            if (teacher == null || !"teacher".equals(teacher.getRole())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "教师不存在");
            }
        }
        classRoom.setTeacherId(teacherId);
        classRoomMapper.updateById(classRoom);
        return true;
    }

    public boolean transferStudent(UserEntity currentUser, Map<String, Object> payload) {
        Long studentId = PayloadUtils.longValue(payload.get("studentId"), null);
        Long toClassId = PayloadUtils.longValue(payload.get("toClassId"), null);
        UserEntity student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "学生不存在");
        }
        if ("teacher".equals(currentUser.getRole())) {
            List<Long> managedIds = classRoomMapper.selectList(new LambdaQueryWrapper<ClassRoomEntity>()
                    .eq(ClassRoomEntity::getTeacherId, currentUser.getId()))
                .stream()
                .map(ClassRoomEntity::getId)
                .toList();
            if (toClassId != null && !managedIds.contains(toClassId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "教师只能管理自己班级");
            }
            if (student.getClassId() != null && !managedIds.contains(student.getClassId()) && (toClassId == null || !managedIds.contains(toClassId))) {
                throw new ApiException(HttpStatus.FORBIDDEN, "无权限移动该学生");
            }
        }
        userMapper.update(
            null,
            new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, student.getId())
                .set(UserEntity::getClassId, toClassId)
        );
        return true;
    }

    public PageData<ApiDtos.UserProfileDto> myClassStudents(UserEntity currentUser, HttpServletRequest request) {
        List<Long> classIds = classRoomMapper.selectList(new LambdaQueryWrapper<ClassRoomEntity>()
                .eq(ClassRoomEntity::getTeacherId, currentUser.getId()))
            .stream()
            .map(ClassRoomEntity::getId)
            .toList();
        if (classIds.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0);
        }
        String keyword = trim(request.getParameter("keyword"));
        Page<UserEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getRole, "student")
            .in(UserEntity::getClassId, classIds)
            .orderByDesc(UserEntity::getId);
        if (!keyword.isEmpty()) {
            wrapper.and(group -> group.like(UserEntity::getUsername, keyword).or().like(UserEntity::getName, keyword));
        }
        Page<UserEntity> result = userMapper.selectPage(page, wrapper);
        Map<Long, ClassRoomEntity> classMap = referenceLookupService.loadClasses(result.getRecords().stream()
            .map(UserEntity::getClassId).filter(Objects::nonNull).toList());
        List<ApiDtos.UserProfileDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toUserProfile(item, classMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    private ApiDtos.ClassInfoDto toClassInfo(ClassRoomEntity classRoom) {
        Map<Long, UserEntity> teacherMap = referenceLookupService.loadUsers(classRoom.getTeacherId() == null ? List.of() : List.of(classRoom.getTeacherId()));
        Map<Long, Long> studentCountMap = referenceLookupService.loadStudentCountsByClassIds(List.of(classRoom.getId()));
        return viewAssembler.toClassInfo(classRoom, teacherMap, studentCountMap);
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String orEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
