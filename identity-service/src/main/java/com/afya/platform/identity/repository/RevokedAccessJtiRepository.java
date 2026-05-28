package com.afya.platform.identity.repository;

import com.afya.platform.identity.model.RevokedAccessJti;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessJtiRepository extends JpaRepository<RevokedAccessJti, String> {

    boolean existsByJti(String jti);
}
