package com.metrovice.rp.ems;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.department.DepartmentService;
import com.metrovice.rp.device.DeviceMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmergencyService implements Listener, CommandExecutor {
    private static final long DOWNED_SECONDS = 60L;
    private static final double MEDIC_RANGE = 48.0;
    private static final double REVIVE_RANGE = 5.0;
    private final JavaPlugin plugin;
    private final DepartmentService departments;
    private final NamespacedKey medBagKey;
    private final Map<UUID, Long> downedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> bleedoutReady = new ConcurrentHashMap<>();

    public EmergencyService(JavaPlugin plugin, DepartmentService departments) {
        this.plugin = plugin;
        this.departments = departments;
        this.medBagKey = new NamespacedKey(plugin, "ems_medical_bag");
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDowned, 20L, 20L);
    }

    public ItemStack createMedicalBag() {
        ItemStack bag = new ItemStack(Material.CHEST);
        ItemMeta meta = bag.getItemMeta();
        meta.setDisplayName(MetroViceRP.color("&b&lحقيبة الإسعاف"));
        meta.setLore(java.util.List.of(
                MetroViceRP.color("&7معدات علاج مخصصة للمسعفين"),
                MetroViceRP.color("&eاضغط بزر الفأرة الأيمن قرب مصاب لإنعاشه")));
        meta.getPersistentDataContainer().set(medBagKey, PersistentDataType.BYTE, (byte) 1);
        bag.setItemMeta(meta);
        return bag;
    }

    @EventHandler
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (downedUntil.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (player.getHealth() - event.getFinalDamage() > 0.0) return;
        event.setCancelled(true);
        enterDowned(player);
    }

    private void enterDowned(Player player) {
        long until = System.currentTimeMillis() / 1000L + DOWNED_SECONDS;
        downedUntil.put(player.getUniqueId(), until);
        bleedoutReady.put(player.getUniqueId(), false);
        player.setHealth(Math.min(2.0, player.getMaxHealth()));
        player.setFoodLevel(6);
        player.setInvulnerable(true);
        player.setWalkSpeed(0.0f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60 * 10, 10, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60 * 10, 0, false, false, true));
        player.sendTitle("§c§lتم إسقاطك", "§fاطلب مسعفًا من هاتفك", 5, 50, 10);
        player.sendMessage(MetroViceRP.color("&cأنت في حالة إسقاط. أمامك 60 ثانية لطلب الإسعاف."));
        notifyMedics(player, false);
    }

    private void tickDowned() {
        long now = System.currentTimeMillis() / 1000L;
        for (UUID id : downedUntil.keySet()) {
            Player patient = Bukkit.getPlayer(id);
            if (patient == null || !patient.isOnline()) {
                downedUntil.remove(id);
                bleedoutReady.remove(id);
                continue;
            }
            long left = downedUntil.getOrDefault(id, now) - now;
            if (left > 0) {
                patient.sendActionBar("§cحالة إسقاط §8| §f" + left + " ثانية §8| §bاطلب مسعفًا من الهاتف");
                continue;
            }
            if (!bleedoutReady.getOrDefault(id, false)) {
                bleedoutReady.put(id, true);
                if (hasNearbyMedic(patient)) {
                    patient.sendTitle("§eالمسعف قريب", "§fانتظر وصوله أو اطلب المساعدة من الهاتف", 5, 60, 10);
                    downedUntil.put(id, now + 60L);
                    notifyMedics(patient, true);
                } else {
                    patient.sendTitle("§cانتهى وقت الانتظار", "§fافتح هاتفك واختر التحلل أو اطلب مسعفًا", 5, 80, 10);
                    openEmergencyMenu(patient);
                }
            } else if (!hasNearbyMedic(patient)) {
                patient.sendActionBar("§cيمكنك الآن اختيار التحلل من قائمة الإسعاف في هاتفك");
            } else {
                downedUntil.put(id, now + 30L);
            }
        }
    }

    private boolean hasNearbyMedic(Player patient) {
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.equals(patient) || !candidate.getWorld().equals(patient.getWorld())) continue;
            if (candidate.getLocation().distanceSquared(patient.getLocation()) > MEDIC_RANGE * MEDIC_RANGE) continue;
            try {
                if (departments.isOnDuty(candidate.getUniqueId(), "EMS")) return true;
            } catch (SQLException ex) {
                plugin.getLogger().warning("EMS duty check failed: " + ex.getMessage());
            }
        }
        return false;
    }

    private void notifyMedics(Player patient, boolean urgent) {
        String message = MetroViceRP.color("&b[بلاغ إسعاف] &f" + patient.getName()
                + " &7يحتاج مساعدة في &f" + patient.getWorld().getName()
                + " &8(" + patient.getLocation().getBlockX() + ", "
                + patient.getLocation().getBlockY() + ", " + patient.getLocation().getBlockZ() + ")"
                + (urgent ? " &c- عاجل" : ""));
        for (Player medic : Bukkit.getOnlinePlayers()) {
            try {
                if (departments.isOnDuty(medic.getUniqueId(), "EMS")) medic.sendMessage(message);
            } catch (SQLException ex) {
                plugin.getLogger().warning("Could not notify EMS player " + medic.getName());
            }
        }
    }

    public void openEmergencyMenu(Player player) {
        DeviceMenuHolder holder = new DeviceMenuHolder("ems");
        Inventory inv = Bukkit.createInventory(holder, 27, "§8§lMETRO VICE §8| §bEMS");
        holder.inventory(inv);
        boolean downed = downedUntil.containsKey(player.getUniqueId());
        long left = Math.max(0L, downedUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis() / 1000L);
        inv.setItem(11, button(Material.BELL, "&bطلب مسعف", "&7إرسال بلاغ إلى جميع المسعفين المناوبين"));
        inv.setItem(13, button(Material.PAPER, downed ? "&cحالة الإسقاط" : "&aخدمة الإسعاف",
                downed ? "&7الوقت المتبقي: " + left + " ثانية" : "&7يمكنك طلب المساعدة عند الحاجة"));
        if (downed && left <= 0 && !hasNearbyMedic(player)) {
            inv.setItem(15, button(Material.RED_DYE, "&cالتحلل والعودة", "&7إسقاط جميع أغراضك والعودة إلى نقطة المدينة"));
        } else {
            inv.setItem(15, button(Material.GRAY_DYE, "&8التحلل غير متاح الآن", "&7انتظر انتهاء العدّاد وابتعاد المسعفين"));
        }
        inv.setItem(22, button(Material.BARRIER, "&cإغلاق"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DeviceMenuHolder holder)) return;
        if (!holder.view().equals("ems")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot == 22) {
            player.closeInventory();
        } else if (slot == 11) {
            notifyMedics(player, true);
            player.sendMessage(MetroViceRP.color("&aتم إرسال طلب الإسعاف."));
            openEmergencyMenu(player);
        } else if (slot == 15) {
            long left = downedUntil.getOrDefault(player.getUniqueId(), Long.MAX_VALUE) - System.currentTimeMillis() / 1000L;
            if (left <= 0 && downedUntil.containsKey(player.getUniqueId()) && !hasNearbyMedic(player)) {
                bleedout(player);
            } else {
                player.sendMessage(MetroViceRP.color("&cلا يمكنك التحلل الآن."));
                openEmergencyMenu(player);
            }
        }
    }

    @EventHandler
    public void onUseMedicalBag(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta()
                || !held.getItemMeta().getPersistentDataContainer().has(medBagKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
        Player medic = event.getPlayer();
        try {
            if (!departments.isOnDuty(medic.getUniqueId(), "EMS")) {
                medic.sendMessage(MetroViceRP.color("&cحقيبة العلاج مخصصة للمسعفين أثناء المناوبة فقط."));
                return;
            }
        } catch (SQLException ex) {
            medic.sendMessage(MetroViceRP.color("&cتعذر التحقق من رتبتك."));
            return;
        }
        Player nearest = null;
        double best = REVIVE_RANGE * REVIVE_RANGE;
        for (UUID id : downedUntil.keySet()) {
            Player candidate = Bukkit.getPlayer(id);
            if (candidate == null || !candidate.getWorld().equals(medic.getWorld())) continue;
            double distance = candidate.getLocation().distanceSquared(medic.getLocation());
            if (distance <= best) { nearest = candidate; best = distance; }
        }
        if (nearest == null) {
            medic.sendMessage(MetroViceRP.color("&eلا يوجد مصاب قريب منك ضمن 5 بلوكات."));
            return;
        }
        revive(nearest, medic);
    }

    private void revive(Player patient, Player medic) {
        downedUntil.remove(patient.getUniqueId());
        bleedoutReady.remove(patient.getUniqueId());
        patient.setInvulnerable(false);
        patient.setWalkSpeed(0.2f);
        patient.removePotionEffect(PotionEffectType.SLOWNESS);
        patient.removePotionEffect(PotionEffectType.BLINDNESS);
        patient.setHealth(Math.min(10.0, patient.getMaxHealth()));
        patient.setFoodLevel(Math.max(10, patient.getFoodLevel()));
        patient.sendTitle("§a§lتم إسعافك", "§fبواسطة " + medic.getName(), 5, 50, 10);
        medic.sendMessage(MetroViceRP.color("&aتم إنعاش " + patient.getName() + " بنجاح."));
    }

    private void bleedout(Player player) {
        Location dropAt = player.getLocation();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) dropAt.getWorld().dropItemNaturally(dropAt, item.clone());
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) dropAt.getWorld().dropItemNaturally(dropAt, item.clone());
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) dropAt.getWorld().dropItemNaturally(dropAt, offhand.clone());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        downedUntil.remove(player.getUniqueId());
        bleedoutReady.remove(player.getUniqueId());
        player.setInvulnerable(false);
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        Location spawn = configuredRespawn();
        player.teleport(spawn);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.sendTitle("§cتم التحلل", "§fعدت إلى نقطة المدينة وفقدت أغراضك", 5, 70, 10);
    }

    private Location configuredRespawn() {
        String worldName = plugin.getConfig().getString("ems.respawn.world", "");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return Bukkit.getWorlds().get(0).getSpawnLocation();
        double x = plugin.getConfig().getDouble("ems.respawn.x", world.getSpawnLocation().getX());
        double y = plugin.getConfig().getDouble("ems.respawn.y", world.getSpawnLocation().getY());
        double z = plugin.getConfig().getDouble("ems.respawn.z", world.getSpawnLocation().getZ());
        float yaw = (float) plugin.getConfig().getDouble("ems.respawn.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("ems.respawn.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MetroViceRP.color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(MetroViceRP::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("metrovice.admin")) {
            sender.sendMessage(MetroViceRP.color("&cليس لديك صلاحية."));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(MetroViceRP.color("&e/emsbaggive <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MetroViceRP.color("&cاللاعب غير متصل."));
            return true;
        }
        var leftovers = target.getInventory().addItem(createMedicalBag());
        if (!leftovers.isEmpty()) {
            sender.sendMessage(MetroViceRP.color("&cمخزون اللاعب ممتلئ."));
            return true;
        }
        sender.sendMessage(MetroViceRP.color("&aتم تسليم حقيبة الإسعاف إلى " + target.getName()));
        return true;
    }
}
