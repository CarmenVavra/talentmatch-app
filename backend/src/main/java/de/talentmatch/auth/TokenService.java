package de.talentmatch.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import de.talentmatch.config.TalentMatchProperties;

/**
 * Stellt signierte JWTs (HS256) mit der Rolle des Benutzers aus.
 */
@Service
public class TokenService {

    public static final String ROLES_CLAIM = "roles";
    private static final String ISSUER = "talentmatch";

    private final JwtEncoder jwtEncoder;
    private final TalentMatchProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, TalentMatchProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwt().expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .claim(ROLES_CLAIM, List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
