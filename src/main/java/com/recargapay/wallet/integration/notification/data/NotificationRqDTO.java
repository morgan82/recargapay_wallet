package com.recargapay.wallet.integration.notification.data;

public record NotificationRqDTO(
        String email,
        NotificationType notificationType
        //other attr
) {
}
