package de.talentmatch.auth;

import static de.talentmatch.support.WebMvcSecurityTestConfig.hrManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.talentmatch.auth.api.LoginResponse;
import de.talentmatch.auth.api.UserDto;
import de.talentmatch.common.exception.InvalidTokenException;
import de.talentmatch.common.exception.LoginLockedException;
import de.talentmatch.support.WebMvcSecurityTestConfig;

@WebMvcTest(AuthController.class)
@Import(WebMvcSecurityTestConfig.class)
class AuthControllerTest {

    private static final UserDto USER = new UserDto(1L, "katrin.vogel@talentmatch.de", "Katrin", "Vogel",
            "People & Culture", Role.HR_MANAGER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void loginIsPublicAndReturnsToken() throws Exception {
        when(authService.login("katrin.vogel@talentmatch.de", "geheim"))
                .thenReturn(new LoginResponse("token", Instant.parse("2026-10-17T18:00:00Z"), USER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"katrin.vogel@talentmatch.de\",\"password\":\"geheim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.user.role").value("HR_MANAGER"));
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        when(authService.login("katrin.vogel@talentmatch.de", "falsch"))
                .thenThrow(new BadCredentialsException("x"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"katrin.vogel@talentmatch.de\",\"password\":\"falsch\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-Mail oder Passwort ist falsch."));
    }

    @Test
    void registerIsPublicAndReturns201() throws Exception {
        UserDto applicantUser = new UserDto(2L, "max@example.org", "Max", "Mustermann", null, Role.APPLICANT);
        when(authService.register(any())).thenReturn(new LoginResponse("token", Instant.EPOCH, applicantUser));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Max","lastName":"Mustermann","email":"max@example.org",
                                 "password":"geheim-123","privacyAccepted":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("APPLICANT"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"M\",\"email\":\"x\",\"password\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.privacyAccepted").exists());
    }

    @Test
    void loginValidatesEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kein-mail\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(authService.findUser("katrin.vogel@talentmatch.de")).thenReturn(Optional.of(USER));

        mockMvc.perform(get("/api/auth/me").with(hrManager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("People & Culture"));
    }

    @Test
    void changePasswordRequiresMinimumLength() throws Exception {
        mockMvc.perform(post("/api/auth/change-password").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"alt\",\"newPassword\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void changePasswordReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/change-password").with(hrManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"alt-passwort\",\"newPassword\":\"neues-passwort\"}"))
                .andExpect(status().isNoContent());
        verify(authService).changePassword("katrin.vogel@talentmatch.de", "alt-passwort", "neues-passwort");
    }

    @Test
    void lockedLoginReturns429WithTheReason() throws Exception {
        when(authService.login("katrin.vogel@talentmatch.de", "geheim"))
                .thenThrow(new LoginLockedException("Zu viele fehlgeschlagene Anmeldeversuche."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"katrin.vogel@talentmatch.de\",\"password\":\"geheim\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Zu viele fehlgeschlagene Anmeldeversuche."));
    }

    @Test
    void passwordResetRequestIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"max@example.org\"}"))
                .andExpect(status().isNoContent());
        verify(passwordResetService).requestReset("max@example.org");
    }

    @Test
    void passwordResetConfirmValidatesAndReportsInvalidLinks() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"newPassword\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists())
                .andExpect(jsonPath("$.errors.newPassword").exists());

        doThrow(new InvalidTokenException("Der Link ist ungültig oder abgelaufen."))
                .when(passwordResetService).resetPassword("alt", "neues-passwort");
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"alt\",\"newPassword\":\"neues-passwort\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Link ungültig"));
    }
}
