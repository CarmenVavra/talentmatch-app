package de.talentmatch.auth;

import static de.talentmatch.support.WebMvcSecurityTestConfig.hrManager;
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
}
