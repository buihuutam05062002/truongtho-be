package com.truongtho.be.service;

import com.truongtho.be.config.properties.SepayProperties;
import com.truongtho.be.dto.request.SePayWebhookRequest;
import com.truongtho.be.entity.Payment;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class SepayService {
    static final Pattern CODE_PATTERN = Pattern.compile("TT[0-9A-F]{19}");

    final SepayProperties props;

    public boolean isValidApiKey(String authorizationHeader) {
        if (authorizationHeader == null
                || props.getApiKey() == null || props.getApiKey().isBlank()) {
            return false;
        }
        String expected = "Apikey " + props.getApiKey();
        return MessageDigest.isEqual(
                authorizationHeader.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    public String buildQrUrl(Payment payment) {
        return "https://qr.sepay.vn/img"
                + "?acc=" + encode(props.getAccountNumber())
                + "&bank=" + encode(props.getBank())
                + "&amount=" + payment.getAmount().toBigInteger()
                + "&des=" + encode(payment.getTransactionCode())
                + "&template=compact";
    }


    public String extractPaymentCode(SePayWebhookRequest req) {
        if (req.getCode() != null && !req.getCode().isBlank()) {
            return req.getCode().trim().toUpperCase();
        }
        if (req.getContent() == null) {
            return null;
        }
        Matcher m = CODE_PATTERN.matcher(req.getContent().toUpperCase());
        return m.find() ? m.group() : null;
    }






    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}