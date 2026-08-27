# Skills 技能系统

基于原版行为驱动的轻量技能成长系统：挖矿、打怪、砍树、种田、钓鱼、炼药、射箭、挨打——每种行为都能升级对应技能线，**所有技能并行、不用选职业**。里程碑式能力解锁防数值溢出，零依赖开箱即用。

![Version](https://img.shields.io/badge/version-1.0.0-blue) ![License](https://img.shields.io/badge/license-MIT-green) ![API](https://img.shields.io/badge/API-1.16%2B-orange)

## 功能特性

- **8 条技能线并行**：矿工 / 战斗 / 伐木 / 农艺 / 渔夫 / 炼药 / 弓术 / 防御——任何人做任何事，互不排斥
- **里程碑解锁**：每条线 5 个能力，1/9/18/31/50 级解锁；升级本身不加数值（防溢出），关键等级一次性发放能力
- **真实能力效果**：幸运矿工双倍掉落、格挡减伤、剑精通、先手、流血、路障连锁、丰收、种子返还、幸运捕获、甩竿更远、炼金返还、弓精通、眩晕、护盾、免疫、无Debuff……
- **升级三档反馈**：普通升级（简洁提示+音效）、关键里程碑（全屏Title+粒子）、满级（全服广播）
- **经验细调**：矿/石/木/叶/作物/击杀/钓/酿/受击各自经验值配置驱动，垃圾行为（石头/树叶）≈不给经验防白嫖；分段升级曲线（前期轻松后期难）
- **ActionBar 实时经验**：挖矿/打怪时屏幕中上方显示 `矿工 +5 经验 [====----] 35%`
- **管理员神权**：`/技能 管理` 全套——查看/设置/加经验/封线/解锁/清空/全服操作，普通玩家用不了
- **GUI 技能面板**：8 线总览（等级/经验条/已解锁能力）→ 点击看能力详情（效果说明/当前值/满级效果）
- **完整中文 + 跨版本**：spigot-api 1.16.5 编译，兼容 Paper/Spigot 1.16~最新

## 命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/技能` | 打开技能面板 | skills.use |
| `/技能 信息 [玩家]` | 查看技能进度 | skills.use |
| `/技能 设置 <玩家> <线> <等级>` | 设等级（测试） | skills.admin |
| `/技能 管理` | 神权命令入口 | skills.admin |
| `/技能 管理 查看 <玩家>` | 查看他人 | skills.admin |
| `/技能 管理 加经验 <玩家> <线> <经验>` | 加/扣经验 | skills.admin |
| `/技能 管理 封线 <玩家> <线>` | 禁用该线 | skills.admin |
| `/技能 管理 解锁 <玩家> <线>` | 解封 | skills.admin |
| `/技能 管理 清空 <玩家>` | 洗号 | skills.admin |
| `/技能 管理 全服设置 <线> <等级>` | 全服统一 | skills.admin |
| `/技能 管理 全服加 <线> <经验>` | 全服活动 | skills.admin |
| `/技能 管理 重置全服 确认` | 新赛季 | skills.admin |
| `/技能 重载` | 重载配置 | skills.admin |

权限：`skills.use` 所有人 / `skills.admin` 默认 OP。

## 配置示例

```yaml
# exp.yml —— 经验值细调
xp:
  mining:
    high-ore: 12      # 钻石/绿宝石/远古残骸
    mid-ore: 5        # 铁/金/铜/石英/青金石
    low-ore: 2        # 煤
    stone: 0.3        # 石头类（白嫖行为几乎不给）
# 升级曲线（分段递增）
leveling:
  base: 150
  increment-lv1-10: 25
  increment-lv11-30: 45
  increment-lv31-50: 70
  increment-lv51-80: 110

# skills.yml —— 能力数值细调
skills:
  mining:
    ability-1: { name: "幸运矿工", desc: "挖矿时概率双倍掉落", unlock: 1, every: 5, base: 5, per: 5 }
    # ...
```

## 安装

1. 下载 `skills-1.0.0.jar` 放入服务器 `plugins/`
2. 重启服务器
3. 改 `plugins/Skills/exp.yml` / `skills.yml` / `config.yml` 自定义经验与能力数值

## 兼容性

- 支持核心：Spigot / Paper / Purpur / Leaves
- API 版本：1.16+（spigot-api 1.16.5 编译，理论兼容至最新）
- Java：17+
- 前置依赖：无

## 开源协议

MIT License

---

# Skills (English)

Lightweight vanilla-behavior-driven skill system: mining, fighting, foraging, farming, fishing, brewing, archery, defense — every action levels a matching skill line in parallel. **No class selection needed.** Milestone-based ability unlocks prevent stat overflow. Zero dependencies.

## Features

- 8 parallel skill lines (Mining/Fighting/Foraging/Farming/Fishing/Brewing/Archery/Defense)
- Milestone unlocks at levels 1/9/18/31/50; abilities grant discrete one-time effects instead of per-level stat creep
- Real ability effects: double drops, parry, sword mastery, first strike, bleed, bountiful harvest, seed return, lucky catch, longer cast, alchemy refund, bow mastery, stun, shield, immunity, debuff resist…
- 3-tier level-up feedback: normal / milestone (title+particles) / max level (server broadcast)
- Per-action XP fully configurable; junk actions (stone/leaves) give almost nothing
- Admin "God Mode" commands for full player control
- ActionBar live XP display, Chinese GUI, no special-font dependency

## Compatibility

- Server: Spigot / Paper / Purpur / Leaves
- API version: 1.16+
- Java 17+
- Dependencies: none

## License

MIT License

## Author

**TinyAII**