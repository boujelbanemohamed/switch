package com.switchplatform.platform.service.notification;

import com.switchplatform.platform.model.notification.Notification;
import com.switchplatform.platform.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Value("${switch.notification.enabled:false}")
    private boolean notificationEnabled;

    public void sendEmail(String to, String subject, String body) {
        if (!notificationEnabled) {
            log.info("[EMAIL LOG ONLY] To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }
        log.info("Sending email to: {}, subject: {}", to, subject);
        Notification notification = Notification.builder()
                .type("EMAIL")
                .recipient(to)
                .subject(subject)
                .body(body)
                .channel("EMAIL")
                .status("SENT")
                .build();
        notificationRepository.save(notification);
    }

    public void sendSms(String phoneNumber, String message) {
        if (!notificationEnabled) {
            log.info("[SMS LOG ONLY] To: {}, Message: {}", phoneNumber, message);
            return;
        }
        log.info("Sending SMS to: {}, message: {}", phoneNumber, message);
        Notification notification = Notification.builder()
                .type("SMS")
                .recipient(phoneNumber)
                .body(message)
                .channel("SMS")
                .status("SENT")
                .build();
        notificationRepository.save(notification);
    }
}
