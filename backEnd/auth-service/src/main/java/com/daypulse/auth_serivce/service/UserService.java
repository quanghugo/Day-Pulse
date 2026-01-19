package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.UserCreationRequest;
import com.daypulse.auth_serivce.dto.request.UserUpdateRequest;
import com.daypulse.auth_serivce.dto.response.UserResponse;
import com.daypulse.auth_serivce.entity.Role;
import com.daypulse.auth_serivce.entity.User;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.mapper.UserMapper;
import com.daypulse.auth_serivce.repository.RoleRepository;
import com.daypulse.auth_serivce.repository.UserRepository;
import com.daypulse.auth_serivce.util.constant.PredefinedRole;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;

    public UserResponse createUser(UserCreationRequest request){
        if (userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.ROLE_USER).ifPresent(roles::add);
        user.setRoles(roles);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getMyInfo(){
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId){
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasAuthority('APPROVE_DATA')")
    public List<UserResponse> getUsers(){
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id){
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }
}
