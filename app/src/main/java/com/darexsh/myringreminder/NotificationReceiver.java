package com.darexsh.myringreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.os.Build;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderNotification";

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

        int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra("open_home", true);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Notification icon
                .setContentTitle(title)                  // Title
                .setContentText(message)                 // Message
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority
                .setContentIntent(contentIntent)
                .setAutoCancel(true)                     // Remove notification on click
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE); // Sound & vibration

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = notificationManager != null
                ? notificationManager.getNotificationChannel(Constants.REMINDER_CHANNEL_ID)
                : null;
        Log.d(
                TAG,
                "Sending reminder via channel="
                        + Constants.REMINDER_CHANNEL_ID
                        + ", channelFound=" + (channel != null)
                        + ", shouldVibrate=" + (channel != null && channel.shouldVibrate())
        );

        // Check if the app has permission to post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Use a unique ID for each notification based on timestamp to avoid overwriting
        manager.notify(notificationId, builder.build());
    }
}
