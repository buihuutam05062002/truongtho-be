package com.truongtho.be.repository;

import com.truongtho.be.entity.Payment;
import com.truongtho.be.enums.PaymentMethod;
import com.truongtho.be.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,String> {
    boolean existsByInvoice_IdAndPaymentStatus(String invoiceId, PaymentStatus paymentStatus);
    Optional<Payment> findByInvoice_IdAndPaymentStatus(String invoiceId, PaymentStatus status);

    @Query("select p.invoice.id from Payment p where p.id = :id")
    Optional<String> findInvoiceIdById(@Param("id") String id);

    Optional<Payment> findByTransactionCode(String transactionCode);

    @Query("select p.invoice.id from Payment p where p.transactionCode = :code")
    Optional<String> findInvoiceIdByTransactionCode(@Param("code") String code);

    boolean existsByInvoice_IdAndPaymentStatusAndPaymentMethod(
            String invoiceId, PaymentStatus status, PaymentMethod method);

    List<Payment> findAllByInvoice_IdAndPaymentStatus(String invoiceId, PaymentStatus status);

    boolean existsBySepayTransactionId(Long sepayTransactionId);

    @Query("""
            select p.paidAt as paidAt, p.amount as amount, p.invoice.id as invoiceId
            from Payment p
            where p.paymentStatus = com.truongtho.be.enums.PaymentStatus.SUCCESS
              and p.paidAt >= :from and p.paidAt < :to
            """)
    List<PaidRow> findPaidBetween(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}