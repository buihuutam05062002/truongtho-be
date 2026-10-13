package com.truongtho.be.repository;

import com.truongtho.be.entity.Invoice;
import com.truongtho.be.enums.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface InvoiceRepository extends JpaRepository<Invoice,String> {
    boolean existsByDiningTable_IdAndInvoiceStatus(Long tableId, InvoiceStatus invoiceStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") String id);



    @EntityGraph(attributePaths = {"diningTable", "employee", "employee.users", "voucher"})
    @Query("""
        select i from Invoice i
        where (:tableId is null or i.diningTable.id = :tableId)
          and (:status is null or i.invoiceStatus = :status)
        """)
    Page<Invoice> search(@Param("tableId") Long tableId,
                         @Param("status") InvoiceStatus status,
                         Pageable pageable);

    Optional<Invoice> findFirstByDiningTable_IdAndInvoiceStatusOrderByIssuedAtDesc(
            Long tableId, InvoiceStatus status);

}
