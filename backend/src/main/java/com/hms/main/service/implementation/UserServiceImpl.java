package com.hms.main.service.implementation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hms.main.dto.request.user.CreateUserRequest;
import com.hms.main.dto.request.user.UpdateUserRequest;
import com.hms.main.dto.response.user.UserResponse;
import com.hms.main.entity.User;
import com.hms.main.mapper.UserMapper;
import com.hms.main.repository.UserRepository;
import com.hms.main.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toUserResponseDto);
    }

    public Optional<UserResponse> getUserById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toUserResponseDto);
    }

    public UserResponse createUser(CreateUserRequest request) {
        User user = UserMapper.toUser(request);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserResponseDto(savedUser);
    }

    public Optional<UserResponse> updateUser(UUID id, UpdateUserRequest request) {
        return userRepository.findById(id).map(existingUser -> {
            UserMapper.updateUserFromDto(existingUser, request);
            User updateUser = userRepository.save(existingUser);
            return UserMapper.toUserResponseDto(updateUser);
        });
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalStateException("User with id " + id + " does not exist.");
        }
        userRepository.deleteById(id);
    }

}
