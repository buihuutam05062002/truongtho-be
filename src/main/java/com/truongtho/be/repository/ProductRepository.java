package com.truongtho.be.repository;

import com.truongtho.be.entity.Product;
import com.truongtho.be.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ProductRepository extends JpaRepository<Product,Long> {

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            select p from Product p
            where (:keyword is null or lower(p.name) like :keyword)
              and (:categoryId is null or p.category.id = :categoryId)
              and (:status is null or p.productStatus = :status)
            """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("status") ProductStatus status,
                         Pageable pageable);

    @Query("select p.productStatus, count(p) from Product p group by p.productStatus")
    List<Object[]> countByStatus();

    @Query("select p.category.id, count(p) from Product p group by p.category.id")
    List<Object[]> countByCategory();

    boolean existsByCategoryId(Long categoryId);

}
