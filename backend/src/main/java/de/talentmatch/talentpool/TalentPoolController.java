package de.talentmatch.talentpool;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.talentpool.api.PublicTalentPoolDto;
import de.talentmatch.talentpool.api.TalentPoolEntryDto;
import de.talentmatch.talentpool.api.TalentPoolMailDto;
import de.talentmatch.talentpool.api.TalentPoolRequest;

/**
 * Evidenz: HR-Endpunkte unter {@code /api}, Antwortseite der Bewerber:in ohne Anmeldung unter {@code /api/public}.
 */
@RestController
public class TalentPoolController {

    private final TalentPoolService talentPoolService;

    public TalentPoolController(TalentPoolService talentPoolService) {
        this.talentPoolService = talentPoolService;
    }

    @GetMapping("/api/applications/{id}/talent-pool/preview")
    public TalentPoolMailDto preview(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return talentPoolService.preview(id, jwt.getSubject());
    }

    @PostMapping("/api/applications/{id}/talent-pool")
    public ApplicationDetailDto request(@PathVariable Long id, @Valid @RequestBody TalentPoolRequest request) {
        return talentPoolService.request(id, request);
    }

    @GetMapping("/api/talent-pool")
    public List<TalentPoolEntryDto> list() {
        return talentPoolService.list();
    }

    @GetMapping("/api/public/talent-pool/{token}")
    public PublicTalentPoolDto answerPage(@PathVariable String token) {
        return talentPoolService.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Evidenz-Anfrage", "(Link)"));
    }

    @PostMapping("/api/public/talent-pool/{token}/confirm")
    public PublicTalentPoolDto confirm(@PathVariable String token) {
        return talentPoolService.confirm(token);
    }

    @PostMapping("/api/public/talent-pool/{token}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable String token) {
        talentPoolService.decline(token);
    }
}
