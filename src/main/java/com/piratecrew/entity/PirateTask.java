package com.piratecrew.entity;

/** Jobs a crew pirate can be sent to do near where it was given the order. */
public enum PirateTask {
    NONE("Nothing", ""),
    MINE("Mining ore", "Mines exposed ore nearby. Needs a pickaxe that can break the ore."),
    FARM("Farming", "Harvests fully grown crops nearby and replants them."),
    FISH("Fishing", "Fishes from the nearest shore. Needs a fishing rod."),
    WOOD("Chopping wood", "Fells nearby trees and replants saplings. Works faster with an axe.");

    public final String label;
    public final String description;

    PirateTask(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public static PirateTask byId(int id) {
        PirateTask[] v = values();
        return id >= 0 && id < v.length ? v[id] : NONE;
    }
}
