package com.joyboy.polaris_digitech.dto;

import com.joyboy.polaris_digitech.model.BoxState;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BoxResponse {
    private Long id;
    private String txref;
    private Integer weightLimit;
    private Integer batteryCapacity;
    private BoxState state;
    private Integer currentWeight;
    private List<ItemResponse> items;
}