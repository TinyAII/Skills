package nl.tinyaii.skills.listener;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.skill.AbilityDef;
import nl.tinyaii.skills.skill.PlayerDataManager;
import nl.tinyaii.skills.skill.Skill;
import nl.tinyaii.skills.skill.SkillsManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 技能经验与能力监听器：8 条线。
 */
public class SkillsListener implements Listener {

    private final SkillsPlugin plugin;
    private final nl.tinyaii.skills.effect.AbilityEffects effects;
    private static final Set<Material> LOGS = EnumSet.noneOf(Material.class);
    private static final Set<Material> CROPS = EnumSet.noneOf(Material.class);
    private static final Set<Material> ORES = EnumSet.noneOf(Material.class);
    private static final Set<Material> HIGH_ORE = EnumSet.noneOf(Material.class);
    private static final Set<Material> MID_ORE = EnumSet.noneOf(Material.class);
    private static final Set<Material> LOW_ORE = EnumSet.noneOf(Material.class);
    private static final Set<EntityType> HOSTILE = EnumSet.noneOf(EntityType.class);

    static {
        for (Material m : Material.values()) {
            String n = m.name();
            if (m.name().endsWith("_LOG") || m.name().endsWith("_WOOD")
                    || n.equals("MANGROVE_ROOTS")) LOGS.add(m);
            if (n.endsWith("_ORE") || n.equals("ANCIENT_DEBRIS")) {
                ORES.add(m);
                // 分档（按价值）
                if (n.startsWith("DIAMOND") || n.startsWith("EMERALD") || n.equals("ANCIENT_DEBRIS")) HIGH_ORE.add(m);
                else if (n.startsWith("IRON") || n.startsWith("GOLD") || n.startsWith("COPPER")
                        || n.startsWith("NETHER") || n.startsWith("LAPIS") || n.startsWith("REDSTONE")
                        || n.startsWith("QUARTZ")) MID_ORE.add(m);
                else LOW_ORE.add(m);   // 煤等
            }
        }
        CROPS.add(Material.WHEAT); CROPS.add(Material.CARROTS); CROPS.add(Material.POTATOES);
        CROPS.add(Material.BEETROOTS); CROPS.add(Material.PUMPKIN); CROPS.add(Material.MELON);
        HOSTILE.add(EntityType.ZOMBIE); HOSTILE.add(EntityType.SKELETON); HOSTILE.add(EntityType.CREEPER);
        HOSTILE.add(EntityType.SPIDER); HOSTILE.add(EntityType.CAVE_SPIDER); HOSTILE.add(EntityType.ENDERMAN);
        HOSTILE.add(EntityType.WITCH); HOSTILE.add(EntityType.SLIME); HOSTILE.add(EntityType.MAGMA_CUBE);
        HOSTILE.add(EntityType.HUSK); HOSTILE.add(EntityType.STRAY); HOSTILE.add(EntityType.DROWNED);
        HOSTILE.add(EntityType.PHANTOM); HOSTILE.add(EntityType.VINDICATOR); HOSTILE.add(EntityType.RAVAGER);
    }

    public SkillsListener(SkillsPlugin plugin) {
        this.plugin = plugin;
        this.effects = new nl.tinyaii.skills.effect.AbilityEffects(plugin);
    }

    /** 经验值显示：>=1 取整，<1 保留一位小数（避免挖石头"加0"的误导） */
    private String fmtExp(double v) {
        if (v >= 1 || v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }

    // ---------- 经验授予 ----------

    private void addXpWithAbilities(Player p, Skill skill, double base) {
        UUID u = p.getUniqueId();
        PlayerDataManager pm = plugin.getPlayerDataManager();
        SkillsManager sm = plugin.getSkillsManager();
        double amount = base;
        // 各线"经验加成"能力（第2能力位）：miner_exp/fighter_exp/forager_exp/farmer_exp/fisher_exp/brewer_exp/archer_exp/defender_exp
        int level = pm.getLevel(u, skill);
        AbilityDef xpAb = sm.get(skill).abilities[1];
        double bonus = xpAb.valueAt(level);
        amount *= (1 + bonus / 100.0);
        boolean leveled = pm.addXp(u, skill, amount);
        // ActionBar 实时经验反馈（屏幕中上方）：技能名 +N 经验 [进度条]
        int newLv = pm.getLevel(u, skill);
        double xpNow = pm.getXp(u, skill);
        double needNow = pm.xpRequired(newLv);
        int total = 10;
        int filled = (int) (xpNow / needNow * total);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) bar.append(i < filled ? "&a=" : "&8-");
        nl.tinyaii.skills.util.ActionBar.send(p,
                org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&f" + skill.getDisplayName() + " &7+&a" + fmtExp(amount) + " &7经验 &8[" + bar + "&8] &f"
                                + String.format("%.1f", 100.0 * xpNow / needNow) + "%"));
        if (leveled) {
            int newLevel = pm.getLevel(u, skill);
            onLevelUp(p, skill, newLevel);
        }
    }

    /** 升级反馈（三档）：普通升级 / 关键里程碑(能力解锁) / 满级 */
    private void onLevelUp(Player p, Skill skill, int newLevel) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        SkillsManager sm = plugin.getSkillsManager();
        int maxLevel = sm.getMaxLevel();

        // 新解锁的能力（本次升级是否有里程碑）
        java.util.List<AbilityDef> newlyUnlocked = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AbilityDef def = sm.get(skill).abilities[i];
            if (def.tierAt(newLevel) > 0 && def.tierAt(newLevel - 1) == 0) {
                newlyUnlocked.add(def);
            }
        }

        // 满级（最高级反馈）
        if (newLevel >= maxLevel && cfg.getBoolean("feedback.max-level-broadcast", true)) {
            String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&6[境界] &e" + p.getName() + " &a达成 &e" + skill.getDisplayName() + " &a满级！(&e" + maxLevel + "级&a)");
            p.sendTitle(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e** " + skill.getDisplayName() + " 满级 **"),
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b登峰造极，无人能及"), 10, 70, 20);
            for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) online.sendMessage(msg);
            if (cfg.getBoolean("feedback.milestone-sound", true)) {
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            return;
        }

        // 关键里程碑（解锁了新能力 → Title + 粒子）
        if (!newlyUnlocked.isEmpty()) {
            if (cfg.getBoolean("feedback.milestone-title", true)) {
                AbilityDef first = newlyUnlocked.get(0);
                p.sendTitle(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&d** " + first.getName() + " **"),
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7" + skill.getDisplayName() + " 新能力解锁！"), 10, 60, 20);
            }
            plugin.getMessages().send(p, "levelup", "{skill}", skill.getDisplayName(), "{level}", String.valueOf(newLevel));
            for (AbilityDef def : newlyUnlocked) {
                plugin.getMessages().send(p, "ability-unlocked", "{ability}", def.getName());
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&7  「&f" + def.getDesc() + "&7」"));
            }
            if (cfg.getBoolean("feedback.milestone-sound", true)) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
            if (cfg.getBoolean("feedback.particle", true)) {
                p.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.2);
            }
            return;
        }

        // 普通升级（每级都有）
        if (cfg.getBoolean("feedback.normal-levelup", true)) {
            plugin.getMessages().send(p, "levelup", "{skill}", skill.getDisplayName(), "{level}", String.valueOf(newLevel));
            // 每级特性汇总：下一能力预告
            AbilityDef next = nextAbility(skill, newLevel);
            if (next != null) {
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&7下一能力: &f" + next.getName() + " &8(等级 " + next.getUnlock() + ")"));
            }
            if (cfg.getBoolean("feedback.levelup-sound", true)) {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f);
            }
        }
    }

    /** 找下一个未解锁的能力 */
    private AbilityDef nextAbility(Skill skill, int currentLevel) {
        for (int i = 0; i < 5; i++) {
            AbilityDef def = plugin.getSkillsManager().get(skill).abilities[i];
            if (def.tierAt(currentLevel) == 0) return def;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Material m = b.getType();
        var xp = plugin.getExpConfig();
        if (HIGH_ORE.contains(m)) {
            addXpWithAbilities(p, Skill.MINING, xp.getDouble("xp.mining.high-ore", 20));
        } else if (MID_ORE.contains(m)) {
            addXpWithAbilities(p, Skill.MINING, xp.getDouble("xp.mining.mid-ore", 10));
        } else if (LOW_ORE.contains(m)) {
            addXpWithAbilities(p, Skill.MINING, xp.getDouble("xp.mining.low-ore", 6));
        } else if (m == Material.STONE || m.name().equals("DEEPSLATE") || m == Material.COBBLESTONE
                || m.name().endsWith("STONE")) {
            addXpWithAbilities(p, Skill.MINING, xp.getDouble("xp.mining.stone", 2));
        }
        if (LOGS.contains(m)) {
            addXpWithAbilities(p, Skill.FORAGING, xp.getDouble("xp.foraging.log", 10));
            // 伐木线效果
            effects.onLumberjack(e);
            effects.onShredder(e);
        } else if (m.name().endsWith("_LEAVES")) {
            addXpWithAbilities(p, Skill.FORAGING, xp.getDouble("xp.foraging.leaf", 1));
        }
        if (CROPS.contains(m) && isMature(b)) {
            addXpWithAbilities(p, Skill.FARMING, xp.getDouble("xp.farming.crop", 8));
            // 农艺线效果
            effects.onBountifulHarvest(e);
            effects.onGeneticist(e);
        }
        // 矿工线效果 + 伐木斧精通
        effects.onLuckyMiner(e);
        effects.onPickMaster(e);
        effects.onStamina(e);
        effects.onAxeMaster(e);
    }

    /** 作物是否成熟（防"种→挖→种"刷经验） */
    private boolean isMature(Block b) {
        // 南瓜/西瓜用成体方块；小麦/胡萝卜等用 Ageable 满龄
        if (b.getType() == Material.PUMPKIN || b.getType() == Material.MELON) return true;
        org.bukkit.block.data.BlockData d = b.getBlockData();
        if (d instanceof org.bukkit.block.data.Ageable) {
            org.bukkit.block.data.Ageable age = (org.bukkit.block.data.Ageable) d;
            return age.getAge() >= age.getMaximumAge();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        boolean hostile = HOSTILE.contains(e.getEntityType());
        if (!hostile) return;
        // 近战武器 → FIGHTING；弓/弩 → ARCHERY
        ItemStack hand = killer.getInventory().getItemInMainHand();
        String n = hand.getType().name();
        if (n.contains("BOW") || n.contains("CROSSBOW")) {
            addXpWithAbilities(killer, Skill.ARCHERY, plugin.getExpConfig().getDouble("xp.archery.kill", 12));
        } else {
            addXpWithAbilities(killer, Skill.FIGHTING, plugin.getExpConfig().getDouble("xp.fighting.kill", 10));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        // 攻击者是玩家 = PVP：按 config 决定是否给经验（默认不给，防互刷）
        if (e.getDamager() instanceof Player) {
            if (!plugin.getConfig().getBoolean("settings.defense-pvp-xp", false)) return;
        }
        // 只吃 自然怪/玩家(按配置) 的直接攻击，避免溺水/火烧/跌落刷经验
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                && e.getCause() != EntityDamageEvent.DamageCause.PROJECTILE
                && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (e.getDamage() <= 0) return;
        addXpWithAbilities(p, Skill.DEFENSE, plugin.getExpConfig().getDouble("xp.defense.hit", 5));
        // 矿工线硬化护甲：穿戴盔甲受伤减免
        effects.onHardenedArmor(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        // 渔夫线效果：抛竿距离
        effects.onCasterMaster(e);
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        addXpWithAbilities(e.getPlayer(), Skill.FISHING, plugin.getExpConfig().getDouble("xp.fishing.catch", 12));
        // 幸运捕获
        effects.onLuckyCatch(e);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamageOutgoing(EntityDamageByEntityEvent e) {
        // 攻击方效果：剑精通 / 先手 / 流血（战斗线）、勇猛（伐木线）、弓精通 / 眩晕（弓术线）
        if (e.getDamager() instanceof Player) {
            effects.onSwordMaster(e);
            effects.onFirstStrike(e);
            effects.onBleed(e);
            effects.onValor(e);
        }
        // 弓术：弹射物命中
        if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
            effects.onBowMaster(e);
            effects.onStun(e);
        }
        // 受伤方效果：格挡（战斗线）、护盾 / 怪物大师（防御线）
        if (e.getEntity() instanceof Player) {
            effects.onParry(e);
            effects.onShielding(e);
            effects.onMobMaster(e);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent e) {
        effects.onImmunity(e);
        effects.onNoDebuff(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewTake(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() == null) return;
        // 只处理"从酿造台取走"：点击的是酿造台顶栏（结果槽3），且是拿取动作
        if (e.getInventory().getType() != org.bukkit.event.inventory.InventoryType.BREWING) return;
        if (e.getRawSlot() != 3) return;
        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;
        addXpWithAbilities((Player) e.getWhoClicked(), Skill.BREWING, plugin.getExpConfig().getDouble("xp.brewing.take", 8));
        // 炼金师：概率返还材料
        effects.onAlchemist(e);
    }
}