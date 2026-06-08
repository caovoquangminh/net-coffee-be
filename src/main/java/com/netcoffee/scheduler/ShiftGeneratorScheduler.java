package com.netcoffee.scheduler;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.service.ShiftService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftGeneratorScheduler {

    private final ShiftService shiftService;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void generateShiftsForNextWeek() {
        LocalDate today = LocalDate.now(AppConstant.VN_ZONE);
        log.info("Scheduler: generating shifts for next 7 days from {}", today);
        for (int i = 0; i < 7; i++) {
            try {
                shiftService.generateShiftsForDate(today.plusDays(i));
            } catch (Exception e) {
                log.error(
                        "Failed to generate shifts for {}: {}", today.plusDays(i), e.getMessage());
            }
        }
    }
}
