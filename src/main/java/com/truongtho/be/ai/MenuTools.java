package com.truongtho.be.ai;

import com.truongtho.be.dto.request.DishSearchRequest;
import com.truongtho.be.dto.response.DishSearchResponse;
import com.truongtho.be.service.DishSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MenuTools {
    private final DishSearchService dishSearchService;

    @Tool(name = "search_menu",
            description = "Search the menu by meaning, e.g. 'something spicy', 'light dish for kids', 'cheap soup'.")
    public List<DishSearchResponse> searchMenu(
            @ToolParam(description = "What the customer wants, in natural language") String query) {
        return dishSearchService.search(DishSearchRequest.builder().query(query).limit(5).build());
    }
}