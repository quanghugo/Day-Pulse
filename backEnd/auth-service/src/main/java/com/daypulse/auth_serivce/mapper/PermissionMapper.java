package com.daypulse.auth_serivce.mapper;

import com.daypulse.auth_serivce.dto.request.PermissionRequest;
import com.daypulse.auth_serivce.dto.response.PermissionResponse;
import com.daypulse.auth_serivce.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);
    PermissionResponse toPermissionResponse(Permission permission);
}

