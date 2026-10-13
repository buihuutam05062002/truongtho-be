package com.truongtho.be.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
@Getter
@Setter
public class R2Properties {
    private String accountId;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String publicUrl;
}