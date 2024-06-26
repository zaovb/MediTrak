package projects.medicationtracker.Workers;

import static android.os.Build.VERSION.SDK_INT;
import static projects.medicationtracker.Helpers.NotificationHelper.CHANNEL_ID;
import static projects.medicationtracker.Helpers.NotificationHelper.DOSE_TIME;
import static projects.medicationtracker.Helpers.NotificationHelper.GROUP_KEY;
import static projects.medicationtracker.Helpers.NotificationHelper.MEDICATION_ID;
import static projects.medicationtracker.Helpers.NotificationHelper.MESSAGE;
import static projects.medicationtracker.Helpers.NotificationHelper.NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;

import projects.medicationtracker.MainActivity;
import projects.medicationtracker.R;
import projects.medicationtracker.Receivers.EventReceiver;

public class NotificationWorker extends Worker {
    private final Context context;
    public static final int SUMMARY_ID = Integer.MAX_VALUE;
    public static String MARK_AS_TAKEN_ACTION = "markAsTaken";
    public static String SNOOZE_ACTION = "snooze15";

    NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);

        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            final String message = getInputData().getString(MESSAGE);
            final String doseTime = getInputData().getString(DOSE_TIME);
            final long notificationId = getInputData().getLong(NOTIFICATION_ID, System.currentTimeMillis());
            final long medId = getInputData().getLong(MEDICATION_ID, -1);

            Notification notification = createNotification(message, doseTime, notificationId, medId);
            Notification notificationSummary = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setSmallIcon(R.drawable.pill)
                    .setStyle(new NotificationCompat.InboxStyle())
                    .setGroup(GROUP_KEY)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build();

            notificationManager.notify(SUMMARY_ID, notificationSummary);

            // Only fire notification if not other active notification has the same ID
            if (Arrays.stream(notificationManager.getActiveNotifications()).filter(n -> n.getId() == notificationId).toArray().length == 0) {
                notificationManager.notify((int) notificationId, notification);
            }
        } catch (Exception e) {
            Log.e("MediTrak:Notifications", e.getMessage());

            return Result.failure();
        }

        return Result.success();
    }

    /**
     * Creates a notification
     *
     * @param message Message to display in the notification.
     * @return A built notification.
     */
    private Notification createNotification(
            String message,
            String doseTime,
            long notificationId,
            long medId
    ) {
        Intent markTakenIntent = new Intent(this.getApplicationContext(), EventReceiver.class);
        Intent snoozeIntent = new Intent(this.getApplicationContext(), EventReceiver.class);
        String embeddedMedId = "_" + medId;

        markTakenIntent.removeExtra(DOSE_TIME);
        markTakenIntent.removeExtra(DOSE_TIME);

        markTakenIntent.setAction(MARK_AS_TAKEN_ACTION + embeddedMedId);
        markTakenIntent.putExtra(MEDICATION_ID + embeddedMedId, medId);
        markTakenIntent.putExtra(NOTIFICATION_ID + embeddedMedId, notificationId);
        markTakenIntent.putExtra(DOSE_TIME + embeddedMedId, doseTime);

        snoozeIntent.setAction(SNOOZE_ACTION + embeddedMedId);
        snoozeIntent.putExtra(MEDICATION_ID + embeddedMedId, medId);
        snoozeIntent.putExtra(NOTIFICATION_ID + embeddedMedId, notificationId);
        snoozeIntent.putExtra(DOSE_TIME + embeddedMedId, doseTime);

        PendingIntent markAsTakenPendingIntent =
                PendingIntent.getBroadcast(
                        this.getApplicationContext(),
                        0,
                        markTakenIntent,
                        SDK_INT >= Build.VERSION_CODES.S ?
                                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT
                );

        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(
                        getApplicationContext(),
                        0,
                        snoozeIntent,
                        SDK_INT >= Build.VERSION_CODES.S ?
                                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(message)
                        .setSmallIcon(R.drawable.pill)
                        .setGroup(GROUP_KEY)
                        .setAutoCancel(false)
                        .setStyle(new NotificationCompat.BigTextStyle())
                        .addAction(
                                0,
                                context.getString(R.string.mark_as_taken),
                                markAsTakenPendingIntent
                        )
                        .addAction(
                                0,
                                context.getString(R.string.snooze_message),
                                snoozePendingIntent
                        );

        Intent resIntent =
                new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resIntent);

        PendingIntent resPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        SDK_INT >= Build.VERSION_CODES.S ?
                                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resPendingIntent);

        return builder.build();
    }
}
