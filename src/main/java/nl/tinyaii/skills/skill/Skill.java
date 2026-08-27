package nl.tinyaii.skills.skill;

/**
 * 技能线枚举（8 条核心线，老板批准版）。
 * 每条线对应一个原版行为成长线，玩家可并行升级、互不排斥。
 */
public enum Skill {
    MINING("矿工", "挖矿获得经验"),
    FIGHTING("战斗", "打怪获得经验"),
    FORAGING("伐木", "砍树获得经验"),
    FARMING("农艺", "收割获得经验"),
    FISHING("渔夫", "钓鱼获得经验"),
    BREWING("炼药", "酿造获得经验"),
    ARCHERY("弓术", "弓杀获得经验"),
    DEFENSE("防御", "受击获得经验");

    private final String displayName;
    private final String desc;

    Skill(String displayName, String desc) {
        this.displayName = displayName;
        this.desc = desc;
    }

    public String getDisplayName() { return displayName; }
    public String getDesc() { return desc; }

    /** config 内的 key（小写） */
    public String key() { return name().toLowerCase(); }
}