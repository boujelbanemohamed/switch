package com.switchplatform.platform.service.notification;

import com.switchplatform.platform.model.notification.Notification;
import com.switchplatform.platform.repository.notification.NotificationRepository;
import com.switchplatform.platform.service.notification.strategy.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final List<NotificationStrategy> strategies;

    @Value("${switch.notification.enabled:false}")
    private boolean notificationEnabled;

    @Transactional
    public Notification sendEmail(String to, String subject, String body) {
        return send("EMAIL", to, subject, body, "EMAIL");
    }

    @Transactional
    public Notification sendSms(String phoneNumber, String message) {
        return send("SMS", phoneNumber, null, message, "SMS");
    }

    @Transactional
    public Notification sendPush(String deviceToken, String title, String body) {
        return send("PUSH", deviceToken, title, body, "PUSH");
    }

    @Transactional
    public Notification sendWebhook(String url, String payload) {
        return send("WEBHOOK", url, null, payload, "WEBHOOK");
    }

    @Transactional
    public Notification sendFromTemplate(String channel, String recipient,
                                          String templateKey, Map<String, String> variables) {
        String body = templateService.render(templateKey, variables);
        if (body == null) {
            log.warn("Template not found: {}", templateKey);
            return null;
        }
        String subject = templateKey.replace(".", " ").toUpperCase();
        return send(channel, recipient, subject, body, channel);
    }

    @Transactional
    public Notification send(String channel, String recipient,
                              String subject, String body, String type) {
        Notification notification = Notification.builder()
                .type(type != null ? type : channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .channel(channel)
                .status(notificationEnabled ? "PENDING" : "LOG_ONLY")
                .build();

        if (!notificationEnabled) {
            log.info("[{} LOG ONLY] To: {}, Subject: {}", channel, recipient, subject);
            notification.setStatus("LOG_ONLY");
            return notificationRepository.save(notification);
        }

        boolean delivered = false;
        for (NotificationStrategy strategy : strategies) {
            if (strategy.supports(channel)) {
                delivered = strategy.send(notification);
                break;
            }
        }

        notification.setStatus(delivered ? "SENT" : "FAILED");
        Notification saved = notificationRepository.save(notification);
        log.info("Notification {}: id={}, channel={}, recipient={}, status={}",
                delivered ? "sent" : "failed", saved.getId(), channel, recipient, saved.getStatus());
        return saved;
    }
}
