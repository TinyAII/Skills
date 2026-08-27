package nl.tinyaii.skills.skill;

/**
 * 能力定义：每个技能线 5 个能力，按老板定稿的解锁节奏。
 * 配置项：unlock起点 / every间隔 / base基础值 / per每档增量 / maxLevel上限档。
 */
public class AbilityDef {
    private final String id;
    private final String name;
    private String desc;           // 效果描述（详情页展示给玩家）
    private final int unlock;      // 解锁等级起点
    private final int every;       // 升档间隔（级）
    private final double base;     // 基础值
    private final double per;      // 每档增量
    private final int maxLevel;    // 能力最高档数（0=不限，20级封顶类用）

    public AbilityDef(String id, String name, int unlock, int every, double base, double per, int maxLevel) {
        this.id = id;
        this.name = name;
        this.unlock = unlock;
        this.every = every;
        this.base = base;
        this.per = per;
        this.maxLevel = maxLevel;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public int getUnlock() { return unlock; }
    public int getEvery() { return every; }
    public double getBase() { return base; }
    public double getPer() { return per; }
    public int getMaxLevel() { return maxLevel; }

    /** 某个技能等级下，该能力当前生效的档数（0=未解锁） */
    public int tierAt(int skillLevel) {
        if (skillLevel < unlock) return 0;
        int t = (skillLevel - unlock) / every + 1;
        if (maxLevel > 0) t = Math.min(t, maxLevel);
        return t;
    }

    /** 某个技能等级下，该能力当前生效的数值 */
    public double valueAt(int skillLevel) {
        int t = tierAt(skillLevel);
        if (t <= 0) return 0;
        return base + (t - 1) * per;
    }

    /** 该能力最终档数（到满级） */
    public int maxTierAt(int skillMaxLevel) {
        int t = (skillMaxLevel - unlock) / every + 1;
        if (maxLevel > 0) t = Math.min(t, maxLevel);
        return Math.max(0, t);
    }
}