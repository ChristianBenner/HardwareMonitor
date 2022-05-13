/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Additional terms included with this license are to:
 * - Preserve legal notices and author attributions such as this one. Do not remove the original author license notices
 *   from the program
 * - Preserve the donation button and its link to the original authors donation page (christianbenner35@gmail.com)
 * - Only break the terms if given permission from the original author christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.server.message;

import com.bennero.common.messages.RemoveSensorDataPositions;

/**
 * RemoveSensorMessage stores the data of a sensor removal request. The message is sent by a connected client only. The
 * message must include the page that the sensor is to be removed from.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class RemoveSensorMessage {
    private byte sensorId;
    private byte pageId;

    private RemoveSensorMessage(byte sensorId, byte pageId) {
        this.sensorId = sensorId;
        this.pageId = pageId;
    }

    public static RemoveSensorMessage processRemoveSensorMessage(byte[] bytes) {
        final int sensorId = bytes[RemoveSensorDataPositions.SENSOR_ID_POS] & 0xFF;
        final int pageId = bytes[RemoveSensorDataPositions.PAGE_ID_POS] & 0xFF;
        return new RemoveSensorMessage((byte) sensorId, (byte) pageId);
    }

    public byte getSensorId() {
        return sensorId;
    }

    public byte getPageId() {
        return pageId;
    }
}
