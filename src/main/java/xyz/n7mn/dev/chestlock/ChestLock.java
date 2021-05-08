package xyz.n7mn.dev.chestlock;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public final class ChestLock extends JavaPlugin {

    private Connection con = null;
    private List<UUID> addLockList = new ArrayList<>();
    private List<UUID> delLockList = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        try {
            boolean found = false;
            Enumeration<Driver> drivers = DriverManager.getDrivers();

            while (drivers.hasMoreElements()){
                Driver driver = drivers.nextElement();
                if (driver.equals(new com.mysql.cj.jdbc.Driver())){
                    found = true;
                    break;
                }
            }

            if (!found){
                DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            }

            con = DriverManager.getConnection("jdbc:mysql://" + getConfig().getString("MySQLServer") + ":" + getConfig().getString("MySQLPort") + "/" + getConfig().getString("MySQLDatabase") + getConfig().getString("MySQLOption"), getConfig().getString("MySQLUsername"), getConfig().getString("MySQLPassword"));
        } catch (SQLException e){
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        getCommand("lock").setExecutor(new LockCommand(addLockList));
        getCommand("unlock").setExecutor(new UnLockCommand(delLockList));

        getServer().getPluginManager().registerEvents(new ChestListener(this, con, addLockList, delLockList), this);
        getLogger().info(getName() + " Ver "+getDescription().getVersion()+" 起動");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        new Thread(()->{
            if (con != null){
                try {
                    con.close();
                } catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }).start();

        getLogger().info(getName() + " Ver "+getDescription().getVersion()+" 終了");
    }
}
