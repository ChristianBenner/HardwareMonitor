package com.bennero.server.message;

import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.VersionParityResponseDataPositions;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;
import static com.bennero.server.Version.*;

public class VersionParityResponseMessage {
    public static byte[] create(boolean accepted) {
        byte[] message = new byte[MESSAGE_NUM_BYTES];
        message[MESSAGE_TYPE_POS] = MessageType.VERSION_PARITY_RESPONSE_MESSAGE;
        message[VersionParityResponseDataPositions.MAJOR_VERSION_POS] = VERSION_MAJOR;
        message[VersionParityResponseDataPositions.MINOR_VERSION_POS] = VERSION_MINOR;
        message[VersionParityResponseDataPositions.PATCH_VERSION_POS] = VERSION_PATCH;
        message[VersionParityResponseDataPositions.ACCEPTED_POS] = accepted ? (byte)1 : (byte)0;
        return message;
    }
}
