/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.controller;

import alvis.Alvis;
import alvis.AlvisDataModel;
import alvis.algorithm.ParseAlignmentTask;
import alvis.algorithm.ParseAlignmentTask.AlignmentParserResults;
import gui.sequencebundle.JSequenceBundle;
import gui.sequencebundle.SequenceBundleConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.validation.Valid;
import org.sbweb.model.AlvisModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;

/**
 *
 * @author Roland Schwarz <rfs32@cam.ac.uk>
 */
@Controller
@RequestMapping(value = "/SB")
public class SBWebController implements ServletContextAware {

    private ServletContext servletContext;

    @RequestMapping(method = RequestMethod.GET)
    public String viewSequenceBundleConfig(Model model) {
        AlvisModel userForm = new AlvisModel();
        model.addAttribute("userForm", userForm);

        List<SequenceBundleConfig.GapRenderingType> gapRenderingList = new ArrayList<>();
        gapRenderingList.add(SequenceBundleConfig.GapRenderingType.STANDARD);
        gapRenderingList.add(SequenceBundleConfig.GapRenderingType.DISCONNECTED);
        model.addAttribute("gapRenderingList", gapRenderingList);

        List<SequenceBundleConfig.GroupStackingType> groupStackingList = new ArrayList<>();
        groupStackingList.add(SequenceBundleConfig.GroupStackingType.SEPARATE);
        groupStackingList.add(SequenceBundleConfig.GroupStackingType.OVERLAYED);
        model.addAttribute("groupStackingList", groupStackingList);

        List<SequenceBundleConfig.AlignmentType> dataFormatList = new ArrayList<>();
        dataFormatList.add(SequenceBundleConfig.AlignmentType.AMINOACIDS);
        dataFormatList.add(SequenceBundleConfig.AlignmentType.NUCLEOTIDES);
        dataFormatList.add(SequenceBundleConfig.AlignmentType.RNA);
        model.addAttribute("dataFormatList", dataFormatList);
        return "SBWeb";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String processSequenceBundleConfig(@ModelAttribute("userForm") @Valid AlvisModel alvisModel, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "AlvisInput";
        }
        System.setProperty("java.awt.headless", "true");
        JSequenceBundle jsb = new JSequenceBundle();

        // process sequences
        if (alvisModel.getSequences().trim().isEmpty()) {
            alvisModel.setErrorMessage("No sequences to render!");
            return ("AlvisError");
        }
        ParseAlignmentTask alignmentParser = new ParseAlignmentTask(new StringReader(alvisModel.getSequences()),
                Alvis.AlignmentFormat.FASTA,
                AlvisDataModel.AlignmentType.AminoAcid, null);
        AlignmentParserResults alignmentResults;
        try {
            alignmentResults = alignmentParser.parse();
            jsb.setAlignment(alignmentResults.alignment);
//            SequenceAlphabet.AMINOACIDS;
//            AbstractLegendRenderer lr = AbstractLegendRenderer.createAAIndexLegendRenderer(SequenceBundleConfig.AlignmentType.valueOf(alvisModel.getAlignmentType())) ;
            jsb.setLegendRenderer(lr);
        } catch (Exception ex) {
            alvisModel.setErrorMessage(ex.getMessage());
            return ("AlvisError");
        }

        jsb.setBundleConfig(alvisModel);

        File tmpFile;
        try {
            File folder = new File(servletContext.getRealPath("/images"));
            tmpFile = File.createTempFile("alvis", ".png", folder);
            jsb.renderPNGToFile(tmpFile, 300);
            alvisModel.setTempFile(tmpFile);
            alvisModel.setWebPath(servletContext.getContextPath() + "/images/" + tmpFile.getName());

// alvisModel.setWebPath(servletContext.getContextPath("/") + "/images/" + tmpFile.getName());
        } catch (IOException ex) {
            alvisModel.setErrorMessage(ex.getMessage());
            return ("AlvisError");
        }

        // do stuff
        return "SBWebResult";
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        System.out.println("SC: images - " + this.servletContext.getRealPath("/images"));
        System.out.println("SC: root - " + this.servletContext.getRealPath("/"));

    }

//	@InitBinder
//	public void initBinder(WebDataBinder dataBinder) {
//		dataBinder.setRequiredFields(new String[] {"sequence", "cellWidth", "cellHeight", "maxBundleWidth", "tangL", "tangR", "horizontalExtent",  "offsetY", "conservationThreshold",
//		"minAlphaPerThread", "maxAlphaTotal"});
//
//		dataBinder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
//
//	}
}
