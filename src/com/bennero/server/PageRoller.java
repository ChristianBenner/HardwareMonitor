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
import com.bennero.server.pages.CustomisableSensorPage;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

class PageRoller implements Runnable
{
    private final ApplicationCore applicationCore;
    private List<CustomisableSensorPage> customisableSensorPages;
    private CustomisableSensorPage currentCustomisableSensorPage;
    private CustomisableSensorPage previousCustomisableSensorPage;

    private long pageViewStartTimeMs = 0;

    public PageRoller(ApplicationCore applicationCore)
    {
        this.applicationCore = applicationCore;
        this.customisableSensorPages = new ArrayList<>();
        this.currentCustomisableSensorPage = null;
    }

    public void addPage(CustomisableSensorPage page)
    {
        customisableSensorPages.add(page);
    }

    public boolean removePage(byte pageId)
    {
        boolean exists = false;

        for (int i = 0; i < customisableSensorPages.size() && !exists; i++)
        {
            if (customisableSensorPages.get(i).getUniqueId() == pageId)
            {
                customisableSensorPages.remove(i);
                exists = true;

                if (currentCustomisableSensorPage.getUniqueId() == pageId)
                {
                    applicationCore.removePage(currentCustomisableSensorPage);

                    if(customisableSensorPages.size() == 0)
                    {
                        applicationCore.displayConnectedPage();
                        currentCustomisableSensorPage = null;
                        previousCustomisableSensorPage = null;
                    }
                    else
                    {
                        applicationCore.displayPage(previousCustomisableSensorPage, null);
                        currentCustomisableSensorPage = previousCustomisableSensorPage;
                    }
                }
            }
        }

        return exists;
    }

    public boolean updatePage(PageData pageData)
    {
        boolean exists = false;

        for (int i = 0; i < customisableSensorPages.size() && !exists; i++)
        {
            if (customisableSensorPages.get(i).getPageData().getUniqueId() == pageData.getUniqueId())
            {
                exists = true;

                customisableSensorPages.get(i).updatePageData(pageData);
            }
        }

        return exists;
    }

    public boolean addSensor(byte pageId, Sensor sensor)
    {
        boolean pageExists = false;

        // See if the page ID exists in the list of pages
        for (int i = 0; i < customisableSensorPages.size() && !pageExists; i++)
        {
            if ((byte) customisableSensorPages.get(i).getUniqueId() == pageId)
            {
                // Add sensor to the page
                customisableSensorPages.get(i).addSensor(sensor);
                pageExists = true;
            }
        }

        return pageExists;
    }

    public void removeSensor(byte sensorId, byte pageId)
    {
        // Find the page
        boolean found = false;

        for (int i = 0; i < customisableSensorPages.size() && !found; i++)
        {
            if (customisableSensorPages.get(i).getUniqueId() == pageId)
            {
                found = true;
                customisableSensorPages.get(i).removeSensor(sensorId);
            }
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            if (currentCustomisableSensorPage == null)
            {
                // Find the lowest ID page in the list and set that to the current page
                for (int i = 0; i < customisableSensorPages.size(); i++)
                {
                    if (currentCustomisableSensorPage == null || customisableSensorPages.get(i).getUniqueId() < currentCustomisableSensorPage.getUniqueId())
                    {
                        currentCustomisableSensorPage = customisableSensorPages.get(i);
                        pageViewStartTimeMs = System.currentTimeMillis();
                    }
                }

                if (currentCustomisableSensorPage != null)
                {
                    Platform.runLater(() -> applicationCore.displayPage(currentCustomisableSensorPage, null));
                }
            }
            else
            {
                if (currentCustomisableSensorPage.getDurationMs() != 0 && currentCustomisableSensorPage.getNextPageId() != currentCustomisableSensorPage.getUniqueId() &&
                        pageViewStartTimeMs + currentCustomisableSensorPage.getDurationMs() < System.currentTimeMillis())
                {
                    boolean found = false;

                    // Find page in list
                    for (int i = 0; i < customisableSensorPages.size() && !found; i++)
                    {
                        if (customisableSensorPages.get(i).getUniqueId() == currentCustomisableSensorPage.getNextPageId())
                        {
                            found = true;

                            // Show the next page
                            previousCustomisableSensorPage = currentCustomisableSensorPage;
                            currentCustomisableSensorPage = customisableSensorPages.get(i);
                            pageViewStartTimeMs = System.currentTimeMillis();
                            System.out.println("Display Page: " + customisableSensorPages.get(i).getTitle());
                            Platform.runLater(() -> applicationCore.displayPage(currentCustomisableSensorPage, previousCustomisableSensorPage));
                        }
                    }
                }
            }

            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
