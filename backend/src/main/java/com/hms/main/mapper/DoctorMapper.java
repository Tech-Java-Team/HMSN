package com.hms.main.mapper;

import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.entity.Doctor;
import com.hms.main.entity.DoctorSchedule;
import com.hms.main.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DoctorMapper {

    public static Doctor toDoctor(CreateDoctorRequest request) {
        User user = User.builder()
                .fullName(request.user.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .role(Role.DOCTOR)
                .enabled(true)
                .build();

        // 2️⃣ Create Doctor
        Doctor doctor = Doctor.builder()
                .user(user)
                .speciality(request.getSpecialty())
                .licenseNumber(request.getLicenseNumber())
                .yearsOfExperience(request.getYearsOfExperience())
                .build();

        // 3️⃣ Map schedules
        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            Set<DoctorSchedule> schedules = request.getSchedules().stream()
                    .map(s -> {
                        DoctorSchedule schedule = DoctorSchedule.builder()
                                .doctor(doctor)
                                .dayOfWeek(DayOfWeek.valueOf(s.getDayOfWeek().toUpperCase()))
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .isActive(s.getIsActive() != null ? s.getIsActive() : true)
                                .build();
                        return schedule;
                    })
                    .collect(Collectors.toSet());
            doctor.setSchedules(schedules);
        }

        return doctor;
    }

    // --------------------------
    // Doctor -> DoctorResponse
    public DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getUser().getFullName())
                .specialty(doctor.getSpeciality())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .schedules(
                        doctor.getSchedules().stream()
                                .map(s -> DoctorResponse.ScheduleResponse.builder()
                                        .dayOfWeek(s.getDayOfWeek().name())
                                        .startTime(s.getStartTime())
                                        .endTime(s.getEndTime())
                                        .isActive(s.getIsActive())
                                        .build())
                                .collect(Collectors.toList())
                )
                .services(doctor.getServices().stream()
                        .map(s -> DoctorResponse.ServiceResponse.builder()
                                .id(s.getId())
                                .name(s.getName())
                                .description(s.getDescription())
                                .price(s.getPrice())
                                .durationMinutes(s.getDurationMinutes())
                                .isActive(s.getIsActive())
                                .createdAt(s.getCreatedAt().toString())
                                .updatedAt(s.getUpdatedAt().toString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
