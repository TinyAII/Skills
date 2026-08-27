package nl.tinyaii.skills.effect;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.skill.AbilityDef;
import nl.tinyaii.skills.skill.Skill;
import nl.tinyaii.skills.skill.SkillsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 能力效果实现：各技能线主动能力在事件中的实际效果。
 * 按线分组方法，监听器对应调用；数值全部基于该玩家当前技能等级计算。
 *
 * ⛏ 矿工线（本版）：
 *   lucky_miner 幸运矿工 → 双倍掉落几率
 *   pick_master 镐精通   → 挖掘速度提升（短暂 DigSpeed）
 *   stamina     矿洞耐力 → 工具耐久消耗降低（概率免扣）
 *   hardened_armor 硬化护甲 → 穿戴护甲受伤减免
 */
public class AbilityEffects {

    private final SkillsPlugin plugin;

    public AbilityEffects(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- 工具 ----------

    private AbilityDef ability(Skill skill, int idx) {
        return plugin.getSkillsManager().get(skill).abilities[idx];
    }

    private int level(UUID uuid, Skill skill) {
        return plugin.getPlayerDataManager().getLevel(uuid, skill);
    }

    private double value(UUID uuid, Skill skill, int idx) {
        return ability(skill, idx).valueAt(level(uuid, skill));
    }

    private boolean chance(double percent) {
        return percent > 0 && ThreadLocalRandom.current().nextDouble(0, 100) < percent;
    }

    // ---------- ⛏ 矿工线 ----------

    /** 幸运矿工：双倍掉落（挖矿破坏后追加一份掉落物） */
    public void onLuckyMiner(BlockBreakEvent e) {
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.MINING, 0);
        if (pct <= 0) return;
        if (!chance(pct)) return;
        // 掉一份原本的掉落物（复制已存在的掉落物）
        for (ItemStack drop : e.getBlock().getDrops(p.getInventory().getItemInMainHand())) {
            ItemStack extra = drop.clone();
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.3, 0.5), extra);
        }
    }

    /** 镐精通：挖掘速度提升（短暂 DigSpeed 效果，持续 2.5 秒） */
    public void onPickMaster(BlockBreakEvent e) {
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.MINING, 2);
        if (pct <= 0) return;
        // pct 是"挖掘速度提升%"→ DigSpeed 1 级约 +40%；按比例映射
        int level = pct >= 40 ? 2 : (pct >= 20 ? 1 : 0);
        if (level <= 0) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 50, level - 1, false, false));
    }

    /** 矿洞耐力：工具耐久消耗降低（概率免扣耐久） */
    public void onStamina(BlockBreakEvent e) {
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.MINING, 3);
        if (pct <= 0) return;
        if (!chance(pct)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;
        if (!(tool.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable)) return;
        org.bukkit.inventory.meta.Damageable dmg = (org.bukkit.inventory.meta.Damageable) tool.getItemMeta();
        if (dmg.getDamage() <= 0) return;   // 无损耗就不管
        dmg.setDamage(dmg.getDamage() - 1); // 回溯 1 点耐久
        tool.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
    }

    /** 硬化护甲：穿戴护甲受伤减免 */
    public void onHardenedArmor(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.MINING, 4);
        if (pct <= 0) return;
        if (p.getInventory().getArmorContents() == null) return;
        boolean wearingArmor = false;
        for (ItemStack it : p.getInventory().getArmorContents()) {
            if (it != null && !it.getType().isAir()) { wearingArmor = true; break; }
        }
        if (!wearingArmor) return;
        double reduction = Math.min(0.5, pct / 100.0);   // 上限50%
        e.setDamage(e.getDamage() * (1 - reduction));
    }

    // ---------- ⚔ 战斗线 ----------

    /** 格挡：被近战攻击概率减伤（上限20档） */
    public void onParry(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getDamager() instanceof Player) return;   // PVP 不触发格挡（防刷）
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.FIGHTING, 0);
        if (pct <= 0 || !chance(pct)) return;
        double reduction = Math.min(0.6, pct / 100.0);
        e.setDamage(e.getDamage() * (1 - reduction));
    }

    /** 剑精通：近战伤害提升（主手近战武器） */
    public void onSwordMaster(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        Player p = (Player) e.getDamager();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String n = hand.getType().name();
        if (!n.contains("SWORD") && !n.contains("AXE")) return;
        double pct = value(p.getUniqueId(), Skill.FIGHTING, 2);
        if (pct <= 0) return;
        double boost = 1 + Math.min(0.6, pct / 100.0);
        e.setDamage(e.getDamage() * boost);
    }

    /** 先手：对满血怪物首击伤害加成 */
    public void onFirstStrike(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) e.getEntity();
        if (target.getHealth() < target.getMaxHealth()) return;   // 只对满血
        Player p = (Player) e.getDamager();
        double pct = value(p.getUniqueId(), Skill.FIGHTING, 3);
        if (pct <= 0) return;
        double boost = 1 + Math.min(1.0, pct / 100.0);
        e.setDamage(e.getDamage() * boost);
    }

    /** 流血：击中概率让目标流血（持续伤害，6 秒） */
    public void onBleed(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        Player p = (Player) e.getDamager();
        double pct = value(p.getUniqueId(), Skill.FIGHTING, 4);
        if (pct <= 0 || !chance(pct)) return;
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) e.getEntity();
        if (target.hasPotionEffect(org.bukkit.potion.PotionEffectType.WITHER)) return;   // 防重复
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.WITHER, 120, 0, false, false, true));   // 6 秒 I 级
    }

    // ---------- 🪵 伐木线 ----------

    /** 伐木工：砍原木概率额外掉一个原木 */
    public void onLumberjack(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        if (!m.name().endsWith("_LOG") && !m.name().endsWith("_WOOD")) return;
        double pct = value(p.getUniqueId(), Skill.FORAGING, 0);
        if (pct <= 0 || !chance(pct)) return;
        ItemStack log = new ItemStack(m, 1);
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.3, 0.5), log);
    }

    /** 斧精通：砍树短暂手持斧头加速 */
    public void onAxeMaster(BlockBreakEvent e) {
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.FORAGING, 2);
        if (pct <= 0) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!hand.getType().name().contains("AXE")) return;
        int level = pct >= 40 ? 2 : (pct >= 20 ? 1 : 0);
        if (level <= 0) return;
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FAST_DIGGING, 60, level - 1, false, false));
    }

    /** 勇猛：手持斧头近战伤害小增 */
    public void onValor(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!hand.getType().name().contains("AXE")) return;
        double pct = value(p.getUniqueId(), Skill.FORAGING, 3);
        if (pct <= 0) return;
        e.setDamage(e.getDamage() * (1 + Math.min(0.3, pct / 100.0)));
    }

    /** 撕碎：二段额外原木（追加一份） */
    public void onShredder(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        if (!m.name().endsWith("_LOG") && !m.name().endsWith("_WOOD")) return;
        double pct = value(p.getUniqueId(), Skill.FORAGING, 4);
        if (pct <= 0 || !chance(pct)) return;
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.3, 0.5), new ItemStack(m, 1));
    }

    // ---------- 🌾 农艺线 ----------

    /** 丰收：收割成熟作物概率额外掉落一份 */
    public void onBountifulHarvest(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        double pct = value(p.getUniqueId(), Skill.FARMING, 0);
        if (pct <= 0 || !chance(pct)) return;
        // 按作物掉落对应产物
        ItemStack extra = cropProduct(m);
        if (extra != null) {
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.3, 0.5), extra);
        }
    }

    /** 基因学家：收割后种子返还率提升 */
    public void onGeneticist(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        double pct = value(p.getUniqueId(), Skill.FARMING, 3);
        if (pct <= 0 || !chance(pct)) return;
        ItemStack seed = seedOf(m);
        if (seed != null) {
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, -0.3, 0.5), seed);
        }
    }

    private ItemStack cropProduct(Material m) {
        switch (m.name()) {
            case "WHEAT": return new ItemStack(Material.WHEAT, 1);
            case "CARROTS": return new ItemStack(Material.CARROT, 1);
            case "POTATOES": return new ItemStack(Material.POTATO, 1);
            case "BEETROOTS": return new ItemStack(Material.BEETROOT, 1);
            case "PUMPKIN": return new ItemStack(Material.PUMPKIN, 1);
            case "MELON": return new ItemStack(Material.MELON_SLICE, 2);
            default: return null;
        }
    }

    private ItemStack seedOf(Material m) {
        switch (m.name()) {
            case "WHEAT": return new ItemStack(Material.WHEAT_SEEDS, 1);
            case "CARROTS": return new ItemStack(Material.CARROT, 1);
            case "POTATOES": return new ItemStack(Material.POTATO, 1);
            case "BEETROOTS": return new ItemStack(Material.BEETROOT_SEEDS, 1);
            default: return null;
        }
    }

    // ---------- 🎣 渔夫线 ----------

    /** 幸运捕获：钓鱼概率额外鱼获（收竿时掉落附加） */
    public void onLuckyCatch(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.FISHING, 0);
        if (pct <= 0 || !chance(pct)) return;
        ItemStack extra = new ItemStack(Material.COD, 1);   // 默认加一条鳕鱼
        p.getWorld().dropItemNaturally(p.getLocation(), extra);
    }

    /** 甩杆大师：抛竿距离提升 */
    public void onCasterMaster(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.FISHING) return;
        Player p = e.getPlayer();
        double pct = value(p.getUniqueId(), Skill.FISHING, 3);
        if (pct <= 0 || e.getHook() == null) return;
        double boost = 1 + Math.min(0.5, pct / 100.0);
        org.bukkit.util.Vector v = e.getHook().getLocation().getDirection().normalize().multiply(
                e.getHook().getVelocity().length() * boost);
        e.getHook().setVelocity(v);
    }

    // ---------- 🧪 炼药线 ----------

    /** 炼金师：酿造取药概率返还材料（额外给一个下界疣） */
    public void onAlchemist(org.bukkit.event.inventory.InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        double pct = value(p.getUniqueId(), Skill.BREWING, 0);
        if (pct <= 0 || !chance(pct)) return;
        p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(Material.NETHER_WART, 1));
    }

    // ---------- 🏹 弓术线 ----------

    /** 弓精通：弓箭伤害提升 */
    public void onBowMaster(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof org.bukkit.entity.Projectile)) return;
        org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) e.getDamager();
        if (!(proj.getShooter() instanceof Player)) return;
        Player p = (Player) proj.getShooter();
        double pct = value(p.getUniqueId(), Skill.ARCHERY, 2);
        if (pct <= 0) return;
        e.setDamage(e.getDamage() * (1 + Math.min(0.6, pct / 100.0)));
    }

    /** 眩晕：弓箭命中概率击晕目标 */
    public void onStun(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof org.bukkit.entity.Projectile)) return;
        org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) e.getDamager();
        if (!(proj.getShooter() instanceof Player)) return;
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;
        Player p = (Player) proj.getShooter();
        double pct = value(p.getUniqueId(), Skill.ARCHERY, 4);
        if (pct <= 0 || !chance(pct)) return;
        org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) e.getEntity();
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOW, 40, 3, false, false, true));   // 2 秒强减速
    }

    // ---------- 🛡 防御线 ----------

    /** 护盾：受击概率被动减伤（上限20档） */
    public void onShielding(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getDamager() instanceof Player) return;
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.DEFENSE, 0);
        if (pct <= 0 || !chance(pct)) return;
        double reduction = Math.min(0.5, pct / 100.0);
        e.setDamage(e.getDamage() * (1 - reduction));
    }

    /** 怪物大师：对怪物伤害减免（怪物打的直接减） */
    public void onMobMaster(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getDamager() instanceof Player) return;
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.DEFENSE, 2);
        if (pct <= 0) return;
        double reduction = Math.min(0.4, pct / 100.0);
        e.setDamage(e.getDamage() * (1 - reduction));
    }

    /** 免疫：受负面效果概率免疫 */
    public void onImmunity(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getAction() != org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED) return;
        org.bukkit.potion.PotionEffectType type = e.getNewEffect() == null ? null : e.getNewEffect().getType();
        if (type == null) return;
        // 只免疫负面效果
        if (!isNegative(type)) return;
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.DEFENSE, 3);
        if (pct <= 0 || !chance(pct)) return;
        e.setCancelled(true);
    }

    /** 无Debuff：负面效果时长缩短 */
    public void onNoDebuff(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (e.getAction() != org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED) return;
        org.bukkit.potion.PotionEffect effect = e.getNewEffect();
        if (effect == null || !isNegative(effect.getType())) return;
        Player p = (Player) e.getEntity();
        double pct = value(p.getUniqueId(), Skill.DEFENSE, 4);
        if (pct <= 0) return;
        int reduced = (int) (effect.getDuration() * (1 - Math.min(0.5, pct / 100.0)));
        e.setCancelled(true);
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(effect.getType(), reduced,
                effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
    }

    /** 负面效果判定 */
    private boolean isNegative(org.bukkit.potion.PotionEffectType type) {
        switch (type.getName()) {
            case "POISON": case "WITHER": case "SLOWNESS": case "SLOW": case "SLOW_DIGGING": case "MINING_FATIGUE":
            case "WEAKNESS": case "BLINDNESS": case "HUNGER": case "CONFUSION":
            case "UNLUCK": case "DARKNESS": case "LEVITATION": case "GLOWING":
            case "BAD_OMEN":
                return true;
            default:
                return false;
        }
    }
}