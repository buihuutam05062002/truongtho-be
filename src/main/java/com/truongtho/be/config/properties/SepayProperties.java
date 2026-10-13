package com.truongtho.be.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sepay")
@Getter
@Setter
public class SepayProperties {
    private String apiKey;
    private String accountNumber;
    private String bank;
}