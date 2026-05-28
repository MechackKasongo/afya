package com.afya.platform.identity.repository;

import com.afya.platform.identity.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    Optional<Role> findByCodeIgnoreCase(String code);
}
