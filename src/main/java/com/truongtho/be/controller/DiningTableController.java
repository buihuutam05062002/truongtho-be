package com.truongtho.be.controller;

import com.truongtho.be.dto.request.DiningTableRequest;
import com.truongtho.be.dto.response.DiningTableResponse;
import com.truongtho.be.service.DiningTableService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/dining-tables/v1")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiningTableController {
    final DiningTableService diningTableService;

    public DiningTableController(DiningTableService diningTableService) {
        this.diningTableService = diningTableService;
    }

    @GetMapping
    public ResponseEntity<List<DiningTableResponse>> getAll(){
        return ResponseEntity.ok(diningTableService.getAll());
    }
    @GetMapping("{id}")
    public ResponseEntity<DiningTableResponse> getById(@PathVariable("id") Long idx){
        return ResponseEntity.ok(diningTableService.getById(idx));
    }
    @PostMapping
    public ResponseEntity<DiningTableResponse> save(@RequestBody DiningTableRequest diningTableRequest){
        return ResponseEntity.ok(diningTableService.save(diningTableRequest));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<DiningTableResponse> delete(@PathVariable("id") Long idx){
        return ResponseEntity.ok(diningTableService.delete(idx));
    }
}
