package com.example.chatapp;

import android.app.Application;

import com.example.chatapp.firebase.NotificationHelper;

/**
 * Application entry point. Creates the notification channel at startup so notifications
 * that Android displays for background FCM messages use the app's channel.
 */
public class ChatApplication extends Application {

    /**
     * Initializes app-wide state when the process starts.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannel(this);
    }
}
