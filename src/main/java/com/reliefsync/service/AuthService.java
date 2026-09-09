package com.reliefsync.service;

import com.reliefsync.model.User;
import com.reliefsync.model.Role;
import com.reliefsync.repository.UserRepository;
import com.reliefsync.security.PasswordHasher;
import java.util.Optional;
import java.util.Locale;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_]{3,30}");

    private final UserRepository users = new UserRepository();

    public Optional<User> login(String username, String password) {
        String normalized = normalizeUsername(username);
        if (normalized.isEmpty() || password == null) {
            return Optional.empty();
        }
        Optional<String> hash = users.passwordHash(normalized);
        if (hash.isEmpty() || !PasswordHasher.verify(password, hash.get())) {
            return Optional.empty();
        }
        return users.findByUsername(normalized);
    }

    /** Public registration intentionally creates only least-privileged volunteer accounts. */
    public User signup(String fullName, String username, String password, String confirmPassword) {
        String normalizedName = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        String normalizedUsername = normalizeUsername(username);
        if (normalizedName.length() < 2 || normalizedName.length() > 80) {
            throw new IllegalArgumentException("Full name must be between 2 and 80 characters");
        }
        if (!USERNAME.matcher(normalizedUsername).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters using lowercase letters, numbers, or underscores");
        }
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
        if (users.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalStateException("Username '" + normalizedUsername + "' is already registered");
        }
        long id = users.insert(normalizedUsername, normalizedName, Role.VOLUNTEER,
                PasswordHasher.hash(password));
        return new User(id, normalizedUsername, normalizedName, Role.VOLUNTEER);
    }

    public boolean hasAnyUser() {
        return users.count() > 0;
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters");
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!upper || !lower || !digit) {
            throw new IllegalArgumentException(
                    "Password must contain an uppercase letter, a lowercase letter, and a number");
        }
    }
}
