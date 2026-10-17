package de.talentmatch.seed;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.auth.Role;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.auth.UserAccountRepository;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.job.JobPositionRepository;

/**
 * Legt beim ersten Start Demo-Benutzer, Stellen und Bewerbungen an (Szenarien Top-, Mid- und Mismatch-Kandidat).
 * Abschaltbar über {@code talentmatch.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "talentmatch.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserAccountRepository userRepository;
    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TalentMatchProperties properties;
    private final Clock clock;

    public DemoDataSeeder(UserAccountRepository userRepository, JobPositionRepository jobRepository,
                          ApplicationRepository applicationRepository, PasswordEncoder passwordEncoder,
                          TalentMatchProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        if (jobRepository.count() == 0) {
            DemoData data = new DemoData(LocalDate.now(clock));
            data.jobs().forEach(jobRepository::save);
            applicationRepository.saveAll(data.applications());
            LOG.info("Demo-Daten angelegt: {} Stellen, {} Bewerbungen", data.jobs().size(), data.applications().size());
        }
    }

    private void seedUsers() {
        TalentMatchProperties.Seed seed = properties.seed();
        if (userRepository.findByEmailIgnoreCase(seed.hrEmail()).isEmpty()) {
            userRepository.save(new UserAccount(seed.hrEmail(), passwordEncoder.encode(seed.hrPassword()),
                    "Katrin", "Vogel", "People & Culture", Role.HR_MANAGER));
            LOG.info("Demo-Benutzer (HR-Manager) angelegt: {}", seed.hrEmail());
        }
        if (userRepository.findByEmailIgnoreCase(seed.applicantEmail()).isEmpty()) {
            userRepository.save(new UserAccount(seed.applicantEmail(),
                    passwordEncoder.encode(seed.applicantPassword()), "Max", "Mustermann", null, Role.APPLICANT));
            LOG.info("Demo-Benutzer (Bewerber) angelegt: {}", seed.applicantEmail());
        }
    }
}
