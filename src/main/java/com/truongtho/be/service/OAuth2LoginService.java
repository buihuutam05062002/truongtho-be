package com.truongtho.be.service;

import com.truongtho.be.dto.response.AuthResponse;
import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.Role;
import com.truongtho.be.entity.UserAuthProvider;
import com.truongtho.be.entity.Users;
import com.truongtho.be.enums.AuthProvider;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.mapper.UserMapper;
import com.truongtho.be.oauth2.OAuth2ProviderClient;
import com.truongtho.be.oauth2.OAuth2UserInfo;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.RoleRepository;
import com.truongtho.be.repository.UserAuthProviderRepository;
import com.truongtho.be.repository.UsersRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.truongtho.be.service.TokenService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OAuth2LoginService {

    final Map<AuthProvider, OAuth2ProviderClient> clients;
    final UsersRepository usersRepository;
    final UserAuthProviderRepository providerRepository;
    final RoleRepository roleRepository;
    final CustomerRepository customerRepository;
    final TokenService tokenService;

    public OAuth2LoginService(List<OAuth2ProviderClient> clientList,
                              UsersRepository usersRepository,
                              UserAuthProviderRepository providerRepository,
                              RoleRepository roleRepository,
                              CustomerRepository customerRepository,
                              TokenService tokenService) {
        this.clients = clientList.stream()
                .collect(Collectors.toMap(OAuth2ProviderClient::getProvider, Function.identity()));
        this.usersRepository = usersRepository;
        this.providerRepository = providerRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;

        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse login(AuthProvider provider, String credential) {
        OAuth2UserInfo info = getClient(provider).fetchUserInfo(credential);

        Users user = providerRepository
                .findByProviderAndProviderId(provider, info.getProviderId())
                .map(UserAuthProvider::getUser)
                .orElseGet(() -> linkOrCreateUser(info));

        checkUserStatus(user);

        return tokenService.issue(user);
    }

    private OAuth2ProviderClient getClient(AuthProvider provider) {
        OAuth2ProviderClient client = clients.get(provider);
        if (client == null) {
            throw new AppException("Provider không được hỗ trợ");
        }
        return client;
    }

    private Users linkOrCreateUser(OAuth2UserInfo info) {
        if (info.getEmail() == null) {
            throw new AppException("Không lấy được email từ " + info.getProvider());
        }

        Optional<Users> existing = usersRepository.findByEmail(info.getEmail());

        Users user;
        if (existing.isPresent()) {
            if (!info.isEmailVerified()) {
                throw new AppException(
                        "Email đã tồn tại. Hãy đăng nhập bằng tài khoản thường rồi liên kết trong phần cài đặt");
            }
            user = mergeIntoExistingUser(existing.get());
        } else {
            if (!info.isEmailVerified()) {
                throw new AppException("Email chưa được " + info.getProvider() + " xác minh");
            }
            user = createNewUser(info);
        }

        UserAuthProvider link = new UserAuthProvider();
        link.setProvider(info.getProvider());
        link.setProviderId(info.getProviderId());
        link.setUser(user);
        providerRepository.save(link);

        return user;
    }

    private Users mergeIntoExistingUser(Users user) {
        if (!user.isEmailVerified()) {
            user.setPassword(null);
            user.setEmailVerified(true);
            if (user.getUserStatus() == UserStatus.PENDING) {
                user.setUserStatus(UserStatus.ACTIVE);
            }
        }
        return usersRepository.save(user);
    }

    private Users createNewUser(OAuth2UserInfo info) {
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new AppException("Chưa cấu hình role ROLE_CUSTOMER"));

        Users user = new Users();
        user.setEmail(info.getEmail());
        user.setEmailVerified(true);
        user.setFullName(info.getName());
        user.setAvatarUrl(info.getAvatarUrl());
        user.setUserStatus(UserStatus.ACTIVE);
        user.getRoles().add(customerRole);
        user = usersRepository.save(user);

        Customer customer = new Customer();
        customer.setUsers(user);
        customerRepository.save(customer);

        return user;
    }


    private void checkUserStatus(Users user) {
        switch (user.getUserStatus()) {
            case LOCKED -> throw new AppException("Tài khoản đã bị khóa");
            case DELETED -> throw new AppException("Tài khoản không tồn tại");
            case INACTIVE -> throw new AppException("Tài khoản không hoạt động");
            case PENDING -> throw new AppException("Tài khoản chưa được kích hoạt");
            default -> {
            }
        }
    }
}