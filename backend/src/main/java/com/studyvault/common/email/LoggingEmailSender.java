package com.studyvault.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development transport: emails are written to the application log instead of being sent. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String htmlBody) {
        // Full body (links included) so local development can extract action URLs.
        log.info("[DEV-EMAIL] to={} subject={} body={}", to, subject, htmlBody);
    }
}
