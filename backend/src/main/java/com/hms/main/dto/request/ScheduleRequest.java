package com.hms.main.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import lombok.Data;

@Data
public class ScheduleRequest {

    private DayOfWeek dayOfWeekName;
    
    private LocalTime startTime;
    
    private LocalTime endTime;

    private Boolean isActive;
}
