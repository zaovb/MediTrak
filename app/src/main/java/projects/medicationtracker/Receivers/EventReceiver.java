package projects.medicationtracker.Receivers;

import static projects.medicationtracker.Helpers.NotificationHelper.DOSE_TIME;
import static projects.medicationtracker.Helpers.NotificationHelper.MEDICATION_ID;
import static projects.medicationtracker.Helpers.NotificationHelper.NOTIFICATION_ID;
import static projects.medicationtracker.Helpers.NotificationHelper.clearPendingNotifications;
import static projects.medicationtracker.Helpers.NotificationHelper.createNotifications;
import static projects.medicationtracker.Helpers.NotificationHelper.scheduleIn15Minutes;
import static projects.medicationtracker.Workers.NotificationWorker.SNOOZE_ACTION;
import static projects.medicationtracker.Workers.NotificationWorker.SUMMARY_ID;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import java.time.LocalDateTime;
import java.util.ArrayList;

import projects.medicationtracker.Helpers.DBHelper;
import projects.medicationtracker.Helpers.TimeFormatting;
import projects.medicationtracker.Models.Medication;
import projects.medicationtracker.Workers.NotificationWorker;

public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final DBHelper db = new DBHelper(context);
        ArrayList<Medication> medications = db.getMedications();

        if (intent.getAction().contains(NotificationWorker.MARK_AS_TAKEN_ACTION)) {
            String medId = "_" + intent.getAction().split("_")[1];

            markDoseTaken(
                    context,
                    intent.getLongExtra(NOTIFICATION_ID + medId, 0),
                    intent.getLongExtra(MEDICATION_ID + medId, 0),
                    intent.getStringExtra(DOSE_TIME + medId),
                    db
            );
        } else if (intent.getAction().contains(SNOOZE_ACTION)) {
            String medId = "_" + intent.getAction().split("_")[1];

            snoozeFor15(
                    context,
                    intent.getLongExtra(NOTIFICATION_ID + medId, 0),
                    intent.getLongExtra(MEDICATION_ID + medId, 0),
                    intent.getStringExtra(DOSE_TIME + medId),
                    db
            );
        } else {
            for (final Medication medication : medications) {
                prepareNotification(context, medication);
            }
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        StatusBarNotification[] notifications = manager.getActiveNotifications();

        if (notifications.length == 1 && notifications[0].getId() == SUMMARY_ID) {
            manager.cancel(SUMMARY_ID);
        }

        db.close();
    }

    /**
     * Prepares pending notifications on boot
     *
     * @param context    Notification context
     * @param medication Medication in need of a notification
     */
    private void prepareNotification(Context context, Medication medication) {
        clearPendingNotifications(medication, context);
        createNotifications(medication, context);
    }

    /**
     * Marks a dose as taken from the notification
     *
     * @param context        Application context
     * @param notificationId Id of notification to cancel
     * @param medId          ID of medication taken
     * @param doseTimeString Dose time for DB.
     */
    private void markDoseTaken(Context context, long notificationId, long medId, String doseTimeString, DBHelper db) {
        Medication med;
        LocalDateTime doseTime = LocalDateTime.parse(doseTimeString);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        med = db.getMedication(medId);

        long doseId = db.isInMedicationTracker(med, doseTime) ?
                db.getDoseId(med.getId(), TimeFormatting.localDateTimeToDbString(doseTime)) :
                db.addToMedicationTracker(med, doseTime);

        db.updateDoseStatus(
                doseId,
                TimeFormatting.localDateTimeToDbString(LocalDateTime.now().withSecond(0)),
                true
        );

        notificationManager.cancel((int) notificationId);
        db.close();
    }

    private void snoozeFor15(Context context, long notificationId, long medId, String doseTimeString, DBHelper db) {
        Medication med;
        LocalDateTime doseTime = LocalDateTime.parse(doseTimeString);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        med = db.getMedication(medId);

        scheduleIn15Minutes(
                context,
                med,
                doseTime,
                notificationId
        );

        notificationManager.cancel((int) notificationId);
        db.close();
    }
}
