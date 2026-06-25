package com.afya.platform.auth.repository;

import com.afya.platform.auth.model.RevokedAccessJti;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessJtiRepository extends JpaRepository<RevokedAccessJti, String> {

    boolean existsByJti(String jti);
}
