package com.truongtho.be.service;

import com.truongtho.be.entity.Product;
import com.truongtho.be.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuIndexService {
    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    @Transactional()
    public int reindexAll() {
        List<Document> docs = productRepository.findAll().stream().map(this::toDocument).toList();
        if (!docs.isEmpty()) vectorStore.add(docs);
        return docs.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexById(Long id) {
        productRepository.findById(id).ifPresent(p -> vectorStore.add(List.of(toDocument(p))));
    }

    public void remove(Long id) {
        vectorStore.delete(List.of(docId(id)));
    }

    private static String docId(Long id) {
        return UUID.nameUUIDFromBytes(("product:" + id).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Document toDocument(Product p) {
        String category = p.getCategory() != null ? p.getCategory().getName() : "";
        String text = p.getName() + ". " + Objects.toString(p.getDescription(), "") + ". Nhóm: " + category;
        return new Document(docId(p.getId()), text, Map.of("productId", p.getId()));
    }
}