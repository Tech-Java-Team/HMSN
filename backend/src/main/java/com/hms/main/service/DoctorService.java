package com.hms.main.service;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.response.DoctorResponse;

public interface DoctorService {
    
    DoctorResponse createDoctor(CreateDoctorRequest doctor);
}
