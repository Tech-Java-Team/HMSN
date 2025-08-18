package com.hms.main.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.hms.main.dto.response.user.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    private UserResponse user;
    
    private UUID id;
    private String fullName;
    private String specialty;
    private String licenseNumber;
    private Integer yearsOfExperience;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    private List<ScheduleResponse> schedules;
    private List<ServiceResponse> services;
}
