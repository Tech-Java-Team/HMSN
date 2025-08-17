package com.hms.main.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class DoctorResponse {
    
    private UUID id;
    private String fullName;
    private String specialty;
    private Integer yearsOfExperience;

    private List<ScheduleResponse> schedules;
    private List<ServiceResponse> services;
}
