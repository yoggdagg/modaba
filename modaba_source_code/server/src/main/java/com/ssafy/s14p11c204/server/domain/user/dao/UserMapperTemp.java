package com.ssafy.s14p11c204.server.domain.user.dao;

import com.ssafy.s14p11c204.server.domain.user.dto.CurrentUser;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileDetailResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapperTemp {

    Optional<ProfileDetailResponse> selectDetailedProfile(@Param("userId") Long userId);

    Optional<CurrentUser> selectCurrentUserById(@Param("userId") Long userId);

    Optional<CurrentUser> selectCurrentUserByEmail(@Param("email") String email);

    int updateProfile(@Param("userId") long userId, @Param("nickname") String nickname, @Param("imageUrl") String imageUrl);

    int updatePassword(@Param("userId") long userId, @Param("newPassword") String newPassword);

}
