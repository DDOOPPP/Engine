package org.gi.gIEngine.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gi.builder.StatModifierBuilder;
import org.gi.gIEngine.StatLoader;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.gIEngine.service.PlayerStatManager;
import org.gi.stat.*;
import org.gi.stat.enums.ModifierType;
import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatCommand {
    private PlayerStatManager statManager;
    private IStatRegistry statRegistry;
    private StatLoader loader;

    public StatCommand(IStatRegistry statRegistry,StatLoader loader) {
        this.statRegistry = statRegistry;
        statManager = new PlayerStatManager(statRegistry);
        this.loader = loader;
    }

    public boolean execute(CommandSender sender, String[] args){
        if (args.length > 0 && args[0].equals("reload")){
            loader.reload();
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            showStatList(sender);
            return true;
        }

        if (!(sender instanceof Player player)){
            return true;
        }

        PlayerStatHolder holder = statManager.getOrLoad(player);

        if (args.length == 0){
            showAllStats(player,holder);
            return true;
        }

        String subCommand = args[0];

        switch (subCommand){
            case "set" ->
                handleSet(player,holder,args);
            case "add" ->
                handleAdd(player,holder,args);
            case "clear" ->
                handleClear(player,holder,args);
            case "recalc" ->
                handleRecalculate(player,holder);
            default ->
                showStatDetail(player,holder,args);
        }
        return true;
    }
    /**
     * 전체 스탯 확인
     * */
    private void showAllStats(Player player,PlayerStatHolder holder){
        player.sendMessage(" ====== All Stats ====== ");

        for (IStatInstance statInstance : holder.getAllIStatInstances()){
            IStat stat = statInstance.getStat();
            double value = statInstance.getFormatValue();

            String valueStr = "";

            if (stat.isPercent()){
                valueStr = String.format("%.1f%%", value * 100);
            }else if (stat.getScalingType() == ScalingType.INTEGER){
                valueStr = String.valueOf((int) value);
            }
            else{
                valueStr = String.format("%.2f", value);
            }
            ChatColor color = getCategoryColor(stat.getCategory());

            player.sendMessage(color + stat.getDisplayName() + ": " + valueStr);
        }
        player.sendMessage(" ====== All Stats End ====== ");
    }

    private void showStatDetail(Player player,PlayerStatHolder holder, String[] args){
        if (args.length < 2){
            player.sendMessage("Usage: /stat <statId>");
            return;
        }
        String statId = args[1];
        IStatInstance instance = holder.getIStatInstance(statId).orElse(null);

        if (instance == null){
            player.sendMessage(ChatColor.RED+"Not Found Stat: %s".formatted(statId));
            player.sendMessage(ChatColor.GRAY +"Use /stat to check all stats");
            return;
        }

        IStat stat = instance.getStat();

        player.sendMessage(" ====== %s ====== ".formatted(stat.getDisplayName()));
        player.sendMessage("ID: " + stat.getID());
        for (String description : stat.getDescriptions()){
            player.sendMessage(description);
        }
        player.sendMessage("");
        player.sendMessage("Category: " + stat.getCategory());
        player.sendMessage("Base Value: " + instance.getBase());
        player.sendMessage("FLAT Total: "+instance.getTotalFlat());
        player.sendMessage("Percent Total: "+instance.getTotalPercent());
        player.sendMessage("Final Value: " + instance.getFormatValue());
        player.sendMessage("");

        player.sendMessage("Modifiers (%s)개".formatted(instance.getModifierCount()));

        for (IStatModifier modifier : instance.getAllModifiers()){
            String type = modifier.getType() == ModifierType.FLAT ? "+" : "+";
            String value = modifier.getType() == ModifierType.FLAT
                    ? String.valueOf(modifier.getValue())
                    : String.format("%.1f%%", modifier.getValue() * 100);

            ChatColor color = modifier.isExpired() ? ChatColor.LIGHT_PURPLE : ChatColor.WHITE;
            player.sendMessage(color+" - "+modifier.getSource()+": "+type+" "+value);

        }

        player.sendMessage(" ====== %s End ====== ".formatted(stat.getDisplayName()));
    }

    private void handleSet(Player player,PlayerStatHolder holder, String[] args){
        if (args.length < 3){
            player.sendMessage("Usage: /stat set <StatId> <Value>");
            return;
        }

        String statId = args[1];
        double value = 0;

        try{
            value = Double.parseDouble(args[2]);
        }catch (NumberFormatException e){
            player.sendMessage("Failed to parse value: "+args[2]);
            return;
        }
        try{
            holder.setBase(statId, value);
            player.sendMessage("Set %s to %s".formatted(statId, value));
            player.sendMessage("Final Value: "+holder.getStat(statId));
        }catch (IllegalArgumentException e){
            player.sendMessage("Failed to set stat: "+e.getMessage());
        }

    }

    private void handleAdd(Player player, PlayerStatHolder holder, String[] args){
        if (args.length < 4){
            player.sendMessage("Usage: /stat add <StatId> <flat|percent> <Value> [source]");
            return;
        }

        String statId = args[1];
        String typeStr = args[2].toLowerCase();
        double value = 0;

        try{
            value = Double.parseDouble(args[2]);
        }catch (NumberFormatException e){
            player.sendMessage("Failed to parse value: "+args[2]);
            return;
        }

        ModifierType type;

        if (typeStr.equals("flat") || typeStr.equals("f")){
            type = ModifierType.FLAT;
        }else if (typeStr.equals("percent") || typeStr.equals("p")){
            type = ModifierType.PERCENT;
            value = value/100.0;
        }else{
            player.sendMessage("Invalid modifier type: "+typeStr);
            return;
        }

        String source = args.length >= 5 ? args[4] : "command:test";

        IStatModifier modifier = StatModifierBuilder.create()
                .source(source)
                .displayName("test")
                .statId(statId)
                .type(type)
                .value(value)
                .build();

        try{
            holder.addModifier(modifier);

            String valueStr = type == ModifierType.FLAT
                    ? "+" + args[3]
                    : "+" + args[3] + "%";

            player.sendMessage(Stat(statId)+" "+valueStr+" added to "+source+" (ID: "+modifier.getID()+")");
            player.sendMessage("Final Value: "+holder.getStat(statId));
        }catch (IllegalArgumentException e){
            player.sendMessage("Failed to add modifier: "+e.getMessage());
            return;
        }
    }

    private void handleClear(Player player, PlayerStatHolder holder,String[] args){
        if (args.length < 2){
            player.sendMessage("Usage: /stat clear <StatId|all>");
            return;
        }

        String target = args[1].toLowerCase();

        if (target.equals("all")){
            holder.clearAllModifiers();
            player.sendMessage("All modifiers cleared");
        } else {
            holder.getIStatInstance(target).ifPresentOrElse(
                    instance -> {
                        instance.clearModifiers();
                        player.sendMessage("Modifiers cleared for "+Stat(target));
                    },
                    () -> player.sendMessage("Not Found Stat: "+target)
            );
        }
    }

    private void handleRecalculate(Player player, PlayerStatHolder holder){
        holder.recalculateAllStat();
        player.sendMessage("All stats recalculated");
    }

    private void showStatList(CommandSender sender){
        sender.sendMessage(" ====== Stat List ====== ");

        for (StatCategory category : StatCategory.values()){
            List<IStat> categoryStat = new ArrayList<>(statRegistry.getStatsByCategory(category));

            if (categoryStat.isEmpty()){
                continue;
            }

            sender.sendMessage(getCategoryColor(category)+ "[%s Stat]".formatted(category.name()));

            for (IStat stat : categoryStat){
                sender.sendMessage(" - "+Stat(stat.getID())+" ("+stat.getDisplayName()+")");
            }
        }

        sender.sendMessage(" ====== Stat List End ====== ");
    }

    private ChatColor getCategoryColor(StatCategory category){
        switch (category){
            case OFFENSE -> {
                return ChatColor.RED;
            }
            case DEFENSE -> {
                return ChatColor.BLUE;
            }
            default -> {
                return ChatColor.WHITE;
            }
        }
    }

    private String Stat(String statId){
        return statRegistry.get(statId).map(IStat::getDisplayName).orElse(statId);
    }

    public List<String> tabComplete(Player player, String[] args){
        List<String> completions = new ArrayList<>();

        if (args.length == 1){
            completions.add("set");
            completions.add("add");
            completions.add("clear");
            completions.add("reload");
            completions.add("list");
        }else if (args.length == 2){
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("add") || sub.equals("clear")) {
                if (sub.equals("clear")) {
                    completions.add("all");
                }
                completions.addAll(statRegistry.getAll().stream()
                        .map(IStat::getId)
                        .toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")){
            completions.addAll(Arrays.asList("flat", "percent"));
        }

        String lastArg = args[args.length-1].toLowerCase();

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .toList();
    }
}
