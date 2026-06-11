package com.netcoffee.repository;

import com.netcoffee.entity.TWorkShiftEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkShiftRepository extends JpaRepository<TWorkShiftEntity, Long> {

    List<TWorkShiftEntity> findByShiftDate(LocalDate shiftDate);

    List<TWorkShiftEntity> findByShiftDateBetween(LocalDate from, LocalDate to);

    Optional<TWorkShiftEntity> findByShiftNumberAndShiftDate(int shiftNumber, LocalDate shiftDate);

    boolean existsByShiftNumberAndShiftDate(int shiftNumber, LocalDate shiftDate);

    /** Các ca có endTime trong khoảng — dùng cho job đối soát ca đã kết thúc. */
    List<TWorkShiftEntity> findByEndTimeBetween(LocalDateTime from, LocalDateTime to);
}
