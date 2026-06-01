package com.switchplatform.platform.service.notification.strategy;

import com.switchplatform.platform.model.notification.Notification;

public interface NotificationStrategy {

    boolean supports(String channel);
    boolean send(Notification notification);
}
