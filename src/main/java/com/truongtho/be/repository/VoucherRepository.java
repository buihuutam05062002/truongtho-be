package com.truongtho.be.repository;

import com.truongtho.be.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface VoucherRepository extends JpaRepository<Voucher,Long> {
}
