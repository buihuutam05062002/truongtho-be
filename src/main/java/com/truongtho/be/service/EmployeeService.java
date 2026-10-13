package com.truongtho.be.service;

import com.truongtho.be.dto.request.CreateEmployeeRequest;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.EmployeeStatus;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UsersRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeService {

    final UsersRepository usersRepository;
    final RoleRepository roleRepository;
    final EmployeeRepository employeeRepository;
    final PasswordEncoder passwordEncoder;

    @Transactional
    public String createEmployee(CreateEmployeeRequest req) {
        if (usersRepository.existsByUsername(req.getUsername()))
            throw new AppException("Username đã được sử dụng");
        if (usersRepository.existsByEmail(req.getEmail()))
            throw new AppException("Email đã được sử dụng");

        Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new AppException("Chưa cấu hình role ROLE_STAFF"));

        String tempPassword = UUID.randomUUID().toString().substring(0, 10);

        Users user = new Users();
        user.setUsername(req.getUsername());
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setEmailVerified(true);
        user.setUserStatus(UserStatus.ACTIVE);
        user.getRoles().add(staffRole);
        user = usersRepository.save(user);

        Employee emp = new Employee();
        emp.setUsers(user);
        emp.setHireDate(req.getHireDate());
        emp.setEmployeeStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(emp);

        return tempPassword;
    }
}