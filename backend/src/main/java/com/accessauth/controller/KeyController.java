package com.accessauth.controller;

import com.accessauth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/keys")
public class KeyController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public String getSymmetricKey(@PathVariable Long userId) {
        Optional<String> keyOpt = userService.getSymmetricKey(userId);
        return keyOpt.orElseThrow(() -> new RuntimeException("Symmetric key not found for user ID: " + userId));
    }
}
