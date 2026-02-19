package com.ssafy.s14p11c204.server.domain.region.service;

import com.ssafy.s14p11c204.server.domain.region.dto.RegionNeighborhoodDto;

import java.util.List;

public interface RegionService {
    List<String> findAllCities();
    List<String> findDistrictsByCity(String city);
    List<RegionNeighborhoodDto> findNeighborhoods(String city, String district);
}