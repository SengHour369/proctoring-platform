package com.example.test;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Optional;

public class TestsOptional {
    @Test
    void testOplional(){


            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(5); // 5 threads
            scheduler.setThreadNamePrefix("MyScheduler-");
            System.out.println("Test thread in java :"+scheduler.getScheduledExecutor());

            System.out.println();

        }
    }

