package com.truongtho.be.service;

import com.truongtho.be.dto.request.CategoryRequest;
import com.truongtho.be.dto.response.CategoryResponse;
import com.truongtho.be.entity.Category;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(level =  AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class CategoryService {
    final CategoryRepository categoryRepository;


    protected Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy loại món ăn với id = " + id));
    }


    public CategoryResponse getById(Long idx){
        Category category = findById(idx);
        return new CategoryResponse(category.getId(),category.getName());
    }

    @Transactional
    public CategoryResponse save(CategoryRequest categoryRequest){
        Category category = new Category();
        category.setName(categoryRequest.getName());
        categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
    @Transactional
    public CategoryResponse update(Long id,CategoryRequest categoryRequest){
        Category category = findById(id);
        category.setName(categoryRequest.getName());
        categoryRepository.save(category);
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
    public CategoryResponse delete(Long id){
        Category category    = findById(id);
        categoryRepository.deleteById(id);
        return new CategoryResponse(category.getId(),category.getName());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(){
        List<CategoryResponse> categoryResponses = categoryRepository.findAll().stream().map(
                category ->  new CategoryResponse(category.getId(), category.getName()))
                .toList();

        return categoryResponses;
    }





}
