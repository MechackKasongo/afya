package com.afya.platform.identity.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.identity.model.AppUser;
import com.afya.platform.identity.repository.AppUserRepository;
import com.afya.platform.identity.repository.RefreshTokenRepository;
import com.afya.platform.identity.repository.RevokedAccessJtiRepository;
import com.afya.platform.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceAuditTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RevokedAccessJtiRepository revokedAccessJtiRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginFailedPublishesAuditForUnknownUser() {
        when(appUserRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login("unknown", "pwd"));

        verify(auditEventPublisher).publish("LOGIN_FAILED", "USER", "unknown", "unknown", null);
        verify(auditEventPublisher, never()).publish(
                eq("LOGIN_SUCCESS"), eq("USER"), anyString(), anyString(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void loginFailedPublishesAuditForBadPassword() {
        AppUser user = new AppUser();
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setActive(true);
        when(appUserRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login("admin", "wrong"));

        verify(auditEventPublisher).publish("LOGIN_FAILED", "USER", "admin", "admin", null);
    }
}
