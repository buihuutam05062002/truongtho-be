package com.truongtho.be.event;

/** Published inside the payment transaction; listeners run only after it commits. */
public record PaymentCompletedEvent(String invoiceId) {
}
