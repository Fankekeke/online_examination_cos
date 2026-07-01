package com.onlineexamsystem.backend.common.util;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

public final class JsonResponseWriter {

    private JsonResponseWriter() {
    }

    public static void write(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtils.toJson(ApiResponse.error(code, message, null)));
        response.getWriter().flush();
    }
}
