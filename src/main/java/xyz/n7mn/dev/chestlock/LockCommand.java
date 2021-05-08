package xyz.n7mn.dev.chestlock;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

class LockCommand implements CommandExecutor {

    private List<UUID> isEditList;
    public LockCommand(List<UUID> isEditList){
        this.isEditList = isEditList;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player){
            Player player = (Player) sender;

            if (isEditList.size() > 0){
                for (UUID uuid : isEditList){
                    if (player.getUniqueId().equals(uuid)){
                        player.sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"保護したいチェストを開いてください。");
                        return true;
                    }
                }
            }

            player.sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"保護したいチェストを開いてください。");
            isEditList.add(player.getUniqueId());
        }

        return true;
    }
}
