package com.truongtho.be.repository;

import com.truongtho.be.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ProductRepository extends JpaRepository<Product,Long> {
    @EntityGraph(attributePaths = {"category", "images"})
    Page<Product> findAll(Pageable pageable);
}
