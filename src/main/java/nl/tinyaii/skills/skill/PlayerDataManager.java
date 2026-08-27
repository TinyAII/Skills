package nl.tinyaii.skills.skill;

import nl.tinyaii.skills.SkillsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家技能数据存储：每玩家按技能线记录 等级 + 经验（当前级内进度）。
 * data.yml 单入口锁，变动即落盘。
 */
public class PlayerDataManager {

    private static class PlayerData {
        final Map<Skill, Integer> level = new EnumMap<>(Skill.class);
        final Map<Skill, Double> xp = new EnumMap<>(Skill.class);
        final java.util.Set<Skill> banned = new java.util.HashSet<>();   // 封线的技能
    }

    private final SkillsPlugin plugin;
    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();
    private File file;
    private final Object lock = new Object();

    public PlayerDataManager(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        synchronized (lock) {
            data.clear();
            file = new File(plugin.getDataFolder(), "data.yml");
            if (!file.exists()) return;
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yml.getConfigurationSection("players");
            if (root == null) return;
            for (String key : root.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    ConfigurationSection s = root.getConfigurationSection(key);
                    if (s == null) continue;
                    PlayerData pd = new PlayerData();
                    for (Skill skill : Skill.values()) {
                        int lv = s.getInt(skill.key() + ".level", 0);
                        double x = s.getDouble(skill.key() + ".xp", 0);
                        pd.level.put(skill, Math.max(0, lv));
                        pd.xp.put(skill, Math.max(0, x));
                        if (s.getBoolean(skill.key() + ".banned", false)) pd.banned.add(skill);
                    }
                    data.put(uuid, pd);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        synchronized (lock) {
            YamlConfiguration yml = new YamlConfiguration();
            for (Map.Entry<UUID, PlayerData> e : data.entrySet()) {
                String base = "players." + e.getKey() + ".";
                for (Skill skill : Skill.values()) {
                    Integer lv = e.getValue().level.get(skill);
                    Double x = e.getValue().xp.get(skill);
                    yml.set(base + skill.key() + ".banned", e.getValue().banned.contains(skill));
                    yml.set(base + skill.key() + ".level", lv == null ? 0 : lv);
                    yml.set(base + skill.key() + ".xp", x == null ? 0 : x);
                }
            }
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                yml.save(file);
            } catch (IOException ex) {
                plugin.getLogger().severe("保存 data.yml 失败: " + ex.getMessage());
            }
        }
    }

    public PlayerData get(UUID uuid) {
        return data.computeIfAbsent(uuid, u -> new PlayerData());
    }

    // ---------- 经验/等级 ----------

    public int getLevel(UUID uuid, Skill skill) {
        Integer lv = get(uuid).level.get(skill);
        return lv == null ? 0 : lv;
    }

    public double getXp(UUID uuid, Skill skill) {
        Double x = get(uuid).xp.get(skill);
        return x == null ? 0 : x;
    }

    /** 当前级升级所需经验（分段曲线，参数在 exp.yml leveling 段） */
    public double xpRequired(int level) {
        // 分段升级曲线（从 exp.yml 读参数）
        // 需要 = base + 段内累计：每段用各自斜率（level <= 10 用 inc1，<=30 用 inc2，<=50 用 inc3，之后 inc4）
        var cfg = plugin.getExpConfig();
        double base = cfg.getDouble("leveling.base", 150);
        double inc1 = cfg.getDouble("leveling.increment-lv1-10", 25);
        double inc2 = cfg.getDouble("leveling.increment-lv11-30", 45);
        double inc3 = cfg.getDouble("leveling.increment-lv31-50", 70);
        double inc4 = cfg.getDouble("leveling.increment-lv51-80", 110);
        double extra = 0;
        if (level > 50) extra += (level - 50) * inc4;
        if (level > 30) extra += (Math.min(level, 50) - 30) * inc3;
        if (level > 10) extra += (Math.min(level, 30) - 10) * inc2;
        if (level <= 10) extra += level * inc1;
        else extra += 10 * inc1;
        return base + extra;
    }

    /**
     * 加经验，返回是否升级（可能连续升多级）。升级后经验保留溢出部分。
     */
    public boolean addXp(UUID uuid, Skill skill, double amount) {
        int maxLevel = plugin.getSkillsManager().getMaxLevel();
        PlayerData pd = get(uuid);
        if (pd.banned.contains(skill)) return false;   // 封线：不再获得经验

        int lv = pd.level.getOrDefault(skill, 0);
        double xp = pd.xp.getOrDefault(skill, 0.0) + amount;
        boolean leveled = false;
        while (lv < maxLevel && xp >= xpRequired(lv)) {
            xp -= xpRequired(lv);
            lv++;
            leveled = true;
        }
        if (lv >= maxLevel) xp = 0;
        pd.level.put(skill, lv);
        pd.xp.put(skill, xp);
        save();
        return leveled;
    }

    public void setLevel(UUID uuid, Skill skill, int level) {
        get(uuid).level.put(skill, Math.max(0, level));
        get(uuid).xp.put(skill, 0.0);
        save();
    }

    /** 封线/解锁：禁用某技能线（不再获得经验） */
    public void banLine(UUID uuid, Skill skill, boolean ban) {
        PlayerData pd = get(uuid);
        if (ban) pd.banned.add(skill);
        else pd.banned.remove(skill);
        save();
    }

    public boolean isBanned(UUID uuid, Skill skill) {
        return get(uuid).banned.contains(skill);
    }
}