package com.truongtho.be.controller;

import com.truongtho.be.dto.response.LoyaltyResponse;
import com.truongtho.be.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/me/loyalty")
    public ResponseEntity<LoyaltyResponse> myLoyalty(Authentication auth) {
        return ResponseEntity.ok(loyaltyService.getMine(auth.getName()));
    }
}
