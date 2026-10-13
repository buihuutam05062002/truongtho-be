package com.truongtho.be.controller;

import com.truongtho.be.dto.request.SePayWebhookRequest;
import com.truongtho.be.service.PaymentService;
import com.truongtho.be.service.SepayService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/webhooks/sepay")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SePayWebhookController {
    final SepayService sepayService;
    final PaymentService paymentService;


    @PostMapping
    public ResponseEntity<Map<String, Object>> handle(@RequestHeader(value = "Authorization",required = false) String auth, @RequestBody SePayWebhookRequest sePayWebhookRequest){
        if(!sepayService.isValidApiKey((auth))){
            return ResponseEntity.status(401).body(Map.of("success",false));
        }
        paymentService.handleSePayWebhook(sePayWebhookRequest);
        return ResponseEntity.ok(Map.of("success", true));    }


}
