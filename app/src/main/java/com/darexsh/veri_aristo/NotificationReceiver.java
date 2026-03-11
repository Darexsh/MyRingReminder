package com.darexsh.veri_aristo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve notification title and message from the intent
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        if (title == null || title.trim().isEmpty()) {
            title = context.getString(R.string.notifications_channel_name);
        }
        if (message == null || message.trim().isEmpty()) {
            message = context.getString(R.string.notifications_channel_description);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.ic_notification) // Notification icon
                .setContentTitle(title)                  // Title
                .setContentText(message)                 // Message
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority
                .setAutoCancel(true)                     // Remove notification on click
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE); // Sound & vibration

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        // Check if the app has permission to post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Use a unique ID for each notification based on timestamp to avoid overwriting
        int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        manager.notify(notificationId, builder.build());
    }
}
