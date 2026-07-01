package com.onlineexamsystem.backend.security;

import com.onlineexamsystem.backend.common.util.JsonResponseWriter;
import com.onlineexamsystem.backend.entity.UserEntity;
import com.onlineexamsystem.backend.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            JsonResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), 401, "登录凭证无效");
            return;
        }

        try {
            Claims claims = jwtService.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                JsonResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), 401, "用户不存在");
                return;
            }
            if (!"active".equals(user.getStatus())) {
                JsonResponseWriter.write(response, HttpStatus.FORBIDDEN.value(), 1, "账号已停用，请联系管理员");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            JsonResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), 401, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException ex) {
            JsonResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), 401, "登录凭证无效");
        }
    }
}
