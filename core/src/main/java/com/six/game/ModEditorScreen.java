package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.six.game.core.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ModEditorScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final Ui ui;
    private final Table leftListTable;
    private final Table rightFormTable;
    private final Label statusLabel;
    private final String currentModName = "MyCustomMod";

    // 导航按钮组（用于高亮管理）
    private final ButtonGroup<TextButton> navGroup = new ButtonGroup<>();
    private TextButton btnCard, btnEvent, btnEnding, btnVerb, btnScenario, btnConfig;

    // 数据缓存
    private ArrayList<Element> draftElements = new ArrayList<>();
    private ArrayList<EventDef> draftEvents = new ArrayList<>();
    private ArrayList<EndingDef> draftEndings = new ArrayList<>();
    private ArrayList<VerbDef> draftVerbs = new ArrayList<>();
    private ArrayList<ScenarioDef> draftScenarios = new ArrayList<>();
    private HashMap<String, String> draftZh = new HashMap<>();
    private HashMap<String, String> draftEn = new HashMap<>();
    private GameConfig draftConfig = new GameConfig();

    private String currentTab = "";

    public ModEditorScreen(Ui ui) {
        this.ui = ui;

        // 【核心防呆】优先初始化所有容器，彻底杜绝空指针！
        leftListTable = new Table();
        leftListTable.top().pad(10);

        rightFormTable = new Table();
        rightFormTable.top().left().pad(10);

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ui.rect(new Color(0.12f, 0.14f, 0.18f, 1f)));
        stage.addActor(root);

        // ---------------- 顶部导航栏 ----------------
        Table navBar = new Table();
        navBar.setBackground(ui.rect(new Color(0.25f, 0.2f, 0.3f, 1f)));
        navBar.pad(10);

        navGroup.setMinCheckCount(1);
        navGroup.setMaxCheckCount(1);
        navGroup.setUncheckLast(true);

        btnCard = createNavBtn("卡牌制作", "CARD", navBar);
        btnEvent = createNavBtn("事件制作", "EVENT", navBar);
        btnEnding = createNavBtn("结局制作", "ENDING", navBar);
        btnVerb = createNavBtn("动作槽", "VERB", navBar);
        btnScenario = createNavBtn("开局制作", "SCENARIO", navBar);
        btnConfig = createNavBtn("全局配置", "CONFIG", navBar);

        // 加入组（这会触发第一个按钮的选中，但因为容器已初始化，所以很安全）
        navGroup.add(btnCard, btnEvent, btnEnding, btnVerb, btnScenario, btnConfig);

        statusLabel = new Label("当前编辑: " + currentModName, ui.labelStyle(Color.GOLD));
        TextButton btnSave = new TextButton("💾 保存导出全部", ui.buttonStyle());
        TextButton btnBack = new TextButton("返回主菜单", ui.buttonStyle());

        navBar.add(statusLabel).expandX().left().padLeft(20);
        navBar.add(btnSave).padRight(10);
        navBar.add(btnBack);
        root.add(navBar).growX().row();

        // ---------------- 左右分屏 ----------------
        Table mainSplit = new Table();
        root.add(mainSplit).grow().pad(10);

        ScrollPane leftScroll = new ScrollPane(leftListTable);
        leftScroll.setFadeScrollBars(false);
        mainSplit.add(leftScroll).width(350).growY().padRight(15);

        ScrollPane rightScroll = new ScrollPane(rightFormTable);
        rightScroll.setFadeScrollBars(false);
        mainSplit.add(rightScroll).grow();

        btnSave.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { saveModToDisk(); }
        });
        btnBack.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) { ((Main) Gdx.app.getApplicationListener()).showMainMenu(); }
        });

        // 初始化完成，安全触发第一次渲染
        btnCard.setChecked(true);
    }

    private TextButton createNavBtn(String text, String tabId, Table navBar) {
        TextButton btn = new TextButton(text, ui.buttonStyle());
        btn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (btn.isChecked()) {
                    switchTab(tabId);
                }
            }
        });
        navBar.add(btn).padRight(5);
        return btn;
    }

    private void switchTab(String tab) {
        if (tab.equals(currentTab)) return; // 防止重复刷新
        currentTab = tab;
        refreshList();
        buildForm(null);
    }

    // ================== 左侧列表 ==================
    private void refreshList() {
        if (leftListTable == null) return;
        leftListTable.clear();

        String title = "";
        switch (currentTab) {
            case "CARD": title = "已暂存卡牌"; break;
            case "EVENT": title = "已暂存事件"; break;
            case "ENDING": title = "已暂存结局"; break;
            case "VERB": title = "已暂存行动槽"; break;
            case "SCENARIO": title = "已暂存开局"; break;
            case "CONFIG": title = "全局配置"; break;
        }
        leftListTable.add(new Label(title, ui.labelStyle(Color.GOLD))).padBottom(15).left().row();

        if (currentTab.equals("CARD")) {
            for (int i = 0; i < draftElements.size(); i++) addListItem(draftElements.get(i).id, draftElements.get(i), i, draftElements);
        } else if (currentTab.equals("EVENT")) {
            for (int i = 0; i < draftEvents.size(); i++) addListItem(draftEvents.get(i).id, draftEvents.get(i), i, draftEvents);
        } else if (currentTab.equals("ENDING")) {
            for (int i = 0; i < draftEndings.size(); i++) addListItem(draftEndings.get(i).id, draftEndings.get(i), i, draftEndings);
        } else if (currentTab.equals("VERB")) {
            for (int i = 0; i < draftVerbs.size(); i++) addListItem(draftVerbs.get(i).id, draftVerbs.get(i), i, draftVerbs);
        } else if (currentTab.equals("SCENARIO")) {
            for (int i = 0; i < draftScenarios.size(); i++) addListItem(draftScenarios.get(i).id, draftScenarios.get(i), i, draftScenarios);
        }
    }

    private void addListItem(String id, Object data, int index, ArrayList<?> sourceList) {
        Table row = new Table();
        row.setBackground(ui.rect(new Color(0.2f, 0.22f, 0.28f, 1f)));
        row.pad(8);

        TextButton itemBtn = new TextButton(id, ui.buttonStyle());
        itemBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { buildForm(data); }
        });
        row.add(itemBtn).growX().padRight(10);

        TextButton delBtn = new TextButton("X", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                sourceList.remove(index);
                refreshList();
                buildForm(null);
                statusLabel.setText("已删除 [" + id + "]");
            }
        });
        row.add(delBtn).size(30, 30);
        leftListTable.add(row).growX().padBottom(8).row();
    }

    // ================== 右侧表单 ==================
    private void buildForm(Object dataToEdit) {
        if (rightFormTable == null) return;
        rightFormTable.clear();

        if (currentTab.equals("CARD")) buildCardForm((Element) dataToEdit);
        else if (currentTab.equals("EVENT")) buildEventForm((EventDef) dataToEdit);
        else if (currentTab.equals("ENDING")) buildEndingForm((EndingDef) dataToEdit);
        else if (currentTab.equals("VERB")) buildVerbForm((VerbDef) dataToEdit);
        else if (currentTab.equals("SCENARIO")) buildScenarioForm((ScenarioDef) dataToEdit);
        else if (currentTab.equals("CONFIG")) buildConfigForm();
    }

    // --- 卡牌表单 ---
    private void buildCardForm(Element editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? "新增卡牌" : "编辑卡牌: " + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, "卡牌ID:", editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, "卡牌名:", editData != null ? draftZh.getOrDefault("element." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, "英文名:", editData != null ? draftEn.getOrDefault("element." + editData.id + ".name", "") : "");
        TextField lifetimeField = addInput(form, "寿命(-1无限):", editData != null ? String.valueOf(editData.lifetime) : "-1");
        TextField decayField = addInput(form, "衰变产物(ID):", editData != null && editData.decayTo != null ? editData.decayTo : "");

        CheckBox.CheckBoxStyle cbs = new CheckBox.CheckBoxStyle(ui.rect(Color.DARK_GRAY), ui.rect(Color.LIGHT_GRAY), ui.font, Color.WHITE);
        CheckBox stackBox = new CheckBox(" 可堆叠", cbs);
        stackBox.setChecked(editData != null && editData.stackable);
        form.add(stackBox).colspan(2).left().padBottom(15).row();

        // 属性
        Table aspectBox = new Table();
        aspectBox.add(new Label("属性:", ui.labelStyle(Color.WHITE))).left().colspan(4).row();
        if (editData != null) {
            for (String k : editData.aspects.keySet()) addAspectRow(aspectBox, k, draftZh.getOrDefault("aspect." + k + ".name", ""), draftEn.getOrDefault("aspect." + k + ".name", ""), String.valueOf(editData.aspects.get(k)));
        }
        TextButton addAspectBtn = new TextButton("+ 添加属性", ui.buttonStyle());
        addAspectBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addAspectRow(aspectBox, "", "", "", "1"); } });
        aspectBox.add(addAspectBtn).left().row();
        form.add(aspectBox).colspan(2).left().padBottom(15).row();

        // 隐藏属性
        Table hiddenBox = new Table();
        hiddenBox.add(new Label("隐藏属性:", ui.labelStyle(Color.WHITE))).left().colspan(4).row();
        if (editData != null) {
            for (String k : editData.hiddenAspects.keySet()) addAspectRow(hiddenBox, k, draftZh.getOrDefault("aspect." + k + ".name", ""), draftEn.getOrDefault("aspect." + k + ".name", ""), String.valueOf(editData.hiddenAspects.get(k)));
        }
        TextButton addHiddenBtn = new TextButton("+ 添加隐藏属性", ui.buttonStyle());
        addHiddenBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addAspectRow(hiddenBox, "", "", "", "1"); } });
        hiddenBox.add(addHiddenBtn).left().row();
        form.add(hiddenBox).colspan(2).left().padBottom(15).row();

        // 诱发事件
        Table triggerBox = new Table();
        triggerBox.add(new Label("诱发事件:", ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (Element.Trigger t : editData.triggers) addTriggerRow(triggerBox, t.slot, t.event);
        TextButton addTriggerBtn = new TextButton("+ 添加诱发事件", ui.buttonStyle());
        addTriggerBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addTriggerRow(triggerBox, "", ""); } });
        triggerBox.add(addTriggerBtn).left().row();
        form.add(triggerBox).colspan(2).left().padBottom(20).row();

        TextButton saveBtn = new TextButton(editData == null ? "暂存卡牌" : "保存修改", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (idField.getText().isEmpty()) { statusLabel.setText("ID不能为空喵！"); return; }
                if (editData != null) draftElements.remove(editData);
                Element e = new Element(idField.getText());
                e.lifetime = parseFloat(lifetimeField.getText(), -1);
                e.decayTo = decayField.getText();
                e.stackable = stackBox.isChecked();
                e.aspects = parseAspectTable(aspectBox);
                e.hiddenAspects = parseAspectTable(hiddenBox);
                e.triggers = parseTriggerTable(triggerBox);

                draftElements.add(e);
                draftZh.put("element." + e.id + ".name", nameField.getText());
                draftEn.put("element." + e.id + ".name", enNameField.getText());
                statusLabel.setText("卡牌 [" + e.id + "] 已保存喵！");
                refreshList();
                if (editData == null) buildForm(null);
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // --- 事件表单 ---
    private void buildEventForm(EventDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? "新增事件" : "编辑事件: " + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, "事件ID:", editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, "事件名:", editData != null ? draftZh.getOrDefault("event." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, "英文名:", editData != null ? draftEn.getOrDefault("event." + editData.id + ".name", "") : "");
        TextField slotField = addInput(form, "槽位ID(none独立):", editData != null ? editData.slot : "none");

        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("manual", "auto", "linked");
        typeBox.setSelected(editData != null ? editData.start : "manual");
        form.add(new Label("事件类型:", ui.labelStyle(Color.WHITE))).left().padRight(20);
        form.add(typeBox).width(200).padBottom(15).row();

        CheckBox.CheckBoxStyle cbs = new CheckBox.CheckBoxStyle(ui.rect(Color.DARK_GRAY), ui.rect(Color.LIGHT_GRAY), ui.font, Color.WHITE);
        CheckBox autoBox = new CheckBox(" 自动开始", cbs);
        autoBox.setChecked(editData != null && editData.autoStart);
        form.add(autoBox).colspan(2).left().padBottom(15).row();

        TextField timeField = addInput(form, "时间(秒):", editData != null ? String.valueOf(editData.time) : "10");

        // 槽位列表
        Table slotListBox = new Table();
        slotListBox.add(new Label("槽位列表:", ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (SlotDef sd : editData.slots) addSlotDefRow(slotListBox, sd.id, aspectsToString(sd.accepts));
        TextButton addSlotBtn = new TextButton("+ 添加槽位", ui.buttonStyle());
        addSlotBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addSlotDefRow(slotListBox, "", ""); } });
        slotListBox.add(addSlotBtn).left().row();
        form.add(slotListBox).colspan(2).left().padBottom(15).row();

        // 强制吸入
        Table drainBox = new Table();
        drainBox.add(new Label("强制吸入:", ui.labelStyle(Color.WHITE))).left().colspan(2).row();
        if (editData != null) for (String s : editData.forceDrainAspects) addForceDrainRow(drainBox, s);
        TextButton addDrainBtn = new TextButton("+ 添加强制吸入", ui.buttonStyle());
        addDrainBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addForceDrainRow(drainBox, ""); } });
        drainBox.add(addDrainBtn).left().row();
        form.add(drainBox).colspan(2).left().padBottom(15).row();

        // 触发要求
        Table inputBox = new Table();
        inputBox.add(new Label("触发要求:", ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (String k : editData.input.keySet()) addInputRow(inputBox, k, String.valueOf(editData.input.get(k)));
        TextButton addInputBtn = new TextButton("+ 添加要求", ui.buttonStyle());
        addInputBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addInputRow(inputBox, "", "1"); } });
        inputBox.add(addInputBtn).left().row();
        form.add(inputBox).colspan(2).left().padBottom(15).row();

        // 动作树
        form.add(new Label("动作树(可视化):", ui.labelStyle(Color.GOLD))).colspan(2).left().row();
        Table actionTreeBox = new Table();
        actionTreeBox.top().left();
        if (editData != null) for (Action act : editData.action) addActionBlock(actionTreeBox, act);

        TextButton addActionBtn = new TextButton("+ 添加根动作", ui.buttonStyle());
        addActionBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addActionBlock(actionTreeBox, null); } });
        form.add(addActionBtn).colspan(2).left().padBottom(5).row();
        form.add(actionTreeBox).colspan(2).left().padBottom(20).row();

        TextButton saveBtn = new TextButton(editData == null ? "暂存事件" : "保存修改", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (idField.getText().isEmpty()) { statusLabel.setText("ID不能为空喵！"); return; }
                if (editData != null) draftEvents.remove(editData);
                EventDef e = new EventDef();
                e.id = idField.getText();
                e.slot = slotField.getText();
                e.start = typeBox.getSelected();
                e.autoStart = autoBox.isChecked();
                e.time = parseFloat(timeField.getText(), 10);
                e.slots = parseSlotDefTable(slotListBox);
                e.forceDrainAspects = parseForceDrainTable(drainBox);
                e.input = parseInputTable(inputBox);
                e.action = parseActionTree(actionTreeBox);

                draftEvents.add(e);
                draftZh.put("event." + e.id + ".name", nameField.getText());
                draftEn.put("event." + e.id + ".name", enNameField.getText());
                statusLabel.setText("事件 [" + e.id + "] 已保存喵！");
                refreshList();
                if (editData == null) buildForm(null);
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // --- 结局表单 ---
    private void buildEndingForm(EndingDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? "新增结局" : "编辑结局: " + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, "结局ID:", editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, "结局名:", editData != null ? draftZh.getOrDefault("ending." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, "英文名:", editData != null ? draftEn.getOrDefault("ending." + editData.id + ".name", "") : "");

        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("BE", "GE", "TE", "HE");
        typeBox.setSelected(editData != null ? editData.type : "BE");
        form.add(new Label("类型:", ui.labelStyle(Color.WHITE))).left().padRight(20);
        form.add(typeBox).width(200).padBottom(15).row();

        TextButton saveBtn = new TextButton(editData == null ? "暂存结局" : "保存修改", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (idField.getText().isEmpty()) { statusLabel.setText("ID不能为空喵！"); return; }
                if (editData != null) draftEndings.remove(editData);
                EndingDef e = new EndingDef();
                e.id = idField.getText();
                e.type = typeBox.getSelected();
                draftEndings.add(e);
                draftZh.put("ending." + e.id + ".name", nameField.getText());
                draftEn.put("ending." + e.id + ".name", enNameField.getText());
                statusLabel.setText("结局 [" + e.id + "] 已保存喵！");
                refreshList();
                if (editData == null) buildForm(null);
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // --- 动作槽表单 ---
    private void buildVerbForm(VerbDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? "新增动作槽" : "编辑动作槽: " + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, "槽位ID(如work):", editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, "中文名:", editData != null ? draftZh.getOrDefault("slot." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, "英文名:", editData != null ? draftEn.getOrDefault("slot." + editData.id + ".name", "") : "");
        TextField iconField = addInput(form, "图标路径:", editData != null && editData.icon != null ? editData.icon : "");

        TextButton saveBtn = new TextButton(editData == null ? "暂存动作槽" : "保存修改", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (idField.getText().isEmpty()) { statusLabel.setText("ID不能为空喵！"); return; }
                if (editData != null) draftVerbs.remove(editData);
                VerbDef v = new VerbDef();
                v.id = idField.getText();
                v.icon = iconField.getText();
                draftVerbs.add(v);
                draftZh.put("slot." + v.id + ".name", nameField.getText());
                draftEn.put("slot." + v.id + ".name", enNameField.getText());
                statusLabel.setText("动作槽 [" + v.id + "] 已保存喵！");
                refreshList();
                if (editData == null) buildForm(null);
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // --- 开局表单 ---
    private void buildScenarioForm(ScenarioDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? "新增开局" : "编辑开局: " + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, "开局ID(如ayin):", editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, "开局名:", editData != null ? draftZh.getOrDefault("scenario." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, "英文名:", editData != null ? draftEn.getOrDefault("scenario." + editData.id + ".name", "") : "");

        // 初始卡牌
        Table cardBox = new Table();
        cardBox.add(new Label("初始卡牌:", ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) {
            for (EventDef.Product p : editData.cards) addScenarioCardRow(cardBox, p.card, String.valueOf(p.amount));
        }
        TextButton addCardBtn = new TextButton("+ 添加初始卡牌", ui.buttonStyle());
        addCardBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addScenarioCardRow(cardBox, "", "1"); } });
        cardBox.add(addCardBtn).left().row();
        form.add(cardBox).colspan(2).left().padBottom(15).row();

        // 初始事件
        Table eventBox = new Table();
        eventBox.add(new Label("初始事件:", ui.labelStyle(Color.WHITE))).left().colspan(2).row();
        if (editData != null) {
            for (String ev : editData.autoEvents) addScenarioEventRow(eventBox, ev);
        }
        TextButton addEventBtn = new TextButton("+ 添加初始事件", ui.buttonStyle());
        addEventBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { addScenarioEventRow(eventBox, ""); } });
        eventBox.add(addEventBtn).left().row();
        form.add(eventBox).colspan(2).left().padBottom(15).row();

        TextButton saveBtn = new TextButton(editData == null ? "暂存开局" : "保存修改", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (idField.getText().isEmpty()) { statusLabel.setText("ID不能为空喵！"); return; }
                if (editData != null) draftScenarios.remove(editData);
                ScenarioDef s = new ScenarioDef();
                s.id = idField.getText();
                s.cards = parseScenarioCardTable(cardBox);
                s.autoEvents = parseScenarioEventTable(eventBox);

                draftScenarios.add(s);
                draftZh.put("scenario." + s.id + ".name", nameField.getText());
                draftEn.put("scenario." + s.id + ".name", enNameField.getText());
                statusLabel.setText("开局 [" + s.id + "] 已保存喵！");
                refreshList();
                if (editData == null) buildForm(null);
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // --- 全局配置表单 ---
    private void buildConfigForm() {
        Table form = createFormPanel();
        form.add(new Label("全局配置", ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField titleField = addInput(form, "窗口标题:", draftConfig.windowTitle);
        TextField iconField = addInput(form, "窗口图标:", draftConfig.windowIcon);

        TextButton saveBtn = new TextButton("保存配置", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                draftConfig.windowTitle = titleField.getText();
                draftConfig.windowIcon = iconField.getText();
                statusLabel.setText("全局配置已保存喵！");
            }
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    // ================== 动态UI生成与解析（带删除功能）==================
    private void addAspectRow(Table box, String id, String zh, String en, String amt) {
        Table row = new Table();
        row.padBottom(5);
        TextField idF = new TextField(id, ui.textFieldStyle()); idF.setMessageText("id"); row.add(idF).width(100).padRight(5);
        TextField zhF = new TextField(zh, ui.textFieldStyle()); zhF.setMessageText("中文"); row.add(zhF).width(100).padRight(5);
        TextField enF = new TextField(en, ui.textFieldStyle()); enF.setMessageText("英文"); row.add(enF).width(100).padRight(5);
        TextField amtF = new TextField(amt, ui.textFieldStyle()); amtF.setMessageText("数量"); row.add(amtF).width(60).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private HashMap<String, Integer> parseAspectTable(Table box) {
        HashMap<String, Integer> map = new HashMap<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                if (row.getChildren().size >= 4) {
                    String id = ((TextField) row.getChild(0)).getText().trim();
                    String zh = ((TextField) row.getChild(1)).getText().trim();
                    String en = ((TextField) row.getChild(2)).getText().trim();
                    String amt = ((TextField) row.getChild(3)).getText().trim();
                    if (!id.isEmpty()) {
                        map.put(id, parseInt(amt));
                        if (!zh.isEmpty()) draftZh.put("aspect." + id + ".name", zh);
                        if (!en.isEmpty()) draftEn.put("aspect." + id + ".name", en);
                    }
                }
            }
        }
        return map;
    }

    private void addTriggerRow(Table box, String slot, String event) {
        Table row = new Table();
        row.padBottom(5);
        TextField slotF = new TextField(slot, ui.textFieldStyle()); slotF.setMessageText("槽位ID"); row.add(slotF).width(150).padRight(5);
        TextField eventF = new TextField(event, ui.textFieldStyle()); eventF.setMessageText("事件ID"); row.add(eventF).width(150).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private ArrayList<Element.Trigger> parseTriggerTable(Table box) {
        ArrayList<Element.Trigger> list = new ArrayList<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                if (row.getChildren().size >= 2) {
                    String slot = ((TextField) row.getChild(0)).getText().trim();
                    String event = ((TextField) row.getChild(1)).getText().trim();
                    if (!slot.isEmpty() && !event.isEmpty()) {
                        Element.Trigger t = new Element.Trigger();
                        t.slot = slot; t.event = event;
                        list.add(t);
                    }
                }
            }
        }
        return list;
    }

    private void addSlotDefRow(Table box, String id, String accepts) {
        Table row = new Table();
        row.padBottom(5);
        TextField idF = new TextField(id, ui.textFieldStyle()); idF.setMessageText("id"); row.add(idF).width(120).padRight(5);
        TextField accF = new TextField(accepts, ui.textFieldStyle()); accF.setMessageText("限制词条(如money:1)"); row.add(accF).width(180).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private ArrayList<SlotDef> parseSlotDefTable(Table box) {
        ArrayList<SlotDef> list = new ArrayList<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                if (row.getChildren().size >= 2) {
                    String id = ((TextField) row.getChild(0)).getText().trim();
                    String acc = ((TextField) row.getChild(1)).getText().trim();
                    if (!id.isEmpty()) {
                        SlotDef sd = new SlotDef();
                        sd.id = id;
                        sd.accepts = parseAspectsString(acc);
                        list.add(sd);
                    }
                }
            }
        }
        return list;
    }

    private void addForceDrainRow(Table box, String val) {
        Table row = new Table();
        row.padBottom(5);
        TextField f = new TextField(val, ui.textFieldStyle()); f.setMessageText("词条ID"); row.add(f).width(150).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private ArrayList<String> parseForceDrainTable(Table box) {
        ArrayList<String> list = new ArrayList<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                String val = ((TextField) ((Table) c).getChild(0)).getText().trim();
                if (!val.isEmpty()) list.add(val);
            }
        }
        return list;
    }

    private void addInputRow(Table box, String key, String val) {
        Table row = new Table();
        row.padBottom(5);
        TextField kF = new TextField(key, ui.textFieldStyle()); kF.setMessageText("词条"); row.add(kF).width(120).padRight(5);
        TextField vF = new TextField(val, ui.textFieldStyle()); vF.setMessageText("数量"); row.add(vF).width(60).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private HashMap<String, Integer> parseInputTable(Table box) {
        HashMap<String, Integer> map = new HashMap<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                if (row.getChildren().size >= 2) {
                    String key = ((TextField) row.getChild(0)).getText().trim();
                    String val = ((TextField) row.getChild(1)).getText().trim();
                    if (!key.isEmpty()) map.put(key, parseInt(val));
                }
            }
        }
        return map;
    }

    private void addScenarioCardRow(Table box, String id, String amt) {
        Table row = new Table();
        row.padBottom(5);
        TextField idF = new TextField(id, ui.textFieldStyle()); idF.setMessageText("卡牌ID"); row.add(idF).width(150).padRight(5);
        TextField amtF = new TextField(amt, ui.textFieldStyle()); amtF.setMessageText("数量"); row.add(amtF).width(60).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private ArrayList<EventDef.Product> parseScenarioCardTable(Table box) {
        ArrayList<EventDef.Product> list = new ArrayList<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                if (row.getChildren().size >= 2) {
                    String id = ((TextField) row.getChild(0)).getText().trim();
                    String amt = ((TextField) row.getChild(1)).getText().trim();
                    if (!id.isEmpty()) {
                        EventDef.Product p = new EventDef.Product();
                        p.card = id;
                        p.amount = parseInt(amt);
                        list.add(p);
                    }
                }
            }
        }
        return list;
    }

    private void addScenarioEventRow(Table box, String eventId) {
        Table row = new Table();
        row.padBottom(5);
        TextField evF = new TextField(eventId, ui.textFieldStyle()); evF.setMessageText("事件ID"); row.add(evF).width(200).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        box.add(row).left().row();
    }

    private ArrayList<String> parseScenarioEventTable(Table box) {
        ArrayList<String> list = new ArrayList<>();
        for (Actor c : box.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                String ev = ((TextField) ((Table) c).getChild(0)).getText().trim();
                if (!ev.isEmpty()) list.add(ev);
            }
        }
        return list;
    }

    // 辅助方法：生成一行随机选项输入框
    private void addRandomOptionRow(Table container, String target, String weight) {
        Table row = new Table();
        row.padBottom(5);
        TextField idF = new TextField(target, ui.textFieldStyle());
        idF.setMessageText("卡牌ID");
        row.add(idF).width(120).padRight(5);

        TextField wF = new TextField(weight, ui.textFieldStyle());
        wF.setMessageText("权重");
        row.add(wF).width(60).padRight(5);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { row.remove(); } });
        row.add(delBtn).size(30, 30);

        container.add(row).left().row();
    }

    private void addRandomDoRow(Table container, int weight, Action action) {
        Table row = new Table();
        row.setBackground(ui.rect(new Color(0.25f, 0.2f, 0.3f, 1f))); // 区分度高的底色
        row.pad(10).left().top();

        // 顶部：权重 + 删除
        Table top = new Table();
        TextField wF = new TextField(String.valueOf(weight), ui.textFieldStyle());
        wF.setMessageText("权重");
        top.add(new Label("权重:", ui.labelStyle(Color.WHITE))).padRight(5);
        top.add(wF).width(60).padRight(20);

        // 嵌套的动作容器
        Table innerActionContainer = new Table();

        TextButton addActBtn = new TextButton("+ 插入代码块", ui.buttonStyle());
        addActBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                addActionBlock(innerActionContainer, null);
            }
        });
        top.add(addActBtn);

        TextButton delBtn = new TextButton("-", ui.buttonStyle());
        delBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                row.remove();
            }
        });
        top.add(delBtn).padLeft(10);

        row.add(top).left().row();

        // 如果已有动作，直接加载进去
        if (action != null) {
            addActionBlock(innerActionContainer, action);
        }

        row.add(innerActionContainer).left().padTop(5).row();
        container.add(row).left().padBottom(10).row();
    }

    // ================== 动作树（增加红绿辨识度与删除） ==================
    private void addActionBlock(Table container, Action existing) {
        Table block = new Table();
        block.setBackground(ui.rect(new Color(0.2f, 0.22f, 0.28f, 1f)));
        block.pad(10).left().top();

        // 顶部控制条
        Table topRow = new Table();
        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("create", "consume", "drain", "check", "chance", "trigger", "end_game", "unlock_verb", "play_music", "modify_aspects", "random_create", "random_do");
        topRow.add(typeBox).width(120).padRight(5);

        Table params = new Table();
        topRow.add(params);

        // 删除这个动作块的按钮
        TextButton delBlockBtn = new TextButton("移除动作", ui.buttonStyle());
        delBlockBtn.addListener(new ChangeListener() { @Override public void changed(ChangeEvent e, Actor a) { block.remove(); } });
        topRow.add(delBlockBtn).padLeft(20);

        block.add(topRow).left().row();

        // 子节点区域，加上了极其显眼的红绿背景
        Table successBox = new Table(); successBox.setName("successBox");
        successBox.setBackground(ui.rect(new Color(0.1f, 0.3f, 0.15f, 1f))); // 成功绿
        successBox.pad(5);

        Table failBox = new Table(); failBox.setName("failBox");
        failBox.setBackground(ui.rect(new Color(0.35f, 0.1f, 0.1f, 1f))); // 失败红
        failBox.pad(5);

        Runnable updateParams = () -> {
            params.clear();
            String t = typeBox.getSelected();
            if (t.equals("create")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("卡牌ID"); params.add(f).width(100).padRight(5);
                TextField a = new TextField("1", ui.textFieldStyle()); a.setName("amount"); params.add(a).width(50);
            } else if (t.equals("consume") || t.equals("drain")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("词条"); params.add(f).width(100).padRight(5);
                TextField a = new TextField("1", ui.textFieldStyle()); a.setName("amount"); params.add(a).width(50);
            } else if (t.equals("chance")) {
                TextField f = new TextField("50", ui.textFieldStyle()); f.setName("percent"); params.add(f).width(50);
            } else if (t.equals("check")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("aspects"); f.setMessageText("词条组"); params.add(f).width(150);
            } else if (t.equals("trigger")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("事件ID"); params.add(f).width(150);
            } else if (t.equals("end_game")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("结局ID"); params.add(f).width(150);
            } else if (t.equals("unlock_verb")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("槽位ID"); params.add(f).width(150);
            } else if (t.equals("play_music")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("路径"); params.add(f).width(150);
            } else if (t.equals("modify_aspects")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText("卡牌ID"); params.add(f).width(100).padRight(5);
                TextField f2 = new TextField("", ui.textFieldStyle()); f2.setName("aspects"); f2.setMessageText("词条组"); params.add(f2).width(150);
            } else if (t.equals("random_create")) {
                // 创建存放选项的容器
                Table optsContainer = new Table();
                optsContainer.setName("random_opts_container");

                // 如果是编辑现有动作，把之前的数据加载出来
                if (existing instanceof Actions.RandomCreate) {
                    Actions.RandomCreate rc = (Actions.RandomCreate) existing;
                    for (Actions.RandomCreate.Option opt : rc.options) {
                        addRandomOptionRow(optsContainer, opt.target, String.valueOf(opt.weight));
                    }
                } else {
                    // 如果是新建，默认给一行空的
                    addRandomOptionRow(optsContainer, "", "1");
                }

                // 添加加号按钮
                TextButton addOptBtn = new TextButton("+ 添加项", ui.buttonStyle());
                addOptBtn.addListener(new ChangeListener() {
                    @Override public void changed(ChangeEvent e, Actor a) {
                        addRandomOptionRow(optsContainer, "", "1");
                    }
                });

                params.add(optsContainer).left().row();
                params.add(addOptBtn).left().padTop(5);
            } else if (t.equals("random_do")) {
                Table optsContainer = new Table();
                optsContainer.setName("random_do_container");

                if (existing instanceof Actions.RandomDo) {
                    Actions.RandomDo rd = (Actions.RandomDo) existing;
                    for (Actions.RandomDo.Option opt : rd.options) {
                        addRandomDoRow(optsContainer, opt.weight, opt.action);
                    }
                } else {
                    addRandomDoRow(optsContainer, 1, null); // 默认给一个空动作
                }

                TextButton addOptBtn = new TextButton("+ 添加随机动作", ui.buttonStyle());
                addOptBtn.addListener(new ChangeListener() {
                    @Override public void changed(ChangeEvent e, Actor a) {
                        addRandomDoRow(optsContainer, 1, null);
                    }
                });

                params.add(optsContainer).left().row();
                params.add(addOptBtn).left().padTop(5);
            }
        };
        typeBox.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { updateParams.run(); } });

        // 操作按钮
        Table actionBtns = new Table();

        Table succHeader = new Table();
        succHeader.add(new Label("成功时 -> ", ui.labelStyle(Color.GREEN)));
        TextButton addS = new TextButton("+", ui.buttonStyle());
        addS.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { addActionBlock(successBox, null); } });
        succHeader.add(addS);
        actionBtns.add(succHeader).padRight(10);

        Table failHeader = new Table();
        failHeader.add(new Label("失败时 -> ", ui.labelStyle(Color.SALMON)));
        TextButton addF = new TextButton("+", ui.buttonStyle());
        addF.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { addActionBlock(failBox, null); } });
        failHeader.add(addF);
        actionBtns.add(failHeader);

        block.add(actionBtns).left().padTop(10).row();
        block.add(successBox).left().row();
        block.add(failBox).left().row();

        container.add(block).left().padBottom(10).row();

        if (existing != null) {
            if (existing instanceof Actions.Create) typeBox.setSelected("create");
            else if (existing instanceof Actions.Consume) typeBox.setSelected("consume");
            else if (existing instanceof Actions.Drain) typeBox.setSelected("drain");
            else if (existing instanceof Actions.Check) typeBox.setSelected("check");
            else if (existing instanceof Actions.Chance) typeBox.setSelected("chance");
            else if (existing instanceof Actions.TriggerEvent) typeBox.setSelected("trigger");
            else if (existing instanceof Actions.EndGame) typeBox.setSelected("end_game");
            else if (existing instanceof Actions.UnlockVerb) typeBox.setSelected("unlock_verb");
            else if (existing instanceof Actions.PlayMusic) typeBox.setSelected("play_music");
            else if (existing instanceof Actions.ModifyAspects) typeBox.setSelected("modify_aspects");
            updateParams.run();
        } else {
            typeBox.setSelected("create");
        }
    }

    private ArrayList<Action> parseActionTree(Table container) {
        ArrayList<Action> list = new ArrayList<>();
        for (Actor c : container.getChildren()) {
            if (c instanceof Table) list.add(parseActionBlock((Table) c));
        }
        return list;
    }

    private Action parseActionBlock(Table block) {
        String type = ((SelectBox<String>) block.getChild(0)).getSelected();
        Action a = null;
        if (type.equals("create")) {
            Actions.Create c = new Actions.Create();
            c.target = ((TextField) block.findActor("target")).getText();
            c.amount = parseInt(((TextField) block.findActor("amount")).getText());
            a = c;
        } else if (type.equals("consume")) {
            Actions.Consume c = new Actions.Consume();
            c.target = ((TextField) block.findActor("target")).getText();
            c.amount = parseInt(((TextField) block.findActor("amount")).getText());
            a = c;
        } else if (type.equals("drain")) {
            Actions.Drain c = new Actions.Drain();
            c.target = ((TextField) block.findActor("target")).getText();
            c.amount = parseInt(((TextField) block.findActor("amount")).getText());
            a = c;
        } else if (type.equals("chance")) {
            Actions.Chance c = new Actions.Chance();
            c.percent = parseInt(((TextField) block.findActor("percent")).getText());
            a = c;
        } else if (type.equals("check")) {
            Actions.Check c = new Actions.Check();
            c.aspects = parseAspectsString(((TextField) block.findActor("aspects")).getText());
            a = c;
        } else if (type.equals("trigger")) {
            Actions.TriggerEvent c = new Actions.TriggerEvent();
            c.event = ((TextField) block.findActor("target")).getText();
            a = c;
        } else if (type.equals("end_game")) {
            Actions.EndGame c = new Actions.EndGame();
            c.ending = ((TextField) block.findActor("target")).getText();
            a = c;
        } else if (type.equals("unlock_verb")) {
            Actions.UnlockVerb c = new Actions.UnlockVerb();
            c.verb = ((TextField) block.findActor("target")).getText();
            a = c;
        } else if (type.equals("play_music")) {
            Actions.PlayMusic c = new Actions.PlayMusic();
            c.track = ((TextField) block.findActor("target")).getText();
            a = c;
        } else if (type.equals("modify_aspects")) {
            Actions.ModifyAspects c = new Actions.ModifyAspects();
            c.target = ((TextField) block.findActor("target")).getText();
            c.aspects = parseAspectsString(((TextField) block.findActor("aspects")).getText());
            a = c;
        } else if (type.equals("random_create")) {
            Actions.RandomCreate c = new Actions.RandomCreate();
            Table optsContainer = block.findActor("random_opts_container");
            if (optsContainer != null) {
                for (Actor child : optsContainer.getChildren()) {
                    if (child instanceof Table && !(child instanceof Button)) {
                        Table row = (Table) child;
                        if (row.getChildren().size >= 2) {
                            String target = ((TextField) row.getChild(0)).getText().trim();
                            String weightStr = ((TextField) row.getChild(1)).getText().trim();
                            if (!target.isEmpty()) {
                                Actions.RandomCreate.Option opt = new Actions.RandomCreate.Option();
                                opt.target = target;
                                opt.weight = parseInt(weightStr);
                                c.options.add(opt);
                            }
                        }
                    }
                }
            }
            a = c;
        } else if (type.equals("random_do")) {
            Actions.RandomDo c = new Actions.RandomDo();
            Table optsContainer = block.findActor("random_do_container");
            if (optsContainer != null) {
                for (Actor child : optsContainer.getChildren()) {
                    if (child instanceof Table && !(child instanceof Button)) {
                        Table row = (Table) child;
                        // row 里的第一个元素是 top 表，包含权重
                        Table top = (Table) row.getChild(0);
                        String weightStr = ((TextField) top.getChild(1)).getText().trim();
                        // row 里的第二个元素是嵌套动作容器
                        Table actionContainer = (Table) row.getChild(1);

                        Actions.RandomDo.Option opt = new Actions.RandomDo.Option();
                        opt.weight = parseInt(weightStr);

                        // 解析嵌套动作
                        ArrayList<Action> innerActions = parseActionTree(actionContainer);
                        if (!innerActions.isEmpty()) {
                            opt.action = innerActions.get(0); // 假设只取第一个动作
                        }

                        if (opt.action != null) {
                            c.options.add(opt);
                        }
                    }
                }
            }
            a = c;
        }

        if (a != null) {
            Table sBox = block.findActor("successBox");
            if (sBox != null) a.onSuccess.addAll(parseActionTree(sBox));
            Table fBox = block.findActor("failBox");
            if (fBox != null) a.onFail.addAll(parseActionTree(fBox));
        }
        return a;
    }

    // ================== 辅助与落盘 ==================
    private Table createFormPanel() {
        Table form = new Table();
        form.setBackground(ui.rect(new Color(0.2f, 0.22f, 0.28f, 1f)));
        form.pad(20).top().left();
        return form;
    }

    private TextField addInput(Table form, String label, String val) {
        form.add(new Label(label, ui.labelStyle(Color.WHITE))).left().padRight(30).padBottom(15);
        TextField f = new TextField(val != null ? val : "", ui.textFieldStyle());
        form.add(f).width(350).padBottom(15).row();
        return f;
    }

    private int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 1; } }
    private float parseFloat(String s, float def) { try { return Float.parseFloat(s.trim()); } catch (Exception e) { return def; } }
    private String aspectsToString(HashMap<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String k : map.keySet()) { if (sb.length() > 0) sb.append(","); sb.append(k).append(":").append(map.get(k)); }
        return sb.toString();
    }
    private HashMap<String, Integer> parseAspectsString(String str) {
        HashMap<String, Integer> map = new HashMap<>();
        if (str == null || str.isEmpty()) return map;
        for (String pair : str.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length >= 2) map.put(kv[0].trim(), parseInt(kv[1].trim()));
            else if (kv.length == 1 && !kv[0].trim().isEmpty()) map.put(kv[0].trim(), 1);
        }
        return map;
    }

    private Json createJson() {
        Json json = new Json();
        json.setTypeName("type");
        json.addClassTag("create", Actions.Create.class);
        json.addClassTag("consume", Actions.Consume.class);
        json.addClassTag("drain", Actions.Drain.class);
        json.addClassTag("produce", Actions.Produce.class);
        json.addClassTag("trigger", Actions.TriggerEvent.class);
        json.addClassTag("unlock_verb", Actions.UnlockVerb.class);
        json.addClassTag("start_auto", Actions.StartAuto.class);
        json.addClassTag("end_game", Actions.EndGame.class);
        json.addClassTag("chance", Actions.Chance.class);
        json.addClassTag("check", Actions.Check.class);
        json.addClassTag("play_music", Actions.PlayMusic.class);
        json.addClassTag("modify_aspects", Actions.ModifyAspects.class);
        json.addClassTag("random_create", Actions.RandomCreate.class);
        json.setOutputType(JsonWriter.OutputType.json);
        return json;
    }

    private void saveModToDisk() {
        FileHandle dir = Gdx.files.local("mods/" + currentModName + "/");
        dir.mkdirs();
        Json json = createJson();

        if (!draftElements.isEmpty()) dir.child("elements.json").writeString(json.toJson(draftElements), false);
        if (!draftEvents.isEmpty()) dir.child("events.json").writeString(json.toJson(draftEvents), false);
        if (!draftEndings.isEmpty()) dir.child("endings.json").writeString(json.toJson(draftEndings), false);
        if (!draftVerbs.isEmpty()) dir.child("verbs.json").writeString(json.toJson(draftVerbs), false);
        if (!draftScenarios.isEmpty()) dir.child("scenarios.json").writeString(json.toJson(draftScenarios), false);

        dir.child("config.json").writeString(json.toJson(draftConfig), false);
        if (!draftZh.isEmpty()) dir.child("text_zh.json").writeString(json.toJson(draftZh), false);
        if (!draftEn.isEmpty()) dir.child("text_en.json").writeString(json.toJson(draftEn), false);

        statusLabel.setText("✅ 所有数据已保存至 mods/" + currentModName + "/ 喵！重启生效喵！");
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) { ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f); stage.act(delta); stage.draw(); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
