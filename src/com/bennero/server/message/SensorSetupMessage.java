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

import com.bennero.common.Sensor;
import com.bennero.common.Skin;
import com.bennero.common.SkinHelper;
import com.bennero.common.messages.SensorDataPositions;
import javafx.scene.paint.Color;

import static com.bennero.common.Constants.NAME_STRING_NUM_BYTES;
import static com.bennero.common.networking.NetworkUtils.*;

/**
 * SensorSetupMessage stores the data of a sensor creation request. The SensorSetupMessage is sent by a connected client
 * only. The sensor will be created at a specified location of a specified page. If a sensor already exists on the
 * same page, it will be updated with the new information (allowing the change of attributes such as name, position,
 * max value, threshold values, row span and column span).
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class SensorSetupMessage {
    private Sensor sensor;
    private byte pageId;

    private SensorSetupMessage(Sensor sensor, byte pageId) {
        this.sensor = sensor;
        this.pageId = pageId;
    }

    public static SensorSetupMessage processSensorSetupMessage(byte[] bytes) {
        final int sensorId = bytes[SensorDataPositions.ID_POS] & 0xFF;
        final byte pageId = bytes[SensorDataPositions.PAGE_ID_POS];
        final int row = bytes[SensorDataPositions.ROW_POS] & 0xFF;
        final int column = bytes[SensorDataPositions.COLUMN_POS] & 0xFF;
        final int type = bytes[SensorDataPositions.TYPE_POS] & 0xFF;
        final byte skin = bytes[SensorDataPositions.SKIN_POS];
        final float max = readFloat(bytes, SensorDataPositions.MAX_POS);
        final float threshold = readFloat(bytes, SensorDataPositions.THRESHOLD_POS);
        final boolean averageEnabled = bytes[SensorDataPositions.AVERAGE_ENABLED_POS] == (byte) 0x01 ? true : false;
        final int averagingPeriod = readInt(bytes, SensorDataPositions.AVERAGING_PERIOD_POS);
        final int rowSpan = bytes[SensorDataPositions.ROW_SPAN_POS] & 0xFF;
        final int columnSpan = bytes[SensorDataPositions.COLUMN_SPAN_POS] & 0xFF;
        final float initialValue = readFloat(bytes, SensorDataPositions.INITIAL_VALUE_POS);
        final String title = readString(bytes, SensorDataPositions.TITLE_POS, NAME_STRING_NUM_BYTES);

        Sensor sensor = new Sensor((byte)sensorId, row, column, (byte) type, skin, max, threshold, title, title,
                averageEnabled, averagingPeriod, rowSpan, columnSpan);
        sensor.setValue(initialValue);

        // Only process each sensor colour if it is supported by the skin that has been received
        if (SkinHelper.checkSupport(skin, Skin.FOREGROUND_BASE_COLOUR_SUPPORTED)) {
            final Color foregroundColour = Color.rgb(bytes[SensorDataPositions.FOREGROUND_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.FOREGROUND_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.FOREGROUND_COLOUR_B_POS] & 0xFF);
            sensor.setForegroundColour(foregroundColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.AVERAGE_COLOUR_SUPPORTED)) {
            final Color averageColour = Color.rgb(bytes[SensorDataPositions.AVERAGE_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.AVERAGE_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.AVERAGE_COLOUR_B_POS] & 0xFF);
            sensor.setAverageColour(averageColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.NEEDLE_COLOUR_SUPPORTED)) {
            final Color needleColour = Color.rgb(bytes[SensorDataPositions.NEEDLE_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.NEEDLE_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.NEEDLE_COLOUR_B_POS] & 0xFF);
            sensor.setNeedleColour(needleColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.VALUE_COLOUR_SUPPORTED)) {
            final Color valueColour = Color.rgb(bytes[SensorDataPositions.VALUE_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.VALUE_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.VALUE_COLOUR_B_POS] & 0xFF);
            sensor.setValueColour(valueColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.UNIT_COLOUR_SUPPORTED)) {
            final Color unitColour = Color.rgb(bytes[SensorDataPositions.UNIT_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.UNIT_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.UNIT_COLOUR_B_POS] & 0xFF);
            sensor.setUnitColour(unitColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.KNOB_COLOUR_SUPPORTED)) {
            final Color knobColour = Color.rgb(bytes[SensorDataPositions.KNOB_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.KNOB_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.KNOB_COLOUR_B_POS] & 0xFF);
            sensor.setKnobColour(knobColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.BAR_COLOUR_SUPPORTED)) {
            final Color barColour = Color.rgb(bytes[SensorDataPositions.BAR_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.BAR_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.BAR_COLOUR_B_POS] & 0xFF);
            sensor.setBarColour(barColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.THRESHOLD_COLOUR_SUPPORTED)) {
            final Color thresholdColour = Color.rgb(bytes[SensorDataPositions.THRESHOLD_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.THRESHOLD_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.THRESHOLD_COLOUR_B_POS] & 0xFF);
            sensor.setThresholdColour(thresholdColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.TITLE_COLOUR_SUPPORTED)) {
            final Color titleColour = Color.rgb(bytes[SensorDataPositions.TITLE_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.TITLE_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.TITLE_COLOUR_B_POS] & 0xFF);
            sensor.setTitleColour(titleColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.BAR_BACKGROUND_COLOUR_SUPPORTED)) {
            final Color barBackgroundColour = Color.rgb(bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.BAR_BACKGROUND_COLOUR_B_POS] & 0xFF);
            sensor.setBarBackgroundColour(barBackgroundColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.TICK_LABEL_COLOUR_SUPPORTED)) {
            final Color tickLabelColour = Color.rgb(bytes[SensorDataPositions.TICK_LABEL_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.TICK_LABEL_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.TICK_LABEL_COLOUR_B_POS] & 0xFF);
            sensor.setTickLabelColour(tickLabelColour);
        }

        if (SkinHelper.checkSupport(skin, Skin.TICK_MARK_COLOUR_SUPPORTED)) {
            final Color tickMarkColour = Color.rgb(bytes[SensorDataPositions.TICK_MARK_COLOUR_R_POS] & 0xFF,
                    bytes[SensorDataPositions.TICK_MARK_COLOUR_G_POS] & 0xFF,
                    bytes[SensorDataPositions.TICK_MARK_COLOUR_B_POS] & 0xFF);
            sensor.setTickMarkColour(tickMarkColour);
        }

        return new SensorSetupMessage(sensor, pageId);
    }

    public Sensor getSensor() {
        return sensor;
    }

    public byte getPageId() {
        return pageId;
    }
}
