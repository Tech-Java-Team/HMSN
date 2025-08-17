package com.hms.main.dto.request;

import lombok.Data;

@Data
public class ScheduleRequest {
    
    private String dayOfWeek;
    
    private Boolean isActive;
    
    private String startTime;
    
    private String endTime;

    private String dayOfWeekName;
}
