package com.ssafy.s14p11c204.server.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScans({
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.social.mapper.FriendMapper.class),
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.user.Repositories.UserMapper.class),
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.game.dao.RoomMapper.class),
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.user.dao.UserMapperTemp.class),
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.region.mapper.RegionMapper.class),
        @MapperScan(basePackageClasses = com.ssafy.s14p11c204.server.domain.chat.mapper.ChatMapper.class),
})
class MyBatisConfig {
}