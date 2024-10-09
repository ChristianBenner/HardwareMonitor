package com.bennero.server;

import java.util.UUID;

public class Identity {
    private static UUID myUuid;

    public static UUID getMyUuid() {
        if (myUuid == null) {
            myUuid = UUID.randomUUID();
        }

        return myUuid;
    }
}
