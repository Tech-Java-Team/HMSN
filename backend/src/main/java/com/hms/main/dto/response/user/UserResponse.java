package com.hms.main.dto.response.user;

import com.hms.main.entity.enums.BloodType;
import com.hms.main.entity.enums.Gender;
import com.hms.main.entity.enums.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    private String fullName;

    private String email;

    private String phoneNumber;

    private Gender gender;

    private BloodType bloodType;

    private String address;

    private LocalDate dateOfBirth;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private Roles role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}