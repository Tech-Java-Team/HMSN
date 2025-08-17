package com.hms.main.dto.request;

import java.util.List;

import com.hms.main.entity.User;

import lombok.Data;

@Data
public class CreateDoctorRequest {
    
    private User user;

    private String speciality;

    private String licenseNumber;

    private Integer yearsOfExperience;

    private List<ScheduleRequest> schedules;
}
