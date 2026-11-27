package com.truongtho.be.service;

import com.truongtho.be.dto.response.AdminUserResponse;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.EmployeeStatus;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.RefreshTokenRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UsersRepository;
import com.truongtho.be.security.Roles;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAdminService {

    final UsersRepository usersRepository;
    final RoleRepository roleRepository;
    final EmployeeRepository employeeRepository;
    final CustomerRepository customerRepository;
    final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> search(String keyword, String role, UserStatus status, Pageable pageable) {
        String pattern = (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        String roleName = (role == null || role.isBlank()) ? null : role.trim();
        if (roleName != null && !Roles.ALL.contains(roleName))
            throw new AppException("Vai trò không hợp lệ");

        Page<Users> page = usersRepository.search(pattern, roleName, status, pageable);

        List<String> ids = page.getContent().stream().map(Users::getId).toList();
        Map<String, EmployeeStatus> employeeStatus = new HashMap<>();
        for (Employee e : employeeRepository.findAllById(ids))
            employeeStatus.put(e.getId(), e.getEmployeeStatus());

        return page.map(u -> toResponse(u, employeeStatus.get(u.getId())));
    }

    @Transactional
    public AdminUserResponse updateRoles(String actorId, String targetId, Set<String> requested) {
        Set<String> names = requested.stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (names.isEmpty() || !Roles.ALL.containsAll(names))
            throw new AppException("Vai trò không hợp lệ");
        if (actorId.equals(targetId) && !names.contains(Roles.ADMIN))
            throw new AppException("Bạn không thể tự gỡ quyền quản trị của chính mình");

        Users user = findUser(targetId);
        Set<String> before = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        Map<String, Role> byName = new HashMap<>();
        for (Role r : roleRepository.findAll()) byName.put(r.getName(), r);
        for (String n : names)
            if (!byName.containsKey(n)) throw new AppException("Chưa cấu hình role " + n);

        user.getRoles().removeIf(r -> !names.contains(r.getName()));
        for (String n : names)
            if (!before.contains(n)) user.getRoles().add(byName.get(n));

        boolean wasStaff = before.contains(Roles.STAFF);
        boolean isStaff = names.contains(Roles.STAFF);
        if (isStaff && !wasStaff) activateEmployee(user);
        if (!isStaff && wasStaff) deactivateEmployee(user);
        if (names.contains(Roles.CUSTOMER)) ensureCustomer(user);

        if (!before.equals(names))
            refreshTokenRepository.revokeAllByUserId(user.getId()); // JWT carries roles: force a fresh login

        return toResponse(user, employeeStatusOf(user.getId()));
    }

    @Transactional
    public AdminUserResponse updateStatus(String actorId, String targetId, UserStatus status) {
        if (status != UserStatus.ACTIVE && status != UserStatus.LOCKED)
            throw new AppException("Chỉ có thể chuyển tài khoản sang Hoạt động hoặc Khóa");
        if (actorId.equals(targetId) && status == UserStatus.LOCKED)
            throw new AppException("Bạn không thể tự khóa tài khoản của mình");

        Users user = findUser(targetId);
        user.setUserStatus(status);
        if (status == UserStatus.LOCKED) refreshTokenRepository.revokeAllByUserId(user.getId());

        return toResponse(user, employeeStatusOf(user.getId()));
    }

    private void activateEmployee(Users user) {
        Employee emp = employeeRepository.findById(user.getId()).orElseGet(() -> {
            Employee e = new Employee();
            e.setUsers(user);
            e.setHireDate(LocalDate.now());
            return e;
        });
        emp.setEmployeeStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(emp);
    }

    .    private void deactivateEmployee(Users user) {
        employeeRepository.findById(user.getId())
                .ifPresent(e -> e.setEmployeeStatus(EmployeeStatus.RESIGNED));
    }

    private void ensureCustomer(Users user) {
        if (customerRepository.existsById(user.getId())) return;
        Customer c = new Customer();
        c.setUsers(user);
        customerRepository.save(c);
    }

    private EmployeeStatus employeeStatusOf(String userId) {
        return employeeRepository.findById(userId).map(Employee::getEmployeeStatus).orElse(null);
    }

    private Users findUser(String id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng theo id " + id));
    }

    private AdminUserResponse toResponse(Users u, EmployeeStatus employeeStatus) {
        return new AdminUserResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getPhoneNumber(), u.getAvatarUrl(),
                u.getUserStatus(),
                u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                employeeStatus);
    }
}
