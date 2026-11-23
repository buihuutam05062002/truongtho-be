package com.truongtho.be.service;

import com.truongtho.be.dto.request.CreateEmployeeRequest;
import com.truongtho.be.dto.request.UpdateEmployeeRequest;
import com.truongtho.be.dto.response.CreateEmployeeResponse;
import com.truongtho.be.dto.response.EmployeeResponse;
import com.truongtho.be.dto.response.EmployeeStatsResponse;
import com.truongtho.be.dto.response.TempPasswordResponse;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.EmployeeStatus;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.EmployeeMapper;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.RefreshTokenRepository;
import com.truongtho.be.repository.ReservationRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UsersRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeService {

    final UsersRepository usersRepository;
    final RoleRepository roleRepository;
    final EmployeeRepository employeeRepository;
    final PasswordEncoder passwordEncoder;
    final EmployeeMapper employeeMapper;
    final InvoiceRepository invoiceRepository;
    final ReservationRepository reservationRepository;
    final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAll(String keyword, EmployeeStatus status, Pageable pageable) {
        String pattern = (keyword == null || keyword.isBlank())
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        return employeeRepository.search(pattern, status, pageable).map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(String id) {
        return employeeMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public EmployeeStatsResponse getStats() {
        Map<EmployeeStatus, Long> byStatus = new EnumMap<>(EmployeeStatus.class);
        for (Object[] row : employeeRepository.countByStatus()) {
            if (row[0] != null) byStatus.put((EmployeeStatus) row[0], (Long) row[1]);
        }
        return new EmployeeStatsResponse(
                employeeRepository.count(),
                byStatus.getOrDefault(EmployeeStatus.ACTIVE, 0L),
                byStatus.getOrDefault(EmployeeStatus.ON_LEAVE, 0L),
                byStatus.getOrDefault(EmployeeStatus.SUSPENDED, 0L),
                byStatus.getOrDefault(EmployeeStatus.RESIGNED, 0L));
    }

    @Transactional
    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest req) {
        if (usersRepository.existsByEmail(req.getEmail()))
            throw new AppException("Email đã được sử dụng");

        Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new AppException("Chưa cấu hình role ROLE_STAFF"));

        String tempPassword = generateTempPassword();

        Users user = new Users();
        user.setUsername(req.getEmail());
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
        emp = employeeRepository.save(emp);

        return new CreateEmployeeResponse(employeeMapper.toResponse(emp), tempPassword);
    }

    @Transactional
    public EmployeeResponse update(String id, UpdateEmployeeRequest req) {
        Employee emp = findById(id);
        Users user = emp.getUsers();

        user.setFullName(req.getFullName().trim());
        user.setPhoneNumber(req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()
                ? null : req.getPhoneNumber().trim());
        emp.setHireDate(req.getHireDate());
        emp.setEmployeeStatus(req.getEmployeeStatus());

        boolean canLogin = req.getEmployeeStatus() == EmployeeStatus.ACTIVE
                || req.getEmployeeStatus() == EmployeeStatus.ON_LEAVE;
        user.setUserStatus(canLogin ? UserStatus.ACTIVE : UserStatus.LOCKED);

        return employeeMapper.toResponse(emp);
    }

    @Transactional
    public TempPasswordResponse resetPassword(String id) {
        Employee emp = findById(id);
        String tempPassword = generateTempPassword();
        emp.getUsers().setPassword(passwordEncoder.encode(tempPassword));
        return new TempPasswordResponse(tempPassword);
    }

    @Transactional
    public void delete(String id) {
        Employee emp = findById(id);
        if (invoiceRepository.existsByEmployee_Id(id) || reservationRepository.existsByConfirmBy_Id(id))
            throw new AppException("Nhân viên đã có dữ liệu hóa đơn/đặt bàn nên không thể xóa. Hãy chuyển sang trạng thái \"Đã nghỉ việc\".");

        Users user = emp.getUsers();
        refreshTokenRepository.deleteAllByUserId(user.getId());
        employeeRepository.delete(emp);
        employeeRepository.flush();
        usersRepository.delete(user);
    }

    private Employee findById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên theo id " + id));
    }

    private String generateTempPassword() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}
