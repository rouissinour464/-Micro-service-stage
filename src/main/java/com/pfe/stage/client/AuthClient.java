package com.pfe.stage.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "auth-service",
        url = "${auth.service.url}",
        path = "/api/users"
)
public interface AuthClient {

    @GetMapping("/{id}")
    UserResponse getUserById(@PathVariable Long id);

    @GetMapping("/by-role")
    List<UserResponse> getUsersByRole(@RequestParam String role);

    @GetMapping("/encadrants")
    List<Long> getEncadrantIds();
}