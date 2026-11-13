package com.example.chatapp.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.chatapp.R;
import com.example.chatapp.activities.MainActivity;
import com.example.chatapp.utilities.Constants;

/**
 * Creates the app's notification channel and posts notifications for received FCM messages.
 */
public final class NotificationHelper {

    /** Prevents instantiation of this utility class. */
    private NotificationHelper() {
    }

    /**
     * Creates the message notification channel on Android 8.0 and later. Safe to call
     * repeatedly; existing channels are left unchanged.
     *
     * @param context any context
     */
    public static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Reports whether the app may currently post notifications, including the runtime
     * permission on Android 13 and later.
     *
     * @param context any context
     * @return {@code true} if a notification would be shown
     */
    public static boolean canPostNotifications(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Posts a notification that opens {@link MainActivity} when tapped. {@code MainActivity}
     * sends the user to sign-in if there is no active session. Does nothing if notifications
     * are not allowed or there is no text to show.
     *
     * @param context        any context
     * @param title          notification title, or {@code null} to use the app name
     * @param body           notification text
     * @param notificationId ID that lets a repeated message replace its earlier notification
     */
    public static void showMessageNotification(@NonNull Context context, @Nullable String title,
                                               @Nullable String body, int notificationId) {
        if ((title == null || title.isEmpty()) && (body == null || body.isEmpty())) {
            return;
        }
        if (!canPostNotifications(context)) {
            return;
        }
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String resolvedTitle = (title == null || title.isEmpty())
                ? context.getString(R.string.app_name) : title;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.brand_primary))
                .setContentTitle(resolvedTitle)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // The permission was revoked between the check and the post; nothing to show.
        }
    }
}
