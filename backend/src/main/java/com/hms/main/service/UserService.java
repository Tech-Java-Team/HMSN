package com.hms.main.service;

import com.hms.main.dto.request.user.CreateUserRequest;
import com.hms.main.dto.request.user.UpdateUserRequest;
import com.hms.main.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    /**
     * Get a paginated list of all users.
     *
     * @param pageable pagination information
     * @return a page of UserResponse DTOs
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Get a single user by ID.
     *
     * @param id the user's UUID
     * @return an Optional containing UserResponse if found
     */
    Optional<UserResponse> getUserById(UUID id);

    /**
     * Create a new user.
     *
     * @param request DTO containing user creation data
     * @return the created user's response DTO
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Update an existing user.
     *
     * @param id      the user's UUID
     * @param request DTO containing user update data
     * @return an Optional containing the updated UserResponse if the user exists
     */
    Optional<UserResponse> updateUser(UUID id, UpdateUserRequest request);

    /**
     * Delete a user by ID.
     *
     * @param id the user's UUID
     */
    void deleteUser(UUID id);
}
