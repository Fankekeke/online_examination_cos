package com.onlineexamsystem.backend.controller;

import com.onlineexamsystem.backend.common.model.ApiResponse;
import com.onlineexamsystem.backend.common.util.AppPathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FrontendController {

    private final AppPathResolver appPathResolver;

    public FrontendController(AppPathResolver appPathResolver) {
        this.appPathResolver = appPathResolver;
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/static/user/index.html";
    }

    @GetMapping("/app")
    public String app() {
        return "redirect:/static/user/index.html";
    }

    @GetMapping({"/static", "/static/"})
    public String staticRoot() {
        return "redirect:/static/user/index.html";
    }

    @GetMapping({"/{first:^(?!api$|static$|health$).+}", "/{first:^(?!api$|static$|health$).+}/**"})
    public ResponseEntity<?> fallback(@PathVariable String first, jakarta.servlet.http.HttpServletRequest request) throws IOException {
        String requestPath = request.getRequestURI().startsWith("/") ? request.getRequestURI().substring(1) : request.getRequestURI();
        Path userRoot = appPathResolver.getFrontendRootPath();
        Path entry = appPathResolver.getFrontendEntryPath();
        Path target = userRoot.resolve(requestPath).normalize();

        if (StringUtils.hasText(requestPath) && Files.exists(target) && Files.isRegularFile(target) && target.startsWith(userRoot)) {
            Resource resource = new FileSystemResource(target);
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String probe = Files.probeContentType(target);
            if (probe != null) {
                mediaType = MediaType.parseMediaType(probe);
            }
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        }
        if (Files.exists(entry)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(new FileSystemResource(entry));
        }
        return ResponseEntity.status(404).body(ApiResponse.error(404, "前端资源不存在，请先构建前端", null));
    }
}
