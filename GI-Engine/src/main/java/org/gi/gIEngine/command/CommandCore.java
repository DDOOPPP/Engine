package org.gi.gIEngine.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.gi.gIEngine.StatLoader;
import org.gi.stat.IStatRegistry;
import org.gi.stat.StatRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandCore implements CommandExecutor , TabExecutor {
    private final IStatRegistry statRegistry;
    private final StatCommand statCommand;
    private final StatLoader statLoader;

    public CommandCore(IStatRegistry statRegistry, StatLoader loader) {
        this.statRegistry = statRegistry;
        this.statLoader = loader;
        this.statCommand = new StatCommand(statRegistry,loader);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String [] args) {
        String commandName = command.getName();

        switch (commandName){
            case "stat":
                return statCommand.execute(sender, args);
            default: return false;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String [] args) {
        String commandName = command.getName();

        switch (commandName) {
            case "stat":
                if (!(sender instanceof Player player)){
                    return List.of();
                }
                return statCommand.tabComplete(player, args);
            default:
                return List.of();
        }
    }
}
