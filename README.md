# Cultist-gamr

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.

#  (Cultist_ProjectMoon) API 文档 v1.0

本游戏采用**纯数据驱动（Data-Driven）**架构。所有卡牌、事件、结局、开局逻辑均通过 JSON 文件配置。只要遵循本 API 规范，无需修改 Java 代码即可无限扩展游戏内容，制作属于你自己的剧本（Mod）。

## 📂 文件目录结构约定

所有的数据文件位于 `assets/data/` 目录下：

- `config.json`：全局配置
- `elements.json`：卡牌定义
- `verbs.json`：行动（窗口）定义
- `events.json`：事件与动作树定义
- `endings.json`：结局定义
- `scenarios.json`：开局定义
- `text_zh.json` / `text_en.json`：本地化文本映射

---

## ⚙️ 1. config.json (全局配置)

定义游戏的基础参数。

| 字段          | 类型           | 必填 | 说明                                               |
| :------------ | :------------- | :--- | :------------------------------------------------- |
| `language`    | String         | 是   | 默认语言代码，如 `"zh"`。                          |
| `speeds`      | Array\<Float\> | 是   | 右上角倍速控制面板的档位，如 `[0.75, 1, 1.5, 2]`。 |
| `worldWidth`  | Float          | 是   | 世界地图的宽度（像素）。                           |
| `worldHeight` | Float          | 是   | 世界地图的高度（像素）。                           |

---

## 🃏 2. elements.json (卡牌定义)

定义玩家可以拖动、消耗、获取的所有卡牌。

**数据结构示例：**

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

| 字段             | 类型    | 必填 | 说明                                                         |
| :--------------- | :------ | :--- | :----------------------------------------------------------- |
| `id`             | String  | 是   | 全局唯一 ID。**卡牌的 ID 默认视作它自带的一个隐式词条。**    |
| `image`          | String  | 否   | 卡面图片相对路径。不填则显示纯色底。                         |
| `aspects`        | Object  | 否   | 词条（属性）键值对，如 `{"mind": 2}`。                       |
| `lifetime`       | Float   | 否   | 寿命（秒）。不填或填 `-1` 表示永久。到期后触发衰变。         |
| `decayTo`        | String  | 否   | 寿命到期后变成哪张卡。不填则直接消失。                       |
| `stackable`      | Boolean | 否   | 是否可堆叠。如果为 `true`，同种卡牌在桌面上会合并成一叠。    |
| `triggers`       | Array   | 否   | 触发规则列表。                                               |
| `triggers.slot`  | String  | 是   | 行动 ID（如 `"work"`），或者特殊值 `"use"`（表示双击使用）。 |
| `triggers.event` | String  | 是   | 触发的事件 ID。该事件必须是 `start: "manual"` 类型。         |

---

## 🪟 3. verbs.json (行动/窗口定义)

定义玩家可用的常驻行动窗口。这些窗口开局时被解锁。

```json
[
  { "id": "work" },
  { "id": "study" }
]
```

| 字段 | 类型   | 必填 | 说明                                               |
| :--- | :----- | :--- | :------------------------------------------------- |
| `id` | String | 是   | 全局唯一 ID。它同时也是该行动“起始槽位”的默认 ID。 |

---

## 🎬 4. events.json (事件与动作树)

游戏的心脏。负责所有的逻辑运转。

### 4.1 事件主体结构

```json
[
  {
    "id": "time",
    "start": "manual",
    "slot": "none",
    "autoStart": true,
    "time": 60.0,
    "forceDrainAspect": "money",
    "input": { "mind": 1 },
    "slots": [ { "id": "time_slot", "accepts": {"money": 1}, "required": true } ],
    "action": [ 
      { 
        "type": "check", 
        "aspects": { "money": 1 },
        "onSuccess": [ { "type": "consume", "target": "money", "amount": 1 } ],
        "onFail": [ { "type": "trigger", "event": "time_1" } ]
      },
      { "type": "trigger", "event": "time" }
    ],
    "products": [ { "card": "eye", "amount": 1 } ]
  }
]
```

| 字段               | 类型    | 必填 | 说明                                                         |
| :----------------- | :------ | :--- | :----------------------------------------------------------- |
| `id`               | String  | 是   | 全局唯一事件 ID。                                            |
| `start`            | String  | 是   | 启动方式：<br>`manual`: 玩家放牌手动/自动触发。<br>`auto`: 纯后台静默循环。<br>`linked`: 只能被其他事件触发。 |
| `slot`             | String  | 是   | 占用的行动槽 ID。特殊值 `"none"` 表示**不占用常驻槽位**，会自己开一个独立的事件窗口。`auto` 和 `linked` 事件必须是 `"none"`。 |
| `autoStart`        | Boolean | 否   | 仅 `manual` 可用。满足条件后自动开始，不需要点击“开始”按钮。 |
| `time`             | Float   | 是   | 持续时间（秒）。`linked` 填 0 表示立刻结算。                 |
| `forceDrainAspect` | String  | 否   | **强制吸入**。事件运行期间，只要场上出现带有该词条且未锁定的卡牌，就会在每一帧检测并强制将它吸入第一个空槽。 |
| `input`            | Object  | 否   | 启动所需的词条门槛，如 `{"body": 1}`。                       |
| `slots`            | Array   | 否   | 事件额外生成的槽位列表。<br>`id`: 槽位唯一 ID（**全局不可重复**）。<br>`accepts`: 接受的词条。<br>`required`: 是否必填。 |
| `action`           | Array   | 否   | 结算时执行的动作树（见 4.2）。                               |
| `products`         | Array   | 否   | 产物列表。配合 `{"type": "produce"}` 节点发放。<br>`card`: 卡牌 ID。<br>`amount`: 数量。 |

### 4.2 动作树节点 (Action)

动作树由多个节点组成，每个节点执行后必定返回“成功”或“失败”，然后分别执行对应的子节点。所有节点都支持 `"onSuccess": [...]` 和 `"onFail": [...]`。

#### 基础效果

- `{"type": "create", "target": "eye", "amount": 1}`：在桌面上生成卡牌。永远成功。
- `{"type": "consume", "target": "money", "amount": 1}`：从当前**窗口槽位**里吃掉带目标词条的卡。不够吃则失败，且一张都不吃。
- `{"type": "drain", "target": "health", "amount": 1}`：从**整个桌面**吸走带目标词条的牌（未被锁定的）。不够吸则失败。
- `{"type": "produce"}`：发放事件 `products` 列表里的所有产物。永远成功。

#### 控制流

- `{"type": "check", "aspects": {"mind": 1}}`：检查当前窗口/桌面的词条总数是否满足条件。
- `{"type": "chance", "percent": 50}`：抛硬币，按百分比概率判定成功。
- `{"type": "trigger", "event": "next_event"}`：触发另一个事件。若在窗口内则链式推进；否则开新窗口。
- `{"type": "end_game", "ending": "poverty"}`：立刻结束游戏，进入指定结局。

#### 系统指令

- `{"type": "unlock_verb", "verb": "study"}`：解锁一个新的常驻行动窗口。
- `{"type": "start_auto", "event": "time"}`：启动一个 `auto` 类型的后台循环事件（注：当前版本由于时间流逝逻辑特殊，通常由代码接管，此项一般不用）。

---

## 🪦 5. endings.json (结局定义)

```json
[ { "id": "poverty", "type": "BE" } ]
```

| 字段   | 类型   | 说明                                                         |
| :----- | :----- | :----------------------------------------------------------- |
| `id`   | String | 结局唯一 ID。                                                |
| `type` | String | 结局类型（如 `BE` 坏结局, `GE` 好结局, `TE` 真结局）。用于 UI 分类显示。 |

---

## 🚩 6. scenarios.json (开局定义)

玩家在选择界面看到的选项。

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

| 字段         | 类型   | 说明                                     |
| :----------- | :----- | :--------------------------------------- |
| `id`         | String | 开局唯一 ID。                            |
| `cards`      | Array  | 开局赠送的卡牌。                         |
| `verbs`      | Array  | 开局默认解锁的行动 ID。                  |
| `autoEvents` | Array  | 开局自动启动的 `auto` 事件（通常留空）。 |

---

## 🌐 7. 文本本地化 (text_zh.json / text_en.json)

> [!WARNING]  
> **极其重要**：所有在 UI 上显示的文字，**严禁**直接写在 Java 代码里，必须写在这个文件中。系统只加载 JSON 里出现过的字符，写死在 Java 里会导致中文字符变方块。

系统会使用固定的 Key 命名约定去取文字：

- **卡牌**：`element.<卡牌ID>.name` / `.desc`
- **词条**：`aspect.<词条ID>.name`
- **行动**：`slot.<行动ID>.name` / `.desc`
- **槽位**：`slot.<槽位ID>.name` / `.desc`
- **事件**：`event.<事件ID>.name` / `.desc` / `.result` （`.result` 会显示在右下角日志中）
- **结局**：`ending.<结局ID>.name` / `.desc`
- **开局**：`scenario.<开局ID>.name` / `.desc`
- **系统 UI**：`ui.<功能名>` （如 `ui.start`, `ui.paused`, `ui.log`）

*(缺失的 Key 会在界面上显示为 `[key名]` 作为警告)

---

*文档生成于：Angela 研究所开发组。祝你在暗无天日的夜里写模组剧情愉快。* 

*“我真的活过那50天了么？”*
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
