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

    // Tạo ca trước 14 ngày để khi cửa sổ đăng ký mở (T6/T7/CN) thì ca của TUẦN KẾ TIẾP đã sẵn sàng.
    private static final int LOOKAHEAD_DAYS = 14;

    // Chỉ giữ lại ca làm trong 60 ngày gần nhất; ca cũ hơn sẽ bị dọn tự động.
    private static final int RETENTION_DAYS = 60;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void generateShiftsForNextWeek() {
        LocalDate today = LocalDate.now(AppConstant.VN_ZONE);
        log.info("Scheduler: generating shifts for next {} days from {}", LOOKAHEAD_DAYS, today);
        for (int i = 0; i < LOOKAHEAD_DAYS; i++) {
            try {
                shiftService.generateShiftsForDate(today.plusDays(i));
            } catch (Exception e) {
                log.error(
                        "Failed to generate shifts for {}: {}", today.plusDays(i), e.getMessage());
            }
        }
    }

    /** Dọn ca cũ hơn 60 ngày mỗi đêm để bảng ca không phình to. */
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void purgeOldShifts() {
        try {
            int deleted = shiftService.purgeOldShifts(RETENTION_DAYS);
            log.info("Scheduler: purged {} shifts older than {} days", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("Failed to purge old shifts: {}", e.getMessage());
        }
    }
}
