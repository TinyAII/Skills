package nl.tinyaii.skills.command;

import nl.tinyaii.skills.SkillsPlugin;
import nl.tinyaii.skills.gui.SkillsGui;
import nl.tinyaii.skills.skill.Skill;
import nl.tinyaii.skills.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SkillsCommand implements CommandExecutor, TabCompleter {
    private final SkillsPlugin plugin;

    public SkillsCommand(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Messages msg = plugin.getMessages();

        // /技能 → 打开面板
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("控制台请用: /技能 信息 <玩家>"); return true; }
            if (!sender.hasPermission("skills.use")) { msg.send((Player) sender, "no-permission"); return true; }
            new SkillsGui(plugin, (Player) sender).open();
            return true;
        }

        switch (args[0]) {
            case "信息": {
                if (!checkUse(sender)) return true;
                UUID uuid;
                String name;
                if (args.length >= 2 && sender.hasPermission("skills.admin")) {
                    org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
                    uuid = t.getUniqueId(); name = args[1];
                } else if (sender instanceof Player) {
                    uuid = ((Player) sender).getUniqueId(); name = ((Player) sender).getName();
                } else { sender.sendMessage("控制台请用: /技能 信息 <玩家>"); return true; }

                sender.sendMessage(Messages.color("&6==== " + name + " 的技能 ===="));
                for (Skill skill : Skill.values()) {
                    int lv = plugin.getPlayerDataManager().getLevel(uuid, skill);
                    double xp = plugin.getPlayerDataManager().getXp(uuid, skill);
                    double need = plugin.getPlayerDataManager().xpRequired(lv);
                    sender.sendMessage(Messages.color("  " + skill.getDisplayName() + " &7Lv.&f" + lv
                            + " &8" + (int)xp + "/" + (int)need));
                }
                return true;
            }
            case "重置": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /技能 重置 <玩家>（清空该玩家全部技能线）")); return true; }
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
                for (Skill skill : Skill.values()) plugin.getPlayerDataManager().setLevel(t.getUniqueId(), skill, 0);
                if (sender instanceof Player) msg.send((Player) sender, "reloaded");  // 复用提示，稍后加专用
                else sender.sendMessage(msg.raw("reloaded"));
                return true;
            }
            case "设置": {
                if (!checkAdmin(sender)) return true;
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /技能 设置 <玩家> <技能线> <等级>（测试用，如 /技能 设置 自己 矿工 50）")); return true; }
                return doSet(sender, args[1], args[2], args[3]);
            }
            case "重载": {
                if (!checkAdmin(sender)) return true;
                plugin.reloadAll();
                if (sender instanceof Player) msg.send((Player) sender, "reloaded");
                else sender.sendMessage(msg.raw("reloaded"));
                return true;
            }
            case "管理": {
                // 神权命令：普通用户用不了，全需 skills.admin
                if (!checkAdmin(sender)) return true;
                return doAdmin(sender, args);
            }
            default:
                sendHelp(sender);
                return true;
        }
    }

    /** 神权命令全套（仅管理员） */
    private boolean doAdmin(CommandSender s, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(s);
            return true;
        }
        String sub = args[1];
        switch (sub) {
            case "查看": {
                if (args.length < 3) { s.sendMessage(Messages.color("&c用法: /技能 管理 查看 <玩家>")); return true; }
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
                s.sendMessage(Messages.color("&6==== " + args[2] + " 的技能 ===="));
                for (Skill skill : Skill.values()) {
                    int lv = plugin.getPlayerDataManager().getLevel(t.getUniqueId(), skill);
                    double xp = plugin.getPlayerDataManager().getXp(t.getUniqueId(), skill);
                    double need = plugin.getPlayerDataManager().xpRequired(lv);
                    s.sendMessage(Messages.color("  " + skill.getDisplayName() + " &7Lv.&f" + lv
                            + " &8" + (int)xp + "/" + (int)need));
                }
                return true;
            }
            case "设置": {
                if (args.length < 5) { s.sendMessage(Messages.color("&c用法: /技能 管理 设置 <玩家> <线> <等级>")); return true; }
                return doSet(s, args[2], args[3], args[4]);
            }
            case "加经验": {
                if (args.length < 5) { s.sendMessage(Messages.color("&c用法: /技能 管理 加经验 <玩家> <线> <经验>(负数为扣)")); return true; }
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
                Skill skill = skillFor(param(args[3]));
                if (skill == null) { s.sendMessage(Messages.color("&c技能线无效。")); return true; }
                double amount;
                try { amount = Double.parseDouble(args[4]); } catch (Exception ex) { s.sendMessage(Messages.color("&c经验值无效。")); return true; }
                plugin.getPlayerDataManager().addXp(t.getUniqueId(), skill, amount);
                s.sendMessage(Messages.color("&a已给 &e" + args[2] + " &a的 &e" + skill.getDisplayName()
                        + " &a" + (amount >= 0 ? "增加" : "扣除") + " &e" + Math.abs(amount) + " &a经验 → Lv.&f"
                        + plugin.getPlayerDataManager().getLevel(t.getUniqueId(), skill)));
                return true;
            }
            case "封线": {
                if (args.length < 4) { s.sendMessage(Messages.color("&c用法: /技能 管理 封线 <玩家> <线>")); return true; }
                Skill skill = skillFor(param(args[3]));
                if (skill == null) { s.sendMessage(Messages.color("&c技能线无效。")); return true; }
                plugin.getPlayerDataManager().banLine(org.bukkit.Bukkit.getOfflinePlayer(args[2]).getUniqueId(), skill, true);
                s.sendMessage(Messages.color("&c已禁用 &e" + args[2] + " &c的 &e" + skill.getDisplayName() + " &c线（不再获得经验）。"));
                return true;
            }
            case "解锁": {
                if (args.length < 4) { s.sendMessage(Messages.color("&c用法: /技能 管理 解锁 <玩家> <线>")); return true; }
                Skill skill = skillFor(param(args[3]));
                if (skill == null) { s.sendMessage(Messages.color("&c技能线无效。")); return true; }
                plugin.getPlayerDataManager().banLine(org.bukkit.Bukkit.getOfflinePlayer(args[2]).getUniqueId(), skill, false);
                s.sendMessage(Messages.color("&a已解除 &e" + args[2] + " &a的 &e" + skill.getDisplayName() + " &a线禁用。"));
                return true;
            }
            case "清空": {
                if (args.length < 3) { s.sendMessage(Messages.color("&c用法: /技能 管理 清空 <玩家>")); return true; }
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
                for (Skill skill : Skill.values()) plugin.getPlayerDataManager().setLevel(t.getUniqueId(), skill, 0);
                s.sendMessage(Messages.color("&c已清空 &e" + args[2] + " &c全部技能。"));
                return true;
            }
            case "全服设置": {
                if (args.length < 4) { s.sendMessage(Messages.color("&c用法: /技能 管理 全服设置 <线> <等级>")); return true; }
                Skill skill = skillFor(param(args[2]));
                if (skill == null) { s.sendMessage(Messages.color("&c技能线无效。")); return true; }
                int lv;
                try { lv = Integer.parseInt(args[3]); } catch (Exception ex) { lv = -1; }
                if (lv < 0) { s.sendMessage(Messages.color("&c等级无效。")); return true; }
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    plugin.getPlayerDataManager().setLevel(p.getUniqueId(), skill, lv);
                }
                s.sendMessage(Messages.color("&a已将全服在线玩家 &e" + skill.getDisplayName() + " &a设为 &eLv." + lv));
                return true;
            }
            case "全服加": {
                if (args.length < 4) { s.sendMessage(Messages.color("&c用法: /技能 管理 全服加 <线> <经验>")); return true; }
                Skill skill = skillFor(param(args[2]));
                if (skill == null) { s.sendMessage(Messages.color("&c技能线无效。")); return true; }
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (Exception ex) { s.sendMessage(Messages.color("&c经验值无效。")); return true; }
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    plugin.getPlayerDataManager().addXp(p.getUniqueId(), skill, amount);
                }
                s.sendMessage(Messages.color("&a已给全服在线玩家 &e" + skill.getDisplayName() + " &a增加 &e" + amount + " &a经验。"));
                return true;
            }
            case "重置全服": {
                if (args.length >= 3 && args[2].equals("确认")) {
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        for (Skill skill : Skill.values()) plugin.getPlayerDataManager().setLevel(p.getUniqueId(), skill, 0);
                    }
                    s.sendMessage(Messages.color("&c已重置全服技能（新赛季开始！）"));
                } else {
                    s.sendMessage(Messages.color("&c危险操作！确认请输 &e/技能 管理 重置全服 确认"));
                }
                return true;
            }
            default:
                sendAdminHelp(s);
                return true;
        }
    }

    private void sendAdminHelp(CommandSender s) {
        String[] lines = {
                "&6===== 技能神权管理 =====",
                "&e/技能 管理 查看 <玩家> &7- 查看他人技能",
                "&e/技能 管理 设置 <玩家> <线> <等级>",
                "&e/技能 管理 加经验 <玩家> <线> <经验>(负=扣)",
                "&e/技能 管理 封线 <玩家> <线> &7- 禁用该线",
                "&e/技能 管理 解锁 <玩家> <线>",
                "&e/技能 管理 清空 <玩家>",
                "&e/技能 管理 全服设置 <线> <等级>",
                "&e/技能 管理 全服加 <线> <经验>",
                "&e/技能 管理 重置全服 确认 &7- 新赛季",
                "&c注意：以上仅管理员可用，普通玩家无效"
        };
        for (String l : lines) s.sendMessage(Messages.color(l));
    }

    private void sendHelp(CommandSender s) {
        String[] lines = {
                "&6===== Skills 技能系统 =====",
                "&e/技能 &7- 打开技能面板",
                "&e/技能 信息 [玩家] &7- 查看技能进度",
                "&c--- 管理 ---",
                "&e/技能 设置 <玩家> <矿工|战斗|伐木|农艺|渔夫|炼药|弓术|防御> <等级> &7- 测试用直接设等级",
                "&e/技能 重置 <玩家>",
                "&e/技能 重载"
        };
        for (String l : lines) s.sendMessage(Messages.color(l));
    }

    private boolean checkUse(CommandSender s) {
        if (s.hasPermission("skills.use")) return true;
        if (s instanceof Player) plugin.getMessages().send((Player) s, "no-permission");
        else s.sendMessage(plugin.getMessages().raw("no-permission"));
        return false;
    }

    private boolean checkAdmin(CommandSender s) {
        if (s.hasPermission("skills.admin")) return true;
        if (s instanceof Player) plugin.getMessages().send((Player) s, "no-permission");
        else s.sendMessage(plugin.getMessages().raw("no-permission"));
        return false;
    }

    /** 设置等级（神权+测试共用） */
    private boolean doSet(CommandSender sender, String playerName, String skillName, String levelStr) {
        org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(playerName);
        Skill skill = skillFor(param(skillName));
        if (skill == null) {
            sender.sendMessage(Messages.color("&c技能线无效，可用: " + skillNames()));
            return true;
        }
        int lv;
        try { lv = Integer.parseInt(levelStr); } catch (Exception ex) { lv = -1; }
        int max = plugin.getSkillsManager().getMaxLevel();
        if (lv < 0 || lv > max) { sender.sendMessage(Messages.color("&c等级需在 0~" + max + " 之间。")); return true; }
        plugin.getPlayerDataManager().setLevel(t.getUniqueId(), skill, lv);
        sender.sendMessage(Messages.color("&a已将 &e" + playerName + " &a的 &e" + skill.getDisplayName()
                + " &a设置为 &eLv." + lv + " &7(满级" + max + ")"));
        StringBuilder unlocked = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            var def = plugin.getSkillsManager().get(skill).abilities[i];
            if (def.tierAt(lv) > 0) unlocked.append(def.getName()).append(" ");
        }
        if (unlocked.length() > 0) {
            sender.sendMessage(Messages.color("&7该等级已生效能力: &f" + unlocked.toString().trim()));
        }
        return true;
    }

    private Skill skillFor(String s) {
        if (s == null) return null;
        String v = s.toLowerCase();
        for (Skill skill : Skill.values()) {
            if (skill.key().equals(v) || skill.getDisplayName().replace(" ", "").equals(s.replace(" ", ""))
                    || skill.name().toLowerCase().equals(v)) return skill;
        }
        return null;
    }

    private String param(String s) { return s == null ? "" : s; }

    private String skillNames() {
        StringBuilder sb = new StringBuilder();
        for (Skill skill : Skill.values()) sb.append(skill.key()).append(" ");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("信息"));
            if (sender.hasPermission("skills.admin")) subs.addAll(Arrays.asList("设置", "重置", "重载", "管理"));
            for (String s : subs) if (s.startsWith(args[0])) out.add(s);
        } else if (args.length == 3 && args[0].equals("设置") && sender.hasPermission("skills.admin")) {
            for (String s : java.util.Arrays.asList("矿工", "战斗", "伐木", "农艺", "渔夫", "炼药", "弓术", "防御")) {
                if (s.startsWith(args[2])) out.add(s);
            }
        } else if (args.length == 2 && args[0].equals("重置")) {
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}