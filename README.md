[English](./README_en.md) | 中文

# 月计模拟器 (Cultist Project Moon) 数据与系统 API 文档 v1.0.0-alpha.4

本游戏采用**纯数据驱动（Data-Driven）**架构。通过修改 JSON 文件，你可以完全重写剧情、修改数值、增加卡牌、乃至制作大型剧情 Mod，而无需改动任何 Java 代码。

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
*(注：UI 窗口左上角的图标，可以通过在 `text_zh.json` 中写 `slot.<id>.icon` 来配置。)*

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
| `icon` | String | (可选) 事件窗口左上角的图标路径。 |
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
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 结局唯一 ID。 |
| `type` | String | 结局类型（`BE`, `GE`, `TE`, `HE`）。 |
| `review.duration`| Float | 报幕动画的总时长（秒）。 |
| `review.subtitles`| Array | 字幕时间轴。`time`：出现时间，`text`：显示的文字。 |

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
4.  **窗口图标绑定**：
    在 `text_zh.json` 里，给行动写 `"slot.work.icon": "img/icons/hammer.png"`，给事件写 `"event.boss.icon": "img/icons/skull.png"`，UI 窗口的左上角立刻显示对应图标。
5.  **外部系统外框换肤**：
    打包成 exe 后，在外部 `data/config.json` 里写 `"windowTitle"` 和 `"windowIcon"`，连 Windows 最外层的黑框和任务栏图标都会跟着变。
6.  **多重优先级强制吸入**：
    事件里的 `"forceDrainAspects": ["hope", "despair"]`。它会优先吸桌面上的希望，吸不到才吸绝望。
7.  **纯外部 Mod 热重载**：
    在 exe 同级目录建 `mods/MyMod/`，把 JSON、图片、音频扔进去。游戏启动时会优先读取外面的文件，实现**免编译热更新**。

---

## 🛠️ 9. 引擎高级功能（玩家指南）

### 💾 9.1 多存档系统
*   **快捷键**：游戏内按 `Esc` 呼出菜单进行存读档。（空格键用于纯粹暂停/恢复时间）。
*   **菜单操作**：包含 20 个独立存档槽位，支持**自定义存档命名**（如“第一次绝望”）。
*   **存档位置**：保存在游戏根目录下的 `saves/slot_1.json` ~ `slot_20.json`，完美恢复卡牌坐标、事件倒计时、槽位数据以及报幕编年史。

### 🧩 9.2 Mod 模组加载
1. 在游戏根目录新建 `mods` 文件夹。
2. 在 `mods` 下新建任意名称的文件夹（如 `mods/NewStory/`）。
3. 放入同名 JSON（如 `elements.json`）以及 `img`、`audio` 文件夹。
4. 引擎启动时会按顺序合并。**重复的 ID 将覆盖主游戏数据**（注意看控制台日志）。

### 🎵 9.3 音乐系统
*   将音频文件放入 `assets/audio/` 目录。
*   **强烈建议**使用 `.wav` 或 `.ogg` 格式。LibGDX 原生解码器对 `.mp3` 兼容性极差。
