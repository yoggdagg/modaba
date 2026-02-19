package com.ssafy.s14p11c204.server.domain.region.mapper;

import com.ssafy.s14p11c204.server.domain.region.dto.RegionNeighborhoodDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RegionMapper {
    // 1단계: 시/도 목록 조회
    List<String> findAllCities();

    // 2단계: 시/군/구 목록 조회
    List<String> findDistrictsByCity(@Param("city") String city);

    // 3단계: 읍/면/동 목록 조회
    List<RegionNeighborhoodDto> findNeighborhoods(@Param("city") String city, @Param("district") String district);
}