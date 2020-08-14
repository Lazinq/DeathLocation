package me.Lazinq.DeathLocation;

import me.Lazinq.DeathLocation.objects.Data;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DeathLocation extends JavaPlugin implements Listener, CommandExecutor {

    private final HashMap<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, List<Data>> dataMap = new HashMap<>();

    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        loadData();
    }
    public void onDisable(){
        saveData();
    }

    private void saveData() {
        for (Map.Entry<UUID, List<Data>> myDataMap : dataMap.entrySet()) {
            List<Data> deaths = dataMap.get(myDataMap.getKey());
            myDataMap.getValue().forEach(data -> {
                String saveKey = "DeathInfo." + myDataMap.getKey() + ".";
                String intPlace = saveKey + deaths.indexOf(data) + ".";
                getConfig().set(intPlace + "location", locToString(data.getLocation()));
                getConfig().set(intPlace + "reason", data.getReason());
                getConfig().set(intPlace + "time", data.getTime());
            });
        }
        for (UUID uuid : cooldown.keySet()) {
            getConfig().set(uuid.toString(), cooldown.get(uuid));
        }
        saveConfig();
    }

    private void loadData() {
        if (getConfig().getConfigurationSection("DeathInfo") == null) return;
            ConfigurationSection sec = getConfig().getConfigurationSection("DeathInfo");
            for (String uuid : sec.getKeys(false)) {
                ConfigurationSection se = getConfig().getConfigurationSection("DeathInfo." + uuid);
                List<Data> data = new ArrayList<>();
                for (String index : se.getKeys(false)) {
                    Location loc = stringToLoc(se.getString(index + ".location"));
                    String reason = se.getString(index + ".reason");
                    Long time = se.getLong(index + ".time");
                    data.add(new Data(loc, reason, time));
                }
                dataMap.put(UUID.fromString(uuid), data);
            }
            for (String str : getConfig().getConfigurationSection("DeathInfo").getKeys(false)) {
                UUID uuid = UUID.fromString(str);
                long time = getConfig().getLong(str);
                cooldown.put(uuid, time);
            }
                getConfig().set("DeathInfo", null);
            saveConfig();
    }

    public String locToString(Location loc) {
        return loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
    public Location stringToLoc(final String input) {
        final String[] args = input.split(", ");
        return new Location(Bukkit.getWorld(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]), Integer.valueOf(args[3]));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player pl = e.getEntity();
        UUID uuid = pl.getUniqueId();
        List<Data> death = new ArrayList<>();
        if(!dataMap.containsKey(uuid)) {
            death.add(new Data(e.getEntity().getLocation(), e.getDeathMessage(), System.currentTimeMillis()));
            dataMap.put(uuid, death);

        } else if(dataMap.containsKey(uuid)) {
            List<Data> deaths = dataMap.get(uuid);
            deaths.add(new Data(e.getEntity().getLocation(), e.getDeathMessage(), System.currentTimeMillis()));
            if (deaths.size() >= getConfig().getInt("death.maximumMessageAmount")+1) {
                deaths.remove(0);
                }
            }
        }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        final long COOLDOWN_TIME = TimeUnit.SECONDS.toMillis(getConfig().getInt("cooldown.time"));

        long now = System.currentTimeMillis(), lastUsed = cooldown.getOrDefault(player.getUniqueId(), COOLDOWN_TIME);
        if (now - lastUsed < COOLDOWN_TIME) {
            for (String msg : getConfig().getStringList("cooldown.message")) {
                msg = msg.replace("$Player", player.getDisplayName());
                msg = msg.replace("$Cooldown", player.getDisplayName());//onjuist
                player.sendMessage(applyCC(msg));
                return true;
            }
        }

        if (label.equalsIgnoreCase("dp")) {
            if (args.length == 0) {
                if (sender.hasPermission("dp.use")) {
                    if (dataMap.containsKey(uuid)) {
                        for (Data data : dataMap.get(uuid)) {
                            for (String msg : getConfig().getStringList("death.message")) {
                                Long dataTime = data.getTime();
                                SimpleDateFormat sDF = new SimpleDateFormat("HH:mm");
                                String time = sDF.format(dataTime);
                                msg = msg.replace("$Player", player.getDisplayName());msg = msg.replace("$World", data.getLocation().getWorld().getName());msg = msg.replace("$X", Integer.toString(data.getLocation().getBlockX()));msg = msg.replace("$Y", Integer.toString(data.getLocation().getBlockY()));msg = msg.replace("$Z", Integer.toString(data.getLocation().getBlockZ()));msg = msg.replace("$Reason", data.getReason());msg = msg.replace("$Time", time);
                                player.sendMessage(applyCC(msg));
                                this.cooldown.put(player.getUniqueId(), now);
                            }
                        }
                    }
                }
            }
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("dp.reload")) {
                        for (String msg : this.getConfig().getStringList("reload.message")) {
                            sender.sendMessage(applyCC(msg));
                        }
                        this.reloadConfig();
                    }
                }
            }
        }
        return false;
    }

    public String applyCC(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

}
