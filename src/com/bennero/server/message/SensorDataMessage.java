/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.server.message;

import com.bennero.messages.SensorValueDataPositions;

import static com.bennero.networking.NetworkUtils.readFloat;

/**
 * SensorDataMessage stores the data of a sensor update. It is not page dependent (any time the sensor with the given ID
 * is updated it is updated across all pages). The message is sent by a connected client only and contains the new value
 * for the sensor, and the ID of the sensor.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SensorDataMessage
{
    private byte sensorId;
    private float value;

    private SensorDataMessage(byte sensorId, float value)
    {
        this.sensorId = sensorId;
        this.value = value;
    }

    public static SensorDataMessage processSensorDataMessage(byte[] bytes)
    {
        final int sensorId = bytes[SensorValueDataPositions.ID_POS] & 0xFF;
        final float value = readFloat(bytes, SensorValueDataPositions.VALUE_POS);
        return new SensorDataMessage((byte) sensorId, value);
    }

    public byte getSensorId()
    {
        return sensorId;
    }

    public float getValue()
    {
        return value;
    }
}
