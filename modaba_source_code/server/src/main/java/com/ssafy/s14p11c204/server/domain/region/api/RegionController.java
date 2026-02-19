package com.ssafy.s14p11c204.server.domain.region.api;

import com.ssafy.s14p11c204.server.domain.region.dto.RegionNeighborhoodDto;
import com.ssafy.s14p11c204.server.domain.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v0/regions")
@RequiredArgsConstructor
@Tag(name = "RegionController", description = "지역 조회 API")
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/cities")
    @Operation(summary = "시/도 목록 조회", description = "전국의 시/도(1단계) 목록을 가져옵니다.")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(regionService.findAllCities());
    }

    @GetMapping("/districts")
    @Operation(summary = "시/군/구 목록 조회", description = "선택된 시/도에 속한 시/군/구(2단계) 목록을 가져옵니다.")
    public ResponseEntity<List<String>> getDistricts(@RequestParam String city) {
        return ResponseEntity.ok(regionService.findDistrictsByCity(city));
    }

    @GetMapping("/neighborhoods")
    @Operation(summary = "읍/면/동 목록 조회", description = "선택된 시/군/구에 속한 읍/면/동(3단계) 및 중심 좌표 정보를 가져옵니다.")
    public ResponseEntity<List<RegionNeighborhoodDto>> getNeighborhoods(@RequestParam String city, @RequestParam String district) {
        return ResponseEntity.ok(regionService.findNeighborhoods(city, district));
    }
}