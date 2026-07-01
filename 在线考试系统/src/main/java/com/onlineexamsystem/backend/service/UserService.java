package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.onlineexamsystem.backend.security.PasswordHashService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final ClassRoomMapper classRoomMapper;
    private final PasswordHashService passwordHashService;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;

    public UserService(UserMapper userMapper,
                       ClassRoomMapper classRoomMapper,
                       PasswordHashService passwordHashService,
                       ReferenceLookupService referenceLookupService,
                       ViewAssembler viewAssembler) {
        this.userMapper = userMapper;
        this.classRoomMapper = classRoomMapper;
        this.passwordHashService = passwordHashService;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
    }

    public PageData<ApiDtos.UserProfileDto> listUsers(UserEntity currentUser, HttpServletRequest request) {
        String roleFilter = trim(request.getParameter("role"));
        String keyword = trim(request.getParameter("keyword"));
        Page<UserEntity> page = PageUtils.createPage(request);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();

        if ("admin".equals(currentUser.getRole())) {
            // unrestricted
        } else if ("teacher".equals(currentUser.getRole())) {
            List<Long> managedClassIds = managedClassIds(currentUser.getId());
            if (managedClassIds.isEmpty()) {
                return new PageData<>(Collections.emptyList(), 0);
            }
            if (!roleFilter.isEmpty() && !"student".equals(roleFilter)) {
                return new PageData<>(Collections.emptyList(), 0);
            }
            wrapper.eq(UserEntity::getRole, "student")
                .and(group -> group.in(UserEntity::getClassId, managedClassIds).or().isNull(UserEntity::getClassId));
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限访问");
        }

        if (!roleFilter.isEmpty()) {
            wrapper.eq(UserEntity::getRole, roleFilter);
        }
        if (!keyword.isEmpty()) {
            wrapper.and(group -> group.like(UserEntity::getUsername, keyword).or().like(UserEntity::getName, keyword));
        }
        wrapper.orderByDesc(UserEntity::getId);

        Page<UserEntity> result = userMapper.selectPage(page, wrapper);
        Map<Long, ClassRoomEntity> classMap = referenceLookupService.loadClasses(
            result.getRecords().stream().map(UserEntity::getClassId).filter(java.util.Objects::nonNull).toList()
        );
        List<ApiDtos.UserProfileDto> items = result.getRecords().stream()
            .map(item -> viewAssembler.toUserProfile(item, classMap))
            .toList();
        return new PageData<>(items, result.getTotal());
    }

    public ApiDtos.UserProfileDto createUser(Map<String, Object> payload) {
        String username = trim(payload.get("username"));
        String name = trim(payload.get("name"));
        String role = trim(payload.get("role"));
        String password = payload.get("password") == null ? "123456" : String.valueOf(payload.get("password"));

        if (!Set.of("student", "teacher", "admin").contains(role)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "角色不合法");
        }
        if (username.isEmpty() || name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号和姓名不能为空");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setName(name);
        user.setRole(role);
        user.setStatus(trim(payload.getOrDefault("status", "active")));
        user.setPhone(orEmpty(payload.get("phone")));
        user.setEmail(orEmpty(payload.get("email")));
        user.setClassId(PayloadUtils.longValue(payload.get("classId"), null));
        user.setPasswordHash(passwordHashService.generate(password));

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号已存在");
        }
        return viewAssembler.toUserProfile(
            userMapper.selectById(user.getId()),
            referenceLookupService.loadClasses(user.getClassId() == null ? List.of() : List.of(user.getClassId()))
        );
    }

    public ApiDtos.UserProfileDto updateUser(Long userId, Map<String, Object> payload) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "用户不存在");
        }

        String name = trim(payload.getOrDefault("name", user.getName()));
        user.setName(name.isEmpty() ? user.getName() : name);
        String status = trim(payload.getOrDefault("status", user.getStatus()));
        user.setStatus(status.isEmpty() ? user.getStatus() : status);
        user.setPhone(orEmpty(payload.get("phone")));
        user.setEmail(orEmpty(payload.get("email")));

        if ("student".equals(user.getRole())) {
            user.setClassId(PayloadUtils.longValue(payload.get("classId"), null));
        }

        String password = trim(payload.get("password"));
        if (!password.isEmpty()) {
            user.setPasswordHash(passwordHashService.generate(password));
        }

        userMapper.updateById(user);
        return viewAssembler.toUserProfile(
            userMapper.selectById(user.getId()),
            referenceLookupService.loadClasses(user.getClassId() == null ? List.of() : List.of(user.getClassId()))
        );
    }

    public boolean deleteUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "管理员账号不可删除");
        }
        try {
            return userMapper.deleteById(userId) > 0;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户存在关联业务数据，暂无法删除，请先清理相关数据");
        }
    }

    public ApiDtos.UserProfileDto updateSelf(UserEntity currentUser, Map<String, Object> payload) {
        String name = trim(payload.getOrDefault("name", currentUser.getName()));
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "姓名不能为空");
        }
        currentUser.setName(name);
        currentUser.setPhone(orEmpty(payload.get("phone")));
        currentUser.setEmail(orEmpty(payload.get("email")));
        currentUser.setBio(orEmpty(payload.get("bio")));
        currentUser.setAvatar(orEmpty(payload.get("avatar")));
        userMapper.updateById(currentUser);
        return viewAssembler.toUserProfile(
            userMapper.selectById(currentUser.getId()),
            referenceLookupService.loadClasses(currentUser.getClassId() == null ? List.of() : List.of(currentUser.getClassId()))
        );
    }

    public boolean updatePassword(UserEntity currentUser, Map<String, Object> payload) {
        String oldPassword = orEmpty(payload.get("oldPassword"));
        String newPassword = orEmpty(payload.get("newPassword"));
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请填写完整密码信息");
        }
        if (newPassword.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密码长度不能少于6位");
        }
        if (!passwordHashService.matches(oldPassword, currentUser.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "当前密码错误");
        }
        currentUser.setPasswordHash(passwordHashService.generate(newPassword));
        userMapper.updateById(currentUser);
        return true;
    }

    private List<Long> managedClassIds(Long teacherId) {
        return classRoomMapper.selectList(new LambdaQueryWrapper<ClassRoomEntity>()
                .eq(ClassRoomEntity::getTeacherId, teacherId))
            .stream()
            .map(ClassRoomEntity::getId)
            .collect(Collectors.toList());
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String orEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
