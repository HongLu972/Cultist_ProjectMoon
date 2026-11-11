[English](./README_en.md) | [中文](./README.md)

# Cultist Simulator (Project Moon Mod) – Data & System API Documentation v1.0.0-alpha.5

This game is built on a **purely data-driven** architecture. By modifying JSON files, you can completely rewrite the story, adjust numerical values, add new cards, or even create large-scale story mods—all without touching a single line of Java code. **Additionally, the game features a built-in visual Mod Editor, allowing you to easily create your own mods without writing any code!**

## 📂 File Directory Structure
*   `assets/data/`: Core game data (cards, events, text).
*   `assets/audio/`: Audio files (`.wav` or `.ogg` are strongly recommended).
*   `mods/`: External mod folder (located at the same level as the game executable).
*   `saves/`: Auto-generated game save folder.

---

## ⚙️ 1. Global Configuration (`config.json`)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `language` | String | Yes | Default language code, e.g., `"zh"`. |
| `speeds` | Array | Yes | Speed multipliers, e.g., `[0.75, 1, 1.5, 2]`. |
| `worldWidth` | Float | Yes | Width of the world map (in pixels). |
| `worldHeight`| Float | Yes | Height of the world map (in pixels). |
| `windowTitle`| String | No | The title of the OS window frame. |
| `windowIcon` | String | No | The icon path for the OS window frame (e.g., `"icon.png"`). |

---

## 🃏 2. Card Definitions (`elements.json`)
Defines all cards that players can drag, consume, or obtain.

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
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Globally unique ID. |
| `image` | String | (Optional) Relative path to the card image. |
| `aspects` | Object | (Optional) Visible aspects, shown in UI. |
| `hiddenAspects` | Object | (Optional) **Hidden aspects**. Logic works, but never shown in UI. |
| `lifetime` | Float | (Optional) Lifetime in seconds. Omit or set to `-1` for permanent. |
| `decayTo` | String | (Optional) ID of the card this decays into. |
| `stackable` | Boolean | (Optional) Whether the card can be stacked. |
| `triggers` | Array | (Optional) Trigger rules. `slot` can be a verb ID or `"use"`. `event` is the event ID. |

---

## 🪟 3. Verb Windows (`verbs.json`)
Defines the permanent action windows available to the player.

```json
[ { "id": "work", "icon": "img/icons/work_hammer.png" }, { "id": "study" } ]
```

---

## 🎬 4. Event System (`events.json`)
The heart of the game. Contains event bodies, timelines, forced draining, and action trees.

### 4.1 Event Body Structure
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
    "action": [ ...action tree... ],
    "products": [ { "card": "eye", "amount": 1 } ]
  }
]
```
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Globally unique event ID. |
| `start` | String | Start mode: <br>`manual`: Player triggers by placing cards.<br>`auto`: Silent background loop.<br>`linked`: Only triggered by other events. |
| `slot` | String | ID of the occupied verb slot. Use `"none"` for an independent window. |
| `autoStart`| Boolean | (Optional) Only for `manual`. Starts automatically when conditions are met. |
| `time` | Float | Duration in seconds. |
| `icon` | String | (Optional) Icon path for the event window. |
| `forceDrainAspects` | Array | (Optional) **Priority-based forced draining**. |
| `input` | Object | (Optional) Aspect threshold required to start, e.g., `{"body": 1}`. |
| `slots` | Array | (Optional) List of extra slots. `accepts` restricts aspects; `required` means mandatory. |
| `action` | Array | (Optional) Action tree executed upon resolution. |
| `products` | Array | (Optional) List of products. Delivered via the `{"type": "produce"}` node. |

### 4.2 Action Tree Nodes (Action)
Action trees consist of multiple nodes. All nodes support `"onSuccess": [...]` and `"onFail": [...]`.

#### Basic Effects
*   `{"type": "create", "target": "eye", "amount": 1}`: Creates cards on the table. Always succeeds.
*   `{"type": "consume", "target": "money", "amount": 1}`: Consumes cards from the **current window's slots**. Fails if not enough.
*   `{"type": "drain", "target": "health", "amount": 1}`: Drains cards from the **entire table**.
*   `{"type": "produce"}`: Delivers all products in the event's `products` list.

#### Control Flow
*   `{"type": "check", "aspects": {"mind": 1}}`: Checks aspect counts.
*   `{"type": "chance", "percent": 50}`: Coin flip.
*   `{"type": "trigger", "event": "next_event"}`: Triggers another event.
*   `{"type": "end_game", "ending": "poverty"}`: Ends the game.

#### System Commands
*   `{"type": "unlock_verb", "verb": "study"}`: Unlocks a new permanent action window.
*   `{"type": "play_music", "track": "audio/bgm_boss.ogg"}`: Switches background music.
*   `{"type": "modify_aspects", "target": "health", "aspects": {"tired": 1, "body": -1}}`: **Modifies card instance aspects**.

---

## 🪦 5. Ending System (`endings.json`)
Defines various endings and **"Cinematic ending credits review subtitles."**

---

## 🚩 6. Scenario System (`scenarios.json`)
Options presented to the player on the new game screen.

---

## 🌐 7. Localization (`text_zh.json` / `text_en.json`)
> [!WARNING]  
> **Extremely Important**: All text displayed in the UI **must not** be hardcoded in Java. 

---

## 🕵️‍♂️ 8. Hidden Features
1.  **Hidden Aspects**: `"hiddenAspects": {"cogito": 100}`.
2.  **Slot Restrictions**: `"accepts": { "money": 1 }`.
3.  **Temporary Aspects**: `modify_aspects` action.
4.  **Priority-Based Draining**: `"forceDrainAspects": ["hope", "despair"]`.
5.  **Mod Hot-Reloading**: Prioritizes external files.

---

## 🛠️ 9. Engine Advanced Features

### 🧩 9.2 Visual Mod Editor
*   Click `🛠️ Create Mod` to open. 
*   No JSON knowledge required! Form-based interface for aspects, visual Scratch-like blocks for action trees, auto-generates bilingual text files, and exports to `mods/MyCustomMod/`.