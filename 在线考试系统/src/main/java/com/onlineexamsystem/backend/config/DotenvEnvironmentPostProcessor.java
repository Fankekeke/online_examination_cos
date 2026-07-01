package com.onlineexamsystem.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "onlineExamSystemDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String workingDirectory = System.getProperty("user.dir");
        Dotenv dotenv = Dotenv.configure()
            .directory(workingDirectory)
            .filename(".env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

        Map<String, Object> properties = new LinkedHashMap<>();
        dotenv.entries().forEach(entry -> {
            if (StringUtils.hasText(entry.getKey())) {
                properties.put(entry.getKey(), entry.getValue());
            }
        });

        if (!properties.isEmpty() && environment.getPropertySources().get(PROPERTY_SOURCE_NAME) == null) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
