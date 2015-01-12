/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sbweb.controller;

import alvis.Alvis;
import alvis.AlvisDataModel;
import alvis.algorithm.ParseAlignmentTask;
import com.general.gui.progress.Progressable;
import gui.sequencebundle.JSequenceBundle;
import gui.sequencebundle.SequenceBundleConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
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
    private static HashMap<String, JSequenceBundle> jSequenceBundleMap = new HashMap();

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

    class RenderStatus {

        int min;
        int max;
        int value;
        boolean isFinished;

        public RenderStatus(int min, int max, int value, int status) {
            this.min = min;
            this.max = max;
            this.value = value;
            this.isFinished = Progressable.STATE_IDLE == status;

        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean isIsFinished() {
            return isFinished;
        }

        public void setIsFinished(boolean isFinished) {
            this.isFinished = isFinished;
        }

    }

    @RequestMapping(value = "/seq/status", method = {RequestMethod.POST, RequestMethod.OPTIONS}, produces = "application/json")
    public @ResponseBody
    RenderStatus seqStatus(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String filename = request.getParameter("filename");
        Progressable pm = jSequenceBundleMap.get(filename).getProgressModel();
        RenderStatus result = new RenderStatus(pm.getMinimum(), pm.getMaximum(), pm.getValue(), pm.getState());
        return result;
    }

    @RequestMapping(value = "/seq/remove", method = {RequestMethod.POST, RequestMethod.OPTIONS})
    public @ResponseBody
    void removeStatus(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String filename = request.getParameter("filename");
        jSequenceBundleMap.remove(filename);
    }

    @RequestMapping(value = "/seq", method = {RequestMethod.POST, RequestMethod.OPTIONS}, produces = "application/json")
    public @ResponseBody
    AlvisModel seq(HttpServletRequest request,
            HttpServletResponse response,
            @RequestPart("file") MultipartFile file, @RequestBody String x) throws Exception {
        String seq = new String(file.getBytes(), "UTF-8");

        return foo(request, seq);
    }

    @RequestMapping(value = "/seq2", method = {RequestMethod.POST, RequestMethod.OPTIONS}, produces = "application/json")
    public @ResponseBody
    AlvisModel seq2(HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        String seq = "";
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().indexOf("sequence") == 0) {
                seq = java.net.URLDecoder.decode(cookie.getValue(), "UTF-8");
            }
        }
        return foo(request, seq);
    }

    private AlvisModel foo(HttpServletRequest request, String seq) {
        System.setProperty("java.awt.headless", "true");
        JSequenceBundle jsb = new JSequenceBundle();
        AlvisModel alvisModel = requestToAlvisModel(request.getParameterMap());
        // process sequences
        if (seq.isEmpty()) {
            alvisModel.setErrorMessage("No sequences to render!");
            return alvisModel;
        }
        alvisModel.setSequences(seq);
        AlvisDataModel.AlignmentType alignmentType = alvisModel.getAlignmentType().getAlignmentType();
        ParseAlignmentTask alignmentParser = new ParseAlignmentTask(new StringReader(alvisModel.getSequences()), Alvis.AlignmentFormat.FASTA, alignmentType, null);
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
            jSequenceBundleMap.put(tmpFile.getName(), jsb);
            alvisModel.setTempFile(tmpFile);
            alvisModel.setWebPath(servletContext.getContextPath() + "/images/" + tmpFile.getName());
        } catch (IOException ex) {
            alvisModel.setErrorMessage(ex.getMessage());
            return alvisModel;
        }

        // do stuff
        return alvisModel;
    }

    private AlvisModel requestToAlvisModel(Map<String, String[]> paramMap) {
        AlvisModel alvisModel = new AlvisModel();
        alvisModel.setCellHeight(Integer.parseInt(paramMap.get("cellHeight")[0]));
        alvisModel.setHorizontalExtent(Float.parseFloat(paramMap.get("horizontalExtent")[0]));
        alvisModel.setShowingVerticalLines(Boolean.parseBoolean(paramMap.get("showingVerticalLines")[0]));
        alvisModel.setConservationThreshold(Double.parseDouble(paramMap.get("conservationThreshold")[0]));
        alvisModel.setGapRendering(SequenceBundleConfig.GapRenderingType.valueOf(paramMap.get("gapRendering")[0]));
        alvisModel.setShowingConsensus(Boolean.parseBoolean(paramMap.get("_showingConsensus")[0]));
        alvisModel.setCellWidth(Integer.parseInt(paramMap.get("cellWidth")[0]));
        alvisModel.setMaxBundleWidth(Integer.parseInt(paramMap.get("maxBundleWidth")[0]));
        alvisModel.setRadius(Integer.parseInt(paramMap.get("radius")[0]));
        alvisModel.setyAxis(SequenceBundleConfig.YAxis.valueOf(paramMap.get("yAxis")[0]));
        alvisModel.setLineColor(SequenceBundleConfig.LineColor.valueOf(paramMap.get("lineColor")[0]));
        alvisModel.setAlignmentType(SequenceBundleConfig.AlignmentType.valueOf(paramMap.get("alignmentType")[0]));

        return alvisModel;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        System.out.println("SC: images - " + this.servletContext.getRealPath("/images"));
        System.out.println("SC: root - " + this.servletContext.getRealPath("/"));

    }
}
