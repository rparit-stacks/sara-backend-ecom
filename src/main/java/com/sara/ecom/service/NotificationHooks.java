package com.sara.ecom.service;

import com.sara.ecom.entity.Order;

/**
 * Placeholder for future notification integrations (e.g. WhatsApp, SMS, push).
 * Currently does nothing; all WhatsApp/WA Sender functionality has been removed.
 */
public class NotificationHooks {

    public void onOrderPlaced(Order order) {
        // no-op – intentionally disabled
    }

    public void onOrderStatusChanged(Order order) {
        // no-op – intentionally disabled
    }

    public void onPaymentStatusChanged(Order order) {
        // no-op – intentionally disabled
    }
}

