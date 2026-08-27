package nl.tinyaii.skills.gui;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.skill.AbilityDef;
import nl.tinyaii.skills.skill.PlayerDataManager;
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
 * 技能面板：8 条技能线网格 + 点击查看能力详情。
 * 每格显示：技能名 / 等级 / 经验进度条 / 已解锁能力数。
 */
public class SkillsGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "技能";
    private final SkillsPlugin plugin;
    private final Player player;

    public SkillsGui(SkillsPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(new SkillsHolder(), 54, TITLE);
        Skill[] skills = Skill.values();
        // 每个技能一个格子：2~9, 11~18, 20~27, 29~36（前4行）
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 17};
        for (int i = 0; i < skills.length; i++) {
            if (i >= slots.length) break;
            inv.setItem(slots[i], skillIcon(skills[i]));
        }

        // 底部说明
        inv.setItem(45, named(Material.BOOK, "&7点击技能查看能力详情", new ArrayList<>()));
        inv.setItem(49, named(Material.BARRIER, "&c关闭", new ArrayList<>()));
        player.openInventory(inv);
    }

    private ItemStack skillIcon(Skill skill) {
        int level = plugin.getPlayerDataManager().getLevel(player.getUniqueId(), skill);
        double xp = plugin.getPlayerDataManager().getXp(player.getUniqueId(), skill);
        double need = plugin.getPlayerDataManager().xpRequired(level);
        int unlocked = 0;
        SkillsManager.SkillConfig cfg = plugin.getSkillsManager().get(skill);
        for (AbilityDef def : cfg.abilities) if (def.tierAt(level) > 0) unlocked++;

        Material icon = iconFor(skill);
        ItemStack it = new ItemStack(icon);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(skill.getDisplayName() + "  &7(" + String.format("%.1f", 100.0 * level / plugin.getSkillsManager().getMaxLevel()) + "% 进度)"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7等级: &f" + level + " &7/ &f" + plugin.getSkillsManager().getMaxLevel()));
            int total = 10;
            int filled = level >= plugin.getSkillsManager().getMaxLevel() ? total : (int)(xp / need * total);
            // 注意：MC 默认字体缺 ●○■ 等 Unicode 字形（显示成口方块）→ 用 ASCII 安全的 = 和 - 做进度条；
            // 关键：整根字符串拼完**统一**过 color()，循环里的 &a/&8 也必须转义（否则字面量直接泄漏）
            StringBuilder raw = new StringBuilder("&7经验 &8[");
            for (int i = 0; i < total; i++) raw.append(i < filled ? "&a=" : "&8-");
            raw.append("&8] &f").append((int)xp).append("/").append((int)need);
            lore.add(Messages.color(raw.toString()));
            lore.add(Messages.color("&7已解锁能力: &e" + unlocked + " &7/ 5"));
            lore.add("");
            lore.add(Messages.color("&e点击查看能力"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    public static Material iconFor(Skill skill) {
        switch (skill) {
            case MINING: return Material.DIAMOND_PICKAXE;
            case FIGHTING: return Material.IRON_SWORD;
            case FORAGING: return Material.OAK_LOG;
            case FARMING: return Material.WHEAT;
            case FISHING: return Material.FISHING_ROD;
            case BREWING: return Material.BREWING_STAND;
            case ARCHERY: return Material.BOW;
            case DEFENSE: return Material.IRON_CHESTPLATE;
            default: return Material.BOOK;
        }
    }

    static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            List<String> out = new ArrayList<>();
            for (String l : lore) out.add(Messages.color(l));
            meta.setLore(out);
            it.setItemMeta(meta);
        }
        return it;
    }
}