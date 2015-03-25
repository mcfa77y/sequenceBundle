/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spee.sbweb.model;

import com.dictiography.collections.IndexedNavigableSet;
import com.dictiography.collections.IndexedTreeSet;
import com.general.utils.GUIutils;
import com.general.utils.GeneralUtils;
import de.biozentrum.bioinformatik.sequence.Sequence;
import gui.sequencebundle.SequenceBundleColorModel;
import gui.sequencebundle.SequenceBundleConfig;
import gui.sequencebundle.SequenceBundleRendererEvent;
import gui.sequencebundle.SequenceBundleRendererListener;
import gui.sequencebundle.SequenceBundleResources;
import gui.sequencebundle.event.GridPoint;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.EventListenerList;
import org.jaitools.tiledimage.DiskMemImage;
import org.jblas.util.Random;

/**
 *
 * @author joelau
 */
public class WebSequenceBundleRenderer {

    private Sequence consensus;

    // CONSTANTS
    public static enum RenderingStrategy {

        DIRECT_RENDERER, TILE_RENDERER, FRAGMENT_RENDERER
    };
    public static final Color MARKER_COLOR = Color.BLACK;
    static final int OFFSET_X = 10; // space between left edge of drawing area and the legend

    public static final Color LINE_COLOR = new Color(0, 0, 80, 25);
    public static final Color TEXT_COLOR = new Color(0, 0, 80, 255);
    private static final int COLUMNS_PER_TILE = 5;
    private static final int MAX_FIND_ALPHA_ITERATIONS = 500;
    private static final int CURVE_DOWN = 0;
    private static final int CURVE_UP = 1;

    private final WebJSequenceBundle view;
    private final EventListenerList listenerList = new EventListenerList();

    boolean initialised = false;

    IndexedNavigableSet<Integer> groupIDs;

    //int baseX = 25; // step size in X direction (effectively X-axis zoom)
    int baseY = 20;//45; // step size in the Y direction (effectively Y-axis zoom)
    private int drawingAreaHeight = 300;
    private int drawingAreaWidth = 1000;
    private byte[][] cumulativeOffset;
    private int alpha[];
    int numberOfGroups;
//    private CubicCurve2D[][] curves;
//	private Line2D[][] lines;
    private int findAlphaIterationCount;
    private int[] sequenceIndexToGroupIndex;
    private int[][] groupIndexToSequenceIndices;
    private GraphicsConfiguration graphicsConfig = null;
    private Future mainWorker;
    private Future selectionWorker;
    SequenceBundleConfig bundleConfig;
    private int[][] yPositions;
    static private BufferedImage[][][][] fragments;
    static private BufferedImage[] lineFragments;
    private byte[][] yPositionsInStack;
    int maxColumnOffset;
    ExecutorService threadPool = Executors.newFixedThreadPool(1);

    public WebSequenceBundleRenderer(WebJSequenceBundle view) {
        this.view = view;
        this.bundleConfig = view.getBundleConfig();
        this.view.addPropertyChangeListener(WebJSequenceBundle.PROP_BUNDLECONFIG, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                bundleConfig = WebSequenceBundleRenderer.this.view.getBundleConfig();
            }
        });
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            this.graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        init();
    }

    public WebSequenceBundleRenderer(WebJSequenceBundle view, SequenceBundleConfig bundleConfig) {
        this.view = view;
        this.bundleConfig = bundleConfig;
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            this.graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        init();
    }

    // BEGIN INITIALIZATION METHODS
    public void init() { // should be called once after the alignment has been set
        if (mainWorker != null) {
            mainWorker.cancel(false);
        }
        if (selectionWorker != null) {
            selectionWorker.cancel(false);
        }

        if (view.getAlignment() == null || view.getAlphabet() == null) {
            yPositions = null;
            yPositionsInStack = null;
            cumulativeOffset = null;
//			curves=null;
            initialised = false;
        } else {
            calculateSize();
            setupArraysAndPositions();
            determineAlpha();
            fragments = null; // set to null so the renderer can reinitialize
            lineFragments = null;
            renderAlphabetOverlayFragments();
            initialised = true;
        }

        // change tileWidth depending on the configured cellWidth
        bundleConfig.addPropertyChangeListener(SequenceBundleConfig.PROP_CELLWIDTH, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                calculateSize();
            }
        });
    }

    BufferedImage createCompatibleImage(int width, int height) {
        int scaledWidth = (int) Math.ceil(width * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        int scaledHeight = (int) Math.ceil(height * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        BufferedImage image;
        if (graphicsConfig != null) {
            image = graphicsConfig.createCompatibleImage(scaledWidth, scaledHeight, Transparency.TRANSLUCENT);
        } else {
            image = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        }

        return image;
    }

    VolatileImage createCompatibleVolatileImage(int width, int height) {
        int scaledWidth = (int) Math.ceil(width * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        int scaledHeight = (int) Math.ceil(height * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        VolatileImage image;
        image = graphicsConfig.createCompatibleVolatileImage(scaledWidth, scaledHeight, Transparency.TRANSLUCENT);
        while ((image.validate(graphicsConfig) == VolatileImage.IMAGE_INCOMPATIBLE)) {
            return createCompatibleVolatileImage(width, height);
        }

        Graphics2D g = image.createGraphics();
        g.setTransform(new AffineTransform());
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, scaledWidth - 1, scaledHeight - 1);
        g.setComposite(AlphaComposite.SrcOver);

        return image;
    }

    Graphics2D createCompatibleImageGraphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        GUIutils.setQualityRenderingHints(g, bundleConfig.getAntiAliasing(), bundleConfig.getSpeedOverQuality());
        setGlobalGraphicsParameters(g);
        return g;
    }

    Graphics2D createCompatibleImageGraphics(VolatileImage img) {
        Graphics2D g = img.createGraphics();
        GUIutils.setQualityRenderingHints(g, bundleConfig.getAntiAliasing(), bundleConfig.getSpeedOverQuality());
        setGlobalGraphicsParameters(g);
        return g;
    }

    DiskMemImage createDiskMemImage(int width, int height) {
        int scaledWidth = (int) Math.ceil(width * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        int scaledHeight = (int) Math.ceil(height * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);

        ColorModel cm = ColorModel.getRGBdefault();
//		BufferedImage test = createCompatibleImage(1, 1);
//		ColorModel cm = test.getColorModel();
        SampleModel sm = cm.createCompatibleSampleModel(getScaledTileWidth(), getScaledTileHeight());

        return new DiskMemImage(scaledWidth, scaledHeight, sm, cm);
    }

    Graphics2D createDiskMemImageGraphics(DiskMemImage img) {
        Graphics2D g = img.createGraphics();
        GUIutils.setQualityRenderingHints(g, bundleConfig.getAntiAliasing(), bundleConfig.getSpeedOverQuality());
        setGlobalGraphicsParameters(g);
        return g;
    }

    Graphics2D setGlobalGraphicsParameters(Graphics2D g) {
        // set scale
        double scale = bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI;
        AffineTransform at = (AffineTransform) g.getTransform().clone(); // DiskMemImageGraphics doesn't return a copy on getTransform as it should
        at.setTransform(
                scale,
                at.getShearY(),
                at.getShearX(),
                scale,
                at.getTranslateX(),
                at.getTranslateY());
        g.setTransform(at);

        // set font
        g.setFont(SequenceBundleResources.SB_FONT_REGULAR);

        return g;
    }

    int getTileWidth() {
        int tileWidth = bundleConfig.getCellWidth() * COLUMNS_PER_TILE;
        return tileWidth;
    }

    int getScaledTileWidth() {
        int tileWidth = (int) Math.ceil(getTileWidth() * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        return tileWidth;
    }

    int getTileHeight() {
        return getDrawingAreaHeight();
    }

    int getScaledTileHeight() {
        int tileHeight = (int) Math.ceil(getTileHeight() * bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI);
        return tileHeight;
    }

    /**
     * Determines the per-thread alpha to use for each group individually
     */
    private void determineAlpha() {
        int ngroups = groupIDs.size();
        alpha = new int[ngroups];
        int maxSequencesPerCell[] = new int[ngroups];

        for (int j = 0; j < ngroups; j++) {
            for (int i = 0; i < view.getAlignment().getLength(); i++) {
                int[] sequenceIndices;
                if (bundleConfig.getGroupStacking() == SequenceBundleConfig.GroupStackingType.OVERLAYED) {
                    sequenceIndices = GeneralUtils.range(0, view.getAlignment().getSequenceCount());
                } else {
                    sequenceIndices = groupIndexToSequenceIndices[j];
                }

                ArrayList<Integer> visibleSequenceIndices = new ArrayList(sequenceIndices.length);
                for (int seq : sequenceIndices) {
                    if (bundleConfig.getSequenceVisibilityModel().isVisible(seq, i)) {
                        visibleSequenceIndices.add(seq);
                    }
                }
                int[] tmp = GeneralUtils.integerArrayToInt(visibleSequenceIndices.toArray(new Integer[]{}));
                Map<Character, Integer> counts = view.getAlignment().getSymbolCounts(i, tmp);
                try {
                    int newmax = Collections.max(counts.values());
                    if (newmax > maxSequencesPerCell[j]) {
                        maxSequencesPerCell[j] = newmax;
                    }
                } catch (NoSuchElementException ex) {
                }
            }
            if (maxSequencesPerCell[j] > 0) {
                int c = (int) Math.ceil(maxSequencesPerCell[j] / (float) bundleConfig.getMaxBundleWidth());
                double newalpha;
                try {
                    newalpha = findalpha(0.0, 0.75, bundleConfig.getMaxAlphaTotal(), c) * 255;
                } catch (MaxIterationsExceededException ex) {
                    newalpha = Math.min(255, 6000 / (view.getAlignment().getSequenceCount() + 1));
                }
                alpha[j] = (int) Math.max(newalpha, bundleConfig.getMinAlphaPerThread() * 255);
            } else {
                alpha[j] = 0;
            }
        }
    }

    private double alphafun(double basealpha, int npixel) {
        if (npixel == 1) {
            return basealpha;
        }
        return basealpha + (1 - basealpha) * alphafun(basealpha, npixel - 1);
    }

    private double findalpha(double left, double right, double target, int npixel) throws MaxIterationsExceededException {
        if (findAlphaIterationCount > MAX_FIND_ALPHA_ITERATIONS) {
            findAlphaIterationCount = 0;
            throw new MaxIterationsExceededException("Number of iterations exceeded (" + MAX_FIND_ALPHA_ITERATIONS + ")");
        }
        findAlphaIterationCount++;
        double leftval = alphafun(left, npixel);
        double rightval = alphafun(right, npixel);
        double slope = (rightval - leftval) / (right - left);

        double middle = (target - leftval) / slope;
        double middleval = alphafun(middle, npixel);
        if (Math.abs(middleval - target) < 0.05) {
            return middle;
        }
        if (middleval < target) {
            return (findalpha(middle, right, target, npixel));
        } else {
            return (findalpha(left, middle, target, npixel));
        }
    }

    private void calculateSize() {
        baseY = bundleConfig.getMaxBundleWidth() + 1;
        maxColumnOffset = 0;
        for (int i = 0; i < bundleConfig.getColumnOffset().length; i++) {
            if (maxColumnOffset < bundleConfig.getColumnOffset()[i]) {
                maxColumnOffset++;
            }
        }
        drawingAreaHeight = 2 * bundleConfig.getOffsetY() + (view.getAlphabet().size() + 1) * bundleConfig.getCellHeight() + maxColumnOffset * bundleConfig.getCellHeight();
        drawingAreaWidth = columnToXRight(view.getAlignment().length() - 1) + 1;
    }

    public int calculateFragmentWidth(int fromCol, int toCol) {
        return columnToXRight((toCol - fromCol) - 1) + 1;
    }

    private void setupArraysAndPositions() {
        cumulativeOffset = new byte[view.getAlignment().getLength()][view.getAlphabet().size() + 1]; // acount for the GAP char, and for not found characters which will return -1 in the index
        yPositions = new int[view.getAlignment().getSequenceCount() + 1][view.getAlignment().getLength()];
        yPositionsInStack = new byte[view.getAlignment().getSequenceCount() + 1][view.getAlignment().getLength()];

        // groupIDs contains the numeric ids of the groups. The position in the
        // indexed set is the group index. This is for the case where the group ids are not consecutive.
        groupIDs = new IndexedTreeSet<>(view.getSequenceGroups().values());
        int nSequencesInDefGroup = view.getAlignment().getSequenceCount() - view.getSequenceGroups().size();
        if (nSequencesInDefGroup > 0) {
            groupIDs.add(0);
        }
        numberOfGroups = groupIDs.size();

        // this array now maps the sequence indices in the alignment to the group indices (not
        // the group ids. So instead of always looking up the group a sequence and then finding
        // its index we can just refer to this array.
        sequenceIndexToGroupIndex = new int[view.getAlignment().getSequenceCount() + 1];
        for (int i = 0; i < view.getAlignment().getSequenceCount(); i++) {
            int group = view.getSequenceGroups().get(view.getAlignment().getSequence(i));
            sequenceIndexToGroupIndex[i] = groupIDs.entryIndex(group);
        }
        sequenceIndexToGroupIndex[view.getAlignment().getSequenceCount()] = 0; // consensus always in group 0 which always has index 0

        // this array does exactly the opposite, it maps group indices to sequence indices
        groupIndexToSequenceIndices = new int[numberOfGroups][];
        for (Integer group : groupIDs) {
            int groupIndex = groupIDs.entryIndex(group);
            List<Integer> sequenceIndexList = new ArrayList<>();
            for (int i = 0; i < view.getAlignment().getSequenceCount(); i++) {
                if (view.getSequenceGroups().get(view.getAlignment().getSequence(i)).equals(group)) {
                    sequenceIndexList.add(i);
                }
            }
            // convert to int[]
            int[] sequenceIndices = new int[sequenceIndexList.size()];
            for (int i = 0; i < sequenceIndexList.size(); i++) {
                sequenceIndices[i] = sequenceIndexList.get(i);
            }
            groupIndexToSequenceIndices[groupIndex] = sequenceIndices;
        }

        StringBuffer consensusChars = new StringBuffer(view.getAlignment().getLength());

        // determine positions, positions in stack and consensus
        for (int seqIndex = 0; seqIndex <= view.getAlignment().getSequenceCount(); seqIndex++) {
            int groupIndex;
            if (bundleConfig.getGroupStacking() == SequenceBundleConfig.GroupStackingType.SEPARATE) {
                groupIndex = sequenceIndexToGroupIndex[seqIndex];
            } else {
                groupIndex = 0; // assume all in default group, i.e. stack on top of each other
            }
            int lastAlphabetIndex = -1;
            for (int colIndex = 0; colIndex < view.getAlignment().getLength(); colIndex++) {
                char position = bundleConfig.getGapChar();
                if (seqIndex < view.getAlignment().getSequenceCount()) {
                    position = view.getAlignment().characterAt(seqIndex, colIndex);
                } else { // consensus
                    Map<Character, Integer> counts = view.getAlignment().getSymbolCounts(colIndex);
                    Integer max = 0;
                    for (Map.Entry<Character, Integer> e : counts.entrySet()) {
                        if (e.getValue() > max) {
                            max = e.getValue();
                            position = e.getKey();
                        }
                    }
                    consensusChars.append(position);
                }
                int alphabetIndex;
                if (position == bundleConfig.getGapChar()) {
                    alphabetIndex = view.getAlphabet().size();
                } else {
                    alphabetIndex = view.getAlphabet().indexOf(position);
                }

                if (lastAlphabetIndex == alphabetIndex) {
                    yPositionsInStack[seqIndex][colIndex] = yPositionsInStack[seqIndex][colIndex - 1];
                } else {
                    yPositionsInStack[seqIndex][colIndex] = cumulativeOffset[colIndex][alphabetIndex];
                    if (bundleConfig.getGroupStacking() == SequenceBundleConfig.GroupStackingType.SEPARATE) {
                        cumulativeOffset[colIndex][alphabetIndex] = (byte) (((cumulativeOffset[colIndex][alphabetIndex] + 1) % getGroupBundleWidth()) & 0xFF);
                    } else {
                        cumulativeOffset[colIndex][alphabetIndex] = (byte) (((cumulativeOffset[colIndex][alphabetIndex] + 1) % bundleConfig.getMaxBundleWidth()) & 0xFF);
                    }
                }
                int idx = colIndex % bundleConfig.getColumnOffset().length;
                int colOffset = bundleConfig.getColumnOffset()[idx];
                yPositions[seqIndex][colIndex] = bundleConfig.getOffsetY() + (bundleConfig.getCellHeight() - bundleConfig.getMaxBundleWidth()) / 2 + 1 + alphabetIndex * bundleConfig.getCellHeight() + groupIndex * getGroupBundleWidth() + yPositionsInStack[seqIndex][colIndex] + colOffset * bundleConfig.getCellHeight();

                lastAlphabetIndex = alphabetIndex;
            }
        }
        consensus = new Sequence("consensus", consensusChars);

        cumulativeOffset = null;
    }

    private CubicCurve2D createCurve(int seq, int col) {
        // setup bezier curves
        int span = getCellSpan();
        int z = seq;
        int ii = col;

        CubicCurve2D curve = null;
        int xfrom = columnToXCenter(ii) + (int) Math.round(span / 2.0);
        int xto = columnToXCenter(ii + 1) - 1 - (int) Math.round(span / 2.0);
        if (ii < view.getAlignment().getLength() - 1) {
            curve = new CubicCurve2D.Float(xfrom, yPositions[z][ii], xfrom + bundleConfig.getTangL(), yPositions[z][ii], xto - bundleConfig.getTangR(), yPositions[z][ii + 1], xto, yPositions[z][ii + 1]);
        }
        return curve;
    }

    private Line2D createLine(int seq, int col) {
        Line2D line;
        int span = getCellSpan();
        int z = seq;
        int ii = col;
        int xfrom = columnToXCenter(ii) + (int) Math.round(span / 2.0);
        int xto = columnToXCenter(ii + 1) - 1 - (int) Math.round(span / 2.0);
        line = new Line2D.Float(xfrom - span - 1, yPositions[z][ii], xfrom - 1, yPositions[z][ii]);
        return line;
    }

    private int getCellSpan() {
        int span = (int) Math.floor(bundleConfig.getCellWidth() * bundleConfig.getHorizontalExtent());
        if (span % 2 == 0) { // even span, make odd so it has a symmetric center pixel
            span = span - 1;
        }
        return span;
    }

    private int calculateAlphabetDistance(int seq, int col) {
        int idxFrom = col % bundleConfig.getColumnOffset().length;
        int colOffsetFrom = bundleConfig.getColumnOffset()[idxFrom];
        int idxTo = (col + 1) % bundleConfig.getColumnOffset().length;
        int colOffsetTo = bundleConfig.getColumnOffset()[idxTo];

        Sequence sequence;
        if (seq == view.getAlignment().getSequenceCount()) {
            sequence = consensus;
        } else {
            sequence = view.getAlignment().getSequence(seq);
        }
        char alphaChar = sequence.charAt(col);
        int alphaIdxFrom = view.getAlphabet().indexOf(alphaChar);
        if (alphaIdxFrom == -1) { // gap char or not found
            alphaIdxFrom = view.getAlphabet().size();
        }
        int alphaIdxTo;
        if (col == view.getAlignment().getLength() - 1) { // last column
            alphaIdxTo = alphaIdxFrom;
            colOffsetTo = colOffsetFrom;
        } else {
            alphaIdxTo = view.getAlphabet().indexOf(sequence.charAt(col + 1));
        }

        if (alphaIdxTo == -1) { // gap char or not found
            alphaIdxTo = view.getAlphabet().size();
        }
        return (alphaIdxFrom + colOffsetFrom - (alphaIdxTo + colOffsetTo));
    }

    private int getGroupBundleWidth() {
        int groupBundleWidth = bundleConfig.getMaxBundleWidth() / numberOfGroups;
        return groupBundleWidth;
    }

    // start render methods
    private synchronized void renderBundleFragments(RenderWorker worker) {
        if (this.lineFragments != null && this.fragments != null) {
            return;
        }

        BufferedImage[][][][] localFragments;
        BufferedImage[] localLineFragments;

        // we need one fragment per alphabet x alphabet
        int span = getCellSpan();
        int xFrom = bundleConfig.getCellWidth() / 2 + (int) Math.round(span / 2.0);
        int xTo = bundleConfig.getCellWidth() + bundleConfig.getCellWidth() / 2 - 1 - (int) Math.round(span / 2.0);
        int width = Math.abs(xFrom - xTo) + 1;

        // draw line fragment
        localLineFragments = new BufferedImage[numberOfGroups + 1];
        for (int k = 0; k <= numberOfGroups; k++) {
            if (worker.isCancelled()) {
                return;
            }
            localLineFragments[k] = createCompatibleImage(span + 1, 1);
            Graphics2D gline = createCompatibleImageGraphics(localLineFragments[k]);

            if (k < numberOfGroups) {
                Color groupColor = bundleConfig.getColorModel().getGroupColors().get(groupIDs.exact(k));
                Color adjustedColor = new Color(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), alpha[k]);
                gline.setColor(adjustedColor);
            } else {
                gline.setColor(bundleConfig.getColorModel().getSelectionColor());
            }
            gline.drawLine(0, 0, span, 0);
            gline.dispose();
        }

        // draw curve fragments
        int maxOffset = 0;
        for (int i = 0; i < bundleConfig.getColumnOffset().length; i++) {
            if (maxOffset < bundleConfig.getColumnOffset()[i]) {
                maxOffset = bundleConfig.getColumnOffset()[i];
            }
        }
        double current = 0;
        double max = (view.getAlphabet().size() + 1. + maxOffset) * (2. * bundleConfig.getMaxBundleWidth());
        double progress = 0.;
        localFragments = new BufferedImage[view.getAlphabet().size() + 1 + maxOffset][2 * bundleConfig.getMaxBundleWidth()][2][numberOfGroups + 1];
        for (int i = 0; i < (view.getAlphabet().size() + 1 + maxOffset); i++) {
            for (int j = 0; j < 2 * bundleConfig.getMaxBundleWidth(); j++) {
                progress = current / max;
                current += 1;
                System.out.println("render bundle fragments progress: " + MessageFormat.format("{0,number,#.##%}", progress));
                int alphaHeight = i * bundleConfig.getCellHeight();
                int stackHeight = j - bundleConfig.getMaxBundleWidth();
                int totalHeight = Math.abs(alphaHeight + stackHeight) + 1;
                CubicCurve2D curveDown = new CubicCurve2D.Float(0, 0, 0 + bundleConfig.getTangL(), 0, width - 1 - bundleConfig.getTangR(), totalHeight - 1, width - 1, totalHeight - 1);
                CubicCurve2D curveUp = new CubicCurve2D.Float(0, totalHeight - 1, 0 + bundleConfig.getTangL(), totalHeight - 1, width - 1 - bundleConfig.getTangR(), 0, width - 1, 0);
                for (int k = 0; k <= numberOfGroups; k++) {
                    if (worker.isCancelled()) {
                        return;
                    }
                    Color adjustedColor;
                    if (k < numberOfGroups) {
                        Color groupColor = bundleConfig.getColorModel().getGroupColors().get(groupIDs.exact(k));
                        adjustedColor = new Color(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), alpha[k]);
                    } else {
                        adjustedColor = bundleConfig.getColorModel().getSelectionColor();
                    }

                    // render down curves
                    localFragments[i][j][CURVE_DOWN][k] = createCompatibleImage(width, totalHeight);
                    Graphics2D g = createCompatibleImageGraphics(localFragments[i][j][CURVE_DOWN][k]);
                    g.setColor(adjustedColor);
                    g.draw(curveDown);
                    g.dispose();

                    // render up curves
                    localFragments[i][j][CURVE_UP][k] = createCompatibleImage(width, totalHeight);
                    g = createCompatibleImageGraphics(localFragments[i][j][CURVE_UP][k]);
                    g.setColor(adjustedColor);
                    g.draw(curveUp);
                    g.dispose();
                }
            }
        }

        this.lineFragments = localLineFragments;
        this.fragments = localFragments;
    }

    BufferedImage[] renderCellShadingFragments() {
        BufferedImage[] cellShadingFragments = new BufferedImage[view.getAlphabet().size() + 1];
        for (int i = 0; i <= view.getAlphabet().size(); i++) {
            cellShadingFragments[i] = createCompatibleImage(bundleConfig.getCellWidth(), bundleConfig.getCellHeight());
            Graphics2D g = createCompatibleImageGraphics(cellShadingFragments[i]);
            renderCellShadingFragment(g, i);
            g.dispose();
        }
        return cellShadingFragments;
    }

    private void renderCellShadingFragment(Graphics2D g, int alphaIdx) {
        int idx = alphaIdx % 2;
        Color color;
        if (idx == 0) {
            color = Color.BLACK;
        } else {
            color = Color.WHITE;
        }
        Color finalColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 10);
        g.setColor(finalColor);
        g.fillRect(0, 0, bundleConfig.getCellWidth(), bundleConfig.getCellHeight());
    }

    void renderCellShading(Graphics2D g) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);
        GridPoint.GridRectangle gr = new GridPoint.GridRectangle(new GridPoint(0, 0), new GridPoint(view.getAlignment().length(), view.getAlphabet().size()));
        for (GridPoint p : gr) {
            Point pixel = gridToPixel(p);
            g.translate(pixel.x, pixel.y);
            renderCellShadingFragment(g, p.y);
            g.translate(-pixel.x, -pixel.y);
        }
        g.dispose();
    }

    BufferedImage[] renderAlphabetOverlayFragments() {
        BufferedImage[] alphabetOverlayFragments = new BufferedImage[view.getAlphabet().size() + 1];
        for (int i = 0; i <= view.getAlphabet().size(); i++) {
            alphabetOverlayFragments[i] = createCompatibleImage(bundleConfig.getCellWidth(), bundleConfig.getCellHeight());
            Graphics2D g = createCompatibleImageGraphics(alphabetOverlayFragments[i]);
            renderAlphabetOverlayFragment(g, i);
            g.dispose();
        }
        return alphabetOverlayFragments;
    }

    private void renderAlphabetOverlayFragment(Graphics2D g, int alphaIdx) {
        Character c;
        if (alphaIdx < view.getAlphabet().size()) {
            c = view.getAlphabet().characterAt(alphaIdx);
        } else {
            c = bundleConfig.getGapChar();
        }
        Color color = view.getBundleConfig().getColorModel().getAlphabetColors().get(c);
        if (color == null || c == bundleConfig.getGapChar()) {
            color = view.getBundleConfig().getColorModel().getAlphabetColors().getDefaultColor();
        }
        if (color != null) {
            Color finalColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 10);
            g.setColor(finalColor);
            g.fillRect(0, 0, bundleConfig.getCellWidth(), bundleConfig.getCellHeight());
        }

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(c.toString(), g);
        if (color != null) {
            g.setColor(color);
        } else {
            g.setColor(SequenceBundleColorModel.DEFAULT_ALPHABET_COLOR);
        }
        g.drawString(c.toString(), (int) ((bundleConfig.getCellWidth() - bounds.getWidth()) / 2) + 1, (int) ((bundleConfig.getCellHeight() + bounds.getHeight() - 2) / 2) - 1);
    }

    void renderAlphabetOverlay(Graphics2D g) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);
        GridPoint.GridRectangle gr = new GridPoint.GridRectangle(new GridPoint(0, 0), new GridPoint(view.getAlignment().length(), view.getAlphabet().size()));
        for (GridPoint p : gr) {
            Point pixel = gridToPixel(p);
            g.translate(pixel.x, pixel.y);
            renderAlphabetOverlayFragment(g, p.y);
            g.translate(-pixel.x, -pixel.y);
        }
        g.dispose();
    }

    void renderSelectionMarker(Graphics2D g) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);

        int cellWidth = bundleConfig.getCellWidth();
        int cellHeight = bundleConfig.getCellHeight();

        g.setColor(bundleConfig.getColorModel().getSelectionMarkerColor());
        g.fillRect(0, 0, cellWidth, cellHeight);

        g.dispose();
    }

    BufferedImage renderSelectionMarker() {
        int cellWidth = bundleConfig.getCellWidth();
        int cellHeight = bundleConfig.getCellHeight();

        BufferedImage selectionMarker = createCompatibleImage(cellWidth, cellHeight);
        Graphics2D g2 = createCompatibleImageGraphics(selectionMarker);
        renderSelectionMarker(g2);
        g2.dispose();

        return selectionMarker;
    }

    void renderColumnMarker(Graphics2D g2) {
        g2 = (Graphics2D) g2.create();
        setGlobalGraphicsParameters(g2);

        int offset = 3;
        int size = Math.min(23, bundleConfig.getCellWidth());

        g2.setColor(bundleConfig.getColorModel().getSelectionMarkerColor());
        g2.translate(0, 0);
        Polygon p = new Polygon();
        p.addPoint(0 + offset, 0 + offset);
        p.addPoint(size - offset, 0 + offset);
        p.addPoint(size / 2, size - offset);
        g2.fillPolygon(p);
        g2.setColor(bundleConfig.getColorModel().getSelectionCaretColor());
        g2.drawPolygon(p);

        g2.dispose();
    }

    BufferedImage renderColumnMarker() {
        int size = Math.min(23, bundleConfig.getCellWidth());
        BufferedImage columnMarker = createCompatibleImage(size, size);
        Graphics2D g2 = createCompatibleImageGraphics(columnMarker);
        renderColumnMarker(g2);
        g2.dispose();

        return columnMarker;
    }

    void renderSelectionCaret(Graphics2D g2) {
        g2 = (Graphics2D) g2.create();
        setGlobalGraphicsParameters(g2);

        int cellWidth = bundleConfig.getCellWidth();
        int cellHeight = bundleConfig.getCellHeight();

        g2.setColor(bundleConfig.getColorModel().getSelectionCaretColor());
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(0, 0, cellWidth - 1, cellHeight - 1);

        g2.dispose();
    }

    BufferedImage renderSelectionCaret() {
        int cellWidth = bundleConfig.getCellWidth();
        int cellHeight = bundleConfig.getCellHeight();

        BufferedImage selectionCaret = createCompatibleImage(cellWidth, cellHeight);
        Graphics2D g2 = createCompatibleImageGraphics(selectionCaret);
        renderSelectionCaret(g2);
        g2.dispose();

        return selectionCaret;
    }

    void renderConsensusSequence(Graphics2D g, int columnFrom, int columnTo) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);

        int[] consensusIndex = new int[]{view.getAlignment().getSequenceCount()}; // consensus is first additional
        Color[] consensusColor = new Color[]{bundleConfig.getColorModel().getConsensusColor()};
        int[] segmentIndices = GeneralUtils.range(columnFrom, columnTo);
        Stroke s = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.translate(-columnToXLeft(columnFrom), 0);
        drawBundleSegments(g, consensusIndex, segmentIndices, consensusColor, false, true);
        g.setStroke(s);

        g.dispose();
    }

    BufferedImage renderConsensusSequence(int columnFrom, int columnTo) {
        BufferedImage img = createCompatibleImage(columnToXRight(columnTo) - columnToXLeft(columnFrom), getDrawingAreaHeight());
        Graphics2D g = createCompatibleImageGraphics(img);
        renderConsensusSequence(g, columnFrom, columnTo);
        g.dispose();

        return img;
    }

    RenderWorker renderFragmentSequences(Graphics2D gc, RenderingStrategy renderingStrategy, int fromIndex, int toIndex) {
        Graphics2D[] g = new Graphics2D[numberOfGroups];
        for (int i = 0; i < g.length; i++) {
            g[i] = gc;
        }
        //return renderSequences(g, renderingStrategy);

        Color[] groupColors = new Color[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            int groupID = groupIDs.exact(i);
            groupColors[i] = bundleConfig.getColorModel().getGroupColors().get(groupID);
        }

        // return drawSequencesInBackground(g, groupIndexToSequenceIndices, groupColors, false, renderingStrategy);
//        return drawSequencesInBackground(g, groupIndexToSequenceIndices, groupColors, false, renderingStrategy);
        // return drawSequencesInBackground(graphics, sequenceIndices, 0, view.getAlignment().length() - 1, colors, isSelection, renderingStrategy);
        return drawSequencesInBackground(g, groupIndexToSequenceIndices, fromIndex, toIndex, groupColors, false, renderingStrategy);

    }

    RenderWorker renderSequences(Graphics2D gc, RenderingStrategy renderingStrategy) {
        Graphics2D[] g = new Graphics2D[numberOfGroups];
        for (int i = 0; i < g.length; i++) {
            g[i] = gc;
        }
        return renderSequences(g, renderingStrategy);
    }

    RenderWorker renderSequences(Graphics2D[] g, RenderingStrategy renderingStrategy) {
        Color[] groupColors = new Color[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            int groupID = groupIDs.exact(i);
            groupColors[i] = bundleConfig.getColorModel().getGroupColors().get(groupID);
        }
        return drawSequencesInBackground(g, groupIndexToSequenceIndices, groupColors, false, renderingStrategy);
    }

    RenderWorker renderSequences(Graphics2D[] g, int columnFrom, int columnTo, RenderingStrategy renderingStrategy) {
        Color[] groupColors = new Color[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            int groupID = groupIDs.exact(i);
            groupColors[i] = bundleConfig.getColorModel().getGroupColors().get(groupID);
        }
        return drawSequencesInBackground(g, groupIndexToSequenceIndices, columnFrom, columnTo, groupColors, false, renderingStrategy);
    }

    RenderWorker renderSequences(DiskMemImage[] sequenceImages, RenderingStrategy renderingStrategy) {
        Graphics2D[] graphics = new Graphics2D[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            sequenceImages[i] = createDiskMemImage(getDrawingAreaWidth(), getDrawingAreaHeight());
            graphics[i] = sequenceImages[i].createGraphics();
        }
        return renderSequences(graphics, renderingStrategy);
    }

    RenderWorker renderSequences(DiskMemImage[] sequenceImages, int columnFrom, int columnTo, RenderingStrategy renderingStrategy) {
        Graphics2D[] graphics = new Graphics2D[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            graphics[i] = createDiskMemImageGraphics(sequenceImages[i]);
            graphics[i].translate(columnToXLeft(columnFrom), 0);
        }
        return renderSequences(graphics, columnFrom, columnTo, renderingStrategy);
    }

    DiskMemImage renderSelectedSequences(int[] selectionIndices) {
        DiskMemImage selectedSequenceImage = createDiskMemImage(getDrawingAreaWidth(), getDrawingAreaHeight());
        renderSelectedSequences(selectedSequenceImage, selectionIndices, 0, view.getAlignment().length() - 1);
        return selectedSequenceImage;
    }

    RenderWorker renderSelectedSequences(DiskMemImage selectedSequenceImage, int[] selectionIndices, int columnFrom, int columnTo) {
        RenderWorker worker = null;
        if (selectionIndices != null && selectionIndices.length > 0) {
            Graphics2D g = selectedSequenceImage.createGraphics();
            g.translate(columnToXLeft(columnFrom), 0);
            worker = drawSequencesInBackground(new Graphics2D[]{g}, new int[][]{selectionIndices}, columnFrom, columnTo, null, true, RenderingStrategy.FRAGMENT_RENDERER);
        }
        return worker;
    }

    /**
     * Renders the column headers at the correct position in the bundle
     *
     * @param g
     */
    void renderColumnHeaders(Graphics2D g) {
        renderColumnHeaders(g, 0, view.getAlignment().length() - 1);
    }

    void renderColumnHeaders(Graphics2D g, int columnFrom, int columnTo) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);

        g.setColor(TEXT_COLOR);
        for (int i = columnFrom; i <= columnTo; i++) {
            int xposchar = columnToXCenter(i - columnFrom);
            String text = Integer.toString(i + 1);
            Font f = g.getFont();
            TextLayout layout = new TextLayout(text, f, g.getFontRenderContext());
            while (layout.getBounds().getWidth() >= (bundleConfig.getCellWidth() - 4)) {
                f = new Font(f.getName(), f.getStyle(), f.getSize() - 1);
                layout = new TextLayout(text, f, g.getFontRenderContext());
                g.setFont(f);
            }
            g.drawString(text, xposchar - (int) layout.getBounds().getWidth() / 2, bundleConfig.getOffsetY() - 5);
        }

        g.dispose();
    }

    DiskMemImage renderColumnHeaders() {
        DiskMemImage img = createDiskMemImage(getDrawingAreaWidth(), bundleConfig.getOffsetY());
        Graphics2D g = createDiskMemImageGraphics(img);
        renderColumnHeaders(g);
        g.dispose();

        return img;
    }

    /**
     * Renders the background horizontal lines at the positions defined by the
     * config object.
     *
     * @param gc
     */
    void renderBackgroundHorizontalLines(Graphics2D gc) {
        gc = (Graphics2D) gc.create();
        setGlobalGraphicsParameters(gc);
        for (int col = 0; col < view.getAlignment().length(); col++) {
            Point p = gridToPixel(new GridPoint(col, 0));
            gc.translate(p.x, p.y);
            renderBackgroundHorizontalLines(gc, bundleConfig.getCellWidth());
            gc.translate(-p.x, -p.y);
        }
        gc.dispose();
    }

    /**
     * Internal rendering function to render the actual line of a given length
     * without absolute positioning in the bundle.
     *
     * @param g
     * @param width
     */
    private void renderBackgroundHorizontalLines(Graphics2D g, int width) {
        g.setColor(LINE_COLOR);

        g.drawLine(0, 0, width, 0);
        for (int i = 0; i <= view.getAlphabet().size(); i++) {
            int offset = (i + 1) * bundleConfig.getCellHeight();
            g.drawLine(0, offset, width, offset);
        }
    }

    /**
     * Renders a BufferedImage fragment of a horizontal line without absolute
     * placement in the bundle for later placement.
     *
     * @return
     */
    BufferedImage renderBackgroundHorizontalLineFragment() {
        int width = bundleConfig.getCellWidth();
        int height = getDrawingAreaHeight();
        BufferedImage img = createCompatibleImage(width, height);
        Graphics2D g = createCompatibleImageGraphics(img);
        renderBackgroundHorizontalLines(g, width);
        g.dispose();

        return img;
    }

    /**
     * Renders the background vertical lines at the correct positions in the
     * given graphics context.
     *
     * @param g
     */
    void renderBackgroundVerticalLines(Graphics2D g) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);
        g.translate(0, bundleConfig.getOffsetY());
        for (int col = 0; col < view.getAlignment().length(); col++) {
            g.translate(columnToXLeft(col), 0);
            renderBackgroundVerticalLine(g);
            g.translate(-columnToXLeft(col), 0);
        }

        g.dispose();
    }

    /**
     * Renders the background vertical lines at the correct positions in the
     * given graphics context.
     *
     * @param g
     */
    void renderBackgroundVerticalLines(Graphics2D g, int fromCol, int toCol) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);
        g.translate(0, bundleConfig.getOffsetY());
        for (int col = fromCol; col < toCol; col++) {
            g.translate(columnToXLeft(col), 0);
            renderBackgroundVerticalLine(g);
            g.translate(-columnToXLeft(col), 0);
        }

        g.dispose();
    }

    /**
     * Internal rendering routine that draws the lines
     *
     * @param g
     * @param width
     */
    private void renderBackgroundVerticalLine(Graphics2D g) {
        // begin draw vertical divider lines
        g.setColor(LINE_COLOR);
        int xpos = columnToXLeft(0);
        int height = maxColumnOffset * bundleConfig.getCellHeight() + (view.getAlphabet().size() + 1) * bundleConfig.getCellHeight();
        g.drawLine(xpos, 0, xpos, height);
    }

    /**
     * Renders an image of the vertical lines to be placed later, one image is
     * one column of the bundle
     *
     * @return
     */
    BufferedImage renderBackgroundVerticalLineFragment() {
        BufferedImage img = createCompatibleImage(bundleConfig.getCellWidth(), getDrawingAreaHeight());
        Graphics2D g = createCompatibleImageGraphics(img);
        renderBackgroundVerticalLine(g);
        g.dispose();
        return img;
    }

    /**
     * Draws the given sequence indices on a worker thread;
     *
     * @param img An array of images to which to write to
     * @param sequenceIndices An array of array of int, giving the indices to
     * draw per image
     * @param colors One color object per image
     * @param adjustAlpha whether to adjust the alpha
     */
    private RenderWorker drawSequencesInBackground(Graphics2D[] graphics, int[][] sequenceIndices, int columnFrom, int columnTo, Color[] colors, boolean isSelection, RenderingStrategy renderingStrategy) {
        if (view.getAlignment().getLength() == 0 || sequenceIndices.length == 0) {
            return null;
        }

        RenderWorker worker = null;

        if (mainWorker != null && !isSelection) {
            mainWorker.cancel(true);
        }
        if (selectionWorker != null && isSelection) {
            selectionWorker.cancel(true);
//			fireRenderingCancelled(new SequenceBundleRendererEvent(this));
        }

        switch (renderingStrategy) {
            case FRAGMENT_RENDERER:
                worker = new FragmentRenderWorker(graphics, sequenceIndices, columnFrom, columnTo, isSelection);
                break;
            case TILE_RENDERER:
                worker = new TileRenderWorker(graphics, sequenceIndices, colors, !isSelection);
                break;
            case DIRECT_RENDERER:
                worker = new DirectRenderWorker(graphics, sequenceIndices, colors, !isSelection);
                break;
        }

        if (isSelection) {
            selectionWorker = threadPool.submit(worker);
        } else {
            mainWorker = threadPool.submit(worker);
        }

//		worker.execute();
        return worker;
    }

    private RenderWorker drawSequencesInBackground(Graphics2D[] graphics, int[][] sequenceIndices, Color[] colors, boolean isSelection, RenderingStrategy renderingStrategy) {
        return drawSequencesInBackground(graphics, sequenceIndices, 0, view.getAlignment().length() - 1, colors, isSelection, renderingStrategy);
    }

    void drawBundleSegments(Graphics2D g, int[] sequenceIndices, int[] segmentIndices, Color[] colors, boolean adjustAlpha, boolean honorVisibilityModel) {
        g = (Graphics2D) g.create();
        setGlobalGraphicsParameters(g);

        for (int y : segmentIndices) {
            for (int i = 0; i < sequenceIndices.length; i++) {
                int z = sequenceIndices[i];
                if (honorVisibilityModel) {
                    if (!(bundleConfig.getSequenceVisibilityModel().isVisible(z, y) && bundleConfig.getSequenceVisibilityModel().isVisible(z, y + 1))) {
                        continue;
                    }
                }
                Color color = colors[i % colors.length]; // recycle if necessary
                if (!adjustAlpha) {
                    g.setColor(color);
                } else {
                    int groupIndex = sequenceIndexToGroupIndex[z];
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.alpha[groupIndex]));
                }
                if (y < view.getAlignment().getLength() - 1) {
                    g.draw(createCurve(z, y));
                }
                g.draw(createLine(z, y));
            }
        }
        g.dispose();
    }

    public int rowToYTop(int row, int col) {
        int idx = col % bundleConfig.getColumnOffset().length;
        int y = bundleConfig.getOffsetY() + row * bundleConfig.getCellHeight();
        if (idx >= 0 && row >= 0) {
            y += bundleConfig.getColumnOffset()[idx] * bundleConfig.getCellHeight();
        }
        return y;
    }

    protected int columnToXCenter(int column) {
        int x;
        x = (columnToXLeft(column) + columnToXRight(column)) / 2;
        return x;
    }

    protected int columnToXLeft(int column) {
        int x;
        x = column * bundleConfig.getCellWidth();
        return x;
    }

    protected int columnToXRight(int column) {
        return columnToXLeft(column + 1);
    }

    int xToColumn(int x) {
        return x / bundleConfig.getCellWidth();
    }

    /**
     * Returns the pixel coordinates of the current grid point. Pixel
     * coordinates include the legend image, i.e. they are component
     * coordinates.
     *
     * @param p
     * @return
     */
    Point gridToPixel(GridPoint p) {
        Point retval = new Point(columnToXLeft(p.x), rowToYTop(p.y, p.x));
        return retval;
    }

    public void setDrawingAreaHeight(int height) {
        this.drawingAreaHeight = height;
    }

    /**
     * @return the yPositions
     */
    public int[][] getYPositions() {
        return yPositions;
    }

    /**
     * @return the drawingAreaHeight
     */
    public int getDrawingAreaHeight() {
        return drawingAreaHeight;
    }

    /**
     * @return the drawingAreaWidth
     */
    public int getDrawingAreaWidth() {
        return drawingAreaWidth;
    }

    /**
     * @return the initialised
     */
    public boolean isInitialised() {
        return initialised;
    }

    // event listener support
    public void addSequenceBundleRendererListener(SequenceBundleRendererListener l) {
        this.listenerList.add(SequenceBundleRendererListener.class, l);
    }

    public void removeSequenceBundleRendererListener(SequenceBundleRendererListener l) {
        this.listenerList.remove(SequenceBundleRendererListener.class, l);
    }

    protected void fireRenderingStarted(SequenceBundleRendererEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SequenceBundleRendererListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new SequenceBundleRendererEvent(this);
                }
                ((SequenceBundleRendererListener) listeners[i + 1]).renderingStarted(event);
            }
        }
    }

    protected void fireRenderingFinished(SequenceBundleRendererEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SequenceBundleRendererListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new SequenceBundleRendererEvent(this);
                }
                ((SequenceBundleRendererListener) listeners[i + 1]).renderingFinished(event);
            }
        }
    }

    protected void fireRenderingCancelled(SequenceBundleRendererEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SequenceBundleRendererListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new SequenceBundleRendererEvent(this);
                }
                ((SequenceBundleRendererListener) listeners[i + 1]).renderingCancelled(event);
            }
        }
    }

    protected void fireRenderingProgressed(SequenceBundleRendererEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SequenceBundleRendererListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new SequenceBundleRendererEvent(this);
                }
                ((SequenceBundleRendererListener) listeners[i + 1]).renderingProgressed(event);
            }
        }
    }

    private class MaxIterationsExceededException extends Exception {

        public MaxIterationsExceededException(String msg) {
            super(msg);
        }
    }

    private abstract class RenderWorker extends SwingWorker<Object, Integer> {
    }

    private class FragmentRenderWorker extends RenderWorker {

        private long time;
        private final int[][] sequenceIndices;
        private final Graphics2D[] graphics;
        private final boolean isSelection;
        private final int id = Random.nextInt(100);
        private final int columnFrom;
        private final int columnTo;
        private final int columns;

        public FragmentRenderWorker(Graphics2D[] graphics, int[][] sequenceIndices, int columnFrom, int columnTo, boolean isSelection) {
            this.graphics = graphics;
            this.sequenceIndices = sequenceIndices;
            this.isSelection = isSelection;
            this.columnFrom = columnFrom;
            this.columnTo = columnTo;
            this.columns = columnTo - columnFrom;

        }

        @Override
        public Object doInBackground() throws Exception {
            if (this.isCancelled()) {
                throw new CancellationException("user cancelled");
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireRenderingStarted(new SequenceBundleRendererEvent(this, 0, 0, columns * numberOfGroups));
                }
            });

            time = System.currentTimeMillis();
            System.out.println("Worker (" + id + ") start");
            renderBundleFragments(this);

            for (int grp = 0; grp < sequenceIndices.length; grp++) { // iterate over groups
                int fragmentSlot;
                if (isSelection) {
                    fragmentSlot = numberOfGroups;
                } else {
                    fragmentSlot = grp;
                }
                int width = (columnTo - columnFrom + 1) * bundleConfig.getCellWidth();
                Graphics2D g;
                Image tmp;
                if (!GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
                    tmp = createCompatibleVolatileImage(width, getDrawingAreaHeight());
                    g = createCompatibleImageGraphics((VolatileImage) tmp);
                } else {
                    tmp = createCompatibleImage(width, getDrawingAreaHeight());
                    g = createCompatibleImageGraphics((BufferedImage) tmp);
                }
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, getDrawingAreaWidth(), getDrawingAreaHeight());
                g.setComposite(AlphaComposite.SrcOver);
                g.translate(-columnToXLeft(columnFrom), 0);
                int colStart = Math.max(0, columnFrom - 1);
                int colEnd = Math.min(view.getAlignment().getLength() - 1, columnTo);

                Map<Character, Integer> countsHere = null;
                Map<Character, Integer> countsNext = view.getAlignment().getSymbolCounts(colStart);

                for (int col = colStart; col <= colEnd; col++) {
                    boolean lastColumn = col == view.getAlignment().getLength() - 1;
                    countsHere = countsNext;
                    if (!lastColumn) {
                        countsNext = view.getAlignment().getSymbolCounts(col + 1);
                    }
                    for (int seq : sequenceIndices[grp]) {
                        if (this.isCancelled()) {
                            throw new CancellationException("user cancelled");
                        }

                        if (!lastColumn && (!bundleConfig.getSequenceVisibilityModel().isVisible(seq, col) || !bundleConfig.getSequenceVisibilityModel().isVisible(seq, col + 1))) {
                            continue;
                        } else if (lastColumn && !bundleConfig.getSequenceVisibilityModel().isVisible(seq, col)) {
                            continue;
                        }

                        int span = getCellSpan();
                        int xFrom = columnToXCenter(col) + (int) Math.round(span / 2.0);

                        int yFrom = yPositions[seq][col];
                        int yTo;
                        if (lastColumn) {
                            yTo = yFrom;
                        } else {
                            yTo = yPositions[seq][col + 1];
                        }

                        int alphaDifference = Math.abs(calculateAlphabetDistance(seq, col));

                        // check if we're moving to or from a gap column in case of disconnected gap rendering
                        boolean movingToGap = false;
                        if (bundleConfig.getGapRendering().equals(SequenceBundleConfig.GapRenderingType.DISCONNECTED)) {
                            if (seq < view.getAlignment().getSequenceCount() && col < view.getAlignment().length() - 1) {
                                char posThis = view.getAlignment().characterAt(seq, col);
                                char posNext = view.getAlignment().characterAt(seq, col + 1);
                                if (posThis == bundleConfig.getGapChar() ^ posNext == bundleConfig.getGapChar()) { // xor
                                    movingToGap = true;
                                }
                            }
                        }
                        int stackDifference;

                        // check if the conservation is below the given threshold
                        double conservationHere = (double) countsHere.get(view.getAlignment().characterAt(seq, col)) / view.getAlignment().getSequenceCount();
                        double conservationNext = conservationHere;
                        if (!lastColumn) {
                            conservationNext = (double) countsNext.get(view.getAlignment().characterAt(seq, col + 1)) / view.getAlignment().getSequenceCount();
                        }
                        boolean conservationAnyBelowThreshold = conservationHere < bundleConfig.getConservationThreshold() || conservationNext < bundleConfig.getConservationThreshold();
                        boolean conservationBothBelowThreshold = conservationHere < bundleConfig.getConservationThreshold() && conservationNext < bundleConfig.getConservationThreshold();
                        boolean conservationCurrentBelowThreshold = conservationHere < bundleConfig.getConservationThreshold();

                        if (!lastColumn && !movingToGap && !conservationAnyBelowThreshold) {
                            int curveType;
                            int yTopLeft;
                            if (yFrom <= yTo) {
                                yTopLeft = yFrom;
                                curveType = CURVE_DOWN;
                                stackDifference = yPositionsInStack[seq][col + 1] - yPositionsInStack[seq][col] + bundleConfig.getMaxBundleWidth();
                            } else {
                                yTopLeft = yTo;
                                curveType = CURVE_UP;
                                stackDifference = yPositionsInStack[seq][col] - yPositionsInStack[seq][col + 1] + bundleConfig.getMaxBundleWidth();
                            }
                            if (this.isCancelled()) {
                                return null;
                            }
                            g.drawImage(fragments[alphaDifference][stackDifference][curveType][fragmentSlot], xFrom, yTopLeft, null);
                        }
                        if (this.isCancelled()) {
                            throw new CancellationException("user cancelled");
                        }
                        if (!conservationCurrentBelowThreshold && !conservationBothBelowThreshold) {
//						if (!conservationAnyBelowThreshold) {
                            g.drawImage(lineFragments[fragmentSlot], xFrom - span - 1, yFrom, null);
                        }
                    }

                    publish(grp * view.getAlignment().getLength() + col);
                }
                g.dispose();
//				javax.imageio.ImageIO.write(tmp.getSnapshot(), "png", new java.io.File("./test"+grp+".png"));
                graphics[grp].drawImage(tmp, 0, 0, null);
            }

            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            if (!chunks.isEmpty()) {
                Integer last = chunks.get(chunks.size() - 1);
                fireRenderingProgressed(new SequenceBundleRendererEvent(this, columnFrom * numberOfGroups, last - columnFrom, columns * numberOfGroups));
            }
        }

        @Override
        protected void done() {
            boolean cancelled = false;
            try {
                get();
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            } catch (InterruptedException | CancellationException ex) {
                cancelled = true;
            }

            cancelled = cancelled || this.isCancelled();

            time = System.currentTimeMillis() - time;
            if (!cancelled) {
                System.out.println("Worker (" + id + ") end. Rendering time: " + new DecimalFormat("##.####").format(time / 1000.0));
                fireRenderingFinished(new SequenceBundleRendererEvent(this, 0, columns * numberOfGroups, view.getAlignment().getLength() * numberOfGroups));
            } else {
                System.out.println("Worker (" + id + ")cancelled.");
                fireRenderingCancelled(new SequenceBundleRendererEvent(this, 0, columns * numberOfGroups, view.getAlignment().getLength() * numberOfGroups));
            }
        }

    };

    private class TileRenderWorker extends RenderWorker {

        private long time;
        private final int[][] sequenceIndices;
        private final Color[] colors;
        private final boolean adjustAlpha;
        private final int ntiles;
        private final Graphics2D[] graphics;

        public TileRenderWorker(Graphics2D[] graphics, int[][] sequenceIndices, Color[] colors, boolean adjustAlpha) {
            this.graphics = graphics;
            this.sequenceIndices = sequenceIndices;
            this.colors = colors;
            this.adjustAlpha = adjustAlpha;
            this.ntiles = view.getAlignment().getLength() / COLUMNS_PER_TILE + 1;
        }

        @Override
        public Object doInBackground() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireRenderingStarted(new SequenceBundleRendererEvent(this, 0, 0, ntiles));
                }
            });

            time = System.currentTimeMillis();
            System.out.println("Worker start");

            BufferedImage tileImg = createCompatibleImage(getTileWidth(), getTileHeight());
            Graphics2D g = createCompatibleImageGraphics(tileImg);
            AffineTransform baseTransform = g.getTransform();

            for (int grp = 0; grp < sequenceIndices.length; grp++) { // iterate over groups
                for (int tile = 0; tile < ntiles; tile++) {
                    if (this.isCancelled()) {
                        g.dispose();
                        return null;
                    }

                    final int ftile = tile;
                    int tileX = ftile * getTileWidth();

                    // clear image
                    g.setTransform(baseTransform);
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(0, 0, getTileWidth(), getTileHeight());
                    g.setComposite(AlphaComposite.SrcOver);

                    // tile translation
                    g.translate(-tileX, 0);

                    // add the dpi correcton as the first step in the transform
                    AffineTransform t = g.getTransform();
                    int dpiCompensation = (int) Math.round(bundleConfig.getDpi() / SequenceBundleConfig.DEFAULT_DPI) - 1;
                    AffineTransform newt = AffineTransform.getTranslateInstance(dpiCompensation, 0);
                    newt.concatenate(t);
                    g.setTransform(newt);

                    if (!adjustAlpha) {
                        g.setColor(colors[grp]);
                    } else {
                        g.setColor(new Color(colors[grp].getRed(), colors[grp].getGreen(), colors[grp].getBlue(), alpha[grp]));
                    }

                    Path2D[] paths = constructPaths(ftile, sequenceIndices[grp]);
                    for (Path2D p : paths) {
                        g.draw(p);
                    }

                    AffineTransform at = (AffineTransform) graphics[grp].getTransform().clone();
                    graphics[grp].setTransform(new AffineTransform(1.0, at.getShearY(), at.getShearX(), 1.0, at.getTranslateX(), at.getTranslateY()));
                    graphics[grp].drawImage(tileImg, new AffineTransform(1.0, 0, 0, 1.0, ftile * getScaledTileWidth(), 0), null);
                    publish(ftile);
                }
            }

            g.dispose();

            return null;
        }

        private Path2D[] constructPaths(int tile, int[] sequences) {
            final int extraColumns = 1;
            int columnFrom = Math.max(0, tile * COLUMNS_PER_TILE - extraColumns);
            int columnTo = Math.min(view.getAlignment().getLength() - 1, (tile + 1) * COLUMNS_PER_TILE - 1 + extraColumns);
            Path2D.Float[] paths = new Path2D.Float[sequences.length];

            int span = getCellSpan();
            for (int s = 0; s < sequences.length; s++) {
                paths[s] = new Path2D.Float();
                int seqIndex = sequences[s];
                for (int ii = columnFrom; ii <= columnTo; ii++) {
                    int xfrom = columnToXCenter(ii) + (int) Math.round(span / 2.0);

                    if (ii == columnFrom) {
                        paths[s].moveTo(xfrom - span - 1, yPositions[seqIndex][ii]);
                    }
                    if (ii < view.getAlignment().getLength() - 1) {
                        paths[s].append(createCurve(seqIndex, ii), true);
                    } else {
                        paths[s].lineTo(xfrom - 1, yPositions[seqIndex][ii]);
                    }
                }
            }
            return paths;

        }

        @Override
        protected void process(List<Integer> chunks) {
            if (!chunks.isEmpty()) {
                Integer last = chunks.get(chunks.size() - 1);
                fireRenderingProgressed(new SequenceBundleRendererEvent(this, 0, last, ntiles));
            }
        }

        @Override
        protected void done() {
            boolean cancelled = false;
            try {
                get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            } catch (CancellationException ex) {
                cancelled = true;
            }
            time = System.currentTimeMillis() - time;
            if (!cancelled) {
                System.out.println("Worker end. Rendering time: " + new DecimalFormat("##.####").format(time / 1000.0));
                fireRenderingFinished(new SequenceBundleRendererEvent(this, 0, ntiles, ntiles));
            } else {
                System.out.println("Worker cancelled.");
                fireRenderingCancelled(new SequenceBundleRendererEvent(this, 0, ntiles, ntiles));
            }
        }
    };

    private class DirectRenderWorker extends RenderWorker {

        private long time;
        private final int[][] sequenceIndices;
        private final Color[] colors;
        private final boolean adjustAlpha;
        private final Graphics2D[] graphics;

        public DirectRenderWorker(Graphics2D[] graphics, int[][] sequenceIndices, Color[] colors, boolean adjustAlpha) {
            this.graphics = graphics;
            this.sequenceIndices = sequenceIndices;
            this.colors = colors;
            this.adjustAlpha = adjustAlpha;
        }

        @Override
        public Object doInBackground() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireRenderingStarted(new SequenceBundleRendererEvent(this, 0, 0, numberOfGroups));
                }
            });

            time = System.currentTimeMillis();
            System.out.println("Worker start");

            for (int grp = 0; grp < sequenceIndices.length; grp++) { // iterate over groups
                Graphics2D g = (Graphics2D) graphics[grp].create(); // draw straight onto the graphics context
                setGlobalGraphicsParameters(g);
                GUIutils.setQualityRenderingHints(g, bundleConfig.getAntiAliasing(), bundleConfig.getSpeedOverQuality());

                if (!adjustAlpha) {
                    g.setColor(colors[grp]);
                } else {
                    g.setColor(new Color(colors[grp].getRed(), colors[grp].getGreen(), colors[grp].getBlue(), alpha[grp]));
                }

                Path2D[] paths = constructPaths(sequenceIndices[grp]);
                for (Path2D p : paths) {
                    if (this.isCancelled()) {
                        g.dispose();
                        return null;
                    }
                    g.draw(p);
                }

                g.dispose();
                publish(grp);
            }

            return null;
        }

        private Path2D[] constructPaths(int[] sequences) {
            Path2D.Float[] paths = new Path2D.Float[sequences.length];

            int span = getCellSpan();
            for (int s = 0; s < sequences.length; s++) {
                paths[s] = new Path2D.Float();
                int seqIndex = sequences[s];
                for (int ii = 0; ii < view.getAlignment().getLength(); ii++) {
                    int xfrom = columnToXCenter(ii) + (int) Math.round(span / 2.0);

                    if (ii == 0) {
                        paths[s].moveTo(xfrom - span - 1, yPositions[seqIndex][ii]);
                    }
                    if (ii < view.getAlignment().getLength() - 1) {
                        paths[s].append(createCurve(seqIndex, ii), true);
                    } else {
                        paths[s].lineTo(xfrom - 1, yPositions[seqIndex][ii]);
                    }
                }
            }
            return paths;
        }

        @Override
        protected void process(List<Integer> chunks) {
            if (!chunks.isEmpty()) {
                Integer last = chunks.get(chunks.size() - 1);
                fireRenderingProgressed(new SequenceBundleRendererEvent(this, 0, last, numberOfGroups));
            }
        }

        @Override
        protected void done() {
            boolean cancelled = false;
            try {
                get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            } catch (CancellationException ex) {
                cancelled = true;
            }
            time = System.currentTimeMillis() - time;
            if (!cancelled) {
                System.out.println("Worker end. Rendering time: " + new DecimalFormat("##.####").format(time / 1000.0));
                fireRenderingFinished(new SequenceBundleRendererEvent(this, 0, numberOfGroups, numberOfGroups));
            } else {
                System.out.println("Worker cancelled.");
                fireRenderingCancelled(new SequenceBundleRendererEvent(this, 0, numberOfGroups, numberOfGroups));
            }
        }
    };

}
