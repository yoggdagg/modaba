package com.ssafy.s14p11c204.server.domain.region.dao;

import com.ssafy.s14p11c204.server.domain.region.dto.RegionNeighborhoodDto;
import com.ssafy.s14p11c204.server.domain.region.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegionDao {
    private final RegionMapper regionMapper;

    public List<String> findAllCities() {
        return regionMapper.findAllCities();
    }

    public List<String> findDistrictsByCity(String city) {
        return regionMapper.findDistrictsByCity(city);
    }

    public List<RegionNeighborhoodDto> findNeighborhoods(String city, String district) {
        return regionMapper.findNeighborhoods(city, district);
    }
}