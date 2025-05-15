package com.accessauthorization.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accessauthorization.service.AccessAuthorizationService;
import com.accessauthorization.domain.Challenge;

@RestController
@RequestMapping("/access")
public class AccessAuthorizationController {

    private final AccessAuthorizationService service;

    public AccessAuthorizationController(AccessAuthorizationService service) {
        this.service = service;
    }

    public String returnChallenge(String id) {
        Challenge genChallenge = service.generateChallenge(id);
        if (genChallenge == null) {
            throw new RuntimeException("ID not found or challenge generation failed.");
        }

        return genChallenge.getChallenge();
    }
}
