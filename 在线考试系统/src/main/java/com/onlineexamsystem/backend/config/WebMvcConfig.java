package com.onlineexamsystem.backend.config;

import com.onlineexamsystem.backend.common.util.AppPathResolver;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppPathResolver appPathResolver;

    public WebMvcConfig(AppPathResolver appPathResolver) {
        this.appPathResolver = appPathResolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations(appPathResolver.getStaticRootPath().toUri().toString())
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected org.springframework.core.io.Resource getResource(String resourcePath,
                                                                           org.springframework.core.io.Resource location)
                    throws IOException {
                    PathResource requested = new PathResource(appPathResolver.getStaticRootPath().resolve(resourcePath));
                    return requested.exists() && requested.isReadable() ? requested : null;
                }
            });
    }
}
