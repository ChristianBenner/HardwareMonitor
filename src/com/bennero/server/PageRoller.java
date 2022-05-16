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

package com.bennero.server;

import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import com.bennero.server.pages.CustomisableSensorPage;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

class PageRoller implements Runnable {
    // Tag for logging
    private static final String CLASS_NAME = PageRoller.class.getSimpleName();

    private final ApplicationCore applicationCore;
    private HashMap<Byte, CustomisableSensorPage> customisableSensorPages;
    private CustomisableSensorPage currentCustomisableSensorPage;
    private CustomisableSensorPage previousCustomisableSensorPage;

    private long pageViewStartTimeMs = 0;

    public PageRoller(ApplicationCore applicationCore) {
        this.applicationCore = applicationCore;
        this.customisableSensorPages = new HashMap<>();
        this.currentCustomisableSensorPage = null;
    }

    public void addPage(CustomisableSensorPage page) {
        customisableSensorPages.put(page.getUniqueId(), page);

        // If this is the first page added, then display it
        if (currentCustomisableSensorPage == null) {
            currentCustomisableSensorPage = page;
            pageViewStartTimeMs = System.currentTimeMillis();
            Platform.runLater(() -> applicationCore.displayPage(page, null));
        }
    }

    public boolean removePage(byte pageId) {
        boolean exists = false;

        if (customisableSensorPages.containsKey(pageId)) {
            customisableSensorPages.remove(pageId);
            exists = true;

            if (currentCustomisableSensorPage.getUniqueId() == pageId) {
                applicationCore.removePage(currentCustomisableSensorPage);

                if (customisableSensorPages.size() == 0) {
                    applicationCore.displayConnectedPage();
                    currentCustomisableSensorPage = null;
                    previousCustomisableSensorPage = null;
                } else {
                    applicationCore.displayPage(previousCustomisableSensorPage, null);
                    currentCustomisableSensorPage = previousCustomisableSensorPage;
                }
            }
        }

        return exists;
    }

    public boolean exists(byte id) {
        return customisableSensorPages.containsKey(id);
    }

    public void updatePage(PageData pageData) {
        byte key = pageData.getUniqueId();
        if (customisableSensorPages.containsKey(key)) {
            customisableSensorPages.get(key).updatePageData(pageData);
        }
    }

    public void addSensor(byte pageId, Sensor sensor) {
        if (customisableSensorPages.containsKey(pageId)) {
            customisableSensorPages.get(pageId).addSensor(sensor);
        }
    }

    public void removeSensor(byte sensorId, byte pageId) {
        if (customisableSensorPages.containsKey(pageId)) {
            customisableSensorPages.get(pageId).removeSensor(sensorId);
        }
    }

    public void transformSensor(byte sensorId, byte pageId, byte row, byte column, byte rowSpan, byte columnSpan) {
        if (customisableSensorPages.containsKey(pageId)) {
            customisableSensorPages.get(pageId).transformSensor(sensorId, row, column, rowSpan, columnSpan);
        }
    }

    public void removeAllPages() {
        customisableSensorPages.clear();
        currentCustomisableSensorPage = null;
        previousCustomisableSensorPage = null;
    }

    @Override
    public void run() {
        while (true) {
            if (currentCustomisableSensorPage != null) {
                if (currentCustomisableSensorPage.getDurationMs() != 0 && currentCustomisableSensorPage.getNextPageId() != currentCustomisableSensorPage.getUniqueId() &&
                        pageViewStartTimeMs + currentCustomisableSensorPage.getDurationMs() < System.currentTimeMillis()) {
                    // Show the next page
                    byte nextPage = currentCustomisableSensorPage.getNextPageId();
                    if (customisableSensorPages.containsKey(nextPage)) {
                        previousCustomisableSensorPage = currentCustomisableSensorPage;
                        currentCustomisableSensorPage = customisableSensorPages.get(nextPage);
                        pageViewStartTimeMs = System.currentTimeMillis();
                        Platform.runLater(() -> applicationCore.displayPage(currentCustomisableSensorPage, previousCustomisableSensorPage));
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Logger.log(LogLevel.ERROR, CLASS_NAME, "Failed to sleep thread");
                Logger.log(LogLevel.DEBUG, CLASS_NAME, e.getMessage());
            }
        }
    }
}
