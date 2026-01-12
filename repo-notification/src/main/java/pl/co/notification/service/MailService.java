package pl.co.notification.service;

import pl.co.common.mail.MailMessage;

public interface MailService {
    void send(MailMessage message);
}
