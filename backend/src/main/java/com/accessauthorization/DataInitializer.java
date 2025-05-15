package com.accessauthorization;

import com.accessauthorization.repository.AccessKey;
import com.accessauthorization.repository.AccessKeyRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AccessKeyRepository repository;

    public DataInitializer(AccessKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        try {
            repository.save(new AccessKey("1", "PUBKEY_ABC123"));
            repository.save(new AccessKey("2", "PUBKEY_DEF456"));
            System.out.println("Dados iniciais carregados.");
        } catch (Exception e) {
            System.err.println("Erro ao carregar dados iniciais: " + e.getMessage());
        }
    }
}

