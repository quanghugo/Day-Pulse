package com.daypulse.user_service.mapper;

import com.daypulse.user_service.dto.request.ProfileSetupRequest;
import com.daypulse.user_service.dto.request.ProfileUpdateRequest;
import com.daypulse.user_service.dto.response.UserResponse;
import com.daypulse.user_service.dto.response.UserSummaryResponse;
import com.daypulse.user_service.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {
    UserProfile toUserProfile(ProfileSetupRequest request);
    
    UserResponse toUserResponse(UserProfile userProfile);
    
    UserSummaryResponse toUserSummaryResponse(UserProfile userProfile);
    
    void updateUserProfile(@MappingTarget UserProfile userProfile, ProfileUpdateRequest request);
}
