package org.gi.gIEngine.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gi.builder.StatModifierBuilder;
import org.gi.gIEngine.GIEngine;
import org.gi.gIEngine.StatLoader;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.gIEngine.service.PlayerStatManager;
import org.gi.stat.*;
import org.gi.stat.enums.ModifierType;
import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;
import org.gi.storage.IPlayerDataStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatCommand {

    private final PlayerStatManager statManager;
    private final IStatRegistry statRegistry;
    private final StatLoader loader;

    public StatCommand(IStatRegistry statRegistry, StatLoader loader) {
        this.statRegistry = statRegistry;
        this.statManager = GIEngine.getInstance().getPlayerStatManager();
        this.loader = loader;
    }

    public boolean execute(CommandSender sender, String[] args) {
        // reload, list, saveall은 콘솔에서도 가능
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    handleReload(sender);
                    return true;
                }
                case "list" -> {
                    showStatList(sender);
                    return true;
                }
                case "saveall" -> {
                    handleSaveAll(sender);
                    return true;
                }
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능합니다.");
            return true;
        }

        PlayerStatHolder holder = statManager.getOrLoad(player);

        if (args.length == 0) {
            showAllStats(player, holder);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> handleSet(player, holder, args);
            case "add" -> handleAdd(player, holder, args);
            case "addperm" -> handleAddPermanent(player, holder, args);
            case "clear" -> handleClear(player, holder, args);
            case "recalc" -> handleRecalculate(player, holder);
            case "save" -> handleSave(player, holder);
            case "load" -> handleLoad(player);
            default -> showStatDetail(player, holder, args[0]);
        }

        return true;
    }

    /**
     * 전체 스탯 확인
     */
    private void showAllStats(Player player, PlayerStatHolder holder) {
        player.sendMessage(ChatColor.GOLD + "========== " + player.getName() + " Stats ==========");

        for (IStatInstance statInstance : holder.getAllIStatInstances()) {
            IStat stat = statInstance.getStat();
            double value = statInstance.getFormatValue();

            String valueStr;
            if (stat.isPercent()) {
                valueStr = String.format("%.1f%%", value * 100);
            } else if (stat.getScalingType() == ScalingType.INTEGER) {
                valueStr = String.valueOf((int) value);
            } else {
                valueStr = String.format("%.2f", value);
            }

            ChatColor color = getCategoryColor(stat.getCategory());
            player.sendMessage(color + stat.getDisplayName() + ChatColor.WHITE + ": " + ChatColor.GREEN + valueStr);
        }

        player.sendMessage(ChatColor.GOLD + "==========================================");
    }

    /**
     * 스탯 상세 정보
     */
    private void showStatDetail(Player player, PlayerStatHolder holder, String statId) {
        IStatInstance instance = holder.getIStatInstance(statId).orElse(null);

        if (instance == null) {
            player.sendMessage(ChatColor.RED + "스탯을 찾을 수 없습니다: " + statId);
            player.sendMessage(ChatColor.GRAY + "/stat list 로 등록된 스탯 목록을 확인하세요.");
            return;
        }

        IStat stat = instance.getStat();

        player.sendMessage(ChatColor.GOLD + "===== " + stat.getDisplayName() + " =====");
        player.sendMessage(ChatColor.GRAY + "ID: " + stat.getID());
        for (String desc : stat.getDescriptions()) {
            player.sendMessage(ChatColor.GRAY + desc);
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "Base: " + ChatColor.YELLOW + instance.getBase());
        player.sendMessage(ChatColor.WHITE + "Flat 합계: " + ChatColor.YELLOW + "+" + instance.getTotalFlat());
        player.sendMessage(ChatColor.WHITE + "Percent 합계: " + ChatColor.YELLOW + "+" + String.format("%.1f%%", instance.getTotalPercent() * 100));
        player.sendMessage(ChatColor.WHITE + "최종값: " + ChatColor.GREEN + instance.getFormatValue());
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Modifiers (" + instance.getModifierCount() + "개):");

        for (IStatModifier modifier : instance.getAllModifiers()) {
            String valueStr = modifier.getType() == ModifierType.FLAT
                    ? "+" + modifier.getValue()
                    : "+" + String.format("%.1f%%", modifier.getValue() * 100);

            // 영구 Modifier 표시
            boolean isPermanent = modifier.getSource().startsWith("permanent:");
            ChatColor color = isPermanent ? ChatColor.LIGHT_PURPLE : ChatColor.WHITE;
            String permTag = isPermanent ? " [영구]" : "";

            player.sendMessage(color + "  - " + modifier.getSource() + ": " + valueStr + permTag);
        }
    }

    /**
     * 기본값 설정
     */
    private void handleSet(Player player, PlayerStatHolder holder, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "사용법: /stat set <statId> <value>");
            return;
        }

        String statId = args[1];
        double value;

        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요: " + args[2]);
            return;
        }

        try {
            holder.setBase(statId, value);
            player.sendMessage(ChatColor.GREEN + getStatName(statId) + " 기본값을 " + value + "로 설정했습니다.");
            player.sendMessage(ChatColor.GRAY + "최종값: " + holder.getStat(statId));
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "등록되지 않은 스탯입니다: " + statId);
        }
    }

    /**
     * 일반 수정자 추가 (임시)
     */
    private void handleAdd(Player player, PlayerStatHolder holder, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "사용법: /stat add <statId> <flat|percent> <value> [source]");
            return;
        }

        String statId = args[1];
        String typeStr = args[2].toLowerCase();
        double value;

        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요: " + args[3]);
            return;
        }

        ModifierType type;
        if (typeStr.equals("flat") || typeStr.equals("f")) {
            type = ModifierType.FLAT;
        } else if (typeStr.equals("percent") || typeStr.equals("p") || typeStr.equals("%")) {
            type = ModifierType.PERCENT;
            value = value / 100.0;
        } else {
            player.sendMessage(ChatColor.RED + "타입은 flat 또는 percent 입니다.");
            return;
        }

        String source = args.length >= 5 ? args[4] : "command:test";

        IStatModifier modifier = StatModifierBuilder.create()
                .source(source)
                .displayName("테스트 수정자")
                .statId(statId)
                .type(type)
                .value(value)
                .build();

        try {
            holder.addModifier(modifier);

            String valueDisplay = type == ModifierType.FLAT
                    ? "+" + args[3]
                    : "+" + args[3] + "%";

            player.sendMessage(ChatColor.GREEN + getStatName(statId) + "에 " + valueDisplay + " 수정자를 추가했습니다.");
            player.sendMessage(ChatColor.GRAY + "최종값: " + holder.getStat(statId));
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "등록되지 않은 스탯입니다: " + statId);
        }
    }

    /**
     * 영구 수정자 추가 (저장됨)
     * /stat addperm <statId> <flat|percent> <value> <source>
     */
    private void handleAddPermanent(Player player, PlayerStatHolder holder, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "사용법: /stat addperm <statId> <flat|percent> <value> <source>");
            player.sendMessage(ChatColor.GRAY + "예: /stat addperm attack_power flat 50 title:warrior");
            return;
        }

        String statId = args[1];
        String typeStr = args[2].toLowerCase();
        double value;

        try {
            value = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요: " + args[3]);
            return;
        }

        ModifierType type;
        if (typeStr.equals("flat") || typeStr.equals("f")) {
            type = ModifierType.FLAT;
        } else if (typeStr.equals("percent") || typeStr.equals("p") || typeStr.equals("%")) {
            type = ModifierType.PERCENT;
            value = value / 100.0;
        } else {
            player.sendMessage(ChatColor.RED + "타입은 flat 또는 percent 입니다.");
            return;
        }

        // permanent: 접두사 자동 추가
        String source = "permanent:" + args[4];

        IStatModifier modifier = StatModifierBuilder.create()
                .source(source)
                .displayName(args[4])
                .statId(statId)
                .type(type)
                .value(value)
                .permanent()
                .build();

        try {
            holder.addModifier(modifier);

            String valueDisplay = type == ModifierType.FLAT
                    ? "+" + args[3]
                    : "+" + args[3] + "%";

            player.sendMessage(ChatColor.LIGHT_PURPLE + "[영구] " + ChatColor.GREEN + getStatName(statId) + "에 " + valueDisplay + " 수정자를 추가했습니다.");
            player.sendMessage(ChatColor.GRAY + "Source: " + source);
            player.sendMessage(ChatColor.GRAY + "최종값: " + holder.getStat(statId));
            player.sendMessage(ChatColor.YELLOW + "이 수정자는 서버 재시작 후에도 유지됩니다.");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "등록되지 않은 스탯입니다: " + statId);
        }
    }

    /**
     * 수정자 제거
     */
    private void handleClear(Player player, PlayerStatHolder holder, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "사용법: /stat clear <statId|all|perm>");
            return;
        }

        String target = args[1].toLowerCase();

        switch (target) {
            case "all" -> {
                holder.clearAllModifiers();
                player.sendMessage(ChatColor.GREEN + "모든 스탯의 수정자를 제거했습니다.");
            }
            case "perm", "permanent" -> {
                int removed = holder.removeAllModifiersBySource("permanent:");
                player.sendMessage(ChatColor.GREEN + "모든 영구 수정자를 제거했습니다. (" + removed + "개)");
            }
            default -> holder.getIStatInstance(target).ifPresentOrElse(
                    instance -> {
                        instance.clearModifiers();
                        player.sendMessage(ChatColor.GREEN + getStatName(target) + "의 수정자를 모두 제거했습니다.");
                        player.sendMessage(ChatColor.GRAY + "최종값: " + instance.getFinal());
                    },
                    () -> player.sendMessage(ChatColor.RED + "스탯을 찾을 수 없습니다: " + target)
            );
        }
    }

    /**
     * 스탯 재계산
     */
    private void handleRecalculate(Player player, PlayerStatHolder holder) {
        holder.recalculateAllStat();
        player.sendMessage(ChatColor.GREEN + "모든 스탯을 재계산했습니다.");
    }

    /**
     * 플레이어 데이터 저장
     */
    private void handleSave(Player player, PlayerStatHolder holder) {
        IPlayerDataStorage storage = GIEngine.getInstance().getStorage();

        if (storage == null || !storage.isConnected()) {
            player.sendMessage(ChatColor.RED + "저장소가 비활성화 상태입니다.");
            return;
        }

        statManager.save(holder);
        player.sendMessage(ChatColor.GREEN + "스탯 데이터를 저장했습니다.");
    }

    /**
     * 플레이어 데이터 로드
     */
    private void handleLoad(Player player) {
        IPlayerDataStorage storage = GIEngine.getInstance().getStorage();

        if (storage == null || !storage.isConnected()) {
            player.sendMessage(ChatColor.RED + "저장소가 비활성화 상태입니다.");
            return;
        }

        // 기존 홀더 제거 후 새로 로드
        statManager.unload(player);
        statManager.load(player);

        player.sendMessage(ChatColor.GREEN + "스탯 데이터를 다시 로드했습니다.");
    }

    /**
     * 스탯 설정 리로드
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("giengine.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return;
        }

        var result = loader.reload();
        sender.sendMessage(ChatColor.GREEN + result.getMessage());
    }

    /**
     * 모든 플레이어 저장
     */
    private void handleSaveAll(CommandSender sender) {
        if (!sender.hasPermission("giengine.admin.saveall")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return;
        }

        IPlayerDataStorage storage = GIEngine.getInstance().getStorage();

        if (storage == null || !storage.isConnected()) {
            sender.sendMessage(ChatColor.RED + "저장소가 비활성화 상태입니다.");
            return;
        }

        statManager.saveAll();
        sender.sendMessage(ChatColor.GREEN + "모든 플레이어 데이터를 저장했습니다. (" + statManager.getLoadedPlayerCount() + "명)");
    }

    /**
     * 등록된 스탯 목록
     */
    private void showStatList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== 등록된 스탯 목록 (" + statRegistry.getStatCount() + "개) =====");

        for (StatCategory category : StatCategory.values()) {
            List<IStat> categoryStats = new ArrayList<>(statRegistry.getStatsByCategory(category));
            if (categoryStats.isEmpty()) continue;

            sender.sendMessage(getCategoryColor(category) + "【" + category.name() + "】");
            for (IStat stat : categoryStats) {
                sender.sendMessage(ChatColor.GRAY + "  - " + stat.getID() + " (" + stat.getDisplayName() + ")");
            }
        }
    }

    private String getStatName(String statId) {
        return statRegistry.get(statId)
                .map(IStat::getDisplayName)
                .orElse(statId);
    }

    private ChatColor getCategoryColor(StatCategory category) {
        return switch (category) {
            case OFFENSIVE -> ChatColor.RED;
            case DEFENSIVE -> ChatColor.BLUE;
            case UTILITY -> ChatColor.YELLOW;
            case MISC -> ChatColor.GRAY;
        };
    }

    /**
     * 탭 완성
     */
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                    "set", "add", "addperm", "clear", "recalc",
                    "save", "load", "reload", "saveall", "list"
            ));
            completions.addAll(statRegistry.getAll().stream()
                    .map(IStat::getID)
                    .toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "set", "add", "addperm" -> {
                    completions.addAll(statRegistry.getAll().stream()
                            .map(IStat::getID)
                            .toList());
                }
                case "clear" -> {
                    completions.add("all");
                    completions.add("perm");
                    completions.addAll(statRegistry.getAll().stream()
                            .map(IStat::getID)
                            .toList());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("addperm")) {
                completions.addAll(Arrays.asList("flat", "percent"));
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("addperm")) {
            // 영구 수정자 소스 예시
            completions.addAll(Arrays.asList("title:warrior", "achievement:first_kill", "rank:vip"));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .toList();
    }
}