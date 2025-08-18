package com.hms.main.service;

import java.util.List;
import java.util.UUID;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.entity.User;

public interface DoctorService {
    
    DoctorResponse createDoctor(User currentUser, CreateDoctorRequest doctor);

    DoctorResponse updateDoctor(User currentUser, UUID doctorId, CreateDoctorRequest request);

    void deleteDoctor(User currentUser, UUID doctorId);

    List<DoctorResponse> getAllDoctors();
}
