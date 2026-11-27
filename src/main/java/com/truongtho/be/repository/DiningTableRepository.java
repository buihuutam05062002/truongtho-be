package com.truongtho.be.repository;

import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.enums.TableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface DiningTableRepository extends JpaRepository<DiningTable,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);


    @Query("select t.tableStatus, count(t) from DiningTable t group by t.tableStatus")
    List<Object[]> countByStatus();



    long countByTableStatusNot(TableStatus status);

    boolean existsByTableStatusNotAndCapacityGreaterThanEqual(
            TableStatus status, Integer capacity);
}
