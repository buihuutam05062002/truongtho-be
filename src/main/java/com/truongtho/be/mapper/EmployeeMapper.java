package com.truongtho.be.mapper;

import com.truongtho.be.dto.response.EmployeeResponse;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Users;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {
    public EmployeeResponse toResponse(Employee e) {
        Users u = e.getUsers();
        return EmployeeResponse.builder()
                .id(e.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .avatarUrl(u.getAvatarUrl())
                .hireDate(e.getHireDate())
                .employeeStatus(e.getEmployeeStatus())
                .build();
    }
}
