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
        connected = serialPort.openPort();
        if(!connected) {
            return;
        }

        Logger.log(LogLevel.INFO, LOGGER_TAG, "Opened serial port: " + serialPort.getSystemPortName());
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.EVEN_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
    }

    private boolean handshake(EventHandler<SerialConnectionEvent> handler) {
        byte[] readBuffer = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(readBuffer, Message.NUM_BYTES);
        if (numRead < Message.NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, "Serial port error"));});
            return false;
        }

        if (!Message.isValid(readBuffer)) {
            Logger.logf(LogLevel.ERROR, LOGGER_TAG, "Corrupted message received: checksum mismatch");
            return false;
        }

        if(Message.getType(readBuffer) != MessageType.VERSION_PARITY) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected message type in response to version parity request: " +  readBuffer[0]);
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, "Bad editor data"));});
            return false;
        }

        VersionParityMessage in = new VersionParityMessage(readBuffer);

        // Announce connection request
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Received version parity message");

        // Is the version compatible
        MessageUtils.Compatibility compatibility = MessageUtils.isVersionCompatible(VERSION_MAJOR, VERSION_MINOR,
                in.getVersionMajor(), in.getVersionMinor());
        boolean accepted = compatibility == MessageUtils.Compatibility.COMPATIBLE;

        if(accepted) {
            connectedUUID = in.getSenderUuid();
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Editor connected: " + connectedUUID.toString());

            VersionParityResponseMessage out = new VersionParityResponseMessage(Identity.getMyUuid(), true,
                    VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, true, "");
            serialPort.writeBytes(out.write(), Message.NUM_BYTES);

            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(true, ""));});
        } else {
            String formattedErr = String.format("Version mismatch: Editor[%d.%d.%d], Monitor[%d.%d.%d]",
                    in.getVersionMajor(), in.getVersionMinor(), in.getVersionPatch(), VERSION_MAJOR, VERSION_MINOR,
                    VERSION_PATCH);
            VersionParityResponseMessage out = new VersionParityResponseMessage(Identity.getMyUuid(), true,
                    VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH, false, formattedErr);
            serialPort.writeBytes(out.write(), Message.NUM_BYTES);

            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, formattedErr));});
        }

        return accepted;
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
        if(!connected) {
            return;
        }

        new Thread(() -> {
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Attempting to connect via serial");
            while(!handshake(handler)) {
                Logger.log(LogLevel.INFO, LOGGER_TAG, "Re-attempting serial connection");
            }

            // Handshake successful now listen for messages from editor
            Logger.log(LogLevel.INFO, LOGGER_TAG, "Editor connection handshake successful");
            while (serialPort.isOpen()) {
                read();
            }
        }).start();
    }

    private void read() {
        byte[] bytes = new byte[Message.NUM_BYTES];
        int numRead = serialPort.readBytes(bytes, Message.NUM_BYTES);
        if (numRead < Message.NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
        }

        boolean valid = Message.isValid(bytes);
        if (!valid) {
            // err, ask for re-send
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Invalid checksum on received message");

            // Todo: when this happens we should flush the entire serial port buffer (throw away) as there may
            //  be some bad data there and respond to the hardware monitor editor stating bad message
        } else {
            valid = readMessage(bytes);
        }

        ConfirmationMessage out = new ConfirmationMessage(Identity.getMyUuid(), valid);
        serialPort.writeBytes(out.write(), Message.NUM_BYTES);
    }
}
