package me.foxikle.frosty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Items {
    public static ItemStack EVERGREEN_GIFT;
    public static ItemStack FROSTY_GIFT;
    public static ItemStack SCARLET_GIFT;

    static {
        try {
            EVERGREEN_GIFT = createGiftItem(Component.text("Evergreen Gift", TextColor.color(60, 103, 70)), new URL("http://textures.minecraft.net/texture/cf40765a750f38f224bb8defaee806bb6058f84818338146ebf94a02e63d885f"), "EVERGREEN_GIFT", Component.text("Right click to open!", NamedTextColor.GRAY));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            FROSTY_GIFT = createGiftItem(Component.text("Frosty Gift", TextColor.color(116, 219, 237)), new URL("http://textures.minecraft.net/texture/69b564a6f73283112a70b9ce7e15753eb86bd12e7659ec4d0dc0855c6bea76e"), "FROSTY_GIFT",  Component.text("Right click to open!", NamedTextColor.GRAY));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            SCARLET_GIFT = createGiftItem(Component.text("Scarlet Gift", TextColor.color(133, 32, 27)), new URL("http://textures.minecraft.net/texture/45368f5635ff6c3407f0f356c5b6e0947bcd5e38490c9aa8b8b582a4f21ae3cb"), "SCARLET_GIFT",  Component.text("Right click to open!", NamedTextColor.GRAY));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static ItemStack createGiftItem(Component name, URL skinURL, String ID, Component... lore){
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // texture
        PlayerProfile profile =  Bukkit.createPlayerProfile(UUID.fromString("92864445-51c5-4c3b-9039-517c9927d1b4"), "foo");
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinURL);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.displayName(name);

        // meta
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(Main.ID_KEY, PersistentDataType.STRING, ID);
        meta.lore(List.of(lore));

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getFrostyReward() {
        ItemStack item;
        int random = new Random().nextInt(101);
        if(random == 100) {
            item = new ItemStack(Material.NETHERITE_BLOCK);
        } else if (random % 10 == 0) {
            item = new ItemStack(Material.EXPERIENCE_BOTTLE);
            item.setAmount(16);
        } else if (random % 7 == 0) {
            item = new ItemStack(Material.TOTEM_OF_UNDYING);
            item.setAmount(1);
        } else if (random % 3 == 0) {
            item = new ItemStack(Material.DIAMOND);
            item.setAmount(2);
        } else {
            item = new ItemStack(Material.EMERALD);
            item.setAmount(24);
        }

        return item;
    }

    public static ItemStack getScarletReward() {
        ItemStack item;
        int random = new Random().nextInt(101);
        if(random == 100) {
            item = new ItemStack(Material.DIAMOND_BLOCK);
        } else if (random % 10 == 0) {
            item = new ItemStack(Material.RAW_GOLD);
            item.setAmount(16);
        } else if (random % 7 == 0) {
            item = new ItemStack(Material.LAPIS_LAZULI);
        } else if (random % 3 == 0) {
            int armor = new Random().nextInt(4);
            switch (armor) {
                case 0 -> item = new ItemStack(Material.IRON_BOOTS);
                case 1 -> item = new ItemStack(Material.IRON_LEGGINGS);
                case 2 -> item = new ItemStack(Material.IRON_CHESTPLATE);
                case 3 -> item = new ItemStack(Material.IRON_HELMET);
                default -> item = new ItemStack(Material.AIR);
            }
        } else {
            item = new ItemStack(Material.IRON_NUGGET);
            item.setAmount(9);
        }

        return item;
    }

    public static ItemStack getEvergreenReward() {
        ItemStack item;
        int random = new Random().nextInt(101);
        if(random == 100) {
            item = new ItemStack(Material.DIAMOND);
        } else if (random % 10 == 0) {
            item = new ItemStack(Material.RAW_IRON);
            item.setAmount(16);
        } else if (random % 7 == 0) {
            item = new ItemStack(Material.COPPER_INGOT);
        } else if (random % 3 == 0) {
            int armor = new Random().nextInt(4);
            switch (armor) {
                case 0 -> item = new ItemStack(Material.LEATHER_BOOTS);
                case 1 -> item = new ItemStack(Material.LEATHER_LEGGINGS);
                case 2 -> item = new ItemStack(Material.LEATHER_CHESTPLATE);
                case 3 -> item = new ItemStack(Material.LEATHER_HELMET);
                default -> item = new ItemStack(Material.AIR); // won't ever happen
            }
        } else {
            item = new ItemStack(Material.DIRT);
            item.setAmount(4);
        }

        return item;
    }
}
