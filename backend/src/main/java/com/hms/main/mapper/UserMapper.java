package com.hms.main.mapper;

import com.hms.main.dto.request.auth.RegisterRequest;
import com.hms.main.dto.request.user.CreateUserRequest;
import com.hms.main.dto.request.user.UpdateUserRequest;
import com.hms.main.dto.response.user.UserResponse;
import com.hms.main.entity.User;
import com.hms.main.entity.enums.Gender;
import com.hms.main.entity.enums.Roles;

public class UserMapper {

    public static UserResponse toUserResponseDto(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .bloodType(user.getBloodType())
                .address(user.getAddress())
                .dateOfBirth(user.getDateOfBirth())
                .emergencyContactName(user.getEmergencyContactName())
                .emergencyContactPhone(user.getEmergencyContactPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static User toUser(CreateUserRequest request) {
        if (request == null) return null;

        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender() != null ? request.getGender() : Gender.OTHER)
                .bloodType(request.getBloodType())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .role(request.getRole() != null ? request.getRole() : Roles.PATIENT)
                .build();
    }

    public static User toRegisterRequest(RegisterRequest request) {
        if (request == null) return null;

        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender() != null ? request.getGender() : Gender.OTHER)
                .bloodType(request.getBloodType())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .build();
    }

    public static void updateUserFromDto(User user, UpdateUserRequest request) {
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBloodType() != null) user.setBloodType(request.getBloodType());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmergencyContactName() != null) user.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) user.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
    }
}
