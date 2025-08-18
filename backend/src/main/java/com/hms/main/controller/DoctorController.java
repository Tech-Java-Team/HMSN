package com.hms.main.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.entity.User;
import com.hms.main.service.DoctorService;

@Controller
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }
    
    @PostMapping
    public DoctorResponse createDoctor(@AuthenticationPrincipal User currentUser, @RequestBody CreateDoctorRequest doctor) {
        return doctorService.createDoctor(currentUser, doctor);
    }

        // Update Doctor
    @PutMapping("/{id}")
    public DoctorResponse updateDoctor(@AuthenticationPrincipal User currentUser,
                                       @PathVariable UUID id,
                                       @RequestBody CreateDoctorRequest doctor) {
        return doctorService.updateDoctor(currentUser, id, doctor);
    }

    // Delete Doctor
    @DeleteMapping("/{id}")
    public void deleteDoctor(@AuthenticationPrincipal User currentUser,
                             @PathVariable UUID id) {
        doctorService.deleteDoctor(currentUser, id);
    }

    // Get All Doctors
    @GetMapping
    public List<DoctorResponse> getAllDoctors() {
        return doctorService.getAllDoctors();
    }

}
