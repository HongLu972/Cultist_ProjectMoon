[English](./README_en.md) | [中文](./README.md)

# Cultist Simulator (Project Moon Mod) – Data & System API Documentation v1.0.0-beta.3

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
*(The verb's icon is shown on its map token; if no icon is defined, the token falls back to auto-fitted text.)*

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

#### Random Nodes
Weights don't need to sum to 100 — the engine normalizes automatically.
*   `{"type": "random_create", "options": [{"target": "eye", "weight": 70}, {"target": "passion", "weight": 30}]}`: Randomly creates one card by weight.
*   `{"type": "random_do", "options": [{"weight": 1, "action": {"type": "create", "target": "eye", "amount": 1}}, {"weight": 3, "action": {"type": "chance", "percent": 50}}]}`: Randomly executes one nested action tree by weight.
*   `{"type": "random_trigger", "options": [{"event": "found_person", "weight": 1}, {"event": "nothing", "weight": 99}]}`: Randomly triggers one event by weight.

#### State & Narrative
*   `{"type": "transform", "target": "illness", "to": "health", "amount": 1}`: **Transforms cards in place** (`amount: -1` converts every match). Slots first, then unlocked cards on the table. Preserves stack counts, temp aspects, and slot positions.
*   `{"type": "record", "text": "The lights went out."}`: Writes a line into the review chronicle (visible on the ending screen).
*   `{"type": "set_flag", "flag": "met_carmen", "value": 1}`: Sets a story flag (saved with the game).
*   `{"type": "check_flag", "flag": "met_carmen", "value": 1}`: Succeeds if the flag value is ≥ the given value.
*   `{"type": "check_count", "target": "evidence", "amount": 3}`: Succeeds if the total count of a card (by id, including cards in slots, stack-aware) on the field is ≥ amount.
*   `{"type": "destroy", "target": "curse", "amount": 1}`: Destroys up to `amount` unlocked loose cards on the table matching the target aspect (or id) (`amount: -1` = all; cards inside windows/slots are untouched; fails if nothing was destroyed).
*   `{"type": "stop_event", "event": "time_A"}`: Stops every run of the given event (background loops, delayed triggers, running windows); succeeds if anything was stopped.

#### System Commands
*   `{"type": "unlock_verb", "verb": "study"}`: Unlocks a new permanent action window.
*   `{"type": "play_music", "track": "audio/bgm_boss.ogg"}`: Switches background music.
*   `{"type": "modify_aspects", "target": "health", "aspects": {"tired": 1, "body": -1}}`: **Modifies card instance aspects**.

---

## 🪦 5. Ending System (`endings.json`)
Defines various endings and **"Cinematic ending credits review subtitles."**

Ending text (`ending.<id>.desc` and review subtitles) supports **placeholders** replaced with table counts at resolution:
*   `{casualty}` / `{readiness}` / `{evacuation}` / `{evidence}` → count of the matching card.
*   `{record_count}` → number of chronicle records.

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

### 💾 9.1 Multi-Save System
*   **Shortcuts**: Press `Esc` in-game (or click ☰ top-left) to open the **full-screen system menu** (resume / save / load / mod manager / volume). (Press `Space` to purely pause/resume time).
*   **Menu Operations**: Includes 20 independent save slots with **custom naming** (e.g., "First Despair").
*   **Save Location**: Stored in `saves/slot_1.json` ~ `slot_20.json`. Perfectly restores card coordinates, event timers, slot data, and the review chronicle.

### 🧩 9.2 Mod Loading
1. Create a `mods` folder in the game root directory.
2. Create a subfolder with any name (e.g., `mods/NewStory/`).
3. Place JSON files with the same names inside (e.g., `elements.json`) along with `img` and `audio` folders.
4. The engine merges them sequentially at startup. **Duplicate IDs will override the base game data**.

### 🧩 9.3 Visual Mod Editor
*   Click `🛠️ Create Mod` to open.
*   No JSON knowledge required! Form-based interface for aspects, visual Scratch-like blocks for action trees, auto-generates bilingual text files, and exports to `mods/MyCustomMod/`.

### 🎵 9.4 Music System
*   Place audio files in the `assets/audio/` directory.
*   **Strongly recommended** to use `.wav` or `.ogg` format. LibGDX's native decoder has extremely poor compatibility with `.mp3`.
