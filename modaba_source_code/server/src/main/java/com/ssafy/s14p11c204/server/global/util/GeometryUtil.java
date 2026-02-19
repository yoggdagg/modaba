package com.ssafy.s14p11c204.server.global.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

@Component
public class GeometryUtil {

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final WKTReader wktReader = new WKTReader();

    /**
     * WKT 문자열을 JTS Geometry 객체로 변환
     */
    public Geometry parseWkt(String wkt) {
        try {
            return wktReader.read(wkt);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid WKT format: " + wkt, e);
        }
    }

    /**
     * 위도, 경도로 Point 객체 생성
     */
    public Point createPoint(double lat, double lng) {
        return geometryFactory.createPoint(new Coordinate(lng, lat)); // JTS는 (x, y) = (lng, lat) 순서
    }

    /**
     * 점이 구역 안에 포함되는지 확인
     */
    public boolean contains(Geometry boundary, double lat, double lng) {
        Point point = createPoint(lat, lng);
        return boundary.contains(point);
    }
    
    /**
     * 점이 구역 밖으로 나갔는지 확인 (Buffer 적용 가능)
     * bufferDistance: 허용 오차 (단위: 도. 약 0.00001 = 1m)
     */
    public boolean isOutOfBound(Geometry boundary, double lat, double lng, double bufferDistance) {
        Point point = createPoint(lat, lng);
        // 버퍼를 적용한 구역 생성 (약간 더 넓게)
        Geometry bufferedBoundary = boundary.buffer(bufferDistance);
        return !bufferedBoundary.contains(point);
    }
    
    /**
     * 점이 대상 구역(또는 점) 근처에 있는지 확인
     * distanceLimit: 허용 거리 (단위: 도. 약 0.00001 = 1m)
     */
    public boolean isNear(Geometry target, double lat, double lng, double distanceLimit) {
        Point point = createPoint(lat, lng);
        // 대상 구역에 버퍼를 적용하여, 그 안에 점이 들어오는지 확인
        return target.buffer(distanceLimit).contains(point);
    }
}
