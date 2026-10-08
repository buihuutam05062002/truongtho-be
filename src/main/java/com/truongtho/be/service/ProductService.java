package com.truongtho.be.service;

import com.truongtho.be.dto.request.ProductImageRequest;
import com.truongtho.be.dto.request.ProductRequest;
import com.truongtho.be.dto.response.ProductResponse;
import com.truongtho.be.entity.Category;
import com.truongtho.be.entity.Product;
import com.truongtho.be.entity.ProductImage;
import com.truongtho.be.enums.UploadFolder;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.ProductMapper;
import com.truongtho.be.repository.ProductImageRepository;
import com.truongtho.be.repository.ProductRepository;
import com.truongtho.be.exception.BadRequestException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ProductService {
    final ProductRepository productRepository;
    final ProductMapper productMapper;
    final FileStorageService fileStorageService;
    final CategoryService categoryService;

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn theo id " + id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setCategory(categoryService.findById(request.getCategoryId()));
        syncImages(product, request.getProductImageRequests());
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findById(id);
        productMapper.updateEntity(request, product);
        product.setCategory(categoryService.findById(request.getCategoryId()));
        syncImages(product, request.getProductImageRequests());
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        List<String> urls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        productRepository.delete(product);
        deleteFilesAfterCommit(urls);
    }

    private void syncImages(Product product, List<ProductImageRequest> requests) {
        if (requests == null) requests = List.of();

        Map<Long, ProductImage> current = product.getImages().stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        List<String> promoted = new ArrayList<>();
        deleteFilesOnRollback(promoted);

        List<ProductImage> result = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            ProductImageRequest req = requests.get(i);
            ProductImage image;

            if (req.getId() != null) {
                image = current.remove(req.getId());
                if (image == null) {
                    throw new BadRequestException("Ảnh id " + req.getId() + " không thuộc sản phẩm này");
                }
            } else {
                String url = fileStorageService.promote(req.getKey(), UploadFolder.PRODUCT);
                promoted.add(url);
                image = new ProductImage();
                image.setImageUrl(url);
            }
            image.setAltText(req.getAltText());
            image.setPosition(i);
            result.add(image);
        }

        List<String> removedUrls = current.values().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        product.clearImages();
        result.forEach(product::addImage);

        deleteFilesAfterCommit(removedUrls);
    }

    private void deleteFilesAfterCommit(List<String> urls) {
        if (urls.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                urls.forEach(ProductService.this::safeDelete);
            }
        });
    }

    private void deleteFilesOnRollback(List<String> urls) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    urls.forEach(ProductService.this::safeDelete);
                }
            }
        });
    }

    private void safeDelete(String url) {
        try {
            fileStorageService.delete(url);
        } catch (Exception e) {
            log.warn("Không xóa được file trên R2: {}", url, e);
        }
    }
}