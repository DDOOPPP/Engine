package org.gi.gIEngine.command;

import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.C;
import org.gi.builder.DamageSourceBuilder;
import org.gi.damage.DamageType;
import org.gi.damage.IDamageCalculator;
import org.gi.damage.IDamageResult;
import org.gi.damage.IDamageSource;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.gIEngine.service.PlayerStatManager;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 데미지 테스트 명령어
 *
 * /damage <대상> <데미지> [타입] - 대상에게 데미지
 * /damage simulate <데미지> [타입] - 자신에게 데미지 시뮬레이션 (실제 피해 X)
 * /damage info - 데미지 관련 스탯 보기
 */
public class DamageCommand {
    private final PlayerStatManager statManager;
    private final IDamageCalculator damageCalculator;

    public DamageCommand(PlayerStatManager statManager, IDamageCalculator damageCalculator){
        this.statManager = statManager;
        this.damageCalculator = damageCalculator;
    }

    public boolean execute(CommandSender sender, String[] args){
        if (!(sender instanceof Player player)){
            sender.sendMessage("Only player can execute this command");
            return true;
        }

        if (!player.isOp()){
            return true;
        }

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "simulate", "sim" -> handleSimulate(player, args);
            case "info" -> handleInfo(player);
            default -> handleDamage(player, args);
        }

        return true;
    }

    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "===== 데미지 명령어 =====");
        player.sendMessage(ChatColor.YELLOW + "/damage <플레이어> <데미지> [타입]" + ChatColor.GRAY + " - 대상에게 데미지");
        player.sendMessage(ChatColor.YELLOW + "/damage simulate <데미지> [타입]" + ChatColor.GRAY + " - 시뮬레이션 (피해 X)");
        player.sendMessage(ChatColor.YELLOW + "/damage info" + ChatColor.GRAY + " - 데미지 관련 스탯 보기");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "타입: physical(물리), magical(마법), true(고정)");
    }

    /**
     * 실제 데미지 적용
     * /damage <플레이어> <데미지> [타입]
     */
    private void handleDamage(Player attacker, String[] args) {
        if (args.length < 2){
            attacker.sendMessage(ChatColor.RED+"사용법: /damage <플레이어> <데미지> [타입]");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            attacker.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다: " + args[0]);
            return;
        }

        double baseDamage;

        try{
            baseDamage = Double.parseDouble(args[1]);
        }catch (NumberFormatException e){
            attacker.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요: " + args[1]);
            return;
        }

        DamageType damageType = DamageType.PHYSICAL;
        if (args.length >= 3) {
            damageType = parseDamageType(args[2]);
            if (damageType == null) {
                attacker.sendMessage(ChatColor.RED + "올바른 타입: physical, magical, true");
                return;
            }
        }

        PlayerStatHolder target = statManager.getHolder(targetPlayer).orElse(null);
        PlayerStatHolder attackerHolder = statManager.getHolder(attacker).orElse(null);

        if (target == null || attackerHolder == null) {
            attacker.sendMessage(ChatColor.RED + "플레이어 데이터 로딩 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        IDamageSource source = DamageSourceBuilder.create()
                .attacker(attackerHolder)
                .damageType(damageType)
                .baseDamage(baseDamage)
                .skillScaling(1.0)
                .sourceId("command:damage")
                .displayName("커맨드 데미지")
                .canCritical(true)
                .canBlock(true)
                .canEvade(true)
                .build();

        IDamageResult result = damageCalculator.calculate(source,target);

        if (result.getFinalDamage() > 0) {
            targetPlayer.damage(result.getFinalDamage());
        }

        sendDamageResult(attacker, targetPlayer, result);
    }

    private void handleSimulate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "사용법: /damage simulate <데미지> [타입]");
            return;
        }

        double baseDamage;
        try {
            baseDamage = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요: " + args[1]);
            return;
        }

        // 데미지 타입
        DamageType damageType = DamageType.PHYSICAL;
        if (args.length >= 3) {
            damageType = parseDamageType(args[2]);
            if (damageType == null) {
                player.sendMessage(ChatColor.RED + "올바른 타입: physical, magical, true");
                return;
            }
        }

        PlayerStatHolder holder = statManager.getHolder(player).orElse(null);
        if (holder == null) {
            player.sendMessage(ChatColor.RED + "데이터 로딩 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        IDamageSource source = DamageSourceBuilder.create()
                .attacker(null)
                .damageType(damageType)
                .baseDamage(baseDamage)
                .skillScaling(0)
                .sourceId("command:simulate")
                .displayName("시뮬레이션 데미지")
                .canCritical(false)
                .canBlock(false)
                .canEvade(false)
                .build();

        IDamageResult result = damageCalculator.calculate(source,holder);

        // 결과 출력 (데미지 적용 안 함)
        player.sendMessage(ChatColor.GOLD + "===== 데미지 시뮬레이션 =====");
        player.sendMessage(ChatColor.WHITE + "입력 데미지: " + ChatColor.YELLOW + baseDamage);
        player.sendMessage(ChatColor.WHITE + "데미지 타입: " + ChatColor.YELLOW + damageType.getDisplayName());
        player.sendMessage("");

        sendSimulationResult(player, result);
    }

    /**
     * 시뮬레이션 결과 출력
     */
    private void sendSimulationResult(Player player, IDamageResult result) {
        // 결과 타입
        String resultTypeStr = switch (result.getResultType()) {
            case HIT -> ChatColor.WHITE + "적중";
            case CRITICAL -> ChatColor.GOLD + "★ 치명타!";
            case EVADED -> ChatColor.GRAY + "회피됨";
            case BLOCKED -> ChatColor.AQUA + "블록됨";
            case FULL_BLOCKED -> ChatColor.AQUA + "완벽한 블록!";
        };
        player.sendMessage(ChatColor.WHITE + "결과: " + resultTypeStr);

        if (result.isEvade()) {
            player.sendMessage(ChatColor.GRAY + "데미지 0 (회피)");
            return;
        }

        // 계산 과정
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "【계산 과정】");
        player.sendMessage(ChatColor.GRAY + "  원본 데미지: " + ChatColor.WHITE + String.format("%.1f", result.getRawDamage()));

        if (result.isCritical()) {
            player.sendMessage(ChatColor.GOLD + "  → 치명타 적용됨");
        }

        if (result.getMitigationDamage() > 0) {
            player.sendMessage(ChatColor.GRAY + "  방어 감소: " + ChatColor.RED + "-" + String.format("%.1f", result.getMitigationDamage()));
        }

        if (result.isBlocked()) {
            player.sendMessage(ChatColor.GRAY + "  블록 감소: " + ChatColor.AQUA + "-" + String.format("%.1f", result.getBlockDamage()));
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "최종 데미지: " + ChatColor.GREEN + String.format("%.1f", result.getFinalDamage()));
    }

    /**
     * 스탯 포맷
     */
    private String formatStat(String name, double value, boolean isPercent) {
        String valueStr;
        if (isPercent) {
            valueStr = String.format("%.1f%%", value * 100);
        } else {
            valueStr = String.format("%.0f", value);
        }
        return ChatColor.GRAY + name + ": " + ChatColor.WHITE + valueStr;
    }

    /**
     * 데미지 관련 스탯 정보
     */
    private void handleInfo(Player player) {
        PlayerStatHolder holder = statManager.getHolder(player).orElse(null);
        if (holder == null) {
            player.sendMessage(ChatColor.RED + "데이터 로딩 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "===== " + player.getName() + " 전투 스탯 =====");

        // 공격 스탯
        player.sendMessage(ChatColor.RED + "【공격】");
        player.sendMessage(formatStat("  공격력", holder.getStat("attack_power"), false));
        player.sendMessage(formatStat("  마법력", holder.getStat("magic_power"), false));
        player.sendMessage(formatStat("  치명타 확률", holder.getStat("critical_chance"), true));
        player.sendMessage(formatStat("  치명타 피해", holder.getStat("critical_damage"), true));
        player.sendMessage(formatStat("  명중률", holder.getStat("accuracy"), true));
        player.sendMessage(formatStat("  방어구 관통", holder.getStat("armor_penetration"), false));
        player.sendMessage(formatStat("  마법 관통", holder.getStat("magic_penetration"), false));

        // 방어 스탯
        player.sendMessage(ChatColor.BLUE + "【방어】");
        player.sendMessage(formatStat("  최대 체력", holder.getStat("max_health"), false));
        player.sendMessage(formatStat("  방어력", holder.getStat("armor"), false));
        player.sendMessage(formatStat("  마법 저항력", holder.getStat("magic_resistance"), false));
        player.sendMessage(formatStat("  회피율", holder.getStat("evasion"), true));
        player.sendMessage(formatStat("  블록 확률", holder.getStat("block_chance"), true));
        player.sendMessage(formatStat("  블록량", holder.getStat("block_amount"), false));

        // 예상 피해 감소율
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "【예상 피해 감소율】");
        double armor = holder.getStat("armor");
        double magicRes = holder.getStat("magic_resistance");
        double physicalReduction = armor / (armor + 1000) * 100;
        double magicalReduction = magicRes / (magicRes + 1000) * 100;
        player.sendMessage(ChatColor.GRAY + "  물리: " + ChatColor.WHITE + String.format("%.1f%%", physicalReduction));
        player.sendMessage(ChatColor.GRAY + "  마법: " + ChatColor.WHITE + String.format("%.1f%%", magicalReduction));
    }

    /**
     * 데미지 결과 출력
     */
    private void sendDamageResult(Player attacker, Player victim, IDamageResult result) {
        attacker.sendMessage(ChatColor.GOLD + "===== 데미지 결과 =====");
        attacker.sendMessage(ChatColor.WHITE + "대상: " + ChatColor.YELLOW + victim.getName());
        attacker.sendMessage(ChatColor.WHITE + "타입: " + ChatColor.YELLOW + result.getSource().getDamageType().getDisplayName());
        attacker.sendMessage("");

        sendSimulationResult(attacker, result);

        // 피해자에게 알림
        victim.sendMessage(ChatColor.RED + attacker.getName() + "에게 " +
                String.format("%.1f", result.getFinalDamage()) + " 데미지를 받았습니다!");
    }

    private DamageType parseDamageType(String str) {
        return switch (str.toLowerCase()) {
            case "physical", "phys", "p", "물리" -> DamageType.PHYSICAL;
            case "magical", "magic", "m", "마법" -> DamageType.MAGICAL;
            case "true", "t", "고정" -> DamageType.TRUE;
            default -> null;
        };
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 플레이어 이름 또는 서브커맨드
            completions.add("simulate");
            completions.add("info");
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("simulate") || sub.equals("sim")) {
                completions.addAll(Arrays.asList("10", "50", "100", "500"));
            } else if (!sub.equals("info")) {
                // 플레이어 대상 데미지
                completions.addAll(Arrays.asList("10", "50", "100", "500"));
            }
        } else if (args.length == 3) {
            // 데미지 타입
            completions.addAll(Arrays.asList("physical", "magical", "true"));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
