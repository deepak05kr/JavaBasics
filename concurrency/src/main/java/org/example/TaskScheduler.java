package org.example;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskScheduler {
    private PriorityQueue<ScheduleTask> taskQueue = new PriorityQueue<>();
    private ScheduledExecutorService executor;
    private AtomicBoolean isShutdown = new AtomicBoolean(false);
    private Object lock = new Object();

    public TaskScheduler(int numOfWorkers) {
        executor = Executors.newScheduledThreadPool(numOfWorkers);
        processTask();
    }

    private void processTask() {
        executor.scheduleAtFixedRate(() -> {
            if (isShutdown.get()) {
                return;
            }
            synchronized (lock) {
                if (!taskQueue.isEmpty() && taskQueue.peek().startTime.before(new Date())) {
                    ScheduleTask scheduleTask = taskQueue.poll();
                    executor.execute(scheduleTask.task::execute);
                    if (scheduleTask.schedule.isRecurring) {
                        Date nextStartTime = new Date(scheduleTask.startTime.getTime() + scheduleTask.schedule.interval);
                        if (scheduleTask.schedule.occurances > 1) {
                            Schedule nextSchedule = new Schedule(scheduleTask.schedule.interval, scheduleTask.schedule.occurances - 1, nextStartTime);
                            taskQueue.add(new ScheduleTask(nextSchedule, scheduleTask.task));
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        isShutdown.getAndSet(true);
        executor.shutdown();
        try {
            executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void scheduleTask(Task task, Schedule schedule) {
        if (isShutdown.get()) {
            throw new IllegalStateException("Task Executoer is stopped");
        }
        synchronized (lock) {
            ScheduleTask scheduleTask = new ScheduleTask(schedule, task);
            taskQueue.add(scheduleTask);
        }
    }

    static class Schedule {
        private final Date startTime;
        private final long occurances;
        private final int interval;
        private final boolean isRecurring;
        private final boolean isDelayed;

        public Schedule(int interval, long occurances, Date startTime) {
            this.interval = interval;
            this.occurances = occurances;
            this.startTime = startTime;
            this.isRecurring = occurances > 1;
            this.isDelayed = false;
        }

        public Schedule(long delay) {
            this.interval = 0;
            this.occurances = 1;
            this.startTime = new Date(System.currentTimeMillis() + delay);
            this.isRecurring = false;
            this.isDelayed = true;
        }
    }

    class ScheduleTask implements Comparable<ScheduleTask> {
        private final Task task;
        private final Schedule schedule;
        private final Date startTime;

        public ScheduleTask(Schedule schedule, Task task) {
            this.schedule = schedule;
            this.task = task;
            this.startTime = schedule.startTime;
        }

        @Override
        public int compareTo(ScheduleTask o) {
            return this.startTime.compareTo(o.startTime);
        }
    }

    static class Task {
        private final Runnable runnable;

        public Task(Runnable runnable) {
            this.runnable = runnable;
        }

        public void execute() {
            this.runnable.run();
        }
    }
}
