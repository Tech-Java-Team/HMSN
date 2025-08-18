package com.hms.main.dto.request.user;

import com.hms.main.entity.enums.BloodType;
import com.hms.main.entity.enums.Gender;
import com.hms.main.entity.enums.Roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    private String fullName;

    private String phoneNumber;

    private Gender gender;

    private BloodType bloodType;

    private String address;

    private LocalDate dateOfBirth;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private Roles role;
}
