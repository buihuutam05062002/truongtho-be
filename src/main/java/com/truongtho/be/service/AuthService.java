package com.truongtho.be.service;

import com.truongtho.be.dto.request.ChangePasswordRequest;
import com.truongtho.be.dto.request.LoginRequest;
import com.truongtho.be.dto.request.RegisterRequest;
import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.dto.response.UserResponse;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.mapper.UserMapper;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UsersRepository;
import com.truongtho.be.security.CustomUserDetails;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class AuthService {
     final AuthenticationManager authenticationManager;
     final UsersRepository usersRepository;
     final RoleRepository roleRepository;
     final CustomerRepository customerRepository;
     final PasswordEncoder passwordEncoder;
     final TokenService tokenService;
     final UserMapper userMapper;
     final LoginAttemptService loginAttemptService;
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_USED);
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new AppException("Chưa cấu hình role ROLE_CUSTOMER"));

        Users user = new Users();
        user.setEmail(request.getEmail());
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
    public AuthResponse login(LoginRequest request, String ip) {
        String key = request.getEmail().trim().toLowerCase() + "|" + ip;
        loginAttemptService.checkNotLocked(key);
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            loginAttemptService.reset(key);
            Users user = ((CustomUserDetails) auth.getPrincipal()).getUser();
            return tokenService.issue(user);
        } catch (LockedException | DisabledException e) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(key);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }


    @Transactional
    public AuthResponse changePassword(String userId, ChangePasswordRequest req) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        if (user.getPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new AppException(ErrorCode.WRONG_CURRENT_PASSWORD);
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword()))
            throw new AppException("Mật khẩu mới phải khác mật khẩu hiện tại");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        tokenService.logoutAll(user.getId());
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me (String userId){
        Users user =usersRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        return userMapper.toResponse(user);
    }
}