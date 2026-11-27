package com.truongtho.be.security;

import com.truongtho.be.entity.Users;

import java.util.List;

public final class Roles {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String STAFF = "ROLE_STAFF";
    public static final String CUSTOMER = "ROLE_CUSTOMER";

    public static final List<String> ALL = List.of(ADMIN, STAFF, CUSTOMER);

    private Roles() {
    }

    public static boolean has(Users user, String role) {
        return user.getRoles().stream().anyMatch(r -> role.equals(r.getName()));
    }
}
