package com.hms.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.service.DoctorService;

@Controller
@RequestMapping("/api/v1/doctors")
public class DoctorController {

     private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }
    
    @PostMapping
    public DoctorResponse createDoctor(@RequestBody CreateDoctorRequest doctor) {
        return doctorService.createDoctor(doctor);
    }
}
