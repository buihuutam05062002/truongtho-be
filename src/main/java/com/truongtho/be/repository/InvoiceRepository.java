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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface InvoiceRepository extends JpaRepository<Invoice,String> {
    boolean existsByEmployee_Id(String employeeId);
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




    @Query("""
        select i.invoiceStatus, count(i) from Invoice i
        where i.issuedAt >= :from and i.issuedAt < :to
        group by i.invoiceStatus
        """)
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    long countByInvoiceStatus(InvoiceStatus status);

    @EntityGraph(attributePaths = {"diningTable", "employee", "employee.users"})
    List<Invoice> findByEmployee_IdAndInvoiceStatusOrderByIssuedAtDesc(String employeeId, InvoiceStatus status);

    /** Doanh thu theo nhân viên phụ trách (employee null → một dòng "Không xác định"). */
    @Query("""
            select e.id, u.fullName, count(i), sum(i.totalAmount)
            from Invoice i
            left join i.employee e
            left join e.users u
            where i.id in (
                select p.invoice.id from Payment p
                where p.paymentStatus = com.truongtho.be.enums.PaymentStatus.SUCCESS
                  and p.paidAt >= :from and p.paidAt < :to)
            group by e.id, u.fullName
            order by sum(i.totalAmount) desc
            """)
    List<Object[]> revenueByEmployee(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = {"diningTable", "employee", "employee.users"})
    List<Invoice> findTop5ByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(
            LocalDateTime from, LocalDateTime to);

}
