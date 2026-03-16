package com.flowcollect.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.flowcollect.application.reminder.ReminderEngine;

@Component
public class ReminderScheduler {

    private final ReminderEngine reminderEngine;
    private final boolean schedulerEnabled;

    public ReminderScheduler(
            ReminderEngine reminderEngine,
            @Value("${scheduler.enabled:true}") boolean schedulerEnabled
    ) {
        this.reminderEngine = reminderEngine;
        this.schedulerEnabled = schedulerEnabled;
    }

//    @Scheduled(cron = "${scheduler.reminder.cron:0 */15 * * * *}")
    @Scheduled(cron = "${scheduler.reminder.cron:0 * * * * *}")
    public void runAutomatedReminderJob() {
        if (!schedulerEnabled) {
            return;
        }
        reminderEngine.runAutomatedReminders();
    }
}