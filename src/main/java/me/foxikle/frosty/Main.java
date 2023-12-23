package me.foxikle.frosty;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {
    public static NamespacedKey MOB_KEY = new NamespacedKey("frosty", "mob_key");
    public static NamespacedKey ID_KEY = new NamespacedKey("frosty", "id_key");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("o/");
    }
}
