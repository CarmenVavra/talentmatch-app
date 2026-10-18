package de.talentmatch.mail;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.talentmatch.mail.api.OutboxDto;

/**
 * Postausgang für HR: alle von der App erzeugten E-Mails, neueste zuerst.
 */
@RestController
@RequestMapping("/api/outbox")
public class MailController {

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping
    public OutboxDto outbox() {
        return mailService.outbox();
    }
}
