package com.truongtho.be.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.restaurant")
@Getter
@Setter
public class RestaurantProperties {
    private String name;
    private String address;
    private String openingHours;
    private String phone;
    private String intro;
}