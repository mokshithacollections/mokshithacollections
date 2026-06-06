package com.ec.mokshitha_collections.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Exposes the local image-uploads directory as a public static resource so
 * browsers can load product images via {urlPrefix}/...  filenames.
 *
 * Defaults match ImageStorageService: ./uploads on disk, /uploads as URL.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.uploads.root:./uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolute = Path.of(uploadsRoot).toAbsolutePath().normalize();
        // Spring requires the file: URL to end in "/" for directory mapping.
        String location = absolute.toUri().toString();
        if (!location.endsWith("/")) location = location + "/";

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }
}
