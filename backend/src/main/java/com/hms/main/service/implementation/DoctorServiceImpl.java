package com.hms.main.service.implementation;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hms.main.dto.request.CreateDoctorRequest;
import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.entity.Doctor;
import com.hms.main.entity.DoctorSchedule;
import com.hms.main.entity.User;
import com.hms.main.entity.enums.Roles;
import com.hms.main.mapper.DoctorMapper;
import com.hms.main.repository.DoctorRepository;
import com.hms.main.repository.DoctorScheduleRepository;
import com.hms.main.repository.UserRepository;
import com.hms.main.service.DoctorService;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackOn = Exception.class)
    public DoctorResponse createDoctor(User currentUser, CreateDoctorRequest request) {
        if (currentUser.getRole() != Roles.ADMIN) {
            throw new RuntimeException("Only admins can create doctors");
        }
        try {
            validateCreateDoctorRequest(request);

            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setGender(request.getGender());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setAddress(request.getAddress());
            user.setEmergencyContactName(request.getEmergencyContactName());
            user.setEmergencyContactPhone(request.getEmergencyContactPhone());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            user = userRepository.save(user);

            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setSpecialty(request.getSpecialty());
            doctor.setLicenseNumber(request.getLicenseNumber());
            doctor.setYearsOfExperience(request.getYearsOfExperience());

            Doctor savedDoctor = doctorRepository.save(doctor);

            if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
                List<DoctorSchedule> schedules = request.getSchedules().stream()
                        .map(s -> DoctorSchedule.builder()
                                .doctor(savedDoctor)
                                .dayOfWeek(s.getDayOfWeekName())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .isActive(s.getIsActive())
                                .build())
                        .collect(Collectors.toList());

                doctorScheduleRepository.saveAll(schedules);
                savedDoctor.setSchedules(new HashSet<>(schedules));
            }

            return DoctorMapper.toResponse(doctor);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create doctor", e);
        }
    }

    @Transactional
    public DoctorResponse updateDoctor(User currentUser, UUID doctorId, CreateDoctorRequest request) {
        if (currentUser.getRole() != Roles.ADMIN) {
            throw new RuntimeException("Only admins can update doctors");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        User user = doctor.getUser();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setEmergencyContactName(request.getEmergencyContactName());
        user.setEmergencyContactPhone(request.getEmergencyContactPhone());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        doctor.setSpecialty(request.getSpecialty());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setYearsOfExperience(request.getYearsOfExperience());

        doctorScheduleRepository.deleteAll(doctor.getSchedules());
        doctor.getSchedules().clear();

        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<DoctorSchedule> schedules = request.getSchedules().stream()
                    .map(s -> DoctorSchedule.builder()
                            .doctor(doctor)
                            .dayOfWeek(s.getDayOfWeekName())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .isActive(s.getIsActive())
                            .build())
                    .collect(Collectors.toList());

            doctorScheduleRepository.saveAll(schedules);
            doctor.setSchedules(new HashSet<>(schedules));
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);

        return DoctorMapper.toResponse(updatedDoctor);
    }

    @Transactional
    public void deleteDoctor(User currentUser, UUID doctorId) {
        if (currentUser.getRole() != Roles.ADMIN) {
            throw new RuntimeException("Only admins can delete doctors");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        doctorScheduleRepository.deleteAll(doctor.getSchedules());

        doctorRepository.delete(doctor);

        userRepository.delete(doctor.getUser());
    }

    @Transactional
    public List<DoctorResponse> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();

        return doctors.stream()
                .map(DoctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    void validateCreateDoctorRequest(CreateDoctorRequest request) {
        if (request == null) {
            throw new ValidationException("User information is required");
        }
        if (StringUtils.isBlank(request.getSpecialty())) {
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