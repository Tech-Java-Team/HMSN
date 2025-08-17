package com.hms.main.dto.request.user;

import com.hms.main.entity.enums.BloodType;
import com.hms.main.entity.enums.Gender;
import com.hms.main.entity.enums.Roles;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    private String fullName;

    private String email;

    private String password;

    private String phoneNumber;

    private Gender gender;

    private BloodType bloodType;

    private String address;

    private LocalDate dateOfBirth;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private Roles role;
}
