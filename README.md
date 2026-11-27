[English](./README_en.md) | 中文

# 月计模拟器 (Cultist Project Moon) 数据与系统 API 文档 v1.0.0-beta.3

本游戏采用**纯数据驱动（Data-Driven）**架构。通过修改 JSON 文件，你可以完全重写剧情、修改数值、增加卡牌、乃至制作大型剧情 Mod，而无需改动任何 Java 代码。**同时，游戏内置了强大的可视化 Mod 编辑器，即便你不会写代码也能轻松搓出属于你的模组喵！**

## 📂 文件目录结构约定
*   `assets/data/`：核心数据（卡牌、事件、文本）
*   `assets/audio/`：音频文件（强烈建议使用 `.wav` 或 `.ogg` 格式）
*   `mods/`：外部模组文件夹（与游戏根目录同级，用于覆盖或增加内容）
*   `saves/`：自动生成的游戏存档文件夹

---

## ⚙️ 1. 基础配置 (config.json)
| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `language` | String | 是 | 默认语言代码，如 `"zh"`。 |
| `speeds` | Array | 是 | 右上角倍速控制面板的档位，如 `[0.75, 1, 1.5, 2]`。 |
| `worldWidth` | Float | 是 | 世界地图的宽度（像素）。 |
| `worldHeight`| Float | 是 | 世界地图的高度（像素）。 |
| `windowTitle`| String | 否 | 游戏系统外框的标题（支持外置读取）。 |
| `windowIcon` | String | 否 | 游戏系统外框的图标路径（如 `"icon.png"`）。 |

---

## 🃏 2. 卡牌定义 (elements.json)
定义玩家可以拖动、消耗、获取的所有卡牌。

```json
[
  {
    "id": "health",
    "image": "img/cards/health.png",
    "aspects": { "body": 1 },
    "hiddenAspects": { "cogito": 100 },
    "lifetime": -1,
    "decayTo": "disease_flu",
    "stackable": true,
    "triggers": [
      { "slot": "work", "event": "work_basic" },
      { "slot": "use", "event": "use_health" }
    ]
  }
]
```
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 全局唯一 ID。 |
| `image` | String | (可选) 卡面图片相对路径。 |
| `aspects` | Object | (可选) 明面词条，UI 上会显示。 |
| `hiddenAspects` | Object | (可选) **隐藏词条**，底层判定生效，但 UI 上绝不显示。 |
| `lifetime` | Float | (可选) 寿命（秒）。不填或填 `-1` 表示永久。 |
| `decayTo` | String | (可选) 寿命到期后变成哪张卡。不填则消失。 |
| `stackable` | Boolean | (可选) 是否可堆叠合并。 |
| `triggers` | Array | (可选) 触发规则。`slot` 填行动 ID 或 `"use"` (双击使用)，`event` 填事件 ID。 |

---

## 🪟 3. 行动窗口 (verbs.json)
定义玩家可用的常驻行动窗口。

```json
[ { "id": "work", "icon": "img/icons/work_hammer.png" }, { "id": "study" } ]
```
*(注：地图上行动小图标的图案由 `icon` 字段（或 `text_zh.json` 中的 `slot.<id>.icon`）配置，未配置则自动缩放显示行动名全文；`slot.<id>.name` / `slot.<id>.desc` 是行动窗口的名称与**默认描述**，会显示在右侧详情面板里。)*

---

## 🎬 4. 事件系统 (events.json)
游戏的心脏。包含事件本体、时间轴、强制吸入与动作树。

### 4.1 事件主体结构
```json
[
  {
    "id": "time",
    "start": "manual",
    "slot": "none",
    "autoStart": true,
    "time": 30.0,
    "icon": "img/icons/time.png",
    "forceDrainAspects": ["hope", "despair"],
    "slots": [ { "id": "time_slot", "accepts": { "despair": 1 }, "required": true } ],
    "action": [ ...动作树... ],
    "products": [ { "card": "eye", "amount": 1 } ]
  }
]
```
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 全局唯一事件 ID。 |
| `start` | String | 启动方式：<br>`manual`: 玩家放牌触发。<br>`auto`: 后台静默循环。<br>`linked`: 只能被其他事件触发。 |
| `slot` | String | 占用的行动槽 ID。填 `"none"` 表示开一个独立的事件窗口。`auto` 和 `linked` 事件必须是 `"none"`。 |
| `autoStart`| Boolean | (可选) 仅 `manual` 可用。满足条件后自动开始，不用点“开始”。 |
| `time` | Float | 持续时间（秒）。 |
| `icon` | String | (可选) 事件图标路径：`slot:"none"` 的事件用它作为**地图小图标**的图案（也可用文本键 `event.<id>.icon` 配置）。 |
| `forceDrainAspects` | Array | (可选) **按优先级强制吸入**。事件运行期间，只要场上有该词条的未锁定卡牌，且自身槽位为空，就会被强行吸进去（如 `["hope", "despair"]` 优先吸希望，没有再吸绝望）。 |
| `input` | Object | (可选) 启动所需的词条门槛，如 `{"body": 1}`。 |
| `slots` | Array | (可选) 事件额外生成的槽位列表。`id`: 唯一 ID；`accepts`: 限制放入卡牌必须带有的词条；`required`: 是否必填。 |
| `action` | Array | (可选) 结算时执行的动作树。 |
| `products` | Array | (可选) 产物列表。配合 `{"type": "produce"}` 节点发放。 |

### 4.2 动作树节点 (Action)
动作树由多个节点组成，每个节点执行后必定返回“成功”或“失败”，然后分别执行对应的子节点。所有节点都支持 `"onSuccess": [...]` 和 `"onFail": [...]`。

#### 基础效果
*   `{"type": "create", "target": "eye", "amount": 1}`：在桌面上生成卡牌。永远成功。
*   `{"type": "consume", "target": "money", "amount": 1}`：从当前**窗口槽位**里吃掉带目标词条的卡。不够吃则失败。
*   `{"type": "drain", "target": "health", "amount": 1}`：从**整个桌面**吸走带目标词条的牌（未锁定）。不够吸则失败。
*   `{"type": "produce"}`：发放事件 `products` 列表里的所有产物。

#### 控制流
*   `{"type": "check", "aspects": {"mind": 1}}`：检查当前窗口/桌面的词条总数是否满足条件。
*   `{"type": "chance", "percent": 50}`：抛硬币，按百分比概率判定成功。
*   `{"type": "trigger", "event": "next_event"}`：触发另一个事件。
*   `{"type": "end_game", "ending": "poverty"}`：立刻结束游戏，进入指定结局。

#### 随机节点
权重不需要总和为 100，引擎会自动按总权重归一化计算概率。
*   `{"type": "random_create", "options": [{"target": "eye", "weight": 70}, {"target": "passion", "weight": 30}]}`：按权重随机创造一张卡牌。
*   `{"type": "random_do", "options": [{"weight": 1, "action": {"type": "create", "target": "eye", "amount": 1}}, {"weight": 3, "action": {"type": "chance", "percent": 50}}]}`：按权重随机执行一段动作代码（`action` 里可嵌套任意动作树，实现子分支）。
*   `{"type": "random_trigger", "options": [{"event": "found_person", "weight": 1}, {"event": "nothing", "weight": 99}]}`：按权重随机触发一个事件。

#### 状态与叙事
*   `{"type": "transform", "target": "illness", "to": "health", "amount": 1}`：**原位变牌**。把场上的牌变成另一张（`amount: -1` 表示全部变）。优先槽内，其次桌面未锁定；保留堆叠数量、临时词条与所在槽位。
*   `{"type": "record", "text": "灯光熄灭了。"}`：往报幕编年史里写一行字（结局画面可见）。
*   `{"type": "set_flag", "flag": "met_carmen", "value": 1}`：设置剧情旗标（随存档保存）。
*   `{"type": "check_flag", "flag": "met_carmen", "value": 1}`：检查旗标是否 ≥ 指定值。
*   `{"type": "check_count", "target": "evidence", "amount": 3}`：检查场上某卡牌（按 id，含槽内，按堆叠计数）总数是否 ≥ amount。
*   `{"type": "destroy", "target": "curse", "amount": 1}`：摧毁桌面上带 target 词条（或 id）的未锁定散牌（`amount: -1` = 全部；窗口槽里的牌不受影响；一张都没摧到或不够数则失败）。
*   `{"type": "stop_event", "event": "time_A"}`：停止指定事件的一切运行（后台循环、延时触发、正在跑的窗口），停到了即成功。

#### 系统指令
*   `{"type": "unlock_verb", "verb": "study"}`：解锁一个新的常驻行动窗口。
*   `{"type": "play_music", "track": "audio/bgm_boss.ogg"}`：无缝切换背景音乐。
*   `{"type": "modify_aspects", "target": "health", "aspects": {"tired": 1, "body": -1}}`：**修改卡牌实例的属性**。给卡牌加上或减去临时的词条，UI 上会实时显示，随存档保存。

---

## 🪦 5. 结局系统 (endings.json)
定义了游戏的各种结局及**“电影结束式的报幕清算字幕”**。

```json
[
  {
    "id": "te_underlight",
    "type": "TE",
    "review": {
      "duration": 16.0,
      "subtitles": [
        { "time": 0.0, "text": "火光与浓烟散去。" },
        { "time": 4.0, "text": "你身边是无数具再也无法醒来的冰冷躯体。" }
      ]
    }
  }
]
```

**结局文本占位符**（`ending.<id>.desc` 与报幕字幕通用）：结算画面会把文本里的占位符替换成场面上对应卡牌的数量。
*   `{casualty}` / `{readiness}` / `{evacuation}` / `{evidence}` → 对应同名卡牌在场上的数量。
*   `{record_count}` → 编年史（chronicle）已记录的事件条数。

---

## 🚩 6. 开局系统 (scenarios.json)
玩家在新游戏界面看到的选项。

```json
[
  {
    "id": "ayin",
    "cards": [ { "card": "carmen_invitation", "amount": 1 } ],
    "verbs": [ "work" ],
    "autoEvents": []
  }
]
```

---

## 🌐 7. 本地化文本 (text_zh.json / text_en.json)
> [!WARNING]  
> **极其重要**：所有在 UI 上显示的文字，**严禁**直接写在 Java 代码里。系统会扫描 JSON 里出现过的字符来动态生成中文字体，写死在代码里会导致字变成方块或报错。

固定 Key 命名约定：
- **卡牌**：`element.<ID>.name` / `.desc`
- **词条**：`aspect.<ID>.name`
- **行动/槽位**：`slot.<ID>.name` / `.desc` / `.icon`
- **事件**：`event.<ID>.name` / `.desc` / `.result` / `.icon` (结算时写入日志)
- **结局**：`ending.<ID>.name` / `.desc`
- **系统 UI**：`ui.start`, `ui.paused`, `ui.log` 等。

---

## 🕵️‍♂️ 8. 隐藏特性大全（聪明蛋必看！）

1.  **隐藏词条（暗牌机制）**：
    在 `elements.json` 里写 `"hiddenAspects": {"cogito": 100}`。玩家在 UI 上看不到这个词条，但把它放进要求 `cogito` 的槽位时，系统底层判定会直接通过！
2.  **单槽位词条限制**：
    在 `slots` 里写 `"accepts": { "money": 1 }`。玩家拖动卡牌时，如果不满足该词条，卡牌会被直接弹回，塞不进去。
3.  **实例临时属性**：
    利用 `modify_aspects` 动作，可以给卡牌吃“临时Buff”或“Debuff”。比如锻炼后给健康牌加 `-1身体, +1疲倦`。这个属性会实打实存进档里。
4.  **多重优先级强制吸入**：
    事件里的 `"forceDrainAspects": ["hope", "despair"]`。它会优先吸桌面上的希望，吸不到才吸绝望。
5.  **纯外部 Mod 热重载**：
    在 exe 同级目录建 `mods/MyMod/`，把 JSON、图片、音频扔进去。游戏启动时会优先读取外面的文件，实现**免编译热更新**。

---

## 🛠️ 9. 引擎高级功能（玩家与开发者指南）

### 💾 9.1 多存档系统
*   **快捷键**：游戏内按 `Esc`（或点左上角 ☰）呼出**全屏系统菜单**（返回游戏 / 存读档 / 打开 Mod 管理器 / 音量调节）。（空格键用于纯粹暂停/恢复时间）。
*   **菜单操作**：包含 20 个独立存档槽位，支持**自定义存档命名**。
*   **存档位置**：保存在游戏根目录下的 `saves/slot_1.json` ~ `slot_20.json`，完美恢复卡牌坐标、事件倒计时、槽位数据以及报幕编年史。

### 🧩 9.2 可视化 Mod 编辑器
*   在游戏主界面（或左上角系统菜单）点击 `🛠️ 制作 Mod` 即可打开。
*   **无需懂 JSON 代码**！通过表单化界面，点点鼠标就能完成：卡牌的属性添加、动作树的可视化积木拼接、自动生成中英文翻译字典等。
*   编辑完成后点击保存，自动生成符合引擎规范的 JSON 并放入 `mods/MyCustomMod/` 文件夹喵！

### 🎵 9.3 音乐系统
*   将音频文件放入 `assets/audio/` 目录。
*   **强烈建议**使用 `.wav` 或 `.ogg` 格式。LibGDX 原生解码器对 `.mp3` 兼容性极差。
