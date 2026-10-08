package com.truongtho.be.repository;

import com.truongtho.be.entity.InvoiceDetail;
import com.truongtho.be.entity.InvoiceDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, InvoiceDetailId> {
}
