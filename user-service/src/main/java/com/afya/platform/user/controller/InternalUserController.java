package com.afya.platform.user.controller;

import com.afya.platform.user.dto.InternalAuthProfileResponse;
import com.afya.platform.user.service.UserAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/internal")
public class InternalUserController {

    private final UserAdminService userAdminService;

    public InternalUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/auth-profile/by-username/{username}")
    public InternalAuthProfileResponse byUsername(@PathVariable String username) {
        return userAdminService.internalAuthProfileByUsername(username);
    }

    @GetMapping("/auth-profile/{id}")
    public InternalAuthProfileResponse byId(@PathVariable Long id) {
        return userAdminService.internalAuthProfileById(id);
    }
}
