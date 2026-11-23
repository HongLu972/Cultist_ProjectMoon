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
import java.util.Map;

public class ModEditorScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final Ui ui;
    private final Table leftListTable;
    private final Table rightFormTable;

    private final TextField modNameField;
    private String currentModName = "MyCustomMod";

    private final boolean isZh;

    private final ButtonGroup<TextButton> navGroup = new ButtonGroup<>();
    private TextButton btnCard, btnEvent, btnEnding, btnVerb, btnScenario, btnConfig;

    private final ArrayList<Element> draftElements = new ArrayList<>();
    private final ArrayList<EventDef> draftEvents = new ArrayList<>();
    private final ArrayList<EndingDef> draftEndings = new ArrayList<>();
    private final ArrayList<VerbDef> draftVerbs = new ArrayList<>();
    private final ArrayList<ScenarioDef> draftScenarios = new ArrayList<>();
    private final HashMap<String, String> draftZh = new HashMap<>();
    private final HashMap<String, String> draftEn = new HashMap<>();
    private final GameConfig draftConfig = new GameConfig();

    private String currentTab = "";
    private final Table toastTable = new Table();

    public ModEditorScreen(Ui ui) {
        this.ui = ui;

        GameConfig config = null;
        if (Gdx.app.getApplicationListener() instanceof Main) {
            config = ((Main) Gdx.app.getApplicationListener()).config;
        }
        this.isZh = (config == null) || "zh".equalsIgnoreCase(config.language);

        leftListTable = new Table();
        leftListTable.top().pad(10);
        rightFormTable = new Table();
        rightFormTable.top().left().pad(10);

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ui.rect(new Color(0.12f, 0.14f, 0.18f, 1f)));
        stage.addActor(root);

        toastTable.setFillParent(true);
        toastTable.top().padTop(100);
        toastTable.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        stage.addActor(toastTable);

        Table navBar = new Table();
        navBar.setBackground(ui.rect(new Color(0.25f, 0.2f, 0.3f, 1f)));
        navBar.pad(10);

        navGroup.setMinCheckCount(1);
        navGroup.setMaxCheckCount(1);
        navGroup.setUncheckLast(true);

        btnCard = createNavBtn(t("卡牌制作", "Cards"), "CARD", navBar);
        btnEvent = createNavBtn(t("事件制作", "Events"), "EVENT", navBar);
        btnEnding = createNavBtn(t("结局制作", "Endings"), "ENDING", navBar);
        btnVerb = createNavBtn(t("动作槽", "Verbs"), "VERB", navBar);
        btnScenario = createNavBtn(t("开局制作", "Scenarios"), "SCENARIO", navBar);
        btnConfig = createNavBtn(t("全局配置", "Config"), "CONFIG", navBar);

        TextButton btnImport = createActionBtn(t("📂 导入模组", "📂 Import Mod"), this::importMod);
        navBar.add(btnImport).padRight(10);

        navGroup.add(btnCard, btnEvent, btnEnding, btnVerb, btnScenario, btnConfig);

        navBar.add(new Label(t("当前模组名:", "Current Mod Name:"), ui.labelStyle(Color.WHITE))).padLeft(10).padRight(5);
        modNameField = new TextField(currentModName, ui.textFieldStyle());
        navBar.add(modNameField).width(200).padRight(10);

        TextButton btnSave = createActionBtn(t("💾 保存导出全部", "💾 Save & Export"), this::saveModToDisk);
        TextButton btnBack = createActionBtn(t("返回主菜单", "Back to Menu"), () -> ((Main) Gdx.app.getApplicationListener()).showMainMenu());

        navBar.add().expandX();
        navBar.add(btnSave).padRight(10);
        navBar.add(btnBack);
        root.add(navBar).growX().row();

        Table mainSplit = new Table();
        root.add(mainSplit).grow().pad(10);

        ScrollPane leftScroll = new ScrollPane(leftListTable);
        leftScroll.setFadeScrollBars(false);
        mainSplit.add(leftScroll).width(350).growY().padRight(15);

        ScrollPane rightScroll = new ScrollPane(rightFormTable);
        rightScroll.setFadeScrollBars(false);
        mainSplit.add(rightScroll).grow();

        btnCard.setChecked(true);
    }

    private String t(String zh, String en) {
        return isZh ? zh : en;
    }

    private void showToast(String msg, Color color) {
        toastTable.clearActions();
        toastTable.clear();
        Label label = new Label(msg, ui.labelStyle(color));
        Table bg = new Table();
        bg.setBackground(ui.rect(new Color(0, 0, 0, 0.85f)));
        bg.pad(10, 20, 10, 20);
        bg.add(label);

        toastTable.add(bg);
        toastTable.getColor().a = 1f;
        toastTable.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(2f),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.5f)
        ));
    }

    private TextButton createNavBtn(String text, String tabId, Table navBar) {
        TextButton btn = new TextButton(text, ui.buttonStyle());
        btn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (btn.isChecked()) switchTab(tabId);
            }
        });
        navBar.add(btn).padRight(5);
        return btn;
    }

    private TextButton createActionBtn(String text, Runnable onClick) {
        TextButton btn = new TextButton(text, ui.buttonStyle());
        btn.setProgrammaticChangeEvents(false);
        btn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    if (onClick != null) onClick.run();
                    btn.setChecked(false);
                }
            }
        });
        return btn;
    }

    private void switchTab(String tab) {
        if (tab.equals(currentTab)) return;
        currentTab = tab;
        refreshList();
        buildForm(null);
    }

    private void refreshList() {
        if (leftListTable == null) return;
        leftListTable.clear();

        String title = "";
        switch (currentTab) {
            case "CARD": title = t("已暂存卡牌", "Drafted Cards"); break;
            case "EVENT": title = t("已暂存事件", "Drafted Events"); break;
            case "ENDING": title = t("已暂存结局", "Drafted Endings"); break;
            case "VERB": title = t("已暂存行动槽", "Drafted Verbs"); break;
            case "SCENARIO": title = t("已暂存开局", "Drafted Scenarios"); break;
            case "CONFIG": title = t("全局配置", "Global Config"); break;
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

        TextButton itemBtn = createActionBtn(id, () -> buildForm(data));
        row.add(itemBtn).growX().padRight(10);

        final int idx = index;
        TextButton delBtn = createActionBtn("X", () -> {
            sourceList.remove(idx);
            refreshList();
            buildForm(null);
        });
        row.add(delBtn).size(30, 30);
        leftListTable.add(row).growX().padBottom(8).row();
    }

    private void importMod() {
        new Thread(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            chooser.setDialogTitle(t("选择要导入的 Mod 文件夹", "Select Mod Folder to Import"));
            chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

            java.io.File modsDir = new java.io.File(Gdx.files.local("mods").file().getAbsolutePath());
            if (!modsDir.exists()) modsDir.mkdirs();
            chooser.setCurrentDirectory(modsDir);

            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                java.io.File selectedDir = chooser.getSelectedFile();
                String dirName = selectedDir.getName();

                Gdx.app.postRunnable(() -> {
                    try {
                        Json json = createJson();
                        ArrayList<Element> tempEl = new ArrayList<>();
                        ArrayList<EventDef> tempEv = new ArrayList<>();
                        ArrayList<EndingDef> tempEn = new ArrayList<>();
                        ArrayList<VerbDef> tempVb = new ArrayList<>();
                        ArrayList<ScenarioDef> tempSc = new ArrayList<>();
                        HashMap<String, String> tempZh = new HashMap<>();
                        HashMap<String, String> tempEnMap = new HashMap<>();

                        FileHandle elFile = new FileHandle(selectedDir.getAbsolutePath() + "/elements.json");
                        if (elFile.exists()) tempEl.addAll(json.fromJson(ArrayList.class, Element.class, elFile.readString("UTF-8")));

                        FileHandle evFile = new FileHandle(selectedDir.getAbsolutePath() + "/events.json");
                        if (evFile.exists()) tempEv.addAll(json.fromJson(ArrayList.class, EventDef.class, evFile.readString("UTF-8")));

                        FileHandle enFile = new FileHandle(selectedDir.getAbsolutePath() + "/endings.json");
                        if (enFile.exists()) {
                            json.setTypeName(null);
                            try {
                                tempEn.addAll(json.fromJson(ArrayList.class, EndingDef.class, enFile.readString("UTF-8")));
                            } finally {
                                json.setTypeName("type");
                            }
                        }

                        FileHandle vbFile = new FileHandle(selectedDir.getAbsolutePath() + "/verbs.json");
                        if (vbFile.exists()) tempVb.addAll(json.fromJson(ArrayList.class, VerbDef.class, vbFile.readString("UTF-8")));

                        FileHandle scFile = new FileHandle(selectedDir.getAbsolutePath() + "/scenarios.json");
                        if (scFile.exists()) tempSc.addAll(json.fromJson(ArrayList.class, ScenarioDef.class, scFile.readString("UTF-8")));

                        FileHandle zhFile = new FileHandle(selectedDir.getAbsolutePath() + "/text_zh.json");
                        if (zhFile.exists()) tempZh.putAll(json.fromJson(HashMap.class, String.class, zhFile.readString("UTF-8")));

                        FileHandle enTextFile = new FileHandle(selectedDir.getAbsolutePath() + "/text_en.json");
                        if (enTextFile.exists()) tempEnMap.putAll(json.fromJson(HashMap.class, String.class, enTextFile.readString("UTF-8")));

                        currentModName = dirName;
                        modNameField.setText(currentModName);

                        draftElements.clear(); draftElements.addAll(tempEl);
                        draftEvents.clear(); draftEvents.addAll(tempEv);
                        draftEndings.clear(); draftEndings.addAll(tempEn);
                        draftVerbs.clear(); draftVerbs.addAll(tempVb);
                        draftScenarios.clear(); draftScenarios.addAll(tempSc);
                        draftZh.clear(); draftZh.putAll(tempZh);
                        draftEn.clear(); draftEn.putAll(tempEnMap);

                        refreshList();
                        buildForm(null);
                        showToast(t("导入成功！", "Import Successful!"), Color.GREEN);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showToast(t("导入失败，JSON格式有误", "Import failed: Invalid JSON"), Color.SALMON);
                    }
                });
            }
        }).start();
    }

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

    // ================= 各模块表单构建 =================

    private void buildCardForm(Element editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? t("新增卡牌", "New Card") : t("编辑卡牌: ", "Edit Card: ") + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, t("卡牌ID:", "Card ID:"), editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, t("卡牌名:", "Card Name:"), editData != null ? draftZh.getOrDefault("element." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, t("英文名:", "English Name:"), editData != null ? draftEn.getOrDefault("element." + editData.id + ".name", "") : "");
        TextField lifetimeField = addInput(form, t("寿命(-1无限):", "Lifetime (-1 infinite):"), editData != null ? String.valueOf(editData.lifetime) : "-1");
        TextField decayField = addInput(form, t("衰变产物(ID):", "Decay to (ID):"), editData != null && editData.decayTo != null ? editData.decayTo : "");

        CheckBox.CheckBoxStyle cbs = new CheckBox.CheckBoxStyle(ui.rect(Color.DARK_GRAY), ui.rect(Color.LIGHT_GRAY), ui.font, Color.WHITE);
        CheckBox stackBox = new CheckBox(t(" 可堆叠", " Stackable"), cbs);
        stackBox.setChecked(editData != null && editData.stackable);
        form.add(stackBox).colspan(2).left().padBottom(15).row();

        Table aspectBox = new Table();
        aspectBox.add(new Label(t("属性:", "Aspects:"), ui.labelStyle(Color.WHITE))).left().colspan(4).row();
        if (editData != null) {
            for (String k : editData.aspects.keySet()) addAspectRow(aspectBox, k, draftZh.getOrDefault("aspect." + k + ".name", ""), draftEn.getOrDefault("aspect." + k + ".name", ""), String.valueOf(editData.aspects.get(k)));
        }
        TextButton addAspectBtn = createActionBtn(t("+ 添加属性", "+ Add Aspect"), () -> addAspectRow(aspectBox, "", "", "", "1"));
        aspectBox.add(addAspectBtn).left().row();
        form.add(aspectBox).colspan(2).left().padBottom(15).row();

        Table hiddenBox = new Table();
        hiddenBox.add(new Label(t("隐藏属性:", "Hidden Aspects:"), ui.labelStyle(Color.WHITE))).left().colspan(4).row();
        if (editData != null) {
            for (String k : editData.hiddenAspects.keySet()) addAspectRow(hiddenBox, k, draftZh.getOrDefault("aspect." + k + ".name", ""), draftEn.getOrDefault("aspect." + k + ".name", ""), String.valueOf(editData.hiddenAspects.get(k)));
        }
        TextButton addHiddenBtn = createActionBtn(t("+ 添加隐藏属性", "+ Add Hidden Aspect"), () -> addAspectRow(hiddenBox, "", "", "", "1"));
        hiddenBox.add(addHiddenBtn).left().row();
        form.add(hiddenBox).colspan(2).left().padBottom(15).row();

        Table triggerBox = new Table();
        triggerBox.add(new Label(t("诱发事件:", "Triggers:"), ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (Element.Trigger t : editData.triggers) addTriggerRow(triggerBox, t.slot, t.event);
        TextButton addTriggerBtn = createActionBtn(t("+ 添加诱发事件", "+ Add Trigger"), () -> addTriggerRow(triggerBox, "", ""));
        triggerBox.add(addTriggerBtn).left().row();
        form.add(triggerBox).colspan(2).left().padBottom(20).row();

        TextButton saveBtn = createActionBtn(editData == null ? t("暂存卡牌", "Draft Card") : t("保存修改", "Save Changes"), () -> {
            if (idField.getText().isEmpty()) return;
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
            refreshList();
            if (editData == null) buildForm(null);
            showToast(t("卡牌已暂存", "Card Drafted"), Color.GOLD);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void buildEventForm(EventDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? t("新增事件", "New Event") : t("编辑事件: ", "Edit Event: ") + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();

        TextField idField = addInput(form, t("事件ID:", "Event ID:"), editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, t("事件名:", "Event Name:"), editData != null ? draftZh.getOrDefault("event." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, t("英文名:", "English Name:"), editData != null ? draftEn.getOrDefault("event." + editData.id + ".name", "") : "");
        TextField slotField = addInput(form, t("槽位ID(none独立):", "Slot ID (none for standalone):"), editData != null ? editData.slot : "none");

        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("manual", "auto", "linked");
        typeBox.setSelected(editData != null ? editData.start : "manual");
        form.add(new Label(t("事件类型:", "Event Type:"), ui.labelStyle(Color.WHITE))).left().padRight(20);
        form.add(typeBox).width(200).padBottom(15).row();

        CheckBox.CheckBoxStyle cbs = new CheckBox.CheckBoxStyle(ui.rect(Color.DARK_GRAY), ui.rect(Color.LIGHT_GRAY), ui.font, Color.WHITE);
        CheckBox autoBox = new CheckBox(t(" 自动开始", " Auto Start"), cbs);
        autoBox.setChecked(editData != null && editData.autoStart);
        form.add(autoBox).colspan(2).left().padBottom(15).row();

        TextField timeField = addInput(form, t("时间(秒):", "Duration (s):"), editData != null ? String.valueOf(editData.time) : "10");

        Table slotListBox = new Table();
        slotListBox.add(new Label(t("槽位列表:", "Slot List:"), ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (SlotDef sd : editData.slots) addSlotDefRow(slotListBox, sd.id, aspectsToString(sd.accepts));
        TextButton addSlotBtn = createActionBtn(t("+ 添加槽位", "+ Add Slot"), () -> addSlotDefRow(slotListBox, "", ""));
        slotListBox.add(addSlotBtn).left().row();
        form.add(slotListBox).colspan(2).left().padBottom(15).row();

        Table drainBox = new Table();
        drainBox.add(new Label(t("强制吸入:", "Force Drain:"), ui.labelStyle(Color.WHITE))).left().colspan(2).row();
        if (editData != null) for (String s : editData.forceDrainAspects) addForceDrainRow(drainBox, s);
        TextButton addDrainBtn = createActionBtn(t("+ 添加强制吸入", "+ Add Force Drain"), () -> addForceDrainRow(drainBox, ""));
        drainBox.add(addDrainBtn).left().row();
        form.add(drainBox).colspan(2).left().padBottom(15).row();

        Table inputBox = new Table();
        inputBox.add(new Label(t("触发要求:", "Trigger Requirements:"), ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) for (String k : editData.input.keySet()) addInputRow(inputBox, k, String.valueOf(editData.input.get(k)));
        TextButton addInputBtn = createActionBtn(t("+ 添加要求", "+ Add Requirement"), () -> addInputRow(inputBox, "", "1"));
        inputBox.add(addInputBtn).left().row();
        form.add(inputBox).colspan(2).left().padBottom(15).row();

        form.add(new Label(t("动作树(可视化):", "Action Tree (Visual):"), ui.labelStyle(Color.GOLD))).colspan(2).left().row();
        Table actionTreeBox = new Table();
        actionTreeBox.top().left();
        if (editData != null) for (Action act : editData.action) addActionBlock(actionTreeBox, act);

        TextButton addActionBtn = createActionBtn(t("+ 添加根动作", "+ Add Root Action"), () -> addActionBlock(actionTreeBox, null));
        form.add(addActionBtn).colspan(2).left().padBottom(5).row();
        form.add(actionTreeBox).colspan(2).left().padBottom(20).row();

        TextButton saveBtn = createActionBtn(editData == null ? t("暂存事件", "Draft Event") : t("保存修改", "Save Changes"), () -> {
            if (idField.getText().isEmpty()) return;
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
            refreshList();
            if (editData == null) buildForm(null);
            showToast(t("事件已暂存", "Event Drafted"), Color.GOLD);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void buildEndingForm(EndingDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? t("新增结局", "New Ending") : t("编辑结局: ", "Edit Ending: ") + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();
        TextField idField = addInput(form, t("结局ID:", "Ending ID:"), editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, t("结局名:", "Ending Name:"), editData != null ? draftZh.getOrDefault("ending." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, t("英文名:", "English Name:"), editData != null ? draftEn.getOrDefault("ending." + editData.id + ".name", "") : "");
        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("BE", "GE", "TE", "HE");
        typeBox.setSelected(editData != null ? editData.type : "BE");
        form.add(new Label(t("类型:", "Type:"), ui.labelStyle(Color.WHITE))).left().padRight(20);
        form.add(typeBox).width(200).padBottom(15).row();
        TextButton saveBtn = createActionBtn(editData == null ? t("暂存结局", "Draft Ending") : t("保存修改", "Save Changes"), () -> {
            if (idField.getText().isEmpty()) return;
            if (editData != null) draftEndings.remove(editData);
            EndingDef e = new EndingDef();
            e.id = idField.getText();
            e.type = typeBox.getSelected();
            draftEndings.add(e);
            draftZh.put("ending." + e.id + ".name", nameField.getText());
            draftEn.put("ending." + e.id + ".name", enNameField.getText());
            refreshList();
            if (editData == null) buildForm(null);
            showToast(t("结局已暂存", "Ending Drafted"), Color.GOLD);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void buildVerbForm(VerbDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? t("新增动作槽", "New Verb Slot") : t("编辑动作槽: ", "Edit Verb Slot: ") + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();
        TextField idField = addInput(form, t("槽位ID(如work):", "Slot ID (e.g. work):"), editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, t("中文名:", "Chinese Name:"), editData != null ? draftZh.getOrDefault("slot." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, t("英文名:", "English Name:"), editData != null ? draftEn.getOrDefault("slot." + editData.id + ".name", "") : "");
        TextField iconField = addInput(form, t("图标路径:", "Icon Path:"), editData != null && editData.icon != null ? editData.icon : "");
        TextButton saveBtn = createActionBtn(editData == null ? t("暂存动作槽", "Draft Verb") : t("保存修改", "Save Changes"), () -> {
            if (idField.getText().isEmpty()) return;
            if (editData != null) draftVerbs.remove(editData);
            VerbDef v = new VerbDef();
            v.id = idField.getText();
            v.icon = iconField.getText();
            draftVerbs.add(v);
            draftZh.put("slot." + v.id + ".name", nameField.getText());
            draftEn.put("slot." + v.id + ".name", enNameField.getText());
            refreshList();
            if (editData == null) buildForm(null);
            showToast(t("动作槽已暂存", "Verb Drafted"), Color.GOLD);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void buildScenarioForm(ScenarioDef editData) {
        Table form = createFormPanel();
        form.add(new Label(editData == null ? t("新增开局", "New Scenario") : t("编辑开局: ", "Edit Scenario: ") + editData.id, ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();
        TextField idField = addInput(form, t("开局ID(如ayin):", "Scenario ID (e.g. ayin):"), editData != null ? editData.id : "");
        idField.setDisabled(editData != null);
        TextField nameField = addInput(form, t("开局名:", "Scenario Name:"), editData != null ? draftZh.getOrDefault("scenario." + editData.id + ".name", "") : "");
        TextField enNameField = addInput(form, t("英文名:", "English Name:"), editData != null ? draftEn.getOrDefault("scenario." + editData.id + ".name", "") : "");

        Table cardBox = new Table();
        cardBox.add(new Label(t("初始卡牌:", "Initial Cards:"), ui.labelStyle(Color.WHITE))).left().colspan(3).row();
        if (editData != null) {
            for (EventDef.Product p : editData.cards) addScenarioCardRow(cardBox, p.card, String.valueOf(p.amount));
        }
        TextButton addCardBtn = createActionBtn(t("+ 添加初始卡牌", "+ Add Initial Card"), () -> addScenarioCardRow(cardBox, "", "1"));
        cardBox.add(addCardBtn).left().row();
        form.add(cardBox).colspan(2).left().padBottom(15).row();

        Table eventBox = new Table();
        eventBox.add(new Label(t("初始事件:", "Initial Events:"), ui.labelStyle(Color.WHITE))).left().colspan(2).row();
        if (editData != null) {
            for (String ev : editData.autoEvents) addScenarioEventRow(eventBox, ev);
        }
        TextButton addEventBtn = createActionBtn(t("+ 添加初始事件", "+ Add Initial Event"), () -> addScenarioEventRow(eventBox, ""));
        eventBox.add(addEventBtn).left().row();
        form.add(eventBox).colspan(2).left().padBottom(15).row();

        TextButton saveBtn = createActionBtn(editData == null ? t("暂存开局", "Draft Scenario") : t("保存修改", "Save Changes"), () -> {
            if (idField.getText().isEmpty()) return;
            if (editData != null) draftScenarios.remove(editData);
            ScenarioDef s = new ScenarioDef();
            s.id = idField.getText();
            s.cards = parseScenarioCardTable(cardBox);
            s.autoEvents = parseScenarioEventTable(eventBox);
            draftScenarios.add(s);
            draftZh.put("scenario." + s.id + ".name", nameField.getText());
            draftEn.put("scenario." + s.id + ".name", enNameField.getText());
            refreshList();
            if (editData == null) buildForm(null);
            showToast(t("开局已暂存", "Scenario Drafted"), Color.GOLD);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void buildConfigForm() {
        Table form = createFormPanel();
        form.add(new Label(t("全局配置", "Global Config"), ui.labelStyle(Color.GOLD))).colspan(2).padBottom(20).row();
        TextField titleField = addInput(form, t("窗口标题:", "Window Title:"), draftConfig.windowTitle);
        TextField iconField = addInput(form, t("窗口图标:", "Window Icon:"), draftConfig.windowIcon);
        TextButton saveBtn = createActionBtn(t("保存配置", "Save Config"), () -> {
            draftConfig.windowTitle = titleField.getText();
            draftConfig.windowIcon = iconField.getText();
            showToast(t("配置已保存", "Config Saved"), Color.GREEN);
        });
        form.add(saveBtn).colspan(2).padTop(10).row();
        rightFormTable.add(form).growX().top().pad(10);
    }

    private void addAspectRow(Table box, String id, String zh, String en, String amt) {
        Table row = new Table();
        row.padBottom(5);
        TextField idF = new TextField(id, ui.textFieldStyle()); idF.setMessageText("id"); row.add(idF).width(100).padRight(5);
        TextField zhF = new TextField(zh, ui.textFieldStyle()); zhF.setMessageText(t("中文", "Chinese")); row.add(zhF).width(100).padRight(5);
        TextField enF = new TextField(en, ui.textFieldStyle()); enF.setMessageText(t("英文", "English")); row.add(enF).width(100).padRight(5);
        TextField amtF = new TextField(amt, ui.textFieldStyle()); amtF.setMessageText(t("数量", "Amount")); row.add(amtF).width(60).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField slotF = new TextField(slot, ui.textFieldStyle()); slotF.setMessageText(t("槽位ID", "Slot ID")); row.add(slotF).width(150).padRight(5);
        TextField eventF = new TextField(event, ui.textFieldStyle()); eventF.setMessageText(t("事件ID", "Event ID")); row.add(eventF).width(150).padRight(5);
        slotF.setCursorPosition(slotF.getText().length());
        eventF.setCursorPosition(eventF.getText().length());
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField accF = new TextField(accepts, ui.textFieldStyle()); accF.setMessageText(t("限制词条(如money:1)", "Aspects (e.g. money:1)")); row.add(accF).width(180).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField f = new TextField(val, ui.textFieldStyle()); f.setMessageText(t("词条ID", "Aspect ID")); row.add(f).width(150).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField kF = new TextField(key, ui.textFieldStyle()); kF.setMessageText(t("词条", "Aspect")); row.add(kF).width(120).padRight(5);
        TextField vF = new TextField(val, ui.textFieldStyle()); vF.setMessageText(t("数量", "Amount")); row.add(vF).width(60).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField idF = new TextField(id, ui.textFieldStyle()); idF.setMessageText(t("卡牌ID", "Card ID")); row.add(idF).width(150).padRight(5);
        TextField amtF = new TextField(amt, ui.textFieldStyle()); amtF.setMessageText(t("数量", "Amount")); row.add(amtF).width(60).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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
        TextField evF = new TextField(eventId, ui.textFieldStyle()); evF.setMessageText(t("事件ID", "Event ID")); row.add(evF).width(200).padRight(5);
        TextButton delBtn = createActionBtn("-", row::remove);
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

    // ================= 可视化动作树构建与解析 =================

    private void addActionBlock(Table container, Action existing) {
        Table block = new Table();
        block.setBackground(ui.rect(new Color(0.2f, 0.25f, 0.3f, 1f)));
        block.pad(5).left().top();

        SelectBox<String> typeBox = new SelectBox<>(ui.selectBoxStyle());
        typeBox.setItems("create", "consume", "drain", "check", "chance", "trigger", "end_game", "unlock_verb", "play_music", "modify_aspects", "random_create", "random_trigger", "random_do");
        // 👈 核心修复 1：拉宽下拉框，防止 random_ 截断
        block.add(typeBox).width(180).padRight(10);

        Table params = new Table();
        block.add(params);

        Table successBox = new Table(); successBox.setName("successBox"); successBox.padLeft(20);
        Table failBox = new Table(); failBox.setName("failBox"); failBox.padLeft(20);

        // 用数组包装 existing:保持 effectively final,同时允许在类型切换时判断
        final Action[] currentExistingRef = new Action[] { existing };

        Runnable updateParams = () -> {
            params.clear();
            String tName = typeBox.getSelected();
            // 只有"已有动作的类型 = 当前选中的类型"时,才用它来预填界面
            Action ex = matchesType(currentExistingRef[0], tName) ? currentExistingRef[0] : null;

            if (tName.equals("create")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("卡牌ID", "Card ID")); params.add(f).width(120);
                TextField a = new TextField("1", ui.textFieldStyle()); a.setName("amount"); params.add(a).width(60);
            } else if (tName.equals("consume") || tName.equals("drain")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("词条", "Aspect")); params.add(f).width(120);
                TextField a = new TextField("1", ui.textFieldStyle()); a.setName("amount"); params.add(a).width(60);
            } else if (tName.equals("chance")) {
                TextField f = new TextField("50", ui.textFieldStyle()); f.setName("percent"); params.add(f).width(60);
            } else if (tName.equals("check")) {
                // 词条组:[词条][数量] 一行一个,下面有 [+ 词条] 按钮继续添加
                Map<String, Integer> exAspects = ex instanceof Actions.Check ? ((Actions.Check) ex).aspects : null;
                params.add(createAspectGroupBox("aspects", exAspects)).left();
            } else if (tName.equals("trigger")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("事件ID", "Event ID")); params.add(f).width(200);
            } else if (tName.equals("end_game")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("结局ID", "Ending ID")); params.add(f).width(200);
            } else if (tName.equals("unlock_verb")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("槽位ID", "Slot ID")); params.add(f).width(200);
            } else if (tName.equals("play_music")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("路径", "Path")); params.add(f).width(200);
            } else if (tName.equals("modify_aspects")) {
                TextField f = new TextField("", ui.textFieldStyle()); f.setName("target"); f.setMessageText(t("卡牌ID", "Card ID")); params.add(f).width(120).top().padRight(8);
                Map<String, Integer> exAspects = ex instanceof Actions.ModifyAspects ? ((Actions.ModifyAspects) ex).aspects : null;
                params.add(createAspectGroupBox("aspects", exAspects)).left();
            } else if (tName.equals("random_create") || tName.equals("random_trigger") || tName.equals("random_do")) {
                Table optsBox = new Table();
                optsBox.setName("optsBox");

                if (ex instanceof Actions.RandomCreate) {
                    for (Actions.RandomCreate.Option o : ((Actions.RandomCreate) ex).options) addRandomRow(optsBox, tName, o.target, o.target, String.valueOf(o.weight), null);
                } else if (ex instanceof Actions.RandomTrigger) {
                    for (Actions.RandomTrigger.Option o : ((Actions.RandomTrigger) ex).options) addRandomRow(optsBox, tName, o.event, o.event, String.valueOf(o.weight), null);
                } else if (ex instanceof Actions.RandomDo) {
                    for (Actions.RandomDo.Option o : ((Actions.RandomDo) ex).options) addRandomRow(optsBox, tName, "", "", String.valueOf(o.weight), o.action);
                }

                // 没有数据时,默认给一个空行
                if (optsBox.getChildren().isEmpty()) addRandomRow(optsBox, tName, "", "", "1", null);

                TextButton addOpt = createActionBtn(t("+ 添加项", "+ Add Option"), () -> addRandomRow(optsBox, tName, "", "", "1", null));
                params.add(optsBox).left().row();
                params.add(addOpt).left().padTop(5);
            }

            // 把已有动作的简单参数(目标、数量、概率……)填回输入框。
            // 以前这里没有做,打开已有事件再点"保存修改"会把这些参数清空。
            if (ex != null) fillActionParams(params, ex);
        };

        typeBox.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { updateParams.run(); } });

        Table buttons = new Table();
        buttons.add(new Label(t("成功:", "Success:"), ui.labelStyle(Color.GREEN)));
        TextButton addS = createActionBtn("+", () -> addActionBlock(successBox, null));
        buttons.add(addS).padRight(10);
        buttons.add(new Label(t("失败:", "Fail:"), ui.labelStyle(Color.RED)));
        TextButton addF = createActionBtn("+", () -> addActionBlock(failBox, null));
        buttons.add(addF);

        block.add(buttons).row();
        block.add(successBox).left().row();
        block.add(failBox).left().row();

        container.add(block).left().padBottom(10).row();

        if (currentExistingRef[0] != null) {
            if (currentExistingRef[0] instanceof Actions.Create) typeBox.setSelected("create");
            else if (currentExistingRef[0] instanceof Actions.Consume) typeBox.setSelected("consume");
            else if (currentExistingRef[0] instanceof Actions.Drain) typeBox.setSelected("drain");
            else if (currentExistingRef[0] instanceof Actions.Check) typeBox.setSelected("check");
            else if (currentExistingRef[0] instanceof Actions.Chance) typeBox.setSelected("chance");
            else if (currentExistingRef[0] instanceof Actions.TriggerEvent) typeBox.setSelected("trigger");
            else if (currentExistingRef[0] instanceof Actions.EndGame) typeBox.setSelected("end_game");
            else if (currentExistingRef[0] instanceof Actions.UnlockVerb) typeBox.setSelected("unlock_verb");
            else if (currentExistingRef[0] instanceof Actions.PlayMusic) typeBox.setSelected("play_music");
            else if (currentExistingRef[0] instanceof Actions.ModifyAspects) typeBox.setSelected("modify_aspects");
            else if (currentExistingRef[0] instanceof Actions.RandomCreate) typeBox.setSelected("random_create");
            else if (currentExistingRef[0] instanceof Actions.RandomTrigger) typeBox.setSelected("random_trigger");
            else if (currentExistingRef[0] instanceof Actions.RandomDo) typeBox.setSelected("random_do");
            updateParams.run();
            // 把已有动作的成功/失败分支重建回界面(以前只选中类型,分支在保存时会丢)
            for (Action child : currentExistingRef[0].onSuccess) addActionBlock(successBox, child);
            for (Action child : currentExistingRef[0].onFail) addActionBlock(failBox, child);
        } else {
            typeBox.setSelected("create");
            updateParams.run();   // 新建的块:确保一定有输入框(不依赖 setSelected 是否触发了事件)
        }
    }

    // 👈 核心新增：生成带权重的动态行
    private void addRandomRow(Table box, String type, String target, String event, String weight, Action nestedAction) {
        Table row = new Table();
        row.padBottom(5);

        if (type.equals("random_create")) {
            TextField idF = new TextField(target, ui.textFieldStyle()); idF.setName("target"); idF.setMessageText(t("卡牌ID", "Card ID")); row.add(idF).width(150).padRight(5);
            TextField wF = new TextField(weight, ui.textFieldStyle()); wF.setName("weight"); wF.setMessageText(t("权重", "Weight")); row.add(wF).width(80).padRight(5);
        } else if (type.equals("random_trigger")) {
            TextField idF = new TextField(event, ui.textFieldStyle()); idF.setName("event"); idF.setMessageText(t("事件ID", "Event ID")); row.add(idF).width(150).padRight(5);
            TextField wF = new TextField(weight, ui.textFieldStyle()); wF.setName("weight"); wF.setMessageText(t("权重", "Weight")); row.add(wF).width(80).padRight(5);
        } else if (type.equals("random_do")) {
            TextField wF = new TextField(weight, ui.textFieldStyle()); wF.setName("weight"); wF.setMessageText(t("权重", "Weight")); row.add(wF).width(80).padRight(5);
            Table actContainer = new Table();
            actContainer.setName("actContainer");
            // 以前只有"导入已有数据"时才有动作块,新建的选项里是空的、也没有任何按钮,所以没法填事件。
            // 现在每个选项一定带一个动作块(默认类型 create),可以在下拉框里改成 trigger 等并填写。
            addActionBlock(actContainer, nestedAction);
            row.add(actContainer).padRight(5);
        }

        TextButton delBtn = createActionBtn("-", row::remove);
        row.add(delBtn).size(30, 30).top();
        box.add(row).left().row();
    }

    /** 已有动作的类型是否对应下拉框里的 type 字符串。 */
    private boolean matchesType(Action a, String type) {
        if (a == null) return false;
        switch (type) {
            case "create": return a instanceof Actions.Create;
            case "consume": return a instanceof Actions.Consume;
            case "drain": return a instanceof Actions.Drain;
            case "check": return a instanceof Actions.Check;
            case "chance": return a instanceof Actions.Chance;
            case "trigger": return a instanceof Actions.TriggerEvent;
            case "end_game": return a instanceof Actions.EndGame;
            case "unlock_verb": return a instanceof Actions.UnlockVerb;
            case "play_music": return a instanceof Actions.PlayMusic;
            case "modify_aspects": return a instanceof Actions.ModifyAspects;
            case "random_create": return a instanceof Actions.RandomCreate;
            case "random_trigger": return a instanceof Actions.RandomTrigger;
            case "random_do": return a instanceof Actions.RandomDo;
            default: return false;
        }
    }

    /** 把已有动作的简单参数填回 updateParams 生成的输入框(词条组和随机选项各自单独处理)。 */
    private void fillActionParams(Table params, Action act) {
        if (act instanceof Actions.Create) {
            Actions.Create x = (Actions.Create) act;
            setField(params, "target", x.target);
            setField(params, "amount", String.valueOf(x.amount));
        } else if (act instanceof Actions.Consume) {
            Actions.Consume x = (Actions.Consume) act;
            setField(params, "target", x.target);
            setField(params, "amount", String.valueOf(x.amount));
        } else if (act instanceof Actions.Drain) {
            Actions.Drain x = (Actions.Drain) act;
            setField(params, "target", x.target);
            setField(params, "amount", String.valueOf(x.amount));
        } else if (act instanceof Actions.Chance) {
            setField(params, "percent", String.valueOf(((Actions.Chance) act).percent));
        } else if (act instanceof Actions.TriggerEvent) {
            setField(params, "target", ((Actions.TriggerEvent) act).event);
        } else if (act instanceof Actions.EndGame) {
            setField(params, "target", ((Actions.EndGame) act).ending);
        } else if (act instanceof Actions.UnlockVerb) {
            setField(params, "target", ((Actions.UnlockVerb) act).verb);
        } else if (act instanceof Actions.PlayMusic) {
            setField(params, "target", ((Actions.PlayMusic) act).track);
        } else if (act instanceof Actions.ModifyAspects) {
            setField(params, "target", ((Actions.ModifyAspects) act).target);
        }
    }

    private void setField(Table params, String name, String text) {
        Actor f = params.findActor(name);
        if (f instanceof TextField) ((TextField) f).setText(text == null ? "" : text);
    }

    // ================= 词条组编辑器:[词条1][数量] [词条2][数量] ... [+ 词条] =================

    /** 创建一个词条组编辑器。名字用于解析时查找;existing 非空时按它预填,否则给一个空行。 */
    private Table createAspectGroupBox(String name, Map<String, Integer> existing) {
        Table box = new Table();
        box.setName(name);
        box.left().top();

        Table rows = new Table();
        rows.setName("aspectRows");
        rows.left();
        box.add(rows).left().row();

        TextButton addBtn = createActionBtn(t("+ 词条", "+ Aspect"), () -> addAspectGroupRow(rows, "", "1"));
        box.add(addBtn).left().padTop(3);

        if (existing != null && !existing.isEmpty()) {
            for (Map.Entry<String, Integer> e : existing.entrySet()) {
                addAspectGroupRow(rows, e.getKey(), String.valueOf(e.getValue()));
            }
        } else {
            addAspectGroupRow(rows, "", "1");
        }
        return box;
    }

    private void addAspectGroupRow(Table rows, String key, String amt) {
        Table row = new Table();
        row.padBottom(3);
        TextField k = new TextField(key, ui.textFieldStyle()); k.setName("aKey"); k.setMessageText(t("词条", "Aspect")); row.add(k).width(120).padRight(4);
        TextField a = new TextField(amt, ui.textFieldStyle()); a.setName("aAmt"); a.setMessageText(t("数量", "Amount")); row.add(a).width(55).padRight(4);
        TextButton del = createActionBtn("-", row::remove);
        row.add(del).size(28, 28);
        rows.add(row).left().row();
    }

    private HashMap<String, Integer> parseAspectGroupBox(Table box) {
        HashMap<String, Integer> map = new HashMap<>();
        if (box == null) return map;
        Table rows = box.findActor("aspectRows");
        if (rows == null) return map;
        for (Actor c : rows.getChildren()) {
            if (c instanceof Table && !(c instanceof Button)) {
                Table row = (Table) c;
                Actor k = row.findActor("aKey");
                Actor a = row.findActor("aAmt");
                if (k instanceof TextField && a instanceof TextField) {
                    String key = ((TextField) k).getText().trim();
                    if (!key.isEmpty()) map.put(key, parseInt(((TextField) a).getText()));
                }
            }
        }
        return map;
    }

    private ArrayList<Action> parseActionTree(Table container) {
        ArrayList<Action> list = new ArrayList<>();
        for (Actor c : container.getChildren()) {
            if (c instanceof Table) {
                Action a = parseActionBlock((Table) c);
                if (a != null) list.add(a);
            }
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
            c.aspects = parseAspectGroupBox(block.findActor("aspects"));
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
            c.aspects = parseAspectGroupBox(block.findActor("aspects"));
            a = c;
        } else if (type.equals("random_create")) {
            Actions.RandomCreate c = new Actions.RandomCreate();
            Table optsBox = block.findActor("optsBox");
            if (optsBox != null) {
                for (Actor r : optsBox.getChildren()) {
                    if (r instanceof Table && !(r instanceof Button)) {
                        Table row = (Table) r;
                        if (row.getChildren().size >= 2) {
                            Actions.RandomCreate.Option opt = new Actions.RandomCreate.Option();
                            opt.target = ((TextField) row.findActor("target")).getText().trim();
                            opt.weight = parseInt(((TextField) row.findActor("weight")).getText());
                            if (!opt.target.isEmpty()) c.options.add(opt);
                        }
                    }
                }
            }
            a = c;
        } else if (type.equals("random_trigger")) {
            Actions.RandomTrigger c = new Actions.RandomTrigger();
            Table optsBox = block.findActor("optsBox");
            if (optsBox != null) {
                for (Actor r : optsBox.getChildren()) {
                    if (r instanceof Table && !(r instanceof Button)) {
                        Table row = (Table) r;
                        if (row.getChildren().size >= 2) {
                            Actions.RandomTrigger.Option opt = new Actions.RandomTrigger.Option();
                            opt.event = ((TextField) row.findActor("event")).getText().trim();
                            opt.weight = parseInt(((TextField) row.findActor("weight")).getText());
                            if (!opt.event.isEmpty()) c.options.add(opt);
                        }
                    }
                }
            }
            a = c;
        } else if (type.equals("random_do")) {
            Actions.RandomDo c = new Actions.RandomDo();
            Table optsBox = block.findActor("optsBox");
            if (optsBox != null) {
                for (Actor r : optsBox.getChildren()) {
                    if (r instanceof Table && !(r instanceof Button)) {
                        Table row = (Table) r;
                        if (row.getChildren().size >= 2) {
                            Actions.RandomDo.Option opt = new Actions.RandomDo.Option();
                            opt.weight = parseInt(((TextField) row.findActor("weight")).getText());
                            Table actContainer = row.findActor("actContainer");
                            if (actContainer != null && actContainer.getChildren().size > 0) {
                                opt.action = parseActionBlock((Table) actContainer.getChild(0));
                            }
                            if (opt.action != null) c.options.add(opt);
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
        json.addClassTag("random_trigger", Actions.RandomTrigger.class);
        json.addClassTag("random_do", Actions.RandomDo.class);

        json.setElementType(Element.class, "aspects", Integer.class);
        json.setElementType(Element.class, "hiddenAspects", Integer.class);
        json.setElementType(Element.class, "triggers", Element.Trigger.class);
        json.setElementType(EventDef.class, "input", Integer.class);
        json.setElementType(EventDef.class, "slots", SlotDef.class);
        json.setElementType(EventDef.class, "action", Action.class);
        json.setElementType(EventDef.class, "products", EventDef.Product.class);
        json.setElementType(Action.class, "onSuccess", Action.class);
        json.setElementType(Action.class, "onFail", Action.class);
        json.setElementType(Actions.RandomCreate.class, "options", Actions.RandomCreate.Option.class);
        json.setElementType(Actions.RandomTrigger.class, "options", Actions.RandomTrigger.Option.class);
        json.setElementType(Actions.RandomDo.class, "options", Actions.RandomDo.Option.class);
        json.setElementType(Actions.RandomDo.Option.class, "action", Action.class);

        json.setOutputType(JsonWriter.OutputType.json);
        return json;
    }

    private void saveModToDisk() {
        currentModName = modNameField.getText().trim();
        if (currentModName.isEmpty()) currentModName = "MyCustomMod";

        FileHandle dir = Gdx.files.local("mods/" + currentModName + "/");
        dir.mkdirs();

        Json json = createJson();

        if (!draftElements.isEmpty()) {
            dir.child("elements.json").writeString(json.prettyPrint(draftElements), false, "UTF-8");
        }
        if (!draftEvents.isEmpty()) {
            dir.child("events.json").writeString(json.prettyPrint(draftEvents), false, "UTF-8");
        }

        if (!draftEndings.isEmpty()) {
            json.setTypeName(null);
            try {
                dir.child("endings.json").writeString(json.prettyPrint(draftEndings), false, "UTF-8");
            } finally {
                json.setTypeName("type");
            }
        }

        if (!draftVerbs.isEmpty()) {
            dir.child("verbs.json").writeString(json.prettyPrint(draftVerbs), false, "UTF-8");
        }
        if (!draftScenarios.isEmpty()) {
            dir.child("scenarios.json").writeString(json.prettyPrint(draftScenarios), false, "UTF-8");
        }

        dir.child("config.json").writeString(json.prettyPrint(draftConfig), false, "UTF-8");

        Json textJson = new Json();
        textJson.setTypeName(null);
        textJson.setOutputType(JsonWriter.OutputType.json);

        draftZh.values().removeIf(val -> val == null || val.trim().isEmpty());
        draftEn.values().removeIf(val -> val == null || val.trim().isEmpty());

        if (!draftZh.isEmpty()) {
            dir.child("text_zh.json").writeString(textJson.prettyPrint(draftZh), false, "UTF-8");
        }
        if (!draftEn.isEmpty()) {
            dir.child("text_en.json").writeString(textJson.prettyPrint(draftEn), false, "UTF-8");
        }

        showToast(t("保存成功！", "Saved successfully!"), Color.GREEN);
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) { ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f); stage.act(delta); stage.draw(); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
