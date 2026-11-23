package com.truongtho.be.repository;

import com.truongtho.be.entity.InvoiceDetail;
import com.truongtho.be.entity.InvoiceDetailId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, InvoiceDetailId> {
    List<InvoiceDetail> findAllByInvoiceDetailId_InvoiceId(String invoiceId);

    @Query("""
            select d.product.id, d.product.name, sum(d.quantity), sum(d.total)
            from InvoiceDetail d
            where d.invoice.id in (
                select p.invoice.id from Payment p
                where p.paymentStatus = com.truongtho.be.enums.PaymentStatus.SUCCESS
                  and p.paidAt >= :from and p.paidAt < :to)
            group by d.product.id, d.product.name
            order by sum(d.total) desc
            """)
    List<Object[]> topProducts(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               Pageable pageable);

}
