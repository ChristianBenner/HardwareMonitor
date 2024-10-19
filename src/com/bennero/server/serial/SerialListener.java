package com.bennero.server.serial;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.*;
import com.bennero.server.Identity;
import com.bennero.server.event.*;
import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.event.EventHandler;

import java.util.UUID;

import static com.bennero.server.Version.*;

public class SerialListener {
    private static final String LOGGER_TAG = SerialListener.class.getSimpleName();

    private String port;
    private SerialPort serialPort;
    private boolean connected;
    private UUID connectedUUID;

    private EventHandler disconnectedEvent;
    private EventHandler<PageSetupEvent> pageMessageReceived;
    private EventHandler<SensorSetupEvent> sensorMessageReceived;
    private EventHandler<SensorDataEvent> sensorDataMessageReceived;
    private EventHandler<RemovePageEvent> removePageMessageReceived;
    private EventHandler<RemoveSensorEvent> removeSensorMessageReceived;
    private EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived;
    private EventHandler<FileTransferEvent> fileTransferEventHandler;

    public SerialListener(String port,
                          EventHandler disconnectedEvent,
                          EventHandler<PageSetupEvent> pageMessageReceived,
                          EventHandler<SensorSetupEvent> sensorMessageReceived,
                          EventHandler<RemovePageEvent> removePageMessageReceived,
                          EventHandler<SensorDataEvent> sensorDataMessageReceived,
                          EventHandler<RemoveSensorEvent> removeSensorMessageReceived,
                          EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived,
                          EventHandler<FileTransferEvent> fileTransferEventHandler) {
        this.port = port;
        this.disconnectedEvent = disconnectedEvent;
        this.pageMessageReceived = pageMessageReceived;
        this.sensorMessageReceived = sensorMessageReceived;
        this.removePageMessageReceived = removePageMessageReceived;
        this.sensorDataMessageReceived = sensorDataMessageReceived;
        this.removeSensorMessageReceived = removeSensorMessageReceived;
        this.sensorTransformationMessageReceived = sensorTransformationMessageReceived;
        this.fileTransferEventHandler = fileTransferEventHandler;
        this.connectedUUID = null;

        serialPort = SerialPort.getCommPort(port);
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Attempting to use serial port: " + serialPort.getSystemPortName());
        if(!serialPort.openPort()) {
            return;
        }

        Logger.log(LogLevel.INFO, LOGGER_TAG, "Opened serial port: " + serialPort.getSystemPortName());
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.EVEN_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
    }

    private boolean handshake(byte[] bytes, EventHandler<SerialConnectionEvent> handler) {
        VersionParityMessage in = new VersionParityMessage(bytes);

        // Announce connection request
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Received version parity message");

        // Check if we are already connected to a different editor. It's valid to respond to the connected editor
        // because it may be disconnected from its side and trying to re-connect.
        boolean connectedToDifferentEditor = connected && connectedUUID != null && !connectedUUID.equals(in.getSenderUuid());
        if (connectedToDifferentEditor) {
            String rejectionReason = "Monitor is already connected to a different editor";
            VersionParityResponseMessage out = new VersionParityResponseMessage(Identity.getMyUuid(), true,
                    VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, false, rejectionReason);
            serialPort.writeBytes(out.write(), Message.NUM_BYTES);
            return false;
        }

        // Is the version compatible
        MessageUtils.Compatibility compatibility = MessageUtils.isVersionCompatible(VERSION_MAJOR, VERSION_MINOR,
                in.getVersionMajor(), in.getVersionMinor());
        boolean compatible = compatibility == MessageUtils.Compatibility.COMPATIBLE;
        if(!compatible) {
            String formattedErr = String.format("Version mismatch: Editor[%d.%d.%d], Monitor[%d.%d.%d]",
                    in.getVersionMajor(), in.getVersionMinor(), in.getVersionPatch(), VERSION_MAJOR, VERSION_MINOR,
                    VERSION_PATCH);
            VersionParityResponseMessage out = new VersionParityResponseMessage(Identity.getMyUuid(), true,
                    VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, false, formattedErr);
            serialPort.writeBytes(out.write(), Message.NUM_BYTES);

            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, formattedErr));});
            return false;
        }

        boolean alreadyConnected = connectedUUID != null && connectedUUID.equals(in.getSenderUuid());
        if (!alreadyConnected) {
            connectedUUID = in.getSenderUuid();
            connected = true;
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Editor connected: " + connectedUUID.toString());
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(true, ""));});
        } else {
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Editor already connected: " + connectedUUID.toString());
        }

        VersionParityResponseMessage out = new VersionParityResponseMessage(Identity.getMyUuid(), true,
                VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, true, "");
        serialPort.writeBytes(out.write(), Message.NUM_BYTES);

        return true;
    }

    private boolean readMessage(byte[] bytes) {
        boolean valid = true;
        byte type = Message.getType(bytes);
       // if (type != MessageType.SENSOR_UPDATE) {
            Logger.logf(LogLevel.DEBUG, LOGGER_TAG, "Received message [Type: %s]", MessageType.asString(type));
       // }

        switch (type) {
            case MessageType.SENSOR_UPDATE:
                sensorDataMessageReceived.handle(new SensorDataEvent(new SensorUpdateMessage(bytes)));
                break;
            case MessageType.PAGE_CREATE:
                pageMessageReceived.handle(new PageSetupEvent(new PageCreateMessage(bytes)));
                break;
            case MessageType.SENSOR_CREATE:
                sensorMessageReceived.handle(new SensorSetupEvent(new SensorCreateMessage(bytes)));
                break;
            case MessageType.PAGE_REMOVE:
                removePageMessageReceived.handle(new RemovePageEvent(new PageRemoveMessage(bytes)));
                break;
            case MessageType.SENSOR_REMOVE:
                removeSensorMessageReceived.handle(new RemoveSensorEvent(new SensorRemoveMessage(bytes)));
                break;
            case MessageType.HEARTBEAT:
                valid = handleHeartbeat(new HeartbeatMessage(bytes));
                break;
            case MessageType.SENSOR_TRANSFORM:
                sensorTransformationMessageReceived.handle(new SensorTransformationEvent(new SensorTransformationMessage(bytes)));
                break;
            case MessageType.CONNECTION_REQUEST:
         //       handleConnectionRequest(processConnectionRequestMessageData(bytes));
                break;
            case MessageType.DISCONNECT:
//                Logger.log(LogLevel.DEBUG, CLASS_NAME, "Received disconnect message");
//                handleDisconnect();
                break;
            case MessageType.FILE_TRANSFER:
                FileTransferMessage fileTransferMessage = new FileTransferMessage(bytes);

                // The next read will be the file bytes so read here
                int numBytes = fileTransferMessage.getNumBytes();
                byte[] fileBytes = new byte[numBytes];
                int numRead = serialPort.readBytes(fileBytes, numBytes);
                if (numRead < numBytes) {
                    Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected file read amount on serial port: " +  numRead);
                }

                fileTransferEventHandler.handle(new FileTransferEvent(fileBytes, fileTransferMessage.getFilename(), fileTransferMessage.getTransferType()));
                break;
        }

        return valid;
    }

    private boolean handleHeartbeat(HeartbeatMessage heartbeatMessage) {
        if (!connectedUUID.equals(heartbeatMessage.getSenderUuid())) {
            return false;
        }

        return true;
    }

    public void connect(EventHandler<SerialConnectionEvent> handler) {
        if(!serialPort.isOpen()) {
            return;
        }

        new Thread(() -> {
            // Handshake successful now listen for messages from editor
            while (serialPort.isOpen()) {
                read(handler);
            }
        }).start();
    }

    private void read(EventHandler<SerialConnectionEvent> handler) {
        byte[] bytes = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(bytes, Message.NUM_BYTES);
        if (numRead == -1) {
            Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Failed to read from serial port");
            connected = false;
            connectedUUID = null;
            return;
        }

        if (numRead < Message.NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
        }

        boolean valid = Message.isValid(bytes);
        if (!valid) {
            // err, ask for re-send
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Invalid checksum on received message");

            // Todo: when this happens we should flush the entire serial port buffer (throw away) as there may
            //  be some bad data there and respond to the hardware monitor editor stating bad message

            ConfirmationMessage out = new ConfirmationMessage(Identity.getMyUuid(), false);
            serialPort.writeBytes(out.write(), Message.NUM_BYTES);
            return;
        }

        if (Message.getType(bytes) == MessageType.VERSION_PARITY) {
            handshake(bytes, handler);
            return;
        }

        // Check if the message came from the monitor we are connected to
        UUID senderUuid = Message.getSenderUUID(bytes);
        if (connectedUUID == null || !senderUuid.equals(connectedUUID)) {
            Logger.logf(LogLevel.WARNING, LOGGER_TAG, "Warning, received message from device that is not connected [From: %s] [Connected: %s]", senderUuid.toString(), connectedUUID.toString());
            // Do not reply in this scenario
            return;
        }

        valid = readMessage(bytes);
        ConfirmationMessage out = new ConfirmationMessage(Identity.getMyUuid(), valid);
        serialPort.writeBytes(out.write(), Message.NUM_BYTES);
    }
}
