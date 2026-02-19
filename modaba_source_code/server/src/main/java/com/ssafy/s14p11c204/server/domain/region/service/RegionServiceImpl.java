package com.ssafy.s14p11c204.server.domain.region.service;

import com.ssafy.s14p11c204.server.domain.region.dao.RegionDao;
import com.ssafy.s14p11c204.server.domain.region.dto.RegionNeighborhoodDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {
    private final RegionDao regionDao;

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllCities() {
        return regionDao.findAllCities();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistrictsByCity(String city) {
        return regionDao.findDistrictsByCity(city);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionNeighborhoodDto> findNeighborhoods(String city, String district) {
        return regionDao.findNeighborhoods(city, district);
    }
}