package com.hms.main.dto.request;

import java.time.LocalDate;
import java.util.List;

import com.hms.main.entity.enums.BloodType;
import com.hms.main.entity.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateDoctorRequest {

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

    private String specialty;

    private String licenseNumber;

    private Integer yearsOfExperience;

    private List<ScheduleRequest> schedules;
}
