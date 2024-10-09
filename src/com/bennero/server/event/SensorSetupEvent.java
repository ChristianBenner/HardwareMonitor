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

package com.bennero.server.event;

import com.bennero.common.Sensor;
import com.bennero.common.Skin;
import com.bennero.common.SkinHelper;
import com.bennero.common.messages.SensorCreateMessage;
import javafx.event.Event;
import javafx.scene.paint.Color;

/**
 * SensorSetupEvent creates an event that is used to provide a SensorSetupMessage object back to a listener through an
 * EventHandler
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @see SensorCreateMessage
 * @see Event
 * @since 1.0
 */
public class SensorSetupEvent extends Event {
    private final Sensor sensor;
    private final byte pageId;

    public SensorSetupEvent(final SensorCreateMessage message) {
        super(message, null, null);

        pageId = message.getPageId();

        sensor = new Sensor(message.getSensorId(), message.getRow(), message.getColumn(), message.getSensorType(),
                message.getSkin(), message.getMax(), message.getThreshold(), message.getTitle(), message.getTitle(),
                message.isAverageEnabled(), message.getAveragingPeriodMs(), message.getRowSpan(),
                message.getColumnSpan());
        sensor.setValue(message.getInitialValue());

        // Only process each sensor colour if it is supported by the skin that has been received
        final byte skin = message.getSkin();

        if (SkinHelper.checkSupport(skin, Skin.FOREGROUND_BASE_COLOUR_SUPPORTED)) {
            sensor.setForegroundColour(toColor(message.getForegroundColourR(), message.getForegroundColourG(), message.getForegroundColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.AVERAGE_COLOUR_SUPPORTED)) {
            sensor.setAverageColour(toColor(message.getAverageColourR(), message.getAverageColourG(), message.getAverageColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.NEEDLE_COLOUR_SUPPORTED)) {
            sensor.setNeedleColour(toColor(message.getNeedleColourR(), message.getNeedleColourG(), message.getNeedleColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.VALUE_COLOUR_SUPPORTED)) {
            sensor.setValueColour(toColor(message.getValueColourR(), message.getValueColourG(), message.getValueColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.UNIT_COLOUR_SUPPORTED)) {
            sensor.setUnitColour(toColor(message.getUnitColourR(), message.getUnitColourG(), message.getUnitColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.KNOB_COLOUR_SUPPORTED)) {
            sensor.setKnobColour(toColor(message.getKnobColourR(), message.getKnobColourG(), message.getKnobColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.BAR_COLOUR_SUPPORTED)) {
            sensor.setBarColour(toColor(message.getBarColourR(), message.getBarColourG(), message.getBarColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.THRESHOLD_COLOUR_SUPPORTED)) {
            sensor.setThresholdColour(toColor(message.getThresholdColourR(), message.getThresholdColourG(), message.getThresholdColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.TITLE_COLOUR_SUPPORTED)) {
            sensor.setTitleColour(toColor(message.getTitleColourR(), message.getTitleColourG(), message.getTitleColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.BAR_BACKGROUND_COLOUR_SUPPORTED)) {
            sensor.setBarBackgroundColour(toColor(message.getBarBackgroundColourR(), message.getBarBackgroundColourG(), message.getBarBackgroundColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.TICK_LABEL_COLOUR_SUPPORTED)) {
            sensor.setTickLabelColour(toColor(message.getTickLabelColourR(), message.getTickLabelColourG(), message.getTickLabelColourB()));
        }

        if (SkinHelper.checkSupport(skin, Skin.TICK_MARK_COLOUR_SUPPORTED)) {
            sensor.setTickMarkColour(toColor(message.getTickMarkColourR(), message.getTickMarkColourG(), message.getTickMarkColourB()));
        }
    }

    private Color toColor(byte r, byte g, byte b) {
        return Color.rgb(r & 0xFF, g & 0xFF, b & 0xFF);
    }

    public Sensor getSensor() {
        return sensor;
    }

    public byte getPageId() {
        return pageId;
    }
}