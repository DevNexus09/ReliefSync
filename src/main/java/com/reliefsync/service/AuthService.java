package com.reliefsync.service;

import com.reliefsync.model.User;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.security.PasswordHasher;
import java.util.Optional;

public class AuthService {

    private final UserRepository users = new UserRepository();

    public Optional<User> login(String username, String password) {
        Optional<String> hash = users.passwordHash(username);
        if (hash.isEmpty() || !PasswordHasher.verify(password, hash.get())) {
            return Optional.empty();
        }
        return users.findByUsername(username);
    }

    public boolean hasAnyUser() {
        return users.count() > 0;
    }
}
