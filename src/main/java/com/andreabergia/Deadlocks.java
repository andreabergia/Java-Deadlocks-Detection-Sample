package com.andreabergia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

public class Deadlocks {
    private static Logger logger = LoggerFactory.getLogger(Deadlocks.class);

    public static void main(String[] args) {
        final Object lock1 = new Object();
        final Object lock2 = new Object();

        // First thread: acquires lock1 then lock2
        new Thread(() -> {
            while (true) {
                synchronized (lock1) {
                    synchronized (lock2) {
                        logger.info("Thread 1 got both locks");
                    }
                }
            }
        }, "first-thread").start();

        // Second thread: acquires lock2 then lock1
        new Thread(() -> {
            while (true) {
                synchronized (lock2) {
                    synchronized (lock1) {
                        logger.info("Thread 2 got both locks");
                    }
                }
            }
        }, "second-thread").start();

        // Third thread: monitors for deadlocks
        new Thread(Deadlocks::monitorDeadlocks, "monitor-deadlocks").start();
    }

    public static void monitorDeadlocks() {
        while (true) {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked
            if (threadIds != null) {
                logDeadlockAndQuit(bean, threadIds);
            }
            waitUninterruptedlyForMs(500);
        }
    }

    private static void logDeadlockAndQuit(ThreadMXBean bean, long[] threadIds) {
        logger.error("Threads in deadlocks: {}", Arrays.toString(threadIds));

        ThreadInfo[] info = bean.getThreadInfo(threadIds);
        for (ThreadInfo threadInfo : info) {
            logger.error("Thread \"{}\" is waiting on lock \"{}\" taken by thread \"{}\"",
                    threadInfo.getThreadName(), threadInfo.getLockInfo(), threadInfo.getLockOwnerName());

            // Attempt to log the stack trace, when available
            for (StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
                logger.error("{}::{} @ {}:{}",
                        stackTraceElement.getClassName(), stackTraceElement.getMethodName(),
                        stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
            }
        }

        System.exit(0);
    }

    private static void waitUninterruptedlyForMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Ignore it
        }
    }
}
