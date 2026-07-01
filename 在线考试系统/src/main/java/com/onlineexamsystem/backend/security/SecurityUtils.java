package com.onlineexamsystem.backend.security;

import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.entity.UserEntity;
import java.util.Arrays;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 401, "未提供登录凭证");
        }
        return user;
    }

    public static void requireAnyRole(String... roles) {
        UserEntity user = currentUser();
        Set<String> expected = Set.copyOf(Arrays.asList(roles));
        if (!expected.contains(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限访问");
        }
    }
}
