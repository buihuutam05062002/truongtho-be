package com.truongtho.be.service;

import com.truongtho.be.dto.request.LoginRequest;
import com.truongtho.be.dto.request.RegisterRequest;
import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.mapper.UserMapper;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UsersRepository;
import com.truongtho.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usersRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Username đã được sử dụng");
        }
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new AppException("Chưa cấu hình role ROLE_CUSTOMER"));

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setUserStatus(UserStatus.ACTIVE);
        user.getRoles().add(customerRole);
        user = usersRepository.save(user);

        Customer customer = new Customer();
        customer.setUsers(user);
        customerRepository.save(customer);

        return tokenService.issue(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            Users user = ((CustomUserDetails) auth.getPrincipal()).getUser();
            return tokenService.issue(user);

        } catch (LockedException e) {
            throw new AppException("Tài khoản đã bị khóa");
        } catch (DisabledException e) {
            throw new AppException("Tài khoản chưa được kích hoạt hoặc không hoạt động");
        } catch (BadCredentialsException e) {
            throw new AppException("Sai username hoặc mật khẩu");
        }
    }
}