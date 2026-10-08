package com.truongtho.be.entity;

import com.truongtho.be.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Users {
    @Id
    @Column(name = "user_id")
            @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String fullName;



    String email;
    boolean emailVerified;

    String username;
    String password;

    String phoneNumber;
    String avatarUrl;


    @Enumerated(EnumType.STRING)
    UserStatus userStatus;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
            , inverseJoinColumns = @JoinColumn(name = "role_id")
    )
            @Builder.Default
    Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserAuthProvider> authProviders = new HashSet<>();
}
