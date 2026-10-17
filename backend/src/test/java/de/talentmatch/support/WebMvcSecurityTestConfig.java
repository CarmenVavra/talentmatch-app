package de.talentmatch.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import de.talentmatch.config.SecurityConfig;
import de.talentmatch.config.TalentMatchProperties;

/**
 * Lädt die echte Security-Konfiguration in {@code @WebMvcTest}-Slices.
 */
@TestConfiguration
@EnableConfigurationProperties(TalentMatchProperties.class)
@Import(SecurityConfig.class)
public class WebMvcSecurityTestConfig {

    public static JwtRequestPostProcessor hrManager() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject("katrin.vogel@talentmatch.de"))
                .authorities(new SimpleGrantedAuthority("ROLE_HR_MANAGER"));
    }

    public static JwtRequestPostProcessor applicant() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject("bewerber@talentmatch.de"))
                .authorities(new SimpleGrantedAuthority("ROLE_APPLICANT"));
    }
}
