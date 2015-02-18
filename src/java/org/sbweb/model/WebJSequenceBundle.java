/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.model;

import com.general.gui.dialog.DialogFactory;
import com.general.gui.progress.DefaultProgressable;
import com.general.gui.progress.Progressable;
import de.biozentrum.bioinformatik.alignment.MultipleAlignment;
import de.biozentrum.bioinformatik.sequence.Sequence;
import gui.sequencebundle.JSequenceBundle;
import gui.sequencebundle.SequenceBundleConfig;
import gui.sequencebundle.SequenceBundleRenderer;
import gui.sequencebundle.SequenceBundleRendererEvent;
import gui.sequencebundle.SequenceBundleRendererListener;
import gui.sequencebundle.legend.AbstractLegendRenderer;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jaitools.tiledimage.DiskMemImage;

/**
 *
 * @author joelau
 */
public class WebJSequenceBundle extends JSequenceBundle {

    public DefaultProgressable webProgressModel = new DefaultProgressable();

    public WebJSequenceBundle(MultipleAlignment alignment, Map<Sequence, Integer> sequenceGroups, SequenceBundleConfig bundleConfig) {
        super(alignment, sequenceGroups, bundleConfig);
    }

    public void renderFragmentPNGToFile(final File file, int dpi, int fromIndex, int toIndex) {
        SequenceBundleConfig config = new SequenceBundleConfig(super.getBundleConfig()); // copy the config
        config.setDpi(dpi);

//        final SequenceBundleRenderer imageRenderer = new SequenceBundleRenderer(this, config);
        final WebSequenceBundleRenderer imageRenderer = new WebSequenceBundleRenderer(this, config);

        final AbstractLegendRenderer newLegendRenderer;
        try {
            newLegendRenderer = super.getLegendRenderer().getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(JSequenceBundle.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        newLegendRenderer.setBundleConfig(config);
        newLegendRenderer.setAlphabet(super.getAlphabet());
        final int legendWidth = (int) newLegendRenderer.getLegendSize().getWidth();
        config.setBundleIndent(legendWidth);

        final int width = imageRenderer.getDrawingAreaWidth() + legendWidth;
        final int height = imageRenderer.getDrawingAreaHeight();
        final DiskMemImage theImage = imageRenderer.createDiskMemImage(width, height);

        // draw everything apart from the bundles
        final Graphics2D gc = theImage.createGraphics();
        imageRenderer.setGlobalGraphicsParameters(gc);
        gc.setColor(getBackground());
        gc.fillRect(0, 0, width, height);
        newLegendRenderer.renderLegend(gc);
        gc.translate(config.getBundleIndent(), 0);

        imageRenderer.renderColumnHeaders(gc);
        SequenceBundleConfig bundleConfig = super.getBundleConfig();
        if (bundleConfig.isShowingVerticalLines()) {
            imageRenderer.renderBackgroundVerticalLines(gc);
        }

        if (bundleConfig.isShowingOverlay()) {
            imageRenderer.renderAlphabetOverlay(gc);
        } else {
            imageRenderer.renderCellShading(gc);
        }

        imageRenderer.addSequenceBundleRendererListener(new SequenceBundleRendererListener() {
            // react to renderer events
            @Override
            public void renderingStarted(SequenceBundleRendererEvent e) {
                System.out.println("rendering started: ");

                webProgressModel.setState(Progressable.STATE_RUNNING);
                webProgressModel.setMinimum(e.getStartValue());
                webProgressModel.setMaximum(e.getEndValue());
                webProgressModel.setValue(e.getCurrentValue());
                webProgressModel.setIndeterminate(false);
                webProgressModel.setStatusText("Exporting...");
            }

            @Override
            public void renderingFinished(SequenceBundleRendererEvent e) {
                gc.dispose();
                try {
                    ImageIO.write(theImage, "png", file);
                } catch (IOException ex) {
                    DialogFactory.showErrorMessage(ex.getMessage(), "Error saving PNG:");
                }

                webProgressModel.setState(Progressable.STATE_IDLE);
            }

            @Override
            public void renderingCancelled(SequenceBundleRendererEvent e) {
                webProgressModel.setState(Progressable.STATE_IDLE);
            }

            @Override
            public void renderingProgressed(SequenceBundleRendererEvent e) {
                webProgressModel.setMinimum(e.getStartValue());
                webProgressModel.setMaximum(e.getEndValue());
                webProgressModel.setValue(e.getCurrentValue());
                double percent = (double) e.getCurrentValue() / (double) e.getEndValue();
                System.out.println("rendering progress: " + MessageFormat.format("{0,number,#.##%}", percent));
            }
        });

        imageRenderer.renderFragmentSequences(gc, WebSequenceBundleRenderer.RenderingStrategy.FRAGMENT_RENDERER, fromIndex, toIndex);
        //imageRenderer.renderSequences(gc, WebSequenceBundleRenderer.RenderingStrategy.DIRECT_RENDERER);//svg tile is png

    }

    public DefaultProgressable getWebProgressModel() {
        return webProgressModel;
    }

    public void setWebProgressModel(DefaultProgressable webProgressModel) {
        this.webProgressModel = webProgressModel;
    }

}
