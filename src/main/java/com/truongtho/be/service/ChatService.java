package com.truongtho.be.service;

import com.truongtho.be.ai.PromptProvider;
import com.truongtho.be.dto.request.ChatRequest;
import com.truongtho.be.dto.response.ChatResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.truongtho.be.ai.MenuTools;
import java.time.Duration;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatService {
    ChatClient chatClient;
    PromptProvider promptProvider;
    MenuTools menuTools;

    public ChatService(ChatClient.Builder builder, PromptProvider promptProvider,
                       MenuTools menuTools, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.promptProvider = promptProvider;
        this.menuTools = menuTools;
    }

    public ChatResponse ask(ChatRequest request) {
        String reply = buildPrompt(request).call().content();
        return new ChatResponse(reply);
    }

    public Flux<String> stream(ChatRequest request) {
        return buildPrompt(request)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(30))
                .onErrorReturn("Xin lỗi, hệ thống đang bận. Vui lòng thử lại hoặc liên hệ nhân viên.");
    }

    private ChatClient.ChatClientRequestSpec buildPrompt(ChatRequest request) {
        return chatClient.prompt()
                .system(promptProvider.systemPrompt())
                .tools(menuTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .user(request.getMessage());
    }
}