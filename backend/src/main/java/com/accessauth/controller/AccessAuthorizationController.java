package com.accessauth.controller;

import org.springframework.web.bind.annotation.*;

import com.accessauth.service.AccessAuthorizationService;
import com.accessauth.domain.Challenge;

@RestController
@RequestMapping("/access")
public class AccessAuthorizationController {

    private final AccessAuthorizationService service;

    public AccessAuthorizationController(AccessAuthorizationService service) {
        this.service = service;
    }

    // primeira função recebe id devolve challenge
    @GetMapping("challenge")
    public String returnChallenge(@RequestParam String id) {
        Challenge genChallenge = service.generateChallenge(id);
        if (genChallenge == null) {
            throw new RuntimeException("ID not found or challenge generation failed.");
        }

        return genChallenge.getChallenge();
    }

    // Segunda função recebe siganture verifica e retorna confirmação de sucesso
    @PostMapping("/confirm")
    public String confirmSignature(@RequestBody ConfirmRequest request){
        String response = service.checkResponse(request.getSign(), request.getChallenge());
        if (response == null) {
            throw new RuntimeException("Wrong answer access denied.");
        }
        return response;
    }

    // Classe auxiliar para o request JSON
    public static class ConfirmRequest {
        private String sign;
        private Challenge challenge;

        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }

        public Challenge getChallenge() { return challenge; }
        public void setChallenge(Challenge challenge) { this.challenge = challenge; }
    }
}
