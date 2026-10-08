package com.truongtho.be.repository;

import com.truongtho.be.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
