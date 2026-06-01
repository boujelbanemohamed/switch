package com.switchplatform.platform.service.notification;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationTemplateService {

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public NotificationTemplateService() {
        templates.put("transaction.received",
                "Transaction de {amount} {currency} reçue chez {merchant}.");
        templates.put("transaction.approved",
                "Paiement de {amount} {currency} approuvé chez {merchant}.");
        templates.put("transaction.declined",
                "Paiement de {amount} {currency} refusé chez {merchant}. Motif : {reason}.");
        templates.put("pin.changed",
                "Votre code PIN a été modifié le {date}.");
        templates.put("card.blocked",
                "Votre carte a été bloquée pour sécurité. Contactez le support.");
        templates.put("settlement.completed",
                "Règlement de {amount} {currency} confirmé pour le {date}.");
        templates.put("account.credited",
                "Crédit de {amount} {currency} sur votre compte.");
        templates.put("account.debited",
                "Débit de {amount} {currency} sur votre compte.");
    }

    public String render(String templateKey, Map<String, String> variables) {
        String template = templates.get(templateKey);
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public void addTemplate(String key, String template) {
        templates.put(key, template);
    }
}
