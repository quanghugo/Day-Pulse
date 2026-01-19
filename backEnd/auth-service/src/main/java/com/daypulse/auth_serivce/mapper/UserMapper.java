package com.daypulse.auth_serivce.mapper;

import com.daypulse.auth_serivce.dto.request.UserCreationRequest;
import com.daypulse.auth_serivce.dto.request.UserUpdateRequest;
import com.daypulse.auth_serivce.dto.response.UserResponse;
import com.daypulse.auth_serivce.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
