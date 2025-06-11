package com.accessauth.service;

import com.accessauth.domain.User;
import com.accessauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveUserById(Long id) {
        return userRepository.findByIdAndActiveTrue(id);
    }

    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsByIdAndActiveTrue(id);
    }

    @Transactional(readOnly = true)
    public Optional<String> getSymmetricKey(Long id) {
        return userRepository.getSymKeyById(id);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.findById(id)
                .ifPresent(user -> userRepository.delete(user));
    }

    public boolean deactivateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
