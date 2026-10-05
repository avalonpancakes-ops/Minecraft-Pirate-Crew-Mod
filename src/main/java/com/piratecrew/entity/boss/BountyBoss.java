package com.piratecrew.entity.boss;

/** A Sundered Sea boss: worth bounty to whoever fells it, and introduced with a title when it appears. */
public interface BountyBoss {
    int bountyValue();

    /** The boss's epithet, shown under its name when it arrives. */
    default String epithet() {
        return "";
    }

    /** Ribbon colour for titles and toasts. */
    default int ribbonColor() {
        return 0xE04040;
    }
}
