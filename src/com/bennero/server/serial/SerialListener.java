package com.bennero.server.serial;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.MessageUtils;
import com.bennero.server.event.*;
import com.bennero.server.message.*;
import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.event.EventHandler;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static com.bennero.common.Constants.MESSAGE_NUM_BYTES;
import static com.bennero.common.Constants.MESSAGE_TYPE_POS;
import static com.bennero.server.Version.*;

public class SerialListener {
    private static final String LOGGER_TAG = SerialListener.class.getSimpleName();

    // todo: When UART device arrives check what the serial port name is
//    private static final String RPI_SERIAL_PORT = "/dev/ttyS0";
    private static final String RPI_SERIAL_PORT = "CNCB0";

    private SerialPort serialPort;
    private boolean connected;

    private EventHandler disconnectedEvent;
    private EventHandler<PageSetupEvent> pageMessageReceived;
    private EventHandler<SensorSetupEvent> sensorMessageReceived;
    private EventHandler<SensorDataEvent> sensorDataMessageReceived;
    private EventHandler<RemovePageEvent> removePageMessageReceived;
    private EventHandler<RemoveSensorEvent> removeSensorMessageReceived;
    private EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived;
    private EventHandler<FileTransferEvent> fileTransferEventHandler;

    public SerialListener(EventHandler disconnectedEvent,
                          EventHandler<PageSetupEvent> pageMessageReceived,
                          EventHandler<SensorSetupEvent> sensorMessageReceived,
                          EventHandler<RemovePageEvent> removePageMessageReceived,
                          EventHandler<SensorDataEvent> sensorDataMessageReceived,
                          EventHandler<RemoveSensorEvent> removeSensorMessageReceived,
                          EventHandler<SensorTransformationEvent> sensorTransformationMessageReceived,
                          EventHandler<FileTransferEvent> fileTransferEventHandler) {
        this.disconnectedEvent = disconnectedEvent;
        this.pageMessageReceived = pageMessageReceived;
        this.sensorMessageReceived = sensorMessageReceived;
        this.removePageMessageReceived = removePageMessageReceived;
        this.sensorDataMessageReceived = sensorDataMessageReceived;
        this.removeSensorMessageReceived = removeSensorMessageReceived;
        this.sensorTransformationMessageReceived = sensorTransformationMessageReceived;
        this.fileTransferEventHandler = fileTransferEventHandler;

        serialPort = SerialPort.getCommPort(RPI_SERIAL_PORT);
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
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_BLOCKING, 0, 5000);
    }

    private boolean handshake(EventHandler<SerialConnectionEvent> handler) {
        byte[] readBuffer = new byte[MESSAGE_NUM_BYTES];
        int numRead = serialPort.readBytes(readBuffer, MESSAGE_NUM_BYTES);
        if (numRead < MESSAGE_NUM_BYTES) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, "Serial port error"));});
            return false;
        }

        if(readBuffer[0] != MessageType.VERSION_PARITY_MESSAGE) {
            Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected message type in response to version parity request: " +  readBuffer[0]);
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, "Bad editor data"));});
            return false;
        }

        VersionParityMessage message = VersionParityMessage.processConnectionRequestMessageData(readBuffer);

        // Announce connection request
        Logger.log(LogLevel.INFO, LOGGER_TAG, "Received version parity message");

        // Is the version compatible
        MessageUtils.Compatibility compatibility = MessageUtils.isVersionCompatible(VERSION_MAJOR, VERSION_MINOR,
                message.getMajorVersion(), message.getMinorVersion());
        boolean accepted = compatibility == MessageUtils.Compatibility.COMPATIBLE;
        byte[] response = VersionParityResponseMessage.create(accepted);
        serialPort.writeBytes(response, MESSAGE_NUM_BYTES);

        if(accepted) {
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(true, ""));});
        } else {
            String formattedErr = String.format("Version mismatch: Editor[%d.%d.%d], Monitor[%d.%d.%d]",
                    message.getMajorVersion(), message.getMinorVersion(), message.getPatchVersion(),
                    VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH);
            Platform.runLater(() -> {handler.handle(new SerialConnectionEvent(false, formattedErr));});
        }

        return accepted;
    }

    private void readMessage(byte[] bytes) {
        switch (bytes[MESSAGE_TYPE_POS]) {
            case MessageType.DATA:
                sensorDataMessageReceived.handle(new SensorDataEvent(SensorDataMessage.processSensorDataMessage(bytes)));
                break;
            case MessageType.PAGE_SETUP:
                pageMessageReceived.handle(new PageSetupEvent(PageSetupMessage.readPageSetupMessage(bytes)));
                break;
            case MessageType.SENSOR_SETUP:
                sensorMessageReceived.handle(new SensorSetupEvent(SensorSetupMessage.processSensorSetupMessage(bytes)));
                break;
            case MessageType.REMOVE_PAGE:
                removePageMessageReceived.handle(new RemovePageEvent(RemovePageMessage.processRemovePageMessage(bytes)));
                break;
            case MessageType.REMOVE_SENSOR:
                removeSensorMessageReceived.handle(new RemoveSensorEvent(RemoveSensorMessage.processRemoveSensorMessage(bytes)));
                break;
            case MessageType.SENSOR_TRANSFORMATION_MESSAGE:
                sensorTransformationMessageReceived.handle(new SensorTransformationEvent(SensorTransformationMessage.processSensorTransformationMessage(bytes)));
                break;
            case MessageType.CONNECTION_REQUEST_MESSAGE:
         //       handleConnectionRequest(processConnectionRequestMessageData(bytes));
                break;
            case MessageType.DISCONNECT_MESSAGE:
//                Logger.log(LogLevel.DEBUG, CLASS_NAME, "Received disconnect message");
//                handleDisconnect();
                break;
            case MessageType.FILE_MESSAGE:
                FileMessage fileMessage = FileMessage.processConnectionRequestMessageData(bytes);

                // The next read will be the file bytes so read here
                int size = fileMessage.getSize();
                byte[] fileBytes = new byte[size];
                int numRead = serialPort.readBytes(fileBytes, size);
                if (numRead < size) {
                    Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected file read amount on serial port: " +  numRead);
                }

                fileTransferEventHandler.handle(new FileTransferEvent(fileBytes, fileMessage.getName(), fileMessage.getType()));
                break;
        }
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
                // The total number of bytes to expect per message including the checksum bytes
                int totalReadBytes = MESSAGE_NUM_BYTES + Long.BYTES;

                byte[] bytes = new byte[totalReadBytes];
                int numRead = serialPort.readBytes(bytes, totalReadBytes);
                if (numRead < totalReadBytes) {
                    Logger.log(LogLevel.ERROR, LOGGER_TAG, "Unexpected read amount on serial port: " +  numRead);
                }

                // The last 8 bytes of every message is a checksum
                long receivedChecksum = MessageUtils.readLong(bytes, MESSAGE_NUM_BYTES);
                Checksum checksum = new CRC32();
                checksum.update(bytes, 0, MESSAGE_NUM_BYTES);
                if(checksum.getValue() != receivedChecksum) {
                    // err, ask for re-send
                    Logger.log(LogLevel.ERROR, LOGGER_TAG, "Invalid checksum on received message");

                    // Todo: when this happens we should flush the entire serial port buffer (throw away) as there may
                    //  be some bad data there and respond to the hardware monitor editor stating bad message
                } else {
                    // Todo: respond to the hardware monitor stating good message
                    readMessage(bytes);
                }
            }
        }).start();
    }
}
