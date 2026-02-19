package com.ssafy.s14p11c204.server.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import static org.assertj.core.api.Assertions.assertThat;

class GeometryUtilTest {

    private final GeometryUtil geometryUtil = new GeometryUtil();

    @Test
    @DisplayName("구역 내부 점은 이탈이 아니다")
    void insidePoint() {
        // Given: 0,0 ~ 10,10 정사각형
        String wkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
        Geometry boundary = geometryUtil.parseWkt(wkt);

        // When: (5, 5)
        boolean isOut = geometryUtil.isOutOfBound(boundary, 5.0, 5.0, 0.0);

        // Then
        assertThat(isOut).isFalse();
    }

    @Test
    @DisplayName("구역 외부 점은 이탈이다")
    void outsidePoint() {
        // Given
        String wkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
        Geometry boundary = geometryUtil.parseWkt(wkt);

        // When: (20, 20)
        boolean isOut = geometryUtil.isOutOfBound(boundary, 20.0, 20.0, 0.0);

        // Then
        assertThat(isOut).isTrue();
    }

    @Test
    @DisplayName("버퍼(Buffer) 내의 점은 이탈이 아니다")
    void bufferZone() {
        // Given
        String wkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
        Geometry boundary = geometryUtil.parseWkt(wkt);
        double buffer = 1.0; // 1도 (테스트용 큰 값)

        // When: (10.5, 5) -> 경계선(10)보다 0.5 밖이지만 버퍼(1.0) 안쪽
        boolean isOut = geometryUtil.isOutOfBound(boundary, 5.0, 10.5, buffer);

        // Then
        assertThat(isOut).isFalse();
    }

    @Test
    @DisplayName("버퍼 밖의 점은 이탈이다")
    void outsideBuffer() {
        // Given
        String wkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))";
        Geometry boundary = geometryUtil.parseWkt(wkt);
        double buffer = 1.0;

        // When: (11.5, 5) -> 경계선(10)보다 1.5 밖 (버퍼 1.0 초과)
        boolean isOut = geometryUtil.isOutOfBound(boundary, 5.0, 11.5, buffer);

        // Then
        assertThat(isOut).isTrue();
    }
    
    @Test
    @DisplayName("도넛 모양 구역 (구멍) 테스트")
    void donutShape() {
        // Given: 0~10 정사각형 안에 4~6 구멍 뚫림
        String wkt = "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0), (4 4, 6 4, 6 6, 4 6, 4 4))";
        Geometry boundary = geometryUtil.parseWkt(wkt);

        // 1. 외곽선 안쪽, 구멍 바깥 (정상) -> (2, 2)
        assertThat(geometryUtil.isOutOfBound(boundary, 2.0, 2.0, 0.0)).isFalse();
        
        // 2. 구멍 안쪽 (이탈) -> (5, 5)
        assertThat(geometryUtil.isOutOfBound(boundary, 5.0, 5.0, 0.0)).isTrue();
    }
}
