package com.hms.main.mapper;

import com.hms.main.dto.response.DoctorResponse;
import com.hms.main.dto.response.user.UserResponse;
import com.hms.main.dto.response.ScheduleResponse;
import com.hms.main.entity.Doctor;
import com.hms.main.entity.DoctorSchedule;

import java.util.List;
import java.util.Set;

public class DoctorMapper {

        private DoctorMapper() {
        }

        public static DoctorResponse toResponse(Doctor doctor) {
                if (doctor == null) {
                        return null;
                }

                return DoctorResponse.builder()
                                .id(doctor.getId())
                                .specialty(doctor.getSpecialty())
                                .licenseNumber(doctor.getLicenseNumber())
                                .yearsOfExperience(doctor.getYearsOfExperience())
                                .createdAt(doctor.getCreatedAt())
                                .updatedAt(doctor.getUpdatedAt())
                                .user(UserResponse.builder()
                                                .id(doctor.getUser().getId())
                                                .fullName(doctor.getUser().getFullName())
                                                .email(doctor.getUser().getEmail())
                                                .phoneNumber(doctor.getUser().getPhoneNumber())
                                                .gender(doctor.getUser().getGender())
                                                .dateOfBirth(doctor.getUser().getDateOfBirth())
                                                .address(doctor.getUser().getAddress())
                                                .emergencyContactName(doctor.getUser().getEmergencyContactName())
                                                .emergencyContactPhone(doctor.getUser().getEmergencyContactPhone())
                                                .build())
                                .schedules(mapSchedules(doctor.getSchedules()))
                                .build();
        }

        private static List<ScheduleResponse> mapSchedules(Set<DoctorSchedule> schedules) {
                if (schedules == null)
                        return List.of();

                return schedules.stream()
                                .map(schedule -> ScheduleResponse.builder()
                                                .id(schedule.getId())
                                                .dayOfWeek(schedule.getDayOfWeek())
                                                .startTime(schedule.getStartTime())
                                                .endTime(schedule.getEndTime())
                                                .isActive(schedule.getIsActive())
                                                .build())
                                .toList();
        }

}
