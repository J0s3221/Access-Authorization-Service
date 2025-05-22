package com.accessauth.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public String returnChallenge(String id) {
        Challenge genChallenge = service.generateChallenge(id);
        if (genChallenge == null) {
            throw new RuntimeException("ID not found or challenge generation failed.");
        }

        return genChallenge.getChallenge();
    }

    // Segunda função recebe siganture verifica e retorna confirmação de sucesso
    public String confirmSignature(String sign, Challenge challenge){
        // primeiro chama uma função do service e depois devolve a resposta
        String response = service.checkResponse(sign, challenge);
        if (response == null) {
            throw new RuntimeException("Wrong answer access denied.");
        }

        return response;
    }
}
