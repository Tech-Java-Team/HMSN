package com.hms.main.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse {

    private UUID id;
    private String name;
    private String description;
    private String price;
    private Integer durationMinutes;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
}
