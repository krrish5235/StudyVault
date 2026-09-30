package com.studyvault.common.email;

/** Transport abstraction: SMTP in production, log transport in development. */
public interface EmailSender {
    void send(String to, String subject, String htmlBody);
}
