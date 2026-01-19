package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.PermissionRequest;
import com.daypulse.auth_serivce.dto.response.PermissionResponse;
import com.daypulse.auth_serivce.entity.Permission;
import com.daypulse.auth_serivce.mapper.PermissionMapper;
import com.daypulse.auth_serivce.repository.PermissionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public PermissionResponse createPermissionResponse(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        log.info("Permission created with name: {}", permission.getName());
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        log.info("Fetched {} permissions", permissions.size());
        return permissions.stream()
                .map(permissionMapper::toPermissionResponse)
                .toList();
    }

    public void deletePermission(String permissionName) {
        permissionRepository.deleteById(permissionName);
        log.info("Permission deleted with name: {}", permissionName);
    }
}
