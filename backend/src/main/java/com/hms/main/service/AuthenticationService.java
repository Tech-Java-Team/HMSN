package com.hms.main.service;

import com.hms.main.dto.request.auth.AuthenticationRequest;
import com.hms.main.dto.request.auth.RegisterRequest;
import com.hms.main.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    /**
     * Registers a new user.
     *
     * @param request the user registration data
     * @return an AuthenticationResponse containing the JWT token
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the authentication request containing email and password
     * @return an AuthenticationResponse containing the JWT token
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);
}
