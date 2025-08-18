package com.hms.main.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.main.entity.DoctorSchedule;


public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID>{
    
}
