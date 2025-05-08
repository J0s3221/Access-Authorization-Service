package com.accessAuthorization.service;

import org.springframework.stereotype.Service;

import com.accessAuthorization.repository.AccessKeyRepository;

@Service
public class AccessAuthorizationService {

    private final AccessKeyRepository repository;

    public AccessAuthorizationService(AccessKeyRepository repository) {
        this.repository = repository;
    }

}

