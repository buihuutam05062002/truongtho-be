package com.truongtho.be.service;

import com.truongtho.be.entity.Invoice;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.event.PaymentCompletedEvent;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private static final long TIMEOUT_MS = Duration.ofMinutes(10).toMillis();

    private final InvoiceRepository invoiceRepository;
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(invoiceId, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(invoiceId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(invoiceId, emitter));

        InvoiceStatus status = invoice.getInvoiceStatus();
        send(emitter, status(invoiceId, status));
        if (status != InvoiceStatus.PENDING) emitter.complete();
        return emitter;
    }

    @TransactionalEventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Set<SseEmitter> set = emitters.get(event.invoiceId());
        if (set == null) return;
        for (SseEmitter emitter : set) {
            send(emitter, status(event.invoiceId(), InvoiceStatus.PAID));
            emitter.complete();
        }
    }

    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        emitters.forEach((id, set) -> {
            for (SseEmitter emitter : set) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(id, emitter);
                }
            }
        });
    }

    private String status(String invoiceId, InvoiceStatus status) {
        return "{\"invoiceId\":\"" + invoiceId + "\",\"status\":\"" + status.name() + "\"}";
    }

    private void send(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().name("status").data(json));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    private void remove(String invoiceId, SseEmitter emitter) {
        emitters.computeIfPresent(invoiceId, (k, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }
}
