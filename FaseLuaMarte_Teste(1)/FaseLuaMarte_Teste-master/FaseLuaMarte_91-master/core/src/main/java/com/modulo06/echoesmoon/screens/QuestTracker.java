package com.modulo06.echoesmoon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import java.util.HashMap;
import java.util.Map;

public class QuestTracker {

    private static QuestTracker instance;

    public static final String STAGE_LUA = "LUA";
    public static final String STAGE_MARTE = "MARTE";
    public static final String STAGE_TITA = "TITA";
    public static final String STAGE_CALISTO = "CALISTO";
    public static final String STAGE_AHARIN = "AHARIN";

    private String currentStage = STAGE_LUA;
    private float o2 = 100f;
    private float energy = 100f;
    private int ammo = 20;

    private int moonFragments = 0;
    private int marsCrystals = 0;
    private int titanParts = 0;

    private boolean marsScientistTalked = false;
    private boolean moonMissionComplete = false;
    private boolean marsMissionComplete = false;
    private boolean titanMissionComplete = false;
    private boolean callistoMissionComplete = false;

    private boolean weaponBuilt = false;
    private boolean titanPortalCreated = false;

    // Inventário de Chaves
    private Array<String> inventory = new Array<>();

    // Requisito 22 (Bancada de crafting): materiais e itens craftados,
    // guardados com quantidade (diferente do "inventory" acima, que so
    // guarda chaves como uma flag de posse).
    private Map<String, Integer> materials = new HashMap<>();

    private QuestTracker() {
        loadProgress();
    }

    public static QuestTracker getInstance() {
        if (instance == null) {
            instance = new QuestTracker();
        }
        return instance;
    }

    public void loadProgress() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        currentStage = prefs.getString("currentStage", STAGE_LUA);
        o2 = prefs.getFloat("o2", 100f);
        energy = prefs.getFloat("energy", 100f);
        ammo = prefs.getInteger("ammo", 20);

        moonFragments = prefs.getInteger("moonFragments", 0);
        marsCrystals = prefs.getInteger("marsCrystals", 0);
        titanParts = prefs.getInteger("titanParts", 0);

        marsScientistTalked = prefs.getBoolean("marsScientistTalked", false);
        moonMissionComplete = prefs.getBoolean("moonComplete", false);
        marsMissionComplete = prefs.getBoolean("marsComplete", false);
        titanMissionComplete = prefs.getBoolean("titanComplete", false);
        callistoMissionComplete = prefs.getBoolean("callistoComplete", false);

        weaponBuilt = prefs.getBoolean("weaponBuilt", false);
        titanPortalCreated = prefs.getBoolean("titanPortalCreated", false);

        inventory.clear();
        String invStr = prefs.getString("inventory", "");
        if (!invStr.isEmpty()) {
            for (String item : invStr.split(",")) {
                if (!item.trim().isEmpty()) {
                    inventory.add(item.trim());
                }
            }
        }

        materials.clear();
        String matStr = prefs.getString("materials", "");
        if (!matStr.isEmpty()) {
            for (String pair : matStr.split(",")) {
                if (pair.trim().isEmpty()) continue;
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    try {
                        materials.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void saveProgress() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.putString("currentStage", currentStage);
        prefs.putFloat("o2", o2);
        prefs.putFloat("energy", energy);
        prefs.putInteger("ammo", ammo);

        prefs.putInteger("moonFragments", moonFragments);
        prefs.putInteger("marsCrystals", marsCrystals);
        prefs.putInteger("titanParts", titanParts);

        prefs.putBoolean("marsScientistTalked", marsScientistTalked);
        prefs.putBoolean("moonComplete", moonMissionComplete);
        prefs.putBoolean("marsComplete", marsMissionComplete);
        prefs.putBoolean("titanComplete", titanMissionComplete);
        prefs.putBoolean("callistoComplete", callistoMissionComplete);

        prefs.putBoolean("weaponBuilt", weaponBuilt);
        prefs.putBoolean("titanPortalCreated", titanPortalCreated);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inventory.size; i++) {
            sb.append(inventory.get(i));
            if (i < inventory.size - 1) sb.append(",");
        }
        prefs.putString("inventory", sb.toString());

        StringBuilder matSb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) continue;
            if (!first) matSb.append(",");
            matSb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        prefs.putString("materials", matSb.toString());

        prefs.flush();
    }

    public void saveCheckpoint(String stage, float o2, float energy, int ammo) {
        this.currentStage = stage;
        this.o2 = o2;
        this.energy = energy;
        this.ammo = ammo; // Garantido como int sem perda de conversão
        saveProgress();
    }

    public void resetProgress() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        prefs.clear();
        prefs.flush();
        currentStage = STAGE_LUA;
        o2 = 100f;
        energy = 100f;
        ammo = 20;
        moonFragments = 0;
        marsCrystals = 0;
        titanParts = 0;
        marsScientistTalked = false;
        moonMissionComplete = false;
        marsMissionComplete = false;
        titanMissionComplete = false;
        callistoMissionComplete = false;
        weaponBuilt = false;
        titanPortalCreated = false;
        inventory.clear();
        materials.clear();
    }

    public boolean hasSave() {
        Preferences prefs = Gdx.app.getPreferences("EchoesMoonSave");
        return prefs.contains("currentStage");
    }

    public void addItem(String item) {
        if (!inventory.contains(item, false)) {
            inventory.add(item);
            saveProgress();
        }
    }

    public boolean hasItem(String item) {
        return inventory.contains(item, false);
    }

    // ===== Requisito 22: materiais/itens com quantidade (crafting) =====

    public int getMaterialCount(String name) {
        Integer v = materials.get(name);
        return v == null ? 0 : v;
    }

    public void addMaterial(String name, int qty) {
        if (qty <= 0) return;
        materials.put(name, getMaterialCount(name) + qty);
        saveProgress();
    }

    /** Consome a quantidade pedida. So chama isto depois de confirmar com hasMaterial(). */
    public void consumeMaterial(String name, int qty) {
        int current = getMaterialCount(name);
        int updated = Math.max(0, current - qty);
        materials.put(name, updated);
        saveProgress();
    }

    public boolean hasMaterial(String name, int qty) {
        return getMaterialCount(name) >= qty;
    }

    public Map<String, Integer> getAllMaterials() {
        return materials;
    }

    // Getters e Setters
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String s) { this.currentStage = s; saveProgress(); }

    public float getO2() { return o2; }
    public void setO2(float o2) { this.o2 = o2; }

    public float getEnergy() { return energy; }
    public void setEnergy(float energy) { this.energy = energy; }

    public int getAmmo() { return ammo; }
    public void setAmmo(int ammo) { this.ammo = ammo; }

    public int getMoonFragments() { return moonFragments; }
    public void setMoonFragments(int f) { this.moonFragments = f; saveProgress(); }

    public int getMarsCrystals() { return marsCrystals; }
    public void setMarsCrystals(int c) { this.marsCrystals = c; saveProgress(); }

    public int getTitanParts() { return titanParts; }
    public void setTitanParts(int p) { this.titanParts = p; saveProgress(); }

    public boolean isMarsScientistTalked() { return marsScientistTalked; }
    public void setMarsScientistTalked(boolean t) { this.marsScientistTalked = t; saveProgress(); }

    public boolean isMoonMissionComplete() { return moonMissionComplete; }
    public void setMoonMissionComplete(boolean c) { this.moonMissionComplete = c; saveProgress(); }

    public boolean isMarsMissionComplete() { return marsMissionComplete; }
    public void setMarsMissionComplete(boolean c) { this.marsMissionComplete = c; saveProgress(); }

    public boolean isTitanMissionComplete() { return titanMissionComplete; }
    public void setTitanMissionComplete(boolean c) { this.titanMissionComplete = c; saveProgress(); }

    public boolean isCallistoMissionComplete() { return callistoMissionComplete; }
    public void setCallistoMissionComplete(boolean c) { this.callistoMissionComplete = c; saveProgress(); }

    public boolean isWeaponBuilt() { return weaponBuilt; }
    public void setWeaponBuilt(boolean b) { this.weaponBuilt = b; saveProgress(); }

    public boolean isTitanPortalCreated() { return titanPortalCreated; }
    public void setTitanPortalCreated(boolean b) { this.titanPortalCreated = b; saveProgress(); }
}
