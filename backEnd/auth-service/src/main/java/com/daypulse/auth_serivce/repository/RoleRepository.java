package com.daypulse.auth_serivce.repository;

import com.daypulse.auth_serivce.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
}

