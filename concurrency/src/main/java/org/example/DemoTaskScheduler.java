package org.example;

import java.util.Date;

public class DemoTaskScheduler {
    public static void main(String[] args) {
        TaskScheduler taskScheduler = new TaskScheduler(2);
        TaskScheduler.Task task = new TaskScheduler.Task(() -> System.out.println("Normal Task is running"));
        TaskScheduler.Schedule schedule = new TaskScheduler.Schedule(0, 1, new Date());
        taskScheduler.scheduleTask(task, schedule);


        TaskScheduler.Task recurringTask = new TaskScheduler.Task(() -> System.out.println("Recurring Task is running"));
        TaskScheduler.Schedule recurringSchedule = new TaskScheduler.Schedule(2, 5, new Date());
        taskScheduler.scheduleTask(recurringTask, recurringSchedule);

        TaskScheduler.Task delayedTask = new TaskScheduler.Task(() -> System.out.println("Delayed Task is running"));
        TaskScheduler.Schedule delayedSchedule = new TaskScheduler.Schedule(9000);
        taskScheduler.scheduleTask(delayedTask, delayedSchedule);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        taskScheduler.shutdown();
    }
}
