package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.RoleRequest;
import com.daypulse.auth_serivce.dto.response.RoleResponse;
import com.daypulse.auth_serivce.mapper.RoleMapper;
import com.daypulse.auth_serivce.repository.PermissionRepository;
import com.daypulse.auth_serivce.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request){
        var role = roleMapper.toRole(request);

        var permissions = permissionRepository.findAllById(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }
}
