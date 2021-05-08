package xyz.n7mn.dev.chestlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

class ChestListener implements Listener {

    private final Plugin plugin;
    private final Connection con;
    private List<UUID> editList;
    private List<UUID> delList;

    public ChestListener(Plugin plugin, Connection con, List<UUID> editList, List<UUID> delList){
        this.plugin = plugin;
        this.con = con;

        this.editList = editList;
        this.delList = delList;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void BlockPlaceEvent (BlockPlaceEvent e){
        if (e.getBlockPlaced().getType() != Material.CHEST){
            return;
        }

        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"/lockで保護できます。");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void InventoryOpenEvent(InventoryOpenEvent e){
        // debug用
        // plugin.getLogger().info(e.getEventName());

        UUID openUser = e.getPlayer().getUniqueId();
        Inventory inventory = e.getInventory();
        Location location = inventory.getLocation();

        if (location == null){
            return;
        }

        // plugin.getLogger().info("x: "+location.getBlockX()+" y: "+location.getBlockY()+" z: "+location.getBlockZ());
        // plugin.getLogger().info("chest : " + inventory.getSize());

        if (con == null){
            return;
        }

        boolean isLockMode = false;
        for (UUID uuid : editList){
            if (uuid.equals(openUser)){
                isLockMode = true;
                break;
            }
        }

        for (UUID uuid : delList){
            if (uuid.equals(openUser)){
                isLockMode = true;
                break;
            }
        }
        // plugin.getLogger().info("test : "+isLockMode);

        if (!isLockMode){
            try {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM ChestLockList WHERE x = ? AND y = ? AND z = ? AND Active = 1");
                statement.setInt(1, location.getBlockX());
                statement.setInt(2, location.getBlockY());
                statement.setInt(3, location.getBlockZ());
                ResultSet set = statement.executeQuery();

                if (set.next()){
                    UUID uuid = UUID.fromString(set.getString("LockUser"));

                    if (!uuid.equals(openUser)){
                        e.getView().close();
                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] " + ChatColor.RESET + "チェスト保護がかかっています。");
                        if (e.getPlayer().isOp() || e.getPlayer().hasPermission("chestlock.op")){
                            Player player = plugin.getServer().getPlayer(UUID.fromString(set.getString("LockUser")));
                            if (player != null){
                                e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] " + ChatColor.RESET + player.getName() + "さんが保護をかけています。 (op限定メッセージです。)");
                            } else {
                                e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] " + ChatColor.RESET + "以下のUUIDの方が保護をかけています。 (op限定メッセージです。)\nhttps://ja.namemc.com/profile/"+set.getString("LockUser"));
                            }
                        }
                        e.setCancelled(true);
                    }
                }

                set.close();
                statement.close();

            } catch (SQLException ex){
                ex.printStackTrace();
            }
        } else {
            editList.remove(openUser);

            new Thread(()->{
                boolean isAdd = true;

                for (UUID uuid : delList){
                    if (uuid.equals(openUser)){
                        isAdd = false;
                        break;
                    }
                }

                if (isAdd){
                    try {
                        PreparedStatement statement = con.prepareStatement("SELECT * FROM ChestLockList WHERE WorldUUID = ? AND x = ? AND y = ? AND z = ? AND Active = 1");
                        statement.setString(1, location.getWorld().getUID().toString());
                        statement.setInt(2, location.getBlockX());
                        statement.setInt(3, location.getBlockY());
                        statement.setInt(4, location.getBlockZ());
                        ResultSet set = statement.executeQuery();
                        if (set.next()) {
                            set.close();
                            statement.close();
                            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    e.getView().close();
                                    e.getPlayer().closeInventory();
                                    e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"すでに保護されています。");
                                }
                            });
                            e.setCancelled(true);
                            return;
                        }
                        set.close();
                        statement.close();
                    } catch (SQLException ex){
                        ex.printStackTrace();
                    }

                    try {
                        PreparedStatement statement = con.prepareStatement("INSERT INTO `ChestLockList`(`UUID`, `LockUser`, `WorldUUID`, `x`, `y`, `z`, `Active`) VALUES (?,?,?,?,?,?,?)");
                        statement.setString(1, UUID.randomUUID().toString());
                        statement.setString(2, openUser.toString());
                        statement.setString(3, location.getWorld().getUID().toString());
                        statement.setInt(4, location.getBlockX());
                        statement.setInt(5, location.getBlockY());
                        statement.setInt(6, location.getBlockZ());
                        statement.setBoolean(7, true);
                        statement.execute();
                        statement.close();
                    } catch (SQLException ex){
                        ex.printStackTrace();
                    }


                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            e.getView().close();
                            e.getPlayer().closeInventory();
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"チェスト保護をしました。 解除するには/unlockを実行してください。");
                        }
                    });
                    e.setCancelled(true);
                    return;
                }
                delList.remove(openUser);

                boolean isUnlock = false;
                String unlockUUID = null;
                try {
                    PreparedStatement statement = con.prepareStatement("SELECT * FROM ChestLockList WHERE WorldUUID = ? AND x = ? AND y = ? AND z = ? AND Active = 1");
                    statement.setString(1, location.getWorld().getUID().toString());
                    statement.setInt(2, location.getBlockX());
                    statement.setInt(3, location.getBlockY());
                    statement.setInt(4, location.getBlockZ());
                    ResultSet set = statement.executeQuery();
                    if (set.next()) {
                        if (!openUser.equals(UUID.fromString(set.getString("LockUser")))){
                            if (!e.getPlayer().isOp() && !e.getPlayer().hasPermission("chestlock.op")){
                                set.close();
                                statement.close();
                                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        e.getView().close();
                                        e.getPlayer().closeInventory();
                                        e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"他の人が保護したチェストは解除できません。");
                                    }
                                });
                                return;
                            }
                        }

                        unlockUUID = set.getString("UUID");
                        set.close();
                        statement.close();
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                e.getView().close();
                                e.getPlayer().closeInventory();
                            }
                        });
                        isUnlock = true;
                    } else {
                        set.close();
                        statement.close();
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                e.getView().close();
                                e.getPlayer().closeInventory();
                                e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"保護されていません。");
                            }
                        });
                        return;
                    }
                } catch (SQLException ex){
                    ex.printStackTrace();
                }

                if (isUnlock){
                    try {
                        PreparedStatement statement = con.prepareStatement("UPDATE `ChestLockList` SET `Active`= 0 WHERE UUID = ?");
                        statement.setString(1, unlockUUID);
                        statement.execute();
                        statement.close();
                    } catch (SQLException ex){
                        ex.printStackTrace();
                    }

                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            e.getPlayer().sendMessage(ChatColor.YELLOW + "[ChestLock] "+ChatColor.RESET+"保護解除しました。");
                        }
                    });
                }
            }).start();
        }


        // plugin.getLogger().info("x: "+location.getBlockX()+" y: "+location.getBlockY()+" z: "+location.getBlockZ());
    }

}
