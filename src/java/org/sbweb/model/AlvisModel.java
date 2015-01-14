/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.model;

import com.general.containerModel.Nameable;
import de.biozentrum.bioinformatik.color.ColorModel;
import gui.sequencebundle.SequenceBundleConfig;
import java.awt.Color;
import java.io.File;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Roland Schwarz <rfs32@cam.ac.uk>
 */
public class AlvisModel extends SequenceBundleConfig {

    @NotNull
    @Size(min = 1, max = 1000)
    String sequences;

    String errorMessage;
    File tempFile;
    String webPath;
    private LineColor lineColor;

    public enum LineColor implements Nameable {

        DEFAULT("Default", "Default", new Color(0, 0, 80)),
        NAVY("Navy", "Navy", new Color(0, 51, 153)),
        SKY("Sky", "Sky", new Color(51, 204, 255)),
        SEA("Sea", "Sea", new Color(0, 204, 204)),
        PURPLE("Purple", "Purple", new Color(204, 0, 204)),
        ORANGE("Orange", "Orange", Color.ORANGE),
        RED("Red", "Red", Color.RED);

        private final String name;
        private final String description;
        private final Color color;

        LineColor(String name, String description, Color color) {
            this.name = name;
            this.description = description;
            this.color = color;

        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * @return the color
         */
        public Color getColor() {
            return color;
        }
    }

    public enum CellWidthSize {

        SMALL("SMALL", 45),
        MEDIUM("MEDIUM", 60),
        LARGE("LARGE", 90);
        private final String name;
        private final int size;

        CellWidthSize(String name, int size) {
            this.name = name;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            return size;
        }

    }

    public String getSequences() {
        return sequences;
    }

    public void setSequences(String sequences) {
        this.sequences = sequences;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public File getTempFile() {
        return tempFile;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public String getWebPath() {
        return webPath;
    }

    public void setWebPath(String webPath) {
        this.webPath = webPath;
    }

    /**
     * @return the lineColor
     */
    public LineColor getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor the lineColor to set
     */
    public void setLineColor(LineColor lineColor) {
        this.lineColor = lineColor;
        ColorModel<Integer> cc = getColorModel().getGroupColors();
        System.out.println("current color:" + cc);
        cc.setDefaultColor(lineColor.getColor());
        System.out.println("pose color:" + cc);
    }
}
