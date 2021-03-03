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

package com.bennero.server.pages;

import com.bennero.common.Constants;
import com.bennero.common.PageData;
import com.bennero.common.PageTemplate;
import com.bennero.common.Sensor;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomisableSensorPage class is the key component of the hardware monitor in terms of displaying the sensor graphics
 * and data send by the editor system. It is designed to accept page data objects that define any changes such as the
 * layout, title, subtitle, sensor layouts etc. These page data are processed by the network components of the hardware
 * monitor (from received network messages)
 *
 * @see         PageData
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class CustomisableSensorPage extends StackPane implements PageTemplate
{
    private final static Insets PAGE_PADDING = new Insets(10, 10, 10, 10);

    private PageData pageData;
    private GridPane sensorPane;

    private VBox headerPane;
    private HBox titleBox;
    private HBox subtitleBox;

    private BorderPane borderPane;
    private ArrayList<Sensor> placedSensors;

    private Label titleLabel;
    private Label subtitleLabel;

    private Transition transitionControl;

    public CustomisableSensorPage(PageData pageData)
    {
        this.pageData = pageData;
        super.setBackground(new Background(new BackgroundFill(pageData.getColour(), CornerRadii.EMPTY, Insets.EMPTY)));

        headerPane = new VBox();

        initTitle();
        initSubtitle();
        BorderPane.setAlignment(headerPane, Pos.CENTER);

        borderPane = new BorderPane();
        initGrid();

        borderPane.setTop(headerPane);
        borderPane.setPadding(PAGE_PADDING);

        super.getChildren().add(borderPane);
    }

    public PageData getPageData()
    {
        return pageData;
    }

    public Transition getTransitionControl()
    {
        return this.transitionControl;
    }

    public void setTransitionControl(Transition transitionControl)
    {
        this.transitionControl = transitionControl;
    }

    protected void initGrid()
    {
        placedSensors = new ArrayList<>();
        sensorPane = new GridPane();
        sensorPane.setPadding(new Insets(15, 15, 15, 15));
        sensorPane.setHgap(10.0f);
        sensorPane.setVgap(10.0f);
        BorderPane.setAlignment(sensorPane, Pos.CENTER);
        sensorPane.setAlignment(Pos.CENTER);

        // Configure the grid pane cells to be of equal size
        RowConstraints rc = new RowConstraints();
        rc.setPercentHeight(100d / pageData.getRows());
        for (int y = 0; y < pageData.getRows(); y++)
        {
            sensorPane.getRowConstraints().add(rc);
        }

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100d / pageData.getColumns());
        for (int x = 0; x < pageData.getColumns(); x++)
        {
            sensorPane.getColumnConstraints().add(cc);
        }

        placeSensors();

        borderPane.setCenter(sensorPane);
    }

    protected boolean isSpaceTaken(Sensor sensor)
    {
        boolean taken = false;

        // Check that no other sensor has been placed at that position
        for (int i = 0; i < placedSensors.size() && !taken; i++)
        {
            Sensor placedSensor = placedSensors.get(i);

            if (placedSensor != sensor)
            {
                int startColumn = sensor.getColumn();
                int endColumn = startColumn + sensor.getColumnSpan();
                int startRow = sensor.getRow();
                int endRow = startRow + sensor.getRowSpan();

                int placedStartColumn = placedSensor.getColumn();
                int placedEndColumn = placedStartColumn + placedSensor.getColumnSpan();
                int placedStartRow = placedSensor.getRow();
                int placedEndRow = placedStartRow + placedSensor.getRowSpan();

                boolean withinRow = (startRow >= placedStartRow && startRow < placedEndRow) ||
                        (endRow > placedStartRow && endRow <= placedEndRow);
                boolean withinColumn = (startColumn >= placedStartColumn && startColumn < placedEndColumn) ||
                        (endColumn > placedStartColumn && endColumn <= placedEndColumn);

                if (withinRow && withinColumn)
                {
                    taken = true;
                }
            }
        }

        return taken;
    }

    protected void placeSensors()
    {
        // Add sensors to page
        for (Sensor sensor : pageData.getSensorList())
        {
            if (!isSpaceTaken(sensor))
            {
                placedSensors.add(sensor);

                // This is required in the non-editor version
                StackPane stackPane = new StackPane();
                stackPane.getChildren().add(sensor);

                GridPane.setRowSpan(stackPane, sensor.getRowSpan());
                GridPane.setColumnSpan(stackPane, sensor.getColumnSpan());

                sensorPane.add(stackPane, sensor.getColumn(), sensor.getRow());
            }
        }
    }

    public void initTitle()
    {
        if (pageData.isTitleEnabled())
        {
            titleBox = new HBox();
            titleLabel = new Label(pageData.getTitle());
            titleLabel.setFont(new Font(42));
            switch (pageData.getTitleAlignment())
            {
                case Constants.TEXT_ALIGNMENT_LEFT:
                    titleBox.setAlignment(Pos.CENTER_LEFT);
                    break;
                case Constants.TEXT_ALIGNMENT_CENTER:
                    titleBox.setAlignment(Pos.CENTER);
                    break;
                case Constants.TEXT_ALIGNMENT_RIGHT:
                    titleBox.setAlignment(Pos.CENTER_RIGHT);
                    break;
            }
            titleLabel.setTextFill(pageData.getTitleColour());
            titleBox.getChildren().add(titleLabel);
            headerPane.getChildren().add(titleBox);
        }
    }

    public void initSubtitle()
    {
        if (pageData.isSubtitleEnabled())
        {
            subtitleBox = new HBox();
            subtitleLabel = new Label(pageData.getSubtitle());
            subtitleLabel.setFont(new Font(28));
            switch (pageData.getSubtitleAlignment())
            {
                case Constants.TEXT_ALIGNMENT_LEFT:
                    subtitleBox.setAlignment(Pos.CENTER_LEFT);
                    break;
                case Constants.TEXT_ALIGNMENT_CENTER:
                    subtitleBox.setAlignment(Pos.CENTER);
                    break;
                case Constants.TEXT_ALIGNMENT_RIGHT:
                    subtitleBox.setAlignment(Pos.CENTER_RIGHT);
                    break;
            }
            subtitleLabel.setTextFill(pageData.getSubtitleColour());
            subtitleBox.getChildren().add(subtitleLabel);
            headerPane.getChildren().add(subtitleBox);
        }
    }

    public void updatePageData(PageData pageData)
    {
        setColour(pageData.getColour());
        setTitleColour(pageData.getTitleColour());
        setSubtitleColour(pageData.getSubtitleColour());
        setRows(pageData.getRows());
        setColumns(pageData.getColumns());
        setNextPageId(pageData.getNextPageId());
        setUniqueId(pageData.getUniqueId());
        setTransitionType(pageData.getTransitionType());
        setTransitionTime(pageData.getTransitionTime());
        setDurationMs(pageData.getDurationMs());
        setTitle(pageData.getTitle());
        setTitleEnabled(pageData.isTitleEnabled());
        setTitleAlignment(pageData.getTitleAlignment());
        setSubtitle(pageData.getSubtitle());
        setSubtitleEnabled(pageData.isSubtitleEnabled());
        setSubtitleAlignment(pageData.getSubtitleAlignment());
    }

    @Override
    public int getUniqueId()
    {
        return pageData.getUniqueId();
    }

    @Override
    public void setUniqueId(int id)
    {
        pageData.setUniqueId(id);
    }

    @Override
    public Color getColour()
    {
        return pageData.getColour();
    }

    @Override
    public void setColour(Color colour)
    {
        if (getColour().getRed() != colour.getRed() ||
                getColour().getGreen() != colour.getGreen() ||
                getColour().getBlue() != colour.getBlue())
        {
            pageData.setColour(colour);
            super.setBackground(new Background(new BackgroundFill(colour, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }

    @Override
    public Color getTitleColour()
    {
        return pageData.getTitleColour();
    }

    @Override
    public void setTitleColour(Color colour)
    {
        if (getTitleColour().getRed() != colour.getRed() ||
                getTitleColour().getGreen() != colour.getGreen() ||
                getTitleColour().getBlue() != colour.getBlue())
        {
            pageData.setTitleColour(colour);

            if (titleLabel != null)
            {
                titleLabel.setTextFill(colour);
            }
        }
    }

    @Override
    public Color getSubtitleColour()
    {
        return pageData.getSubtitleColour();
    }

    @Override
    public void setSubtitleColour(Color colour)
    {
        if (getSubtitleColour().getRed() != colour.getRed() ||
                getSubtitleColour().getGreen() != colour.getGreen() ||
                getSubtitleColour().getBlue() != colour.getBlue())
        {
            pageData.setSubtitleColour(colour);

            if (subtitleLabel != null)
            {
                subtitleLabel.setTextFill(colour);
            }
        }
    }

    @Override
    public int getRows()
    {
        return pageData.getRows();
    }

    @Override
    public void setRows(int rows)
    {
        if (getRows() != rows)
        {
            pageData.setRows(rows);
            initGrid();
        }
    }

    @Override
    public int getColumns()
    {
        return pageData.getColumns();
    }

    @Override
    public void setColumns(int columns)
    {
        if (getColumns() != columns)
        {
            pageData.setColumns(columns);
            initGrid();
        }
    }

    @Override
    public int getNextPageId()
    {
        return pageData.getNextPageId();
    }

    @Override
    public void setNextPageId(int nextPageId)
    {
        pageData.setNextPageId(nextPageId);
    }

    @Override
    public int getTransitionType()
    {
        return pageData.getTransitionType();
    }

    @Override
    public void setTransitionType(int transistionType)
    {
        if (getTransitionType() != transistionType)
        {
            pageData.setTransitionType(transistionType);

            // todo: Re-init transition object
        }
    }

    @Override
    public int getTransitionTime()
    {
        return pageData.getTransitionTime();
    }

    @Override
    public void setTransitionTime(int transitionTime)
    {
        pageData.setTransitionTime(transitionTime);

        // todo: Re-init transition object
    }

    @Override
    public int getDurationMs()
    {
        return pageData.getDurationMs();
    }

    @Override
    public void setDurationMs(int durationMs)
    {
        pageData.setDurationMs(durationMs);
    }

    @Override
    public String getTitle()
    {
        return pageData.getTitle();
    }

    @Override
    public void setTitle(String title)
    {
        System.out.println("RECVD TITLE: " + title + ", CHANGE(" + ((getTitle() != title) ? "TRUE" : "FALSE") + ")");
        if (getTitle() != title)
        {
            pageData.setTitle(title);
            titleLabel.setText(title);
        }
    }

    @Override
    public boolean isTitleEnabled()
    {
        return pageData.isTitleEnabled();
    }

    @Override
    public void setTitleEnabled(boolean enabled)
    {
        System.out.println("RECVD TITLEENABLED: " + enabled + ", CHANGE(" + ((isTitleEnabled() != enabled) ? "TRUE" : "FALSE") + ")");
        if (isTitleEnabled() != enabled)
        {
            pageData.setTitleEnabled(enabled);

            if (enabled)
            {
                initTitle();

                // To make sure that the sub-title appears below the title
                if (isSubtitleEnabled())
                {
                    removeSubtitle();
                    initSubtitle();
                }
            }
            else
            {
                removeTitle();
            }
        }
    }

    @Override
    public int getTitleAlignment()
    {
        return pageData.getTitleAlignment();
    }

    @Override
    public void setTitleAlignment(int alignment)
    {
        if (getTitleAlignment() != alignment)
        {
            pageData.setTitleAlignment(alignment);

            // Align the text
            switch (alignment)
            {
                case Constants.TEXT_ALIGNMENT_LEFT:
                    titleBox.setAlignment(Pos.CENTER_LEFT);
                    break;
                case Constants.TEXT_ALIGNMENT_CENTER:
                    titleBox.setAlignment(Pos.CENTER);
                    break;
                case Constants.TEXT_ALIGNMENT_RIGHT:
                    titleBox.setAlignment(Pos.CENTER_RIGHT);
                    break;
            }
        }
    }

    @Override
    public String getSubtitle()
    {
        return pageData.getSubtitle();
    }

    @Override
    public void setSubtitle(String subtitle)
    {
        if (subtitleLabel != null && getSubtitle() != subtitle)
        {
            pageData.setSubtitle(subtitle);
            subtitleLabel.setText(subtitle);
        }
    }

    @Override
    public boolean isSubtitleEnabled()
    {
        return pageData.isSubtitleEnabled();
    }

    @Override
    public void setSubtitleEnabled(boolean subtitleEnabled)
    {
        if (isSubtitleEnabled() != subtitleEnabled)
        {
            pageData.setSubtitleEnabled(subtitleEnabled);

            if (subtitleEnabled)
            {
                initSubtitle();
            }
            else
            {
                removeSubtitle();
            }
        }
    }

    @Override
    public int getSubtitleAlignment()
    {
        return pageData.getSubtitleAlignment();
    }

    @Override
    public void setSubtitleAlignment(int subtitleAlignment)
    {
        if (getSubtitleAlignment() != subtitleAlignment)
        {
            pageData.setSubtitleAlignment(subtitleAlignment);

            // Align the text
            switch (subtitleAlignment)
            {
                case Constants.TEXT_ALIGNMENT_LEFT:
                    subtitleBox.setAlignment(Pos.CENTER_LEFT);
                    break;
                case Constants.TEXT_ALIGNMENT_CENTER:
                    subtitleBox.setAlignment(Pos.CENTER);
                    break;
                case Constants.TEXT_ALIGNMENT_RIGHT:
                    subtitleBox.setAlignment(Pos.CENTER_RIGHT);
                    break;
            }
        }
    }

    @Override
    public List<Sensor> getSensorList()
    {
        return pageData.getSensorList();
    }

    @Override
    public void addSensor(Sensor sensor)
    {
        pageData.addSensor(sensor);
        initGrid();
    }

    @Override
    public void removeSensor(Sensor sensor)
    {
        pageData.removeSensor(sensor);
        placedSensors.remove(sensor);
        initGrid();
    }

    public void removeSensor(byte uniqueId)
    {
        // Find sensor in the list
        boolean found = false;
        for (int i = 0; i < placedSensors.size() && !found; i++)
        {
            if (placedSensors.get(i).getUniqueId() == uniqueId)
            {
                found = true;
                removeSensor(placedSensors.get(i));
            }
        }
    }

    @Override
    public boolean containsSensor(Sensor sensor)
    {
        return pageData.containsSensor(sensor);
    }

    @Override
    public boolean isSpaceFree(Sensor sensor)
    {
        return pageData.isSpaceFree(sensor);
    }

    private void removeTitle()
    {
        if (titleBox != null)
        {
            headerPane.getChildren().remove(titleBox);
        }
    }

    private void removeSubtitle()
    {
        if (subtitleBox != null)
        {
            headerPane.getChildren().remove(subtitleBox);
        }
    }
}
