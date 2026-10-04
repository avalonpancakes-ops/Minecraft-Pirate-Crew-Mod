package com.piratecrew.client;

import com.piratecrew.network.CrewSyncPacket;

/** Latest crew snapshot the server sent us. */
public class ClientCrewData {
    private static CrewSyncPacket latest;
    private static int version = 0;

    public static void update(CrewSyncPacket p) {
        latest = p;
        version++;
    }

    public static CrewSyncPacket get() {
        return latest;
    }

    public static int version() {
        return version;
    }
}
