package com.truongtho.be.service;

import com.truongtho.be.dto.request.DishSearchRequest;
import com.truongtho.be.dto.response.DishSearchResponse;
import com.truongtho.be.enums.ProductStatus;
import com.truongtho.be.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DishSearchService {
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<DishSearchResponse> search(DishSearchRequest request) {
        List<Long> ids = vectorStore.similaritySearch(
                        SearchRequest.builder().query(request.getQuery()).topK(10).similarityThreshold(0.4).build())
                .stream()
                .map(d -> ((Number) d.getMetadata().get("productId")).longValue())
                .toList();

        NumberFormat vn = NumberFormat.getInstance(Locale.forLanguageTag("vi"));
        return productRepository.findAllById(ids).stream()
                .filter(p -> p.getProductStatus() == ProductStatus.AVAILABLE
                        || p.getProductStatus() == ProductStatus.ON_SALE)
                .sorted(Comparator.comparingInt(p -> ids.indexOf(p.getId())))
                .limit(request.getLimit())
                .map(p -> DishSearchResponse.builder()
                        .id(p.getId()).name(p.getName()).description(p.getDescription())
                        .unitPrice(p.getUnitPrice()).priceText(vn.format(p.getUnitPrice()) + "đ")
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                        .build())
                .toList();
    }
}