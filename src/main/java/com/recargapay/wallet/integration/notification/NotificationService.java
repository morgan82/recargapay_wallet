package com.recargapay.wallet.integration.notification;

import com.recargapay.wallet.integration.notification.data.NotificationRqDTO;
import com.recargapay.wallet.integration.notification.data.NotificationRsDTO;
import com.recargapay.wallet.integration.notification.data.NotificationType;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    public String notifyWalletCompleted(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.WALLET_COMPLETED);
        return this.sendNotification(rq).id();
    }

    public String notifyDepositCompleted(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.DEPOSIT_COMPLETED);
        return this.sendNotification(rq).id();
    }

    public String notifyTransferCompleted(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.TRANSFER_COMPLETED);
        return this.sendNotification(rq).id();
    }

    public String notifyWalletPending(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.WALLET_PENDING);
        return this.sendNotification(rq).id();
    }

    public String notifyWithdrawalPending(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.WITHDRAWAL_PENDING);
        return this.sendNotification(rq).id();
    }

    public String notifyWithdrawalFail(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.WITHDRAWAL_FAIL);
        return this.sendNotification(rq).id();
    }

    public String sendWithdrawalCompleted(String email) {
        val rq = new NotificationRqDTO(email, NotificationType.WITHDRAWAL_COMPLETED);
        return this.sendNotification(rq).id();
    }

    //private methods

    private NotificationRsDTO sendNotification(NotificationRqDTO request) {
        //sending notification
        return new NotificationRsDTO(UUID.randomUUID().toString());
    }
}
