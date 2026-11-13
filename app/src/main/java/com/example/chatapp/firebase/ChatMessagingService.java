package com.example.chatapp.firebase;

import androidx.annotation.NonNull;

import com.example.chatapp.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives Firebase Cloud Messaging events for this installation.
 *
 * <p>When the app is in the background, Android displays notification messages
 * automatically and this service is not called for them, so nothing is duplicated. This
 * service only posts a notification when a message reaches it: notification messages while
 * the app is in the foreground, and data messages carrying {@code title}/{@code body}
 * keys.</p>
 */
public class ChatMessagingService extends FirebaseMessagingService {

    /**
     * Stores a refreshed registration token for the signed-in user. If nobody is signed in,
     * the token is registered after the next sign-in instead.
     *
     * @param token the new FCM registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FcmTokenManager.onTokenRefreshed(this, token);
    }

    /**
     * Shows a notification for a message delivered to the app, preferring the notification
     * payload and falling back to {@code title}/{@code body} data keys.
     *
     * @param message the received FCM message
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = null;
        String body = null;
        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            title = notification.getTitle();
            body = notification.getBody();
        }
        Map<String, String> data = message.getData();
        if (title == null) {
            title = data.get(Constants.KEY_MESSAGE_TITLE);
        }
        if (body == null) {
            body = data.get(Constants.KEY_MESSAGE_BODY);
        }
        String messageId = message.getMessageId();
        int notificationId = messageId != null
                ? messageId.hashCode() : (int) System.currentTimeMillis();
        NotificationHelper.showMessageNotification(this, title, body, notificationId);
    }
}
