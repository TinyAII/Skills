package nl.tinyaii.skills;

import nl.tinyaii.skills.command.SkillsCommand;
import nl.tinyaii.skills.gui.SkillsGui;
import nl.tinyaii.skills.gui.GuiListener;
import nl.tinyaii.skills.listener.SkillsListener;
import nl.tinyaii.skills.skill.PlayerDataManager;
import nl.tinyaii.skills.skill.SkillsManager;
import nl.tinyaii.skills.util.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public class SkillsPlugin extends JavaPlugin {

    private SkillsManager skillsManager;
    private PlayerDataManager playerDataManager;
    private Messages messages;
    private org.bukkit.configuration.file.YamlConfiguration expConfig;

    @Override
    public void onEnable() {
        // TinyAII 品牌横幅 —— 必须在所有初始化逻辑之前输出（与 AutoBackup 完全一致）
        getLogger().info(" _____ _                _    ___ ___");
        getLogger().info("|_   _(_)_ __  _   _   / \\  |_ _|_ _|");
        getLogger().info("  | | | | '_ \\| | | | / _ \\  | | | |");
        getLogger().info("  | | | | | | | |_| |/ ___ \\ | | | |");
        getLogger().info("  |_| |_|_| |_|\\__, /_/   \\_\\___|___|");
        getLogger().info("               |___/");
        getLogger().info("Skills 技能系统 v" + getDescription().getVersion() + " - TinyAII 出品");

        saveDefaultConfig();
        if (!new java.io.File(getDataFolder(), "exp.yml").exists()) saveResource("exp.yml", false);
        expConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(getDataFolder(), "exp.yml"));
        messages = new Messages(this);
        skillsManager = new SkillsManager(this);
        skillsManager.load();
        skillsManager.migrate();
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();

        getCommand("技能").setExecutor(new SkillsCommand(this));
        getCommand("技能").setTabCompleter(new SkillsCommand(this));
        getServer().getPluginManager().registerEvents(new SkillsListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("技能系统已启用，共 8 条技能线。指令: /技能");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.save();
    }

    public void reloadAll() {
        reloadConfig();
        migrateConfig();
        messages.reload();
        reloadExp();
        skillsManager.load();
        skillsManager.migrate();
    }

    /** 配置迁移：旧 config.yml 缺失的新键自动从内置默认合并（全家桶模式） */
    private void migrateConfig() {
        java.io.File f = new java.io.File(getDataFolder(), "config.yml");
        if (!f.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration user =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        java.io.InputStream defStream = getResource("config.yml");
        if (defStream == null) return;
        org.bukkit.configuration.file.YamlConfiguration def =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : def.getKeys(true)) {
            if (!user.contains(key)) { user.set(key, def.get(key)); changed = true; }
        }
        if (changed) {
            try { user.save(f); getLogger().info("config.yml 已自动补齐新版配置项。"); }
            catch (Exception e) { getLogger().warning("config.yml 迁移失败: " + e.getMessage()); }
        }
    }

    public org.bukkit.configuration.file.YamlConfiguration getExpConfig() { return expConfig; }

    /** reload 时重读经验配置 */
    public void reloadExp() {
        expConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(getDataFolder(), "exp.yml"));
    }

    public SkillsManager getSkillsManager() { return skillsManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public Messages getMessages() { return messages; }
}