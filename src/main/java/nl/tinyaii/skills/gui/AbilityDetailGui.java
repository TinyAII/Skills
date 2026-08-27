package nl.tinyaii.skills.gui;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.skill.AbilityDef;
import nl.tinyaii.skills.skill.Skill;
import nl.tinyaii.skills.skill.SkillsManager;
import nl.tinyaii.skills.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能能力详情页：展示该技能线的 5 个能力、解锁等级、当前生效档/数值。
 */
public class AbilityDetailGui {

    private final SkillsPlugin plugin;
    private final Player player;
    private final Skill skill;

    public AbilityDetailGui(SkillsPlugin plugin, Player player, Skill skill) {
        this.plugin = plugin;
        this.player = player;
        this.skill = skill;
    }

    public void open() {
        int level = plugin.getPlayerDataManager().getLevel(player.getUniqueId(), skill);
        Inventory inv = Bukkit.createInventory(new DetailHolder(skill), 27,
                ChatColor.DARK_GRAY + "技能 - " + skill.getDisplayName() + " &7(等级 " + level + ")");

        SkillsManager.SkillConfig cfg = plugin.getSkillsManager().get(skill);
        int[] slots = {11, 12, 13, 14, 15};
        for (int i = 0; i < 5; i++) {
            inv.setItem(slots[i], abilityIcon(cfg.abilities[i], level));
        }
        inv.setItem(22, SkillsGui.named(Material.ARROW, "&e返回技能面板", new ArrayList<>()));
        player.openInventory(inv);
    }

    private ItemStack abilityIcon(AbilityDef def, int level) {
        int tier = def.tierAt(level);
        boolean unlocked = tier > 0;
        ItemStack it = new ItemStack(unlocked ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color((unlocked ? "&a" : "&7") + def.getName()));
            List<String> lore = new ArrayList<>();
            // 效果描述（告诉玩家这能力是干嘛的）
            if (def.getDesc() != null && !def.getDesc().isEmpty()) {
                lore.add(Messages.color("&7「&f" + def.getDesc() + "&7」"));
                lore.add("");
            }
            // 解锁与档位
            if (unlocked) {
                double val = def.valueAt(level);
                String valTxt = trunc(val) + (def.getBase() <= 20 ? "%" : "");
                lore.add(Messages.color("&7解锁等级: &f" + def.getUnlock() + " &8(每 &f" + def.getEvery() + " &8级升一档)"));
                lore.add(Messages.color("&7当前档位: &e" + tier + " &8| 当前效果: &e" + valTxt));
            } else {
                lore.add(Messages.color("&7解锁等级: &f" + def.getUnlock() + " &8(每 &f" + def.getEvery() + " &8级升一档)"));
                lore.add(Messages.color("&7未解锁（等级 &e" + def.getUnlock() + " &7解锁）"));
            }
            int maxTier = def.maxTierAt(plugin.getSkillsManager().getMaxLevel());
            String maxVal = trunc(def.valueAt(plugin.getSkillsManager().getMaxLevel()))
                    + (def.getBase() <= 20 ? "%" : "");
            lore.add(Messages.color("&7满级可到: &f" + maxTier + " 档 &8(效果 &f" + maxVal + ")"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String trunc(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100.0) / 100.0);
    }

    /** 详情页 holder（携带技能线） */
    public static class DetailHolder implements org.bukkit.inventory.InventoryHolder {
        public final Skill skill;
        public DetailHolder(Skill skill) { this.skill = skill; }
        @Override public Inventory getInventory() { return null; }
    }
}