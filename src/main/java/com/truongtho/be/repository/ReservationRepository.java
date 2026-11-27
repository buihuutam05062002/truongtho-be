package com.truongtho.be.repository;

import com.truongtho.be.entity.Reservation;
import com.truongtho.be.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    boolean existsByReservationCode(String reservationCode);

    Page<Reservation> findByCustomer_Id(String customerId, Pageable pageable);
    boolean existsByDiningTableId(Long tableId);
    boolean existsByConfirmBy_Id(String employeeId);

    @Query("""
            select r.reservationStatus, count(r) from Reservation r
            where r.reservationTime >= :from and r.reservationTime < :to
            group by r.reservationStatus
            """)
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);


    @Query("""
        select coalesce(sum(r.partySize), 0L) from Reservation r
        where r.reservationStatus in :statuses
          and r.reservationTime >= :from and r.reservationTime < :to
        """)
    long sumPartySize(@Param("statuses") Collection<ReservationStatus> statuses,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    @Query("""
        select count(r) from Reservation r
        where r.reservationStatus in :statuses
          and r.reservationTime >= :from and r.reservationTime < :to
        """)
    long countInRange(@Param("statuses") Collection<ReservationStatus> statuses,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);


    Optional<Reservation> findFirstByDiningTable_IdAndReservationStatusOrderByReservationTimeDesc(Long tableId, ReservationStatus status);



    @Query("""
        select count(r) from Reservation r
        where r.reservationStatus in :statuses
          and r.reservationTime > :from and r.reservationTime < :to
          and r.id <> :excludeId
        """)
    long countOverlapping(@Param("statuses") Collection<ReservationStatus> statuses,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("excludeId") Long excludeId);


}
