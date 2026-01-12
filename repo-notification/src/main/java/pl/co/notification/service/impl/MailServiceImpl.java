package pl.co.notification.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.co.common.mail.MailMessage;
import pl.co.notification.service.MailService;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final SendGrid sendGrid;

    @Value("${sendgrid.from}")
    private String from;

    @Override
    public void send(MailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            return;
        }
        try {
            Email fromEmail = new Email(from);
            Email toEmail = new Email(message.to());
            String subject = message.subject() == null ? "" : message.subject();
            String mimeType = message.html() ? "text/html" : "text/plain";
            Content content = new Content(mimeType, message.body() == null ? "" : message.body());

            Mail mail = new Mail();
            mail.setFrom(fromEmail);
            mail.setSubject(subject);
            mail.addContent(content);
            Personalization personalization = new Personalization();
            personalization.addTo(toEmail);
            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sendGrid.api(request);
        } catch (Exception ignored) {
            // Avoid breaking notification pipeline on mail failures.
        }
    }
}
