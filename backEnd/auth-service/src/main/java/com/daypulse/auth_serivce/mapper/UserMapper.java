package com.daypulse.auth_serivce.mapper;

import com.daypulse.auth_serivce.dto.request.RegisterRequest;
import com.daypulse.auth_serivce.dto.request.UserCreationRequest;
import com.daypulse.auth_serivce.dto.request.UserUpdateRequest;
import com.daypulse.auth_serivce.dto.response.UserResponse;
import com.daypulse.auth_serivce.dto.response.UserSummary;
import com.daypulse.auth_serivce.entity.UserAuth;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "passwordHash", source = "password")
    UserAuth toUserAuth(RegisterRequest request);

    UserAuth toUserAuth(UserCreationRequest request);

    UserResponse toUserResponse(UserAuth user);

    UserSummary toUserSummary(UserAuth user);

    void updateUser(@MappingTarget UserAuth user, UserUpdateRequest request);
}
