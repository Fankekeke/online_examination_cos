package com.onlineexamsystem.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.dto.ApiDtos;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.UserMapper;
import com.onlineexamsystem.backend.security.JwtService;
import com.onlineexamsystem.backend.security.PasswordHashService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final ReferenceLookupService referenceLookupService;
    private final ViewAssembler viewAssembler;
    private final PasswordHashService passwordHashService;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper,
                       ReferenceLookupService referenceLookupService,
                       ViewAssembler viewAssembler,
                       PasswordHashService passwordHashService,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.referenceLookupService = referenceLookupService;
        this.viewAssembler = viewAssembler;
        this.passwordHashService = passwordHashService;
        this.jwtService = jwtService;
    }

    public Map<String, Object> login(Map<String, Object> payload) {
        String username = value(payload.get("username"));
        String password = value(payload.get("password"));
        if (username.isEmpty() || password.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "账号和密码不能为空");
        }

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (user == null || !passwordHashService.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 1, "账号或密码错误");
        }
        if (!"active".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用，请联系管理员");
        }

        ApiDtos.UserProfileDto profile = viewAssembler.toUserProfile(
            user,
            referenceLookupService.loadClasses(user.getClassId() == null ? java.util.List.of() : java.util.List.of(user.getClassId()))
        );
        return Map.of(
            "token", jwtService.createAccessToken(user.getId()),
            "profile", profile
        );
    }

    public ApiDtos.UserProfileDto profile(UserEntity currentUser) {
        return viewAssembler.toUserProfile(
            currentUser,
            referenceLookupService.loadClasses(currentUser.getClassId() == null ? java.util.List.of() : java.util.List.of(currentUser.getClassId()))
        );
    }

    private String value(Object rawValue) {
        return rawValue == null ? "" : String.valueOf(rawValue).trim();
    }
}
