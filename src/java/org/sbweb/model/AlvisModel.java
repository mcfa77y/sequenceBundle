/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.model;

import alvis.AlvisDataModel;
import com.general.containerModel.Nameable;
import de.biozentrum.bioinformatik.color.ColorModel;
import gui.sequencebundle.SequenceBundleConfig;
import gui.sequencebundle.aaindex.AAIndexEntry;
import gui.sequencebundle.aaindex.AAIndexRepository;
import java.awt.Color;
import java.io.File;
import java.util.HashMap;
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

    String errorMessage = "";
    File tempFile;
    String webPath;
    private AlignmentType alignmentType = AlignmentType.AMINOACIDS;
    private YAxis yAxis = YAxis.DEFAULT;
    private LineColor lineColor = LineColor.DEFAULT;
    private int radius = 18;

    private int sequenceCount = 0;
    private int sequenceBases = 0;

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

    public enum AlignmentType implements Nameable {

        AMINOACIDS("Amino Acids", "Groups are kept separate from each other within stack in addition to having their own color.", AlvisDataModel.AlignmentType.AminoAcid),
        NUCLEOTIDES("DNA", "Groups are rendered on top of each other blending the colors.", AlvisDataModel.AlignmentType.DNA),
        RNA("RNA", "Groups are rendered on top of each other blending the colors.", AlvisDataModel.AlignmentType.RNA);

        private final String name;
        private final String description;
        final AlvisDataModel.AlignmentType alignmentType;

        AlignmentType(String name, String description, AlvisDataModel.AlignmentType alignmentType) {
            this.name = name;
            this.description = description;
            this.alignmentType = alignmentType;
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
         * @return the alignmentType
         */
        public AlvisDataModel.AlignmentType getAlignmentType() {
            return alignmentType;
        }

    }

    public enum YAxis implements Nameable {

        DEFAULT("Default", "Default legend)", "foo"),
        HYDROPHOBICITY("Amino Acids", "Hydrophobicity (Zimmerman et al., 1968)", "ZIMJ680101"),
        BULKINESS("Bulkiness", "Bulkiness (Zimmerman et al., 1968)", "ZIMJ680102"),
        POLARITY("Polarity", "Polarity (Zimmerman et al., 1968)", "ZIMJ680103"),
        RFRANK("RF rank", "RF rank (Zimmerman et al., 1968)", "ZIMJ680105"),
        ISOELECTRICPOINT("Isoelectric point", "Isoelectric point (Zimmerman et al., 1968)", "ZIMJ680104");

        private final String name;
        private final String description;
        private final String accentionNumber;
        static private HashMap<String, AAIndexEntry> yAxisRepository = new HashMap();

        YAxis(String name, String description, String accentionNumber) {
            this.name = name;
            this.description = description;
            this.accentionNumber = accentionNumber;
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

        public AAIndexEntry getAAIndex(YAxis yAxis) {
            AAIndexEntry result = yAxisRepository.get(yAxis.getAccentionNumber());
            if (result != null) {
                return result;
            } else {
                for (AAIndexEntry aie : AAIndexRepository.AAIndices) {
                    if (aie.getAccessionNumber().indexOf(yAxis.getAccentionNumber()) == 0) {
                        yAxisRepository.put(yAxis.accentionNumber, aie);
                        return aie;
                    }
                }
                return null;
            }
        }

        /**
         * @return the color
         */
        public String getAccentionNumber() {
            return accentionNumber;
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

    public void appendErrorMessage(String errorMessage) {
        this.errorMessage += errorMessage;
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
        Color foo = cc.getDefaultColor();
        System.out.println("current color:" + foo);
        cc.remove(0);
        cc.put(0, lineColor.getColor());
//        cc.setDefaultColor(lineColor.getColor());
        System.out.println("pose color:" + cc);
    }

    public AlignmentType getAlignmentType() {
        return alignmentType;
    }

    public void setAlignmentType(AlignmentType alignmentType) {
        this.alignmentType = alignmentType;
    }

    public YAxis getyAxis() {
        return yAxis;
    }

    public void setyAxis(YAxis yAxis) {
        this.yAxis = yAxis;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        setTangL(radius);
        setTangR(radius);
        this.radius = radius;
    }

    public int getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(int numberOfSequences) {
        this.sequenceCount = numberOfSequences;
    }

    public int getSequenceBases() {
        return sequenceBases;
    }

    public void setSequenceBases(int sequenceLength) {
        this.sequenceBases = sequenceLength;
    }

}
