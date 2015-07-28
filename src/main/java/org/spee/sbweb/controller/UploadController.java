/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spee.sbweb.controller;

import gui.sequencebundle.SequenceBundleConfig;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;
import org.spee.sbweb.model.AlvisModel;
import org.spee.sbweb.model.WebJSequenceBundle;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;

import alvis.Alvis;
import alvis.AlvisDataModel;
import alvis.algorithm.ParseAlignmentTask;

import com.general.gui.progress.Progressable;

/**
 *
 * @author joelau
 */
@Controller
@MultipartConfig
@RequestMapping(value = "/upload")
public class UploadController implements ServletContextAware {

	final private static Logger logger = Logger.getLogger("controller");
	private ServletContext servletContext;
	final private static HashMap<String, WebJSequenceBundle> jSequenceBundleMap = new HashMap<String, WebJSequenceBundle>();
	final private int MAX_SEQUENCE_BASES = 1000;
	final private int MAX_SEQUENCE_COUNT = 1000;

	@RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD })
	public String form() {
		return "form";
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
			this.isFinished = (Progressable.STATE_IDLE == status)
					&& (this.value > 0);

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

	@RequestMapping(value = "/seq/status", method = { RequestMethod.POST,
			RequestMethod.OPTIONS }, produces = "application/json")
	public @ResponseBody RenderStatus seqStatus(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String filename = request.getParameter("filename");
		Progressable pm = jSequenceBundleMap.get(filename)
				.getWebProgressModel();
		RenderStatus result = new RenderStatus(pm.getMinimum(),
				pm.getMaximum(), pm.getValue(), pm.getState());
		return result;
	}

	@RequestMapping(value = "/seq/remove", method = { RequestMethod.POST,
			RequestMethod.OPTIONS })
	public @ResponseBody RenderStatus removeStatus(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String filename = request.getParameter("filename");
		if (jSequenceBundleMap.get(filename) != null) {
			jSequenceBundleMap.remove(filename);
		}
		return new RenderStatus(0, 10, 5, 1);
	}

	@RequestMapping(value = "/validate", method = { RequestMethod.POST,
			RequestMethod.OPTIONS }, produces = "application/json")
	public @ResponseBody AlvisModel validate(HttpServletRequest request,
			String seq) throws Exception {
		AlvisModel alvisModel = requestToAlvisModel(request.getParameterMap());
		WebJSequenceBundle jsb = new WebJSequenceBundle(null, null, alvisModel);

		// process sequences
		if (seq.isEmpty()) {
			alvisModel.setErrorMessage("No sequences to render!");
			return alvisModel;
		}

		alvisModel.setSequences(seq);
		AlvisDataModel.AlignmentType alignmentType = AlvisDataModel.AlignmentType.AminoAcid;
		ParseAlignmentTask alignmentParser = new ParseAlignmentTask(
				new StringReader(alvisModel.getSequences()),
				Alvis.AlignmentFormat.FASTA, alignmentType, null);
		ParseAlignmentTask.AlignmentParserResults alignmentResults;
		try {
			alignmentResults = alignmentParser.parse();
			jsb.setAlignment(alignmentResults.alignment);
		} catch (Exception ex) {
			alvisModel.appendErrorMessage(ex.getMessage());
			return alvisModel;
		}

		alvisModel.setSequenceBases(jsb.getAlignment().getLength());
		alvisModel.setSequenceCount(jsb.getAlignment().getSequenceCount());

		if (jsb.getAlignment().getLength() > MAX_SEQUENCE_BASES) {
			alvisModel.appendErrorMessage("Sequence must have less than "
					+ MAX_SEQUENCE_BASES + " bases");
		}
		if (jsb.getAlignment().getSequenceCount() > MAX_SEQUENCE_COUNT) {
			alvisModel.appendErrorMessage("Sequence must have less than "
					+ MAX_SEQUENCE_COUNT + " sequences");
		}
		if (alvisModel.getErrorMessage().length() > 0) {
			return alvisModel;
		} else {
			jsb.setBundleConfig(alvisModel);
			// -1 to fix GUI offset so 1 shows no offset
			Integer startIndex = Integer.valueOf(request
					.getParameter("startIndex")) - 1;
			// sanitize start index so that the start index the min of (seq
			// length - cols) and user input index
			startIndex = Math.min(startIndex, jsb.getAlignment().getLength()
					- alvisModel.getCellWidthType().getNumberOfColumns());
			// make sure the starting index is greater than 0
			startIndex = Math.max(0, startIndex);
			renderImage(alvisModel, jsb, startIndex);
			return alvisModel;
		}
	}

	@RequestMapping(value = "/file", method = { RequestMethod.POST,
			RequestMethod.OPTIONS }, produces = "application/json")
	public @ResponseBody AlvisModel upload(HttpServletRequest request,
			HttpServletResponse response,
			@RequestPart("file") MultipartFile file, @RequestBody String x)
			throws Exception {
		String seq = new String(file.getBytes(), "UTF-8");
		return validate(request, seq);
	}

	@RequestMapping(value = "/example", method = { RequestMethod.POST,
			RequestMethod.OPTIONS }, produces = "application/json")
	public @ResponseBody AlvisModel example(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String filename = request.getParameter("filename");
		String exampleFile = servletContext.getRealPath("/resources")
				+ "/examples/" + filename;
		String seq = readFile(exampleFile, StandardCharsets.UTF_8);
		return validate(request, seq);
	}

	@RequestMapping(value = "/paste", method = { RequestMethod.POST,
			RequestMethod.OPTIONS }, produces = "application/json")
	public @ResponseBody AlvisModel paste(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String seq = request.getParameter("sequence");
		return validate(request, seq);
	}

	private static String readFile(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private AlvisModel renderImage(AlvisModel alvisModel,
			WebJSequenceBundle jsb, int fromIndex) {
		File tmpFile;

		try {
			if (!new File(servletContext.getRealPath("/resources/images"))
					.exists()) {
				logger.debug("image dir empty");
				boolean foo = new File(servletContext.getRealPath("/resources")
						+ "/images").mkdirs();
				System.out.println("img dir create?: " + foo);
			}
			File folder = new File(
					servletContext.getRealPath("/resources/images"));
			tmpFile = File.createTempFile("alvis", ".png", folder);
			// jsb.renderPNGToFile(tmpFile, 300);
			int toIndex = Math.min(fromIndex
					+ alvisModel.getCellWidthType().getNumberOfColumns(),
					alvisModel.getSequenceBases());
			jsb.renderFragmentPNGToFile(tmpFile, alvisModel.getDpi(),
					fromIndex, toIndex);

			jSequenceBundleMap.put(tmpFile.getName(), jsb);
			alvisModel.setTempFile(tmpFile);
			alvisModel.setWebPath(servletContext.getContextPath() + "/images/"
					+ tmpFile.getName());
		} catch (Exception ex) {
			alvisModel.setErrorMessage(ex.getMessage());
			return alvisModel;
		}
		return alvisModel;
	}

	private AlvisModel requestToAlvisModel(Map<String, String[]> paramMap) {
		AlvisModel alvisModel = new AlvisModel();
		alvisModel.setHorizontalExtent(Float.parseFloat(paramMap
				.get("horizontalExtent")[0]));
		alvisModel.setShowingVerticalLines(Boolean.parseBoolean(paramMap
				.get("showingVerticalLines")[0]));
		alvisModel.setConservationThreshold(Double.parseDouble(paramMap
				.get("conservationThreshold")[0]));
		alvisModel.setGapRendering(SequenceBundleConfig.GapRenderingType
				.valueOf(paramMap.get("gapRendering")[0]));
		alvisModel.setCellWidth(AlvisModel.CellWidthType.valueOf(
				paramMap.get("cellWidth")[0]).getSize());
		alvisModel.setCellWidthType(AlvisModel.CellWidthType.valueOf(paramMap
				.get("cellWidth")[0]));
		alvisModel.setRadius(Integer.parseInt(paramMap.get("radius")[0]));
		alvisModel.setyAxis(AlvisModel.YAxis.valueOf(paramMap.get("yAxis")[0]));
		alvisModel.setLineColor(AlvisModel.LineColor.valueOf(paramMap
				.get("lineColor")[0]));

		String[] dpi = paramMap.get("dpi");
		if (dpi != null) {
			alvisModel.setDpi(Integer.parseInt(dpi[0]));
		}
		alvisModel.setErrorMessage("");
		return alvisModel;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		System.out.println("UploadController.java - ServletContext: images - "
				+ this.servletContext.getRealPath("/images"));
		System.out.println("UploadController.java - ServletContext: root - "
				+ this.servletContext.getRealPath("/"));

	}
}
