package com.truongtho.be.repository.spec;

import com.truongtho.be.dto.request.ProductFilter;
import com.truongtho.be.entity.Product;
import com.truongtho.be.enums.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecs {

    private static final char ESCAPE = '!';

    private ProductSpecs() {
    }

    /**
     * @param menuOnly true khi dùng cho thực đơn công khai: ẩn các món đã ngừng bán.
     */
    public static Specification<Product> filter(ProductFilter f, boolean menuOnly) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String like = "%" + escapeLike(f.getKeyword().trim().toLowerCase()) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, ESCAPE),
                        cb.like(cb.lower(root.get("description")), like, ESCAPE)));
            }
            if (f.getCategoryId() != null) {
                ps.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));
            }
            if (f.getMinPrice() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("unitPrice"), f.getMinPrice()));
            }
            if (f.getMaxPrice() != null) {
                ps.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("unitPrice"), f.getMaxPrice()));
            }
            if (f.getStatus() != null && !f.getStatus().isEmpty()) {
                ps.add(root.get("productStatus").in(f.getStatus()));
            }
            if (menuOnly) {
                ps.add(cb.notEqual(root.get("productStatus"), ProductStatus.SUSPENDED));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Escape ký tự đặc biệt của LIKE để "50%" hay "a_b" được tìm đúng nghĩa đen. */
    private static String escapeLike(String s) {
        return s.replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
