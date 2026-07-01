package com.onlineexamsystem.backend.service;

import com.onlineexamsystem.backend.common.exception.ApiException;
import com.onlineexamsystem.backend.common.util.AppPathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private final AppPathResolver appPathResolver;

    public UploadService(AppPathResolver appPathResolver) {
        this.appPathResolver = appPathResolver;
    }

    public Map<String, String> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "未选择文件");
        }
        String filename = file.getOriginalFilename();
        String extension = extension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持png/jpg/jpeg/webp/gif格式");
        }
        appPathResolver.ensureDirectories();
        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path savePath = appPathResolver.getUploadRootPath().resolve(savedName);
        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "服务器内部异常");
        }
        return Map.of("url", "/static/uploads/" + savedName);
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index >= 0 ? filename.substring(index + 1).toLowerCase(Locale.ROOT) : "";
    }
}
