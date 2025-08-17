package com.hms.main.dto.response;

import java.time.LocalTime;

import lombok.Data;

@Data
public class ScheduleResponse {
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
