package com.netcoffee.config;

import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TMenuItemAddonEntity;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.MenuItemAddonRepository;
import com.netcoffee.repository.MenuItemRepository;
import com.netcoffee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemAddonRepository menuItemAddonRepository;
    private final MachineRepository machineRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try { seedAdmin(); }         catch (Exception e) { log.error("seedAdmin failed: {}", e.getMessage()); }
        try { seedMenu(); }          catch (Exception e) { log.error("seedMenu failed: {}", e.getMessage()); }
        try { seedMachines(); }      catch (Exception e) { log.error("seedMachines failed: {}", e.getMessage()); }
        try { removeLegacyItems(); } catch (Exception e) { log.error("removeLegacyItems failed: {}", e.getMessage()); }
    }

    private void seedAdmin() {
        if (!userRepository.existsByPhoneNumber("admin")) {
            userRepository.save(TUserEntity.builder()
                    .phoneNumber("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .role(UserRoleEnum.ADMIN)
                    .isActive(true)
                    .build());
            log.info("Admin account created");
        }
    }

    private void seedMachines() {
        if (machineRepository.count() > 0) return;

        int total = 10;
        for (int i = 1; i <= total; i++) {
            String code = String.format("PC-%02d", i);
            machineRepository.save(TMachineEntity.builder()
                    .machineCode(code)
                    .machineName("Máy " + i)
                    .roomZone("Khu A")
                    .status(MachineStatusEnum.AVAILABLE)
                    .build());
        }
        log.info("Machines seeded: {} machines", total);
    }

    private void seedMenu() {
        if (menuItemRepository.count() > 0) return;

        // ── Mì / Cơm ─────────────────────────────────────────────────────────
        TMenuItemEntity miGoi       = save("Mì gói",          15_000, "Mì/Cơm");
        TMenuItemEntity miTrung     = save("Mì trứng",        20_000, "Mì/Cơm");
        TMenuItemEntity miXucXich   = save("Mì xúc xích",     22_000, "Mì/Cơm");
        TMenuItemEntity miBo        = save("Mì bò",           25_000, "Mì/Cơm");
        TMenuItemEntity miTom       = save("Mì tôm",          20_000, "Mì/Cơm");
        TMenuItemEntity miXao       = save("Mì xào",          30_000, "Mì/Cơm");
        TMenuItemEntity comChienTrung = save("Cơm chiên trứng", 25_000, "Mì/Cơm");
        TMenuItemEntity comBoXao    = save("Cơm bò xào",      30_000, "Mì/Cơm");

        List<TMenuItemEntity> miItems = List.of(miGoi, miTrung, miXucXich, miBo, miTom, miXao, comChienTrung, comBoXao);
        for (TMenuItemEntity item : miItems) {
            saveAddon(item.getId(), "Thêm trứng",    7_000);
            saveAddon(item.getId(), "Thêm xúc xích", 8_000);
            saveAddon(item.getId(), "Thêm chả",      8_000);
            saveAddon(item.getId(), "Thêm tôm",      10_000);
            saveAddon(item.getId(), "Thêm gói mì",   8_000);
            saveAddon(item.getId(), "Thêm rau",      3_000);
        }

        // ── Đồ ăn vặt ────────────────────────────────────────────────────────
        save("Cá viên chiên",     15_000, "Đồ ăn vặt");
        save("Khoai tây chiên",   25_000, "Đồ ăn vặt");
        save("Xúc xích chiên",    15_000, "Đồ ăn vặt");
        save("Bánh tráng trộn",   15_000, "Đồ ăn vặt");
        save("Snack Oishi",       10_000, "Đồ ăn vặt");
        save("Snack Poca",        10_000, "Đồ ăn vặt");
        save("Bánh quy",          10_000, "Đồ ăn vặt");
        save("Hạt hướng dương",   10_000, "Đồ ăn vặt");
        save("Kẹo singum",         5_000, "Đồ ăn vặt");

        // ── Nước ngọt ─────────────────────────────────────────────────────────
        save("Pepsi lon",              15_000, "Nước ngọt");
        save("Coca-Cola lon",          15_000, "Nước ngọt");
        save("7UP lon",                15_000, "Nước ngọt");
        save("Sting lon",              15_000, "Nước ngọt");
        save("Mirinda lon",            15_000, "Nước ngọt");
        save("Trà xanh C2",            15_000, "Nước ngọt");
        save("Aquarius",               15_000, "Nước ngọt");
        save("Nước suối",               8_000, "Nước ngọt");
        save("Redbull lon",            25_000, "Nước ngọt");
        save("Number One lon",         15_000, "Nước ngọt");
        save("Nước tăng lực Warrior",  20_000, "Nước ngọt");

        // ── Cà phê ────────────────────────────────────────────────────────────
        save("Cà phê đen nóng",       15_000, "Cà phê");
        save("Cà phê sữa nóng",       20_000, "Cà phê");
        save("Bạc xỉu nóng",          20_000, "Cà phê");
        save("Cà phê đen đá",         15_000, "Cà phê");
        save("Cà phê sữa đá",         20_000, "Cà phê");
        save("Bạc xỉu đá",            20_000, "Cà phê");
        save("Cà phê lon Highlands",  25_000, "Cà phê");
        save("Cà phê lon Nestlé",     20_000, "Cà phê");

        // ── Trà sữa ───────────────────────────────────────────────────────────
        save("Trà sữa trân châu",  30_000, "Trà sữa");
        save("Trà sữa thạch",      32_000, "Trà sữa");
        save("Matcha sữa",         35_000, "Trà sữa");
        save("Trà đào cam sả",     30_000, "Trà sữa");
        save("Trà vải",            28_000, "Trà sữa");
        save("Trà chanh",          20_000, "Trà sữa");

        log.info("Menu seeded: {} items", menuItemRepository.count());
    }

    /** Xóa các món đã bị loại khỏi thực đơn, chạy idempotent mỗi startup. */
    private void removeLegacyItems() {
        List<String> toRemove = List.of("Cháo cá", "Bún bò gói");
        menuItemRepository.findAll().stream()
                .filter(m -> toRemove.contains(m.getName()))
                .forEach(m -> {
                    menuItemAddonRepository.findByMenuItemId(m.getId())
                            .forEach(menuItemAddonRepository::delete);
                    menuItemRepository.delete(m);
                    log.info("Removed legacy menu item: {}", m.getName());
                });
    }

    private TMenuItemEntity save(String name, int priceVnd, String category) {
        return menuItemRepository.save(TMenuItemEntity.builder()
                .name(name)
                .price(BigDecimal.valueOf(priceVnd))
                .category(category)
                .isAvailable(true)
                .build());
    }

    private void saveAddon(Long menuItemId, String name, int extraPriceVnd) {
        menuItemAddonRepository.save(TMenuItemAddonEntity.builder()
                .menuItemId(menuItemId)
                .name(name)
                .extraPrice(BigDecimal.valueOf(extraPriceVnd))
                .isAvailable(true)
                .build());
    }
}
