package com.truongtho.be.controller;

import com.truongtho.be.dto.request.CategoryRequest;
import com.truongtho.be.dto.response.CategoryResponse;
import com.truongtho.be.entity.Category;
import com.truongtho.be.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/categories/v1")
public class CategoryController {
    final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(){
        return ResponseEntity.ok(categoryService.getAll());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryResponse> delete(@PathVariable Long id ){
      return  ResponseEntity.ok(categoryService.delete(id));
    };
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id ){
        return  ResponseEntity.ok(categoryService.getById(id));
    };
    @PostMapping()
    public ResponseEntity<CategoryResponse> save(@RequestBody CategoryRequest categoryRequest){
        return ResponseEntity.ok(categoryService.save(categoryRequest));
    }
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable("id") Long idx,@RequestBody CategoryRequest categoryRequest){
        return ResponseEntity.ok(categoryService.update(idx,categoryRequest));
    }
}
