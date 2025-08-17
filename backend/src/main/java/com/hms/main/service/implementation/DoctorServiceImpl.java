package com.hms.main.service.implementation;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.request.ScheduleRequest;
import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.entity.Doctor;
import com.hms.main.entity.DoctorSchedule;
import com.hms.main.entity.User;
import com.hms.main.mapper.DoctorMapper;
import com.hms.main.repository.DoctorRepository;
import com.hms.main.repository.ScheduleRepository;
import com.hms.main.repository.UserRepository;
import com.hms.main.service.DoctorService;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackOn = Exception.class)
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        try {
            // 1. Validate input (custom method if you want extra checks)
            validateCreateDoctorRequest(request);

            User user = new User();
            user.setFullName(request.getUser().getFullName());
            user.setEmail(request.getUser().getEmail());
            user.setPhoneNumber(request.getUser().getPhoneNumber());
            user.setGender(request.getUser().getGender());
            user.setDateOfBirth(request.getUser().getDateOfBirth());
            user.setAddress(request.getUser().getAddress());
            user.setEmergencyContactName(request.getUser().getEmergencyContactName());
            user.setEmergencyContactPhone(request.getUser().getEmergencyContactPhone());
            user.setPassword(passwordEncoder.encode(request.getUser().getPassword()));

            user = userRepository.save(user);

            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setSpeciality(request.getSpeciality());
            doctor.setLicenseNumber(request.getLicenseNumber());
            doctor.setYearsOfExperience(request.getYearsOfExperience());

            doctor = doctorRepository.save(doctor);

            if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
                List<DoctorSchedule> schedules = request.getSchedules().stream()
                        .map(s -> DoctorSchedule.builder()
                                .doctor(doctor)
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .isActive(s.getIsActive())
                                .build())
                        .collect(Collectors.toList());

                doctorScheduleRepository.saveAll(schedules);
                doctor.setSchedules(schedules);
            }

            return DoctorMapper.toResponse(doctor);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create doctor", e);
        }
    }

}

    void validateCreateDoctorRequest(CreateDoctorRequest request) {
        if (request.getUser() == null) {
            throw new ValidationException("User information is required");
        }
        if (StringUtils.isBlank(request.getSpeciality())) {
            throw new ValidationException("Specialty is required");
        }
        if (StringUtils.isBlank(request.getLicenseNumber())) {
            throw new ValidationException("License number is required");
        }
        if (request.getYearsOfExperience() == null || request.getYearsOfExperience() < 0) {
            throw new ValidationException("Years of experience must be a non-negative number");
        }
    }
}
