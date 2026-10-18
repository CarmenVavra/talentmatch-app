package de.talentmatch.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.talentmatch.common.exception.LoginLockedException;
import de.talentmatch.config.TalentMatchProperties;

class LoginAttemptServiceTest {

    private static final String EMAIL = "max@example.org";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-18T08:00:00Z"));
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        TalentMatchProperties properties = new TalentMatchProperties(null, null, null, null, null, null, null,
                new TalentMatchProperties.Security(3, Duration.ofMinutes(15), Duration.ofHours(1)));
        service = new LoginAttemptService(properties, clock);
    }

    @Test
    void locksAfterTooManyFailuresIgnoringCase() {
        service.recordFailure(EMAIL);
        service.recordFailure(EMAIL);
        assertThatCode(() -> service.checkNotLocked(EMAIL)).doesNotThrowAnyException();

        service.recordFailure(" MAX@example.org ");

        assertThatThrownBy(() -> service.checkNotLocked(EMAIL))
                .isInstanceOf(LoginLockedException.class)
                .hasMessageContaining("15 Minuten");
    }

    @Test
    void lockEndsAfterTheLockDurationAndCountsAnew() {
        lock();
        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.checkNotLocked(EMAIL)).hasMessageContaining("1 Minute ");

        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> service.checkNotLocked(EMAIL)).doesNotThrowAnyException();

        // Nach Ablauf der Sperre beginnt die Zählung von vorn
        service.recordFailure(EMAIL);
        assertThatCode(() -> service.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void resetClearsFailuresAndLock() {
        lock();
        service.reset(EMAIL);

        assertThatCode(() -> service.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void otherAddressesAreNotAffected() {
        lock();

        assertThatCode(() -> service.checkNotLocked("lea@example.org")).doesNotThrowAnyException();
    }

    private void lock() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(EMAIL);
        }
    }

    /** Uhr, die sich im Test vorstellen lässt. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
