package com.hms.main.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.main.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, UUID>  {
    
}
