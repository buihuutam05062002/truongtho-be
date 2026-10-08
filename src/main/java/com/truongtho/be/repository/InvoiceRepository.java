package com.truongtho.be.repository;

import com.truongtho.be.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface InvoiceRepository extends JpaRepository<Invoice,String> {
}
