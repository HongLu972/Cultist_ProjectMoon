# 月计模拟器数据与系统 API 文档 v1.1

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

---

## 🃏 2. 卡牌定义 (elements.json)
定义玩家可以拖动、消耗、获取的所有卡牌。**卡牌的 ID 默认视作它自带的一个隐式词条（Aspect）。**

```json
[
  {
    "id": "health",
    "image": "img/cards/health.png",
    "aspects": { "body": 1 },
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
| `aspects` | Object | (可选) 词条键值对。 |
| `lifetime` | Float | (可选) 寿命（秒）。不填或填 `-1` 表示永久。 |
| `decayTo` | String | (可选) 寿命到期后变成哪张卡。不填则消失。 |
| `stackable` | Boolean | (可选) 是否可堆叠合并。 |
| `triggers` | Array | (可选) 触发规则。`slot` 填行动 ID 或 `"use"` (双击使用)，`event` 填事件 ID。 |

---

## 🪟 3. 行动窗口 (verbs.json)
定义玩家可用的常驻行动窗口（开局解锁）。

```json
[ { "id": "work" }, { "id": "study" } ]
```

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
    "forceDrainAspects": ["hope", "despair"],
    "slots": [ { "id": "time_slot", "required": true } ],
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
| `forceDrainAspects` | Array | (可选) **按优先级强制吸入**。事件运行期间，只要场上有该词条的未锁定卡牌，且自身槽位为空，就会被强行吸进去（如 `["hope", "despair"]` 优先吸希望，没有再吸绝望）。 |
| `input` | Object | (可选) 启动所需的词条门槛，如 `{"body": 1}`。 |
| `slots` | Array | (可选) 事件额外生成的槽位列表。`id`: 唯一 ID；`accepts`: 接受的词条；`required`: 是否必填。 |
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

---

## 🪦 5. 结局系统 (endings.json)
定义了游戏的各种结局及**“结算字幕”**。

```json
[
  {
    "id": "te_underlight",
    "type": "TE",
    "review": {
      "duration": 16.0,
      "subtitles": [
        { "time": 0.0, "text": "火光与浓烟散去。" },
        { "time": 4.0, "text": "你身边是 数 具再也无法醒来的冰冷躯体。" }
      ]
    }
  }
]
```
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 结局唯一 ID。 |
| `type` | String | 结局类型（`BE`, `GE`, `TE`, `HE`）。 |
| `review.duration`| Float | 结算字幕的总时长（秒）。 |
| `review.subtitles`| Array | 字幕时间轴。`time`：出现时间，`text`：文字。<br>|

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
- **行动/槽位**：`slot.<ID>.name` / `.desc`
- **事件**：`event.<ID>.name` / `.desc` / `.result` (结算时写入日志)
- **结局**：`ending.<ID>.name` / `.desc`
- **系统 UI**：`ui.start`, `ui.paused`, `ui.log` 等。

---

## 🛠️ 8. 引擎高级功能（玩家指南）

### 💾 8.1 多存档系统
*   **快捷键**：游戏内按 `Esc` 呼出菜单进行存读档。（空格键用于纯粹暂停/恢复时间）。
*   **菜单操作**：包含 20 个独立存档槽位，支持**自定义存档命名**（如“第一次绝望”、“攻克调律者”）。
*   **存档位置**：保存在游戏根目录下的 `saves/slot_1.json` ~ `slot_20.json`，完美恢复卡牌坐标、事件倒计时、槽位数据以及编年史。

### 🧩 8.2 Mod 模组加载
> [!NOTE]
> 模组系统为高级功能，加载时请留意控制台日志，以防数据冲突。可能有BUG！！！

1. 在游戏根目录新建 `mods` 文件夹。
2. 在 `mods` 下新建任意名称的文件夹（如 `mods/NewStory/`）。
3. 放入同名 JSON（如 `elements.json`, `events.json`）。
4. 引擎启动时会按顺序合并。**重复的 ID 将覆盖主游戏数据**。

### 🎵 8.3 音乐系统
*   将音频文件放入 `assets/audio/` 目录。
*   **强烈建议**使用 `.wav` 或 `.ogg` 格式。LibGDX 原生解码器对 `.mp3` 兼容性极差，容易导致崩溃或系统报错音。
*   可通过事件动作 `play_music` 在剧情高潮时切换 BGM。
