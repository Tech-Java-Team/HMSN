package com.hms.main.service.implementation;

import com.hms.main.dto.request.auth.AuthenticationRequest;
import com.hms.main.dto.request.auth.RegisterRequest;
import com.hms.main.dto.response.AuthenticationResponse;
import com.hms.main.entity.User;
import com.hms.main.mapper.UserMapper;
import com.hms.main.repository.UserRepository;
import com.hms.main.service.AuthenticationService;
import com.hms.main.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email address is already taken.");
        }
        User user = UserMapper.toRegisterRequest(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
        .token(jwtToken)
        .user(UserMapper.toUserResponseDto(user))
        .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication."));

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }
}