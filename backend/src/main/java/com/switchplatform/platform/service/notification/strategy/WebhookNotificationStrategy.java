package com.switchplatform.platform.service.notification.strategy;

import com.switchplatform.platform.model.notification.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class WebhookNotificationStrategy implements NotificationStrategy {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public boolean supports(String channel) {
        return "WEBHOOK".equalsIgnoreCase(channel);
    }

    @Override
    public boolean send(Notification notification) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(notification.getRecipient()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            notification.getBody(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            log.info("Webhook sent to {}, status={}", notification.getRecipient(), response.statusCode());
            return response.statusCode() < 500;
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", notification.getRecipient(), e.getMessage());
            return false;
        }
    }
}
