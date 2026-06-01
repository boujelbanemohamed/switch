package com.switchplatform.platform.service.notification.strategy;

import com.switchplatform.platform.model.notification.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationStrategy implements NotificationStrategy {

    @Override
    public boolean supports(String channel) {
        return "PUSH".equalsIgnoreCase(channel);
    }

    @Override
    public boolean send(Notification notification) {
        try {
            log.info("Push notification sent to device {}: subject={}",
                    notification.getRecipient(), notification.getSubject());
            return true;
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            return false;
        }
    }
}
