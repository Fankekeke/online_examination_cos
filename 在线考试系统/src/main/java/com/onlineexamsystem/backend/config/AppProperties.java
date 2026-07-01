package com.onlineexamsystem.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String jwtSecretKey;
    private long jwtAccessTokenExpiresMinutes;
    private String staticDir;
    private String uploadDir;
    private String frontendDir;
    private String frontendEntry;

    public String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public void setJwtSecretKey(String jwtSecretKey) {
        this.jwtSecretKey = jwtSecretKey;
    }

    public long getJwtAccessTokenExpiresMinutes() {
        return jwtAccessTokenExpiresMinutes;
    }

    public void setJwtAccessTokenExpiresMinutes(long jwtAccessTokenExpiresMinutes) {
        this.jwtAccessTokenExpiresMinutes = jwtAccessTokenExpiresMinutes;
    }

    public String getStaticDir() {
        return staticDir;
    }

    public void setStaticDir(String staticDir) {
        this.staticDir = staticDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getFrontendDir() {
        return frontendDir;
    }

    public void setFrontendDir(String frontendDir) {
        this.frontendDir = frontendDir;
    }

    public String getFrontendEntry() {
        return frontendEntry;
    }

    public void setFrontendEntry(String frontendEntry) {
        this.frontendEntry = frontendEntry;
    }
}
