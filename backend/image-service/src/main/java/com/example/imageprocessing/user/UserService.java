package com.example.imageprocessing.user;

import com.example.imageprocessing.jwt.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository repository, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(User user) {
        // Check if user already exists (use findFirstByUsername to handle existing duplicates)
        if (repository.findFirstByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        user.setPassword(encoder.encode(user.getPassword()));
        repository.save(user);
        return jwtUtil.generateToken(user.getUsername());
    }

    public String login(String username, String password) {
        return repository.findFirstByUsername(username)
                .filter(u -> encoder.matches(password, u.getPassword()))
                .map(u -> jwtUtil.generateToken(u.getUsername()))
                .orElseThrow();
    }
}
