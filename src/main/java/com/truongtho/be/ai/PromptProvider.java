package com.truongtho.be.ai;

import com.truongtho.be.config.properties.RestaurantProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PromptProvider {
    private final String template;
    private final RestaurantProperties restaurant;

    public PromptProvider(@Value("classpath:prompts/food-advisor-v1.txt") Resource resource,
                          RestaurantProperties restaurant) throws IOException {
        this.template = resource.getContentAsString(StandardCharsets.UTF_8);
        this.restaurant = restaurant;
    }

    public String systemPrompt() {
        String now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi")));
        return template
                .replace("{{RESTAURANT_NAME}}", restaurant.getName())
                .replace("{{INTRO}}", restaurant.getIntro())
                .replace("{{ADDRESS}}", restaurant.getAddress())
                .replace("{{OPENING_HOURS}}", restaurant.getOpeningHours())
                .replace("{{CONTACT}}", restaurant.getPhone())
                .replace("{{NOW}}", now);
    }
}