package com.truongtho.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private DataSize maxSize = DataSize.ofMegabytes(5);
    private Set<String> allowedTypes = new HashSet<>();
}