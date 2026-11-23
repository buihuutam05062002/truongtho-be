package com.truongtho.be.controller.admin;

import com.truongtho.be.dto.request.CreateEmployeeRequest;
import com.truongtho.be.dto.request.UpdateEmployeeRequest;
import com.truongtho.be.dto.response.CreateEmployeeResponse;
import com.truongtho.be.dto.response.EmployeeResponse;
import com.truongtho.be.dto.response.EmployeeStatsResponse;
import com.truongtho.be.dto.response.TempPasswordResponse;
import com.truongtho.be.enums.EmployeeStatus;
import com.truongtho.be.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Page<EmployeeResponse> getAll(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) EmployeeStatus status,
                                         Pageable pageable) {
        return employeeService.getAll(keyword, status, pageable);
    }

    @GetMapping("/stats")
    public EmployeeStatsResponse stats() {
        return employeeService.getStats();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable String id) {
        return employeeService.getById(id);
    }

    @PostMapping
    public CreateEmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest req) {
        return employeeService.createEmployee(req);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable String id, @Valid @RequestBody UpdateEmployeeRequest req) {
        return employeeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        employeeService.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    public TempPasswordResponse resetPassword(@PathVariable String id) {
        return employeeService.resetPassword(id);
    }
}
