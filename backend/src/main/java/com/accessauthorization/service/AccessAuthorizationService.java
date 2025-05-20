package com.accessauthorization.service;

import org.springframework.stereotype.Service;

import com.accessauthorization.repository.AccessKeyRepository;
import com.accessauthorization.repository.AccessKey;
import com.accessauthorization.domain.Challenge;

import java.util.Optional;

@Service
public class AccessAuthorizationService {

    private final AccessKeyRepository repository;

    public AccessAuthorizationService(AccessKeyRepository repository) {
        this.repository = repository;
    }

    public Challenge generateChallenge(String id){
        try {
            // first calls the database and verifies the id exists, if it does gets the pubkey
            Optional<AccessKey> accessKeyOptional = repository.findById(id);
            if (accessKeyOptional.isEmpty()) {
                return null;
            }

            AccessKey accessKey = accessKeyOptional.get();
            String publicKey = accessKey.getPublicKey();

            // second creates a challenge object and gives it the pubkey
            Challenge challenge = new Challenge(publicKey);

            return challenge;

        } catch (Exception e) {
            // log exception if necessary
            return null; // Something went wrong
        }    
    }

    public String checkResponse(String sign, Challenge challenge){
        try {
            // first it hashes the challenge that was sent
            String diggest = challenge.hashChallenge();

            // then it it tries to decypher the sign, if it can then the private key and pub key match
            String decypheredSign = challenge.decypher(sign);
            if (decypheredSign == null){
                return null; // the keys don't match it doesn't allow entry
            }

            // then it checks if the decyphered sign matches the diggest expected
            if (diggest.compareTo(decypheredSign) != 0){
                return null; 
            }

            return sign; // this is a place holder i still need to decide how the response goes

        } catch (Exception e){
            return null;
        }
    }

}

