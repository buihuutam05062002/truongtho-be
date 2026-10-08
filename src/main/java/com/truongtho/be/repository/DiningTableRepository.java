package com.truongtho.be.repository;

import com.truongtho.be.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface DiningTableRepository extends JpaRepository<DiningTable,Long> {
}
