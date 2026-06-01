package com.switchplatform.platform.service.notification;

import com.switchplatform.platform.model.notification.Notification;
import com.switchplatform.platform.repository.notification.NotificationRepository;
import com.switchplatform.platform.service.notification.strategy.NotificationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationStrategy emailStrategy;
    @Mock private NotificationStrategy smsStrategy;

    private NotificationTemplateService templateService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        templateService = new NotificationTemplateService();
        notificationService = new NotificationService(notificationRepository, templateService,
                List.of(emailStrategy, smsStrategy));
        ReflectionTestUtils.setField(notificationService, "notificationEnabled", true);

        when(emailStrategy.supports("EMAIL")).thenReturn(true);
        when(smsStrategy.supports("SMS")).thenReturn(true);
    }

    @Test
    void sendEmail_shouldCreateAndSend() {
        when(emailStrategy.send(any())).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendEmail("test@example.com", "Test Subject", "Test Body");

        assertNotNull(result);
        assertEquals("EMAIL", result.getChannel());
        assertEquals("SENT", result.getStatus());
        assertEquals("test@example.com", result.getRecipient());
    }

    @Test
    void sendSms_shouldCreateAndSend() {
        when(smsStrategy.send(any())).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendSms("+21650123456", "Test SMS");

        assertNotNull(result);
        assertEquals("SMS", result.getChannel());
        assertEquals("SENT", result.getStatus());
    }

    @Test
    void sendFromTemplate_shouldRenderAndSend() {
        when(emailStrategy.send(any())).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendFromTemplate("EMAIL", "user@example.com",
                "transaction.approved", Map.of("amount", "100", "currency", "TND", "merchant", "TestMerchant"));

        assertNotNull(result);
        assertEquals("EMAIL", result.getChannel());
        assertTrue(result.getBody().contains("100"));
        assertTrue(result.getBody().contains("TND"));
    }

    @Test
    void sendFromTemplate_shouldReturnNullForUnknownTemplate() {
        Notification result = notificationService.sendFromTemplate("EMAIL", "user@example.com",
                "unknown.template", Map.of());
        assertNull(result);
    }

    @Test
    void send_shouldMarkFailedWhenDeliveryFails() {
        when(emailStrategy.send(any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.send("EMAIL", "fail@example.com", "Subject", "Body", "EMAIL");

        assertEquals("FAILED", result.getStatus());
    }

    @Test
    void send_shouldLogOnlyWhenDisabled() {
        ReflectionTestUtils.setField(notificationService, "notificationEnabled", false);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.send("EMAIL", "log@example.com", "Subject", "Body", "EMAIL");

        assertEquals("LOG_ONLY", result.getStatus());
    }
}
