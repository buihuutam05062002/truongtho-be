package com.truongtho.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {
    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 500, message = "Câu hỏi quá dài")
    String message;

    @NotBlank
    String conversationId;
}