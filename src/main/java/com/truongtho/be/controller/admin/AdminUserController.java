package com.truongtho.be.controller.admin;

import com.truongtho.be.dto.request.UpdateUserRolesRequest;
import com.truongtho.be.dto.request.UpdateUserStatusRequest;
import com.truongtho.be.dto.response.AdminUserResponse;
import com.truongtho.be.enums.UserStatus;
import com.truongtho.be.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Page<AdminUserResponse> search(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String role,
                                          @RequestParam(required = false) UserStatus status,
                                          Pageable pageable) {
        return userAdminService.search(keyword, role, status, pageable);
    }

    @PutMapping("/{id}/roles")
    public AdminUserResponse updateRoles(@PathVariable String id,
                                         @Valid @RequestBody UpdateUserRolesRequest req,
                                         Authentication auth) {
        return userAdminService.updateRoles(auth.getName(), id, req.getRoles());
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse updateStatus(@PathVariable String id,
                                          @Valid @RequestBody UpdateUserStatusRequest req,
                                          Authentication auth) {
        return userAdminService.updateStatus(auth.getName(), id, req.getStatus());
    }
}
