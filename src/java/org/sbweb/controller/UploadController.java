/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.controller;

import alvis.Alvis;
import alvis.AlvisDataModel;
import alvis.algorithm.ParseAlignmentTask;
import gui.sequencebundle.JSequenceBundle;
import gui.sequencebundle.SequenceBundleConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.sbweb.domain.Message;
import org.sbweb.domain.UploadedFile;
import org.sbweb.model.AlvisModel;
import org.sbweb.response.StatusResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author joelau
 */
@Controller
@MultipartConfig
@RequestMapping(value = "/upload")
public class UploadController implements ServletContextAware {

    private static Logger logger = Logger.getLogger("controller");
    private ServletContext servletContext;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    public String form() {
        return "form";
    }

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    public @ResponseBody
    StatusResponse message(@RequestBody Message message) {
// Do custom steps here
// i.e. Persist the message to the database
        logger.debug("Service processing...done");
        return new StatusResponse(true, "Message received");
    }

    @RequestMapping(value = "/file", method = {RequestMethod.POST, RequestMethod.OPTIONS}, produces = "application/json")
    public @ResponseBody
    List<UploadedFile> upload(@RequestParam("file") MultipartFile file) {
        // Do custom steps here
        // i.e. Save the file to a temporary location or database
        logger.debug("Writing file to disk...");
//        System.out.println("formData: " + formData);
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        UploadedFile u = new UploadedFile(file.getOriginalFilename(),
                Long.valueOf(file.getSize()).intValue(),
                "http://localhost:8080/alvis-web-interface/resources/" + file.getOriginalFilename());

        uploadedFiles.add(u);
        return uploadedFiles;
    }

    @RequestMapping(value = "/seq", method = {RequestMethod.POST, RequestMethod.OPTIONS}, produces = "application/json")
    public @ResponseBody
    AlvisModel seq(HttpServletRequest request,
            HttpServletResponse response,
            @RequestPart("file") MultipartFile file, @RequestBody String x) throws Exception {
        String seq = new String(file.getBytes(), "UTF-8");
        Enumeration<String> ff = request.getParameterNames();
// Do custom steps here
        // i.e. Save the file to a temporary location or database
        logger.debug("Writing file to disk...");

        for (String key : Collections.list(ff)) {
            System.out.println(key);
        }
        List<UploadedFile> uploadedFiles = new ArrayList<>();
        UploadedFile u = new UploadedFile(file.getOriginalFilename(),
                Long.valueOf(file.getSize()).intValue(),
                "http://localhost:8080/alvis-web-interface/resources/" + file.getOriginalFilename());

        uploadedFiles.add(u);

        System.setProperty("java.awt.headless", "true");
        JSequenceBundle jsb = new JSequenceBundle();
        AlvisModel alvisModel = paramMapToAlvisModel(request.getParameterMap());
        // process sequences
        if (seq.isEmpty()) {
            alvisModel.setErrorMessage("No sequences to render!");
            return alvisModel;
        }
        alvisModel.setSequences(seq);
        ParseAlignmentTask alignmentParser = new ParseAlignmentTask(new StringReader(alvisModel.getSequences()), Alvis.AlignmentFormat.FASTA, AlvisDataModel.AlignmentType.AminoAcid, null);
        ParseAlignmentTask.AlignmentParserResults alignmentResults;
        try {
            alignmentResults = alignmentParser.parse();
            jsb.setAlignment(alignmentResults.alignment);
        } catch (Exception ex) {
            alvisModel.setErrorMessage(ex.getMessage());
            return alvisModel;
        }

        jsb.setBundleConfig(alvisModel);

        File tmpFile;
        try {
            File folder = new File(servletContext.getRealPath("/images"));
            tmpFile = File.createTempFile("alvis", ".png", folder);
            jsb.renderPNGToFile(tmpFile, 300);
            alvisModel.setTempFile(tmpFile);
            alvisModel.setWebPath(servletContext.getContextPath() + "/images/" + tmpFile.getName());
        } catch (IOException ex) {
            alvisModel.setErrorMessage(ex.getMessage());
            return alvisModel;
        }

        // do stuff
        return alvisModel;
    }

    private AlvisModel paramMapToAlvisModel(Map<String, String[]> paramMap) {
        AlvisModel alvisModel = new AlvisModel();
        alvisModel.setAlignmentType(SequenceBundleConfig.AlignmentType.AMINOACIDS);
        alvisModel.setMinAlphaPerThread(Double.parseDouble(paramMap.get("minAlphaPerThread")[0]));
        alvisModel.setMaxAlphaTotal(Double.parseDouble(paramMap.get("maxAlphaTotal")[0]));
        alvisModel.setCellHeight(Integer.parseInt(paramMap.get("cellHeight")[0]));
        alvisModel.setHorizontalExtent(Float.parseFloat(paramMap.get("horizontalExtent")[0]));
        alvisModel.setOffsetY(Integer.parseInt(paramMap.get("offsetY")[0]));
        alvisModel.setShowingOverlay(Boolean.parseBoolean(paramMap.get("_showingOverlay")[0]));
        alvisModel.setShowingVerticalLines(Boolean.parseBoolean(paramMap.get("showingVerticalLines")[0]));
        alvisModel.setConservationThreshold(Double.parseDouble(paramMap.get("conservationThreshold")[0]));
        alvisModel.setGapRendering(SequenceBundleConfig.GapRenderingType.valueOf(paramMap.get("gapRendering")[0]));
        alvisModel.setShowingConsensus(Boolean.parseBoolean(paramMap.get("_showingConsensus")[0]));
        alvisModel.setCellWidth(Integer.parseInt(paramMap.get("cellWidth")[0]));
        alvisModel.setMaxBundleWidth(Integer.parseInt(paramMap.get("maxBundleWidth")[0]));
        alvisModel.setTangR(Integer.parseInt(paramMap.get("tangR")[0]));
        alvisModel.setShowingHorizontalLines(Boolean.parseBoolean(paramMap.get("_showingHorizontalLines")[0]));
        alvisModel.setShowingVerticalLines(Boolean.parseBoolean(paramMap.get("_showingVerticalLines")[0]));
        alvisModel.setGroupStacking(SequenceBundleConfig.GroupStackingType.valueOf(paramMap.get("groupStacking")[0]));
        alvisModel.setTangL(Integer.parseInt(paramMap.get("tangL")[0]));

        return alvisModel;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        System.out.println("SC: images - " + this.servletContext.getRealPath("/images"));
        System.out.println("SC: root - " + this.servletContext.getRealPath("/"));

    }
}
