package com.joyboy.polaris_digitech.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatteryResponse {
    private String txref;
    private Integer batteryCapacity;
}