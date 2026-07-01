package com.onlineexamsystem.backend;

import com.onlineexamsystem.backend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(basePackages = "com.onlineexamsystem.backend")
@EnableConfigurationProperties(AppProperties.class)
public class OnlineExamSystemBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineExamSystemBackendApplication.class, args);
    }
}
