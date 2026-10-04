package com.piratecrew.crew;

import net.minecraft.ChatFormatting;

public enum CrewRole {
    CAPTAIN("Captain", ChatFormatting.GOLD),
    VICE_CAPTAIN("Vice Captain", ChatFormatting.YELLOW),
    MEMBER("Deckhand", ChatFormatting.WHITE);

    public final String title;
    public final ChatFormatting color;

    CrewRole(String title, ChatFormatting color) {
        this.title = title;
        this.color = color;
    }

    public static CrewRole byId(int id) {
        CrewRole[] v = values();
        return id >= 0 && id < v.length ? v[id] : MEMBER;
    }
}
