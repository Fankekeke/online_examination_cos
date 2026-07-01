package com.onlineexamsystem.backend.common.util;

import com.onlineexamsystem.backend.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;

@Component
public class AppPathResolver {

    private final AppProperties appProperties;

    public AppPathResolver(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Path getProjectRoot() {
        return Paths.get("").toAbsolutePath().normalize();
    }

    public Path getStaticRootPath() {
        return resolve(appProperties.getStaticDir());
    }

    public Path getUploadRootPath() {
        return resolve(appProperties.getUploadDir());
    }

    public Path getFrontendRootPath() {
        return getStaticRootPath().resolve(appProperties.getFrontendDir()).normalize();
    }

    public Path getFrontendEntryPath() {
        return getFrontendRootPath().resolve(appProperties.getFrontendEntry()).normalize();
    }

    public void ensureDirectories() {
        createIfMissing(getStaticRootPath());
        createIfMissing(getUploadRootPath());
    }

    private Path resolve(String relativePath) {
        return getProjectRoot().resolve(relativePath).normalize();
    }

    private void createIfMissing(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new IllegalStateException("初始化目录失败: " + path, ex);
        }
    }
}
