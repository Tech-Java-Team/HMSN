package com.hms.main.service.implementation;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional(rollbackOn = Exception.class)
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
       try {
           
           validateCreateDoctorRequest(request);
           
           User user = userRepository.save(request.getUser());

           if(user != null){
            Doctor doctor = doctorRepository.save({
                user: user,
                other fields
            })
           }else{
            throw exception
           }

           if(docotr != null){
            List<DoctorSchedule> schedule = new List<>();
            
           }


           
           return "";

       } catch (Exception e) {
           throw new Exception("Failed to create doctor");
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

