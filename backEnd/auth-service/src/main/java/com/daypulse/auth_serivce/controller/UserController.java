package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.UserCreationRequest;
import com.daypulse.auth_serivce.dto.request.UserUpdateRequest;
import com.daypulse.auth_serivce.dto.response.ApiBaseResponse;
import com.daypulse.auth_serivce.dto.response.UserResponse;
import com.daypulse.auth_serivce.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping
    ApiBaseResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request){
        log.info("Creating user: {}", request);
        ApiBaseResponse<UserResponse> response = new ApiBaseResponse<>();
        response.setResult(userService.createUser(request));
        return response;
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    UserResponse updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request){
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    String deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
        return "User has been deleted";
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    UserResponse getUser(@PathVariable("userId") String userId){
        return userService.getUser(userId);
    }

    @GetMapping("/my-info")
    @PreAuthorize("isAuthenticated()")
    ApiBaseResponse<UserResponse> getMyInfo() {
        return ApiBaseResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<UserResponse> getUsers(){
        return userService.getUsers();
    }
}
