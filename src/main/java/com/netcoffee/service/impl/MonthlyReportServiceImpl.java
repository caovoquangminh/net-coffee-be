package com.netcoffee.service.impl;

import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.repository.FoodOrderRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonthlyReportServiceImpl {

    private final TransactionRepository transactionRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    @Transactional(readOnly = true)
    public XSSFWorkbook generateMonthlyReport(YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = lastDay.plusDays(1).atStartOfDay();

        XSSFWorkbook wb = new XSSFWorkbook();
        Styles s = new Styles(wb);

        buildOverviewSheet(wb, s, yearMonth, from, to);
        buildDailySheet(wb, s, yearMonth, firstDay, lastDay, from, to);
        buildCustomersSheet(wb, s, yearMonth, from, to);
        buildStaffSheet(wb, s, yearMonth, from, to);

        return wb;
    }

    // ── Sheet 1: Tổng quan ────────────────────────────────────────────────────

    private void buildOverviewSheet(
            XSSFWorkbook wb, Styles s, YearMonth ym, LocalDateTime from, LocalDateTime to) {

        Sheet sheet = wb.createSheet("Tổng quan");
        sheet.setColumnWidth(0, 12_000);
        sheet.setColumnWidth(1, 8_000);

        int r = 0;

        // Title
        Row titleRow = sheet.createRow(r++);
        titleRow.setHeight((short) 900);
        Cell title = titleRow.createCell(0);
        title.setCellValue("BÁO CÁO DOANH THU THÁNG " + ym.format(MONTH_FMT));
        title.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        r++; // blank

        // Period
        BigDecimal netRevenue =
                transactionRepository.sumByTypeBetween(TransactionTypeEnum.DEDUCT, from, to);
        BigDecimal serviceRevenue =
                foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, from, to);
        BigDecimal topUpCash =
                transactionRepository.sumByTypeAndMethodBetween(
                        TransactionTypeEnum.TOPUP, PaymentMethodEnum.CASH, from, to);
        BigDecimal topUpBank =
                transactionRepository.sumByTypeAndMethodBetween(
                        TransactionTypeEnum.TOPUP, PaymentMethodEnum.QR_BANK, from, to);
        BigDecimal totalTopUp = topUpCash.add(topUpBank);
        BigDecimal totalRevenue = netRevenue.add(serviceRevenue);
        BigDecimal customerBalance = userRepository.sumBalanceByRole(UserRoleEnum.CUSTOMER);

        Object[][] rows = {
            {
                "Kỳ báo cáo",
                ym.atDay(1).format(DATE_FMT) + " – " + ym.atEndOfMonth().format(DATE_FMT)
            },
            {"Tổng doanh thu", totalRevenue},
            {"  Tiền net (giờ chơi)", netRevenue},
            {"  Dịch vụ (đặt món)", serviceRevenue},
            {"Tổng tiền nạp nhận", totalTopUp},
            {"  Tiền mặt", topUpCash},
            {"  Chuyển khoản", topUpBank},
            {"Công nợ khách cuối kỳ", customerBalance},
        };

        for (Object[] row : rows) {
            Row excelRow = sheet.createRow(r++);
            Cell labelCell = excelRow.createCell(0);
            labelCell.setCellValue((String) row[0]);
            labelCell.setCellStyle(s.label);

            Cell valueCell = excelRow.createCell(1);
            if (row[1] instanceof BigDecimal) {
                valueCell.setCellValue(((BigDecimal) row[1]).doubleValue());
                valueCell.setCellStyle(s.currency);
            } else {
                valueCell.setCellValue((String) row[1]);
                valueCell.setCellStyle(s.data);
            }
        }
    }

    // ── Sheet 2: Theo ngày ────────────────────────────────────────────────────

    private void buildDailySheet(
            XSSFWorkbook wb,
            Styles s,
            YearMonth ym,
            LocalDate firstDay,
            LocalDate lastDay,
            LocalDateTime from,
            LocalDateTime to) {

        Sheet sheet = wb.createSheet("Doanh thu theo ngày");
        int[] widths = {4000, 6000, 6000, 7000, 6000, 6000, 7000};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);

        int r = 0;

        // Title
        Row titleRow = sheet.createRow(r++);
        titleRow.setHeight((short) 700);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("DOANH THU THEO NGÀY – THÁNG " + ym.format(MONTH_FMT));
        tc.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        r++;

        // Header
        String[] headers = {
            "Ngày", "Tiền net", "Dịch vụ", "Tổng doanh thu", "Nạp tiền mặt", "Nạp CK", "Tổng nạp"
        };
        Row headerRow = sheet.createRow(r++);
        headerRow.setHeight((short) 550);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.header);
        }

        // Build day maps
        List<Object[]> netRows = transactionRepository.dailySumByTypeBetween("DEDUCT", from, to);
        List<Object[]> topUpRows = transactionRepository.dailyTopUpByMethodBetween(from, to);
        List<Object[]> foodRows = foodOrderRepository.dailyFoodRevenueBetween(from, to);

        Map<String, BigDecimal> netByDay = toMap(netRows);
        Map<String, BigDecimal> foodByDay = toMap(foodRows);
        Map<String, BigDecimal> cashByDay = new HashMap<>();
        Map<String, BigDecimal> bankByDay = new HashMap<>();

        for (Object[] row : topUpRows) {
            String day = row[0].toString();
            String method = row[1] != null ? row[1].toString() : "";
            BigDecimal amount = new BigDecimal(row[2].toString());
            if ("CASH".equals(method)) cashByDay.merge(day, amount, BigDecimal::add);
            else if ("QR_BANK".equals(method)) bankByDay.merge(day, amount, BigDecimal::add);
        }

        BigDecimal totNet = BigDecimal.ZERO,
                totFood = BigDecimal.ZERO,
                totCash = BigDecimal.ZERO,
                totBank = BigDecimal.ZERO;

        LocalDate day = firstDay;
        while (!day.isAfter(lastDay)) {
            String key = day.toString();
            BigDecimal net = netByDay.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal food = foodByDay.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal cash = cashByDay.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal bank = bankByDay.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal rev = net.add(food);
            BigDecimal topUp = cash.add(bank);

            Row excelRow = sheet.createRow(r++);
            cell(excelRow, 0, day.format(DATE_FMT), s.data);
            cellNum(excelRow, 1, net, s.currency);
            cellNum(excelRow, 2, food, s.currency);
            cellNum(excelRow, 3, rev, s.currencyBold);
            cellNum(excelRow, 4, cash, s.currency);
            cellNum(excelRow, 5, bank, s.currency);
            cellNum(excelRow, 6, topUp, s.currency);

            totNet = totNet.add(net);
            totFood = totFood.add(food);
            totCash = totCash.add(cash);
            totBank = totBank.add(bank);
            day = day.plusDays(1);
        }

        // Total row
        Row totRow = sheet.createRow(r);
        totRow.setHeight((short) 500);
        cell(totRow, 0, "TỔNG", s.total);
        cellNum(totRow, 1, totNet, s.totalCurrency);
        cellNum(totRow, 2, totFood, s.totalCurrency);
        cellNum(totRow, 3, totNet.add(totFood), s.totalCurrency);
        cellNum(totRow, 4, totCash, s.totalCurrency);
        cellNum(totRow, 5, totBank, s.totalCurrency);
        cellNum(totRow, 6, totCash.add(totBank), s.totalCurrency);
    }

    // ── Sheet 3: Khách hàng ───────────────────────────────────────────────────

    private void buildCustomersSheet(
            XSSFWorkbook wb, Styles s, YearMonth ym, LocalDateTime from, LocalDateTime to) {

        Sheet sheet = wb.createSheet("Top khách hàng");
        int[] widths = {3000, 5000, 7000, 7000, 6000};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);

        int r = 0;
        Row titleRow = sheet.createRow(r++);
        titleRow.setHeight((short) 700);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("TOP KHÁCH HÀNG – THÁNG " + ym.format(MONTH_FMT));
        tc.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        r++;

        String[] headers = {"STT", "Số điện thoại", "Họ tên", "Đã chi tháng này", "Số dư hiện tại"};
        Row headerRow = sheet.createRow(r++);
        headerRow.setHeight((short) 550);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.header);
        }

        List<Object[]> customers = transactionRepository.topCustomersBySpendingBetween(from, to);
        int idx = 1;
        for (Object[] row : customers) {
            Row excelRow = sheet.createRow(r++);
            cell(excelRow, 0, String.valueOf(idx++), s.data);
            cell(excelRow, 1, row[0] != null ? row[0].toString() : "", s.data);
            cell(excelRow, 2, row[1] != null ? row[1].toString() : "", s.data);
            cellNum(excelRow, 3, new BigDecimal(row[3].toString()), s.currency);
            cellNum(excelRow, 4, new BigDecimal(row[2].toString()), s.currency);
        }
    }

    // ── Sheet 4: Nhân viên ────────────────────────────────────────────────────

    private void buildStaffSheet(
            XSSFWorkbook wb, Styles s, YearMonth ym, LocalDateTime from, LocalDateTime to) {

        Sheet sheet = wb.createSheet("Nhân viên");
        int[] widths = {3000, 5000, 7000, 5000, 7000};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);

        int r = 0;
        Row titleRow = sheet.createRow(r++);
        titleRow.setHeight((short) 700);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("HIỆU SUẤT NHÂN VIÊN – THÁNG " + ym.format(MONTH_FMT));
        tc.setCellStyle(s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        r++;

        String[] headers = {"STT", "Số điện thoại", "Họ tên", "Số lần nạp", "Tổng nạp"};
        Row headerRow = sheet.createRow(r++);
        headerRow.setHeight((short) 550);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(s.header);
        }

        List<Object[]> staffRows = transactionRepository.staffTopUpStatsBetween(from, to);
        List<Long> ids = staffRows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, String[]> staffInfo = new HashMap<>();
        if (!ids.isEmpty()) {
            userRepository
                    .findAllById(ids)
                    .forEach(
                            u ->
                                    staffInfo.put(
                                            u.getId(),
                                            new String[] {u.getPhoneNumber(), u.getFullName()}));
        }

        int idx = 1;
        for (Object[] row : staffRows) {
            Long uid = ((Number) row[0]).longValue();
            String[] info = staffInfo.getOrDefault(uid, new String[] {"", "Unknown"});
            Row excelRow = sheet.createRow(r++);
            cell(excelRow, 0, String.valueOf(idx++), s.data);
            cell(excelRow, 1, info[0], s.data);
            cell(excelRow, 2, info[1], s.data);
            cell(excelRow, 3, String.valueOf(((Number) row[1]).longValue()), s.data);
            cellNum(excelRow, 4, new BigDecimal(row[2].toString()), s.currency);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, BigDecimal> toMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), new BigDecimal(row[1].toString()));
        }
        return map;
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void cellNum(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value.doubleValue());
        c.setCellStyle(style);
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private static class Styles {
        final CellStyle title, header, label, data, currency, currencyBold, total, totalCurrency;

        Styles(XSSFWorkbook wb) {
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font boldFont = wb.createFont();
            boldFont.setBold(true);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            DataFormat fmt = wb.createDataFormat();

            title = wb.createCellStyle();
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            header = wb.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(header);

            label = wb.createCellStyle();
            label.setFont(boldFont);
            label.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            label.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(label);

            data = wb.createCellStyle();
            setBorder(data);

            currency = wb.createCellStyle();
            currency.setDataFormat(fmt.getFormat("#,##0"));
            currency.setAlignment(HorizontalAlignment.RIGHT);
            setBorder(currency);

            currencyBold = wb.createCellStyle();
            currencyBold.setFont(boldFont);
            currencyBold.setDataFormat(fmt.getFormat("#,##0"));
            currencyBold.setAlignment(HorizontalAlignment.RIGHT);
            currencyBold.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            currencyBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(currencyBold);

            total = wb.createCellStyle();
            total.setFont(boldFont);
            total.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font wf = wb.createFont();
            wf.setBold(true);
            wf.setColor(IndexedColors.WHITE.getIndex());
            total.setFont(wf);
            setBorder(total);

            totalCurrency = wb.createCellStyle();
            totalCurrency.setFont(wf);
            totalCurrency.setDataFormat(fmt.getFormat("#,##0"));
            totalCurrency.setAlignment(HorizontalAlignment.RIGHT);
            totalCurrency.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            totalCurrency.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(totalCurrency);
        }

        private void setBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
