package nl.tinyaii.skills.skill;

import nl.tinyaii.skills.SkillsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * 技能配置管理器：从 skills.yml 加载每个技能线的满级与 5 个能力定义。
 * 默认值即老板批准的设计稿（unlock/every/base/per）。
 */
public class SkillsManager {

    private final SkillsPlugin plugin;
    private final Map<Skill, SkillConfig> configs = new EnumMap<>(Skill.class);
    private int maxLevel = 80;

    public SkillsManager(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public static class SkillConfig {
        public final Skill skill;
        public final AbilityDef[] abilities = new AbilityDef[5];
        public final int[] unlockOrder = {1, 9, 18, 31, 50};
        public final int[] everyOrder = {5, 8, 10, 15, 20};

        public SkillConfig(Skill skill) {
            this.skill = skill;
        }
    }

    public void load() {
        configs.clear();
        maxLevel = plugin.getConfig().getInt("settings.max-level", 80);

        File f = new File(plugin.getDataFolder(), "skills.yml");
        if (!f.exists()) saveDefaultSkills();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

        for (Skill skill : Skill.values()) {
            SkillConfig cfg = new SkillConfig(skill);
            ConfigurationSection sec = yml.getConfigurationSection("skills." + skill.key());
            for (int i = 0; i < 5; i++) {
                AbilityDef def = defaultsFor(skill, i);   // 兜底
                if (sec != null) {
                    String a = "ability-" + (i + 1);
                    String name = sec.getString(a + ".name", def.getName());
                    String desc = sec.getString(a + ".desc", def.getDesc());
                    int unlock = sec.getInt(a + ".unlock", cfg.unlockOrder[i]);
                    int every = sec.getInt(a + ".every", cfg.everyOrder[i]);
                    double base = sec.getDouble(a + ".base", def.getBase());
                    double per = sec.getDouble(a + ".per", def.getPer());
                    int maxLv = sec.getInt(a + ".max-level", def.getMaxLevel());
                    def = new AbilityDef(def.getId(), name, unlock, every, base, per, maxLv);
                    def.setDesc(desc);
                }
                cfg.abilities[i] = def;
            }
            configs.put(skill, cfg);
        }
    }

    /** 各技能线 5 个能力的默认定义（老板批准设计稿） */
    private AbilityDef defaultsFor(Skill s, int idx) {
        switch (s) {
            case MINING:
                switch (idx) {
                    case 0: return new AbilityDef("lucky_miner", "幸运矿工", 1, 5, 5, 5, 0);
                    case 1: return new AbilityDef("miner_exp", "矿工经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("pick_master", "镐精通", 18, 10, 3, 2, 0);
                    case 3: return new AbilityDef("stamina", "矿洞耐力", 31, 15, 1, 1, 0);
                    default: return new AbilityDef("hardened_armor", "硬化护甲", 50, 20, 3, 3, 0);
                }
            case FIGHTING:
                switch (idx) {
                    case 0: return new AbilityDef("parry", "格挡", 1, 5, 5, 2, 20);
                    case 1: return new AbilityDef("fighter_exp", "战斗经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("sword_master", "剑精通", 18, 10, 3, 2, 0);
                    case 3: return new AbilityDef("first_strike", "先手", 31, 15, 15, 5, 0);
                    default: return new AbilityDef("bleed", "流血", 50, 20, 3, 3, 0);
                }
            case FORAGING:
                switch (idx) {
                    case 0: return new AbilityDef("lumberjack", "伐木工", 1, 5, 10, 10, 0);
                    case 1: return new AbilityDef("forager_exp", "伐木经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("axe_master", "斧精通", 18, 10, 4, 3, 0);
                    case 3: return new AbilityDef("valor", "勇猛", 31, 15, 1, 1, 0);
                    default: return new AbilityDef("shredder", "撕碎", 50, 20, 3, 3, 0);
                }
            case FARMING:
                switch (idx) {
                    case 0: return new AbilityDef("bountiful_harvest", "丰收", 1, 5, 10, 10, 0);
                    case 1: return new AbilityDef("farmer_exp", "农艺经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("scythe_master", "镰刀精通", 18, 10, 3, 2, 0);
                    case 3: return new AbilityDef("geneticist", "基因学家", 31, 15, 1, 2, 0);
                    default: return new AbilityDef("growth_aura", "生长光环", 50, 20, 12, 12, 0);
                }
            case FISHING:
                switch (idx) {
                    case 0: return new AbilityDef("lucky_catch", "幸运捕获", 1, 5, 5, 5, 0);
                    case 1: return new AbilityDef("fisher_exp", "渔夫经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("treasure_hunter", "宝藏猎人", 18, 10, 0.5, 0.5, 0);
                    case 3: return new AbilityDef("caster_master", "甩杆大师", 31, 15, 10, 5, 0);
                    default: return new AbilityDef("epic_catch", "史诗渔获", 50, 20, 2, 1, 0);
                }
            case BREWING:
                switch (idx) {
                    case 0: return new AbilityDef("alchemist", "炼金师", 1, 5, 5, 4, 0);
                    case 1: return new AbilityDef("brewer_exp", "炼药经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("splasher", "投掷师", 18, 10, 0.5, 0.25, 0);
                    case 3: return new AbilityDef("lingering", "滞留大师", 31, 15, 5, 4, 0);
                    default: return new AbilityDef("wise_effect", "睿智效果", 50, 20, 1, 1, 0);
                }
            case ARCHERY:
                switch (idx) {
                    case 0: return new AbilityDef("retrieval", "回收", 1, 5, 5, 5, 0);
                    case 1: return new AbilityDef("archer_exp", "弓术经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("bow_master", "弓精通", 18, 10, 3, 2, 0);
                    case 3: return new AbilityDef("piercing", "穿透", 31, 15, 3, 3, 0);
                    default: return new AbilityDef("stun", "眩晕", 50, 20, 2, 1, 0);
                }
            case DEFENSE:
            default:
                switch (idx) {
                    case 0: return new AbilityDef("shielding", "护盾", 1, 5, 2, 3, 20);
                    case 1: return new AbilityDef("defender_exp", "防御经验", 9, 8, 10, 10, 0);
                    case 2: return new AbilityDef("mob_master", "怪物大师", 18, 10, 2, 3, 0);
                    case 3: return new AbilityDef("immunity", "免疫", 31, 15, 0.5, 0.4, 0);
                    default: return new AbilityDef("no_debuff", "无Debuff", 50, 20, 5, 5, 0);
                }
        }
    }

    private void saveDefaultSkills() {
        plugin.saveResource("skills.yml", false);
        // 若默认资源缺失则手动生成骨架
        if (!new File(plugin.getDataFolder(), "skills.yml").exists()) {
            YamlConfiguration yml = new YamlConfiguration();
            try { yml.save(new File(plugin.getDataFolder(), "skills.yml")); } catch (Exception ignored) {}
        }
    }

    public int getMaxLevel(){ return maxLevel; }
    public SkillConfig get(Skill s){ return configs.get(s); }
    public Map<Skill, SkillConfig> all(){ return configs; }

    /** 迁移：补齐缺失键（复用全家桶模式） */
    public void migrate() {
        File f = new File(plugin.getDataFolder(), "skills.yml");
        if (!f.exists()) return;
        YamlConfiguration user = YamlConfiguration.loadConfiguration(f);
        InputStream defStream = plugin.getResource("skills.yml");
        if (defStream == null) return;
        YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : def.getKeys(true)) {
            if (!user.contains(key)) { user.set(key, def.get(key)); changed = true; }
        }
        if (changed) {
            try { user.save(f); } catch (Exception ignored) {}
        }
    }
}