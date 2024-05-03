package com.bennero.server.message;

import static com.bennero.common.Constants.BACKGROUND_IMAGE_STRING_NUM_BYTES;
import static com.bennero.common.messages.FileDataPositions.*;
import static com.bennero.common.messages.MessageUtils.readInt;
import static com.bennero.common.messages.MessageUtils.readString;

public class FileMessage {
    private final int size;
    private final String name;
    private final byte type;

    private FileMessage(int size, String name, byte type) {
        this.size = size;
        this.name = name;
        this.type = type;
    }

    public static FileMessage processConnectionRequestMessageData(byte[] bytes) {
        final int size = readInt(bytes, SIZE_POS);
        final byte type = bytes[TYPE_POS];
        final String name = readString(bytes, NAME_POS, BACKGROUND_IMAGE_STRING_NUM_BYTES);
        return new FileMessage(size, name, type);
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public byte getType() {
        return type;
    }
}
