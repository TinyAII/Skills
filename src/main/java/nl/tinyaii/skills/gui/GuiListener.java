package nl.tinyaii.skills.gui;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.skill.Skill;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 技能 GUI 点击：主面板点技能 → 详情页；详情页点返回 → 主面板。
 */
public class GuiListener implements Listener {

    private final SkillsPlugin plugin;
    private final java.util.Map<java.util.UUID, Long> lastClick = new java.util.HashMap<>();

    public GuiListener(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof org.bukkit.inventory.InventoryHolder)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        org.bukkit.inventory.InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof SkillsHolder) && !(holder instanceof AbilityDetailGui.DetailHolder)) return;

        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < 300) return;
        lastClick.put(p.getUniqueId(), now);

        // 主面板 → 详情页
        if (holder instanceof SkillsHolder) {
            int slot = e.getRawSlot();
            if (slot == 49) { p.closeInventory(); return; }
            Skill skill = skillAtSlot(slot);
            if (skill != null) new AbilityDetailGui(plugin, p, skill).open();
            return;
        }

        // 详情页 → 返回主面板
        if (holder instanceof AbilityDetailGui.DetailHolder) {
            if (e.getRawSlot() == 22) new SkillsGui(plugin, p).open();
        }
    }

    private Skill skillAtSlot(int slot) {
        // 与 SkillsGui.open() 的槽位映射一致：10,11,12,13,14,15,16,17 → 8 条线
        switch (slot) {
            case 10: return Skill.MINING;
            case 11: return Skill.FIGHTING;
            case 12: return Skill.FORAGING;
            case 13: return Skill.FARMING;
            case 14: return Skill.FISHING;
            case 15: return Skill.BREWING;
            case 16: return Skill.ARCHERY;
            case 17: return Skill.DEFENSE;
            default: return null;
        }
    }
}