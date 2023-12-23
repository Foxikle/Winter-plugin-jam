package me.foxikle.frosty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class Listeners implements Listener {
    private final Main plugin;
    private Map<UUID, UUID> gifters = new HashMap<>();

    public Listeners(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            double num = Math.random();
            Color firstColor;
            Color secondColor;
            if (num <= .65) { //65% chance of being green
                mob.getPersistentDataContainer().set(Main.MOB_KEY, PersistentDataType.STRING, "EVERGREEN_GIFT");
                mob.getEquipment().setItem(EquipmentSlot.HAND, Items.EVERGREEN_GIFT);
                firstColor = Color.GREEN;
                secondColor = Color.fromRGB(235, 179, 30);
            } else if (num <= .95) { //30% chance of being scarlet
                mob.getPersistentDataContainer().set(Main.MOB_KEY, PersistentDataType.STRING, "SCARLET_GIFT");
                mob.getEquipment().setItem(EquipmentSlot.HAND, Items.SCARLET_GIFT);
                firstColor = Color.fromRGB(163, 46, 31);
                secondColor = Color.fromRGB(235, 179, 30);
            } else { //5% chance of being blue
                mob.getPersistentDataContainer().set(Main.MOB_KEY, PersistentDataType.STRING, "FROSTY_GIFT");
                mob.getEquipment().setItem(EquipmentSlot.HAND, Items.FROSTY_GIFT);
                firstColor = Color.TEAL;
                secondColor = Color.SILVER;
            }
            new BukkitRunnable() {
                double var = 0;
                Location loc, first, second;

                @Override
                public void run() {
                    if (mob.isDead()) cancel();

                    var += Math.PI / 16;
                    loc = mob.getEyeLocation();
                    first = loc.clone().add(Math.sin(var) / 4, Math.sin(var) / 4 + .5, Math.cos(var) / 4);
                    second = loc.clone().add(Math.cos(var + Math.PI) / 4, Math.cos(var) / 4 + .5, Math.sin(var + Math.PI) / 4);

                    loc.getWorld().spawnParticle(Particle.REDSTONE, first, 2, new Particle.DustOptions(firstColor, 1));
                    loc.getWorld().spawnParticle(Particle.REDSTONE, second, 1, new Particle.DustOptions(secondColor, 1));
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    @EventHandler
    public void onEntityKill(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof LivingEntity entity) {
            if (entity.getHealth() - e.getFinalDamage() > 0) return; // not a kill
            if (e.getDamager() instanceof Player player) {
                if (entity.getPersistentDataContainer().has(Main.MOB_KEY)) {
                    switch (Objects.requireNonNull(e.getEntity().getPersistentDataContainer().get(Main.MOB_KEY, PersistentDataType.STRING))) {
                        case "FROSTY_GIFT" -> {
                            player.sendMessage(Component.text("You found a ", NamedTextColor.YELLOW).append(Items.FROSTY_GIFT.displayName()).append(Component.text("!", NamedTextColor.YELLOW)));
                            e.getEntity().getLocation().getWorld().dropItemNaturally(entity.getLocation(), Items.FROSTY_GIFT);
                        }
                        case "SCARLET_GIFT" -> {
                            player.sendMessage(Component.text("You found a ", NamedTextColor.YELLOW).append(Items.SCARLET_GIFT.displayName()).append(Component.text("!", NamedTextColor.YELLOW)));
                            entity.getLocation().getWorld().dropItemNaturally(entity.getLocation(), Items.SCARLET_GIFT);
                        }
                        case "EVERGREEN_GIFT" -> {
                            player.sendMessage(Component.text("You found an ", NamedTextColor.YELLOW).append(Items.EVERGREEN_GIFT.displayName()).append(Component.text("!", NamedTextColor.YELLOW)));
                            entity.getLocation().getWorld().dropItemNaturally(entity.getLocation(), Items.EVERGREEN_GIFT);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item != null && item.getType() != Material.AIR) {
            if (item.getItemMeta().getPersistentDataContainer().get(Main.ID_KEY, PersistentDataType.STRING) != null) { // is a gift
                if (event.getAction().isRightClick()) {
                    event.setCancelled(true);
                    ArmorStand ar = event.getPlayer().getWorld().spawn(event.getPlayer().getEyeLocation(), ArmorStand.class);
                    ar.addScoreboardTag(item.getItemMeta().getPersistentDataContainer().get(Main.ID_KEY, PersistentDataType.STRING));
                    ar.setCollidable(false);
                    ar.setVisible(false);
                    ar.getEquipment().setHelmet(item);
                    ar.setFallDistance(100);
                    Vector v = event.getPlayer().getLocation().getDirection();
                    ar.setVelocity(v.setY(Math.abs(v.getY())).multiply(new Vector(.5, 1.3, .5)));
                    item.setAmount(item.getAmount() - 1);
                    ar.addScoreboardTag("GIFT_DISPLAY_IN_AIR");
                    gifters.put(ar.getUniqueId(), event.getPlayer().getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onLand(EntityDamageEvent e) {
        if (e.getEntity().getScoreboardTags().contains("GIFT_DISPLAY_IN_AIR")) {
            e.getEntity().removeScoreboardTag("GIFT_DISPLAY_IN_AIR");
            e.getEntity().addScoreboardTag("GIFT_DISPLAY_ON_GROUND");
            e.getEntity().setGravity(false);
            new BukkitRunnable() {
                int itterations = 0;

                @Override
                public void run() {
                    if (itterations >= 1) {
                        this.cancel();
                        // next part of the animation
                        playExplosionAnimation((ArmorStand) e.getEntity());
                        return;
                    }
                    Player p = Bukkit.getPlayer(gifters.get(e.getEntity().getUniqueId()));
                    e.getEntity().teleport(e.getEntity().getLocation().add(p.getEyeLocation().getDirection().normalize().setY(-1.45)));
                    itterations++;
                }
            }.runTaskTimer(plugin, 0, 5);
            e.setCancelled(true);
        }
    }

    private void playExplosionAnimation(ArmorStand e) {
        if (e.getScoreboardTags().contains("FROSTY_GIFT")) {
            new BukkitRunnable() {
                int iterations;

                @Override
                public void run() {
                    if (iterations >= 60) {
                        for (int i = 0; i < 400; i++) {
                            Snowball snowball = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Snowball.class);
                            snowball.addScoreboardTag("GIFT_SNOWBALL");
                        }
                        for (int i = 0; i < 50; i++) {
                            Snowball snowball = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), negate(getRandom()), getRandom()), Snowball.class);
                            snowball.addScoreboardTag("GIFT_SNOWBALL");
                        }
                        e.getWorld().createExplosion(e, e.getEyeLocation(), 1f, false, false);
                        e.remove();
                        ItemStack reward = Items.getFrostyReward();
                        Player player = Bukkit.getPlayer(gifters.get(e.getUniqueId()));
                        player.getInventory().addItem(reward);
                        player.sendMessage(Component.text("You got ", NamedTextColor.GREEN).append(reward.displayName()).append(Component.text(" x" + reward.getAmount() + " from a frosty gift!", NamedTextColor.GREEN)));
                        Item i = e.getWorld().dropItemNaturally(e.getEyeLocation(), reward);
                        i.setCanPlayerPickup(false);

                        Bukkit.getScheduler().runTaskLater(plugin, i::remove, 90);
                        this.cancel();
                        return;
                    }
                    int spinInc = 20;

                    if (iterations > 20) spinInc += 5;
                    if (iterations > 30) spinInc += 5;
                    if (iterations > 40) spinInc += 7;
                    if (iterations > 45) spinInc += 10;
                    if (iterations > 50) spinInc += 13;
                    if (iterations > 55) spinInc += 17;

                    e.setRotation(e.getYaw() + spinInc, 0);

                    e.teleport(e.getLocation().add(0, .02, 0));

                    iterations++;
                }
            }.runTaskTimer(plugin, 0, 1);
        } else if (e.getScoreboardTags().contains("SCARLET_GIFT")) {
            new BukkitRunnable() {
                int iterations;

                @Override
                public void run() {
                    if (iterations >= 60) {
                        e.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, e.getEyeLocation(), 1);
                        ItemDisplay display = e.getWorld().spawn(e.getEyeLocation(), ItemDisplay.class);
                        display.setBillboard(Display.Billboard.VERTICAL);
                        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(2.5f, 2.5f, 2.5f), new AxisAngle4f()));
                        display.setItemStack(e.getItem(EquipmentSlot.HEAD));
                        e.remove();
                        new BukkitRunnable() {
                            int iterations = 0;
                            double var = 0;
                            Location loc, l1, l2, l3, l4, l5, l6;

                            @Override
                            public void run() {
                                if (iterations >= 40) {
                                    display.remove();

                                    for (int i = 0; i < 30; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.REDSTONE));
                                        item.addScoreboardTag("GIFT_REDSTONE");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    for (int i = 0; i < 30; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.ROSE_BUSH));
                                        item.addScoreboardTag("GIFT_REDSTONE");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    for (int i = 0; i < 30; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.RED_DYE));
                                        item.addScoreboardTag("GIFT_REDSTONE");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    ItemStack reward = Items.getScarletReward();
                                    Player player = Bukkit.getPlayer(gifters.get(e.getUniqueId()));
                                    player.getInventory().addItem(reward);
                                    player.sendMessage(Component.text("You got ", NamedTextColor.GREEN).append(reward.displayName()).append(Component.text(" x" + reward.getAmount() + " from a scarlet gift!", NamedTextColor.GREEN)));
                                    Item i = e.getWorld().dropItemNaturally(e.getEyeLocation(), reward);
                                    i.setCanPlayerPickup(false);

                                    Bukkit.getScheduler().runTaskLater(plugin, i::remove, 90);
                                    cancel();
                                }

                                var += Math.PI / 32;

                                double devideByMe = (iterations / 5.0);
                                if (devideByMe < 1) devideByMe = 1;
                                if (devideByMe > 3) devideByMe = 3;

                                loc = display.getLocation();
                                // shut I know this code has some redundant parts -- I don't care :)
                                l1 = loc.clone().add((Math.sin(var + 0 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 0 * (Math.PI / 3)) * 2) / devideByMe);
                                l2 = loc.clone().add((Math.sin(var + 1 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 1 * (Math.PI / 3)) * 2) / devideByMe);
                                l3 = loc.clone().add((Math.sin(var + 2 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 2 * (Math.PI / 3)) * 2) / devideByMe);
                                l4 = loc.clone().add((Math.sin(var + 3 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 3 * (Math.PI / 3)) * 2) / devideByMe);
                                l5 = loc.clone().add((Math.sin(var + 4 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 4 * (Math.PI / 3)) * 2) / devideByMe);
                                l6 = loc.clone().add((Math.sin(var + 5 * (Math.PI / 3)) * 2) / devideByMe, .5, (Math.cos(var + 5 * (Math.PI / 3)) * 2) / devideByMe);

                                loc.getWorld().spawnParticle(Particle.FLAME, l1, 0, 0, 1, 0, 1);
                                loc.getWorld().spawnParticle(Particle.FLAME, l2, 0, 0, 1, 0, 1);
                                loc.getWorld().spawnParticle(Particle.FLAME, l3, 0, 0, 1, 0, 1);
                                loc.getWorld().spawnParticle(Particle.FLAME, l4, 0, 0, 1, 0, 1);
                                loc.getWorld().spawnParticle(Particle.FLAME, l5, 0, 0, 1, 0, 1);
                                loc.getWorld().spawnParticle(Particle.FLAME, l6, 0, 0, 1, 0, 1);

                                loc.getWorld().spawnParticle(Particle.REDSTONE, l1, 1, new Particle.DustOptions(Color.RED, 1));
                                loc.getWorld().spawnParticle(Particle.REDSTONE, l2, 1, new Particle.DustOptions(Color.RED, 1));
                                loc.getWorld().spawnParticle(Particle.REDSTONE, l3, 1, new Particle.DustOptions(Color.RED, 1));
                                loc.getWorld().spawnParticle(Particle.REDSTONE, l4, 1, new Particle.DustOptions(Color.RED, 1));
                                loc.getWorld().spawnParticle(Particle.REDSTONE, l5, 1, new Particle.DustOptions(Color.RED, 1));
                                loc.getWorld().spawnParticle(Particle.REDSTONE, l6, 1, new Particle.DustOptions(Color.RED, 1));

                                iterations++;

                            }
                        }.runTaskTimer(plugin, 5, 1);

                        this.cancel();
                        return;
                    }
                    int spinInc = 20;

                    if (iterations > 20) spinInc += 5;
                    if (iterations > 30) spinInc += 5;
                    if (iterations > 40) spinInc += 7;
                    if (iterations > 45) spinInc += 10;
                    if (iterations > 50) spinInc += 13;
                    if (iterations > 55) spinInc += 17;

                    e.setRotation(e.getYaw() + spinInc, 0);

                    e.teleport(e.getLocation().add(0, .02, 0));

                    iterations++;
                }
            }.runTaskTimer(plugin, 0, 1);
        } else if (e.getScoreboardTags().contains("EVERGREEN_GIFT")) {
            new BukkitRunnable() {
                int iterations;

                @Override
                public void run() {
                    if (iterations >= 60) {
                        e.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, e.getEyeLocation(), 1);
                        ItemDisplay display = e.getWorld().spawn(e.getEyeLocation(), ItemDisplay.class);
                        display.setBillboard(Display.Billboard.VERTICAL);
                        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(2.5f, 2.5f, 2.5f), new AxisAngle4f()));
                        display.setItemStack(e.getItem(EquipmentSlot.HEAD));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> display.setTeleportDuration(5), 2);
                        e.remove();

                        Bukkit.getScheduler().runTaskLater(plugin, () -> display.teleport(display.getLocation().add(0, 2, 0)), 3);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> display.setTeleportDuration(1), 23);
                        new BukkitRunnable() {
                            int iterations = 0;
                            double totalDistence = 0;
                            @Override
                            public void run() {
                                if(totalDistence >= 3) {
                                    display.remove();

                                    for (int i = 0; i < 60; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.GREEN_CANDLE));
                                        item.addScoreboardTag("GIFT_GREENERY");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    for (int i = 0; i < 60; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.TALL_GRASS));
                                        item.addScoreboardTag("GIFT_GREENERY");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    for (int i = 0; i < 60; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.FERN));
                                        item.addScoreboardTag("GIFT_GREENERY");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    for (int i = 0; i < 60; i++) {
                                        Item item = e.getWorld().spawn(e.getEyeLocation().add(getRandom(), Math.abs(getRandom()), getRandom()), Item.class);
                                        item.setItemStack(new ItemStack(Material.MOSS_BLOCK));
                                        item.addScoreboardTag("GIFT_GREENERY");
                                        item.addScoreboardTag(String.valueOf(System.nanoTime()));
                                        item.setCanPlayerPickup(false);
                                        item.setVelocity(new Vector(getRandom(), Math.abs(getRandom()), getRandom()).multiply(1f));
                                        Bukkit.getScheduler().runTaskLater(plugin, item::remove, 25);
                                    }

                                    ItemStack reward = Items.getEvergreenReward();
                                    Player player = Bukkit.getPlayer(gifters.get(e.getUniqueId()));
                                    player.getInventory().addItem(reward);
                                    player.sendMessage(Component.text("You got ", NamedTextColor.GREEN).append(reward.displayName()).append(Component.text(" x" + reward.getAmount() + " from an evergreen gift!", NamedTextColor.GREEN)));
                                    Item i = e.getWorld().dropItemNaturally(e.getEyeLocation(), reward);
                                    i.setCanPlayerPickup(false);

                                    Bukkit.getScheduler().runTaskLater(plugin, i::remove, 90);
                                    displayGroundShake(display.getLocation());
                                    cancel();
                                }
                                iterations++;
                                totalDistence += iterations*.1;
                                display.teleport(display.getLocation().subtract(0, iterations*.1, 0));
                            }
                        }.runTaskTimer(plugin, 24, 1);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {

                        }, 37);


                        this.cancel();
                        return;
                    }
                    int spinInc = 20;

                    if (iterations > 20) spinInc += 5;
                    if (iterations > 30) spinInc += 5;
                    if (iterations > 40) spinInc += 7;
                    if (iterations > 45) spinInc += 10;
                    if (iterations > 50) spinInc += 13;
                    if (iterations > 55) spinInc += 17;

                    e.setRotation(e.getYaw() + spinInc, 0);

                    e.teleport(e.getLocation().add(0, .02, 0));

                    iterations++;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent e) {
        if (e.getHitBlock() == null) return;
        if (e.getEntity().getScoreboardTags().contains("GIFT_SNOWBALL")) {
            Bukkit.getOnlinePlayers().forEach(player -> player.sendBlockChange(e.getHitBlock().getLocation(), Material.SNOW_BLOCK.createBlockData()));
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> player.sendBlockChange(e.getHitBlock().getLocation(), e.getHitBlock().getBlockData())), 50);
        }
    }

    private double getRandom() {
        double d = new Random().nextDouble();
        if (new Random().nextDouble() > .5)
            d *= -1;
        return d;
    }

    private double negate(double d) {
        if (d > 0)
            return d * -1;
        return d;
    }
    
    private void displayGroundShake(Location loc) {
        int radius = 7;
        World world = loc.getWorld();
        int centerX = loc.getBlockX();
        int centerZ = loc.getBlockZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Location location = new Location(world, x + 0.5, loc.getY(), z + 0.5);
                double distance = location.distance(loc);
                if( distance <= radius) {
                    BlockData original = location.getBlock().getBlockData();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        FallingBlock fb = world.spawn(location, FallingBlock.class);
                        fb.setInvulnerable(true);
                        fb.addScoreboardTag("GROUND_SHAKE");
                        fb.shouldAutoExpire(false);
                        fb.setBlockData(original);
                        fb.setVelocity(new Vector(0, .15, 0));
                    }, (int) (distance * 1.5));
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDropItemEvent e) {
        if(e.getEntity().getScoreboardTags().contains("GROUND_SHAKE"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(EntityChangeBlockEvent e) {
        if(e.getEntity().getScoreboardTags().contains("GROUND_SHAKE"))
            e.setCancelled(true);
    }
}
