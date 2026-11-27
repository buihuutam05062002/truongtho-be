package com.truongtho.be.controller;

import com.truongtho.be.dto.request.ChatRequest;
import com.truongtho.be.dto.response.ChatResponse;
import com.truongtho.be.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        return chatService.ask(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(request);
    }
}