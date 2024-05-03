package com.bennero.server.message;

import com.bennero.common.messages.VersionParityDataPositions;

public class VersionParityMessage {
    private final byte majorVersion;
    private final byte minorVersion;
    private final byte patchVersion;

    private VersionParityMessage(byte majorVersion, byte minorVersion, byte patchVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
    }

    public static VersionParityMessage processConnectionRequestMessageData(byte[] bytes) {
        final int majorVersion = bytes[VersionParityDataPositions.MAJOR_VERSION_POS] & 0xFF;
        final int minorVersion = bytes[VersionParityDataPositions.MINOR_VERSION_POS] & 0xFF;
        final int patchVersion = bytes[VersionParityDataPositions.PATCH_VERSION_POS] & 0xFF;
        return new VersionParityMessage((byte) majorVersion, (byte) minorVersion, (byte) patchVersion);
    }

    public byte getMajorVersion() {
        return majorVersion;
    }

    public byte getMinorVersion() {
        return minorVersion;
    }

    public byte getPatchVersion() {
        return patchVersion;
    }
}
