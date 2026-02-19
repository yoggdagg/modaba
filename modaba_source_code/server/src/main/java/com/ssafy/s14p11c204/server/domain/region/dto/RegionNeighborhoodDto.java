package com.ssafy.s14p11c204.server.domain.region.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegionNeighborhoodDto {
    private Integer regionId;
    private String name; // neighborhood
    private Double lat;  // center_lat
    private Double lng;  // center_lng
}