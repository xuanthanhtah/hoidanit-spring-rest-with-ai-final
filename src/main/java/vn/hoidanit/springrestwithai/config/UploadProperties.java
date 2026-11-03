package vn.hoidanit.springrestwithai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.upload")
public record UploadProperties(
        String baseDir,
        long maxSizeBytes,
        List<String> allowedExtensions,
        List<String> allowedFolders) {
}
