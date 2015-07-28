/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spee.sbweb.controller;

import gui.sequencebundle.JSequenceBundle;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.validation.Valid;

import org.spee.sbweb.model.AlvisModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;

import alvis.Alvis;
import alvis.AlvisDataModel;
import alvis.algorithm.ParseAlignmentTask;
import alvis.algorithm.ParseAlignmentTask.AlignmentParserResults;

/**
 *
 * @author Roland Schwarz <rfs32@cam.ac.uk>
 */
@Controller
@RequestMapping(value = "/")
public class SBWebController implements ServletContextAware {

	private ServletContext servletContext;

	// value of milliseconds in one minute
	private static final int MINS_IN_MS = 60 * 1000;
	// time in milliseconds
	private static final int TIME_TO_CHECK_IMAGE_FOLDER = 60 * MINS_IN_MS;
	private static final int IMAGE_LIFETIME = 60 * MINS_IN_MS;

	@RequestMapping(method = RequestMethod.GET)
	public String viewSequenceBundleConfig(Model model) {
		return "index";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String processSequenceBundleConfig(
			@ModelAttribute("userForm") @Valid AlvisModel alvisModel,
			BindingResult bindingResult, Model model) {

		if (bindingResult.hasErrors()) {
			return "AlvisInput";
		}
		System.setProperty("java.awt.headless", "true");
		JSequenceBundle jsb = new JSequenceBundle(null, null, alvisModel);

		// process sequences
		if (alvisModel.getSequences().trim().isEmpty()) {
			alvisModel.setErrorMessage("No sequences to render!");
			return ("AlvisError");
		}
		ParseAlignmentTask alignmentParser = new ParseAlignmentTask(
				new StringReader(alvisModel.getSequences()),
				Alvis.AlignmentFormat.FASTA,
				AlvisDataModel.AlignmentType.AminoAcid, null);
		AlignmentParserResults alignmentResults;
		try {
			alignmentResults = alignmentParser.parse();
			jsb.setAlignment(alignmentResults.alignment);
			// SequenceAlphabet.AMINOACIDS;
			// AbstractLegendRenderer lr =
			// AbstractLegendRenderer.createAAIndexLegendRenderer(SequenceBundleConfig.AlignmentType.valueOf(alvisModel.getAlignmentType()))
			// ;
			// jsb.setLegendRenderer(lr);
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
			alvisModel.setWebPath(servletContext.getContextPath() + "/images/"
					+ tmpFile.getName());

			// alvisModel.setWebPath(servletContext.getContextPath("/") +
			// "/images/" + tmpFile.getName());
		} catch (IOException ex) {
			alvisModel.setErrorMessage(ex.getMessage());
			return ("AlvisError");
		}

		// do stuff
		return "SBWebResult";
	}

	@RequestMapping(value = "/ezviz", method = { RequestMethod.GET })
	public String seqStatus() {
		return "EzViz";
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		System.out.println("SBWebControler.java - ServletContext: images - "
				+ this.servletContext.getRealPath("/images"));
		System.out.println("SBWebControler.java - ServletContext: root - "
				+ this.servletContext.getRealPath("/"));

		Timer t = new Timer();
		CronJobs mTask = new CronJobs();

		t.scheduleAtFixedRate(mTask, 0, TIME_TO_CHECK_IMAGE_FOLDER);

	}

	class CronJobs extends TimerTask {
		// private static final int HOUR_THRESHOLD = 1 * 60 * 60 * 1000;
		private final File folder = new File(
				servletContext.getRealPath("/resources/images"));

		public CronJobs() {
			// Some stuffs
		}

		private void cleanImageFiles(final File folder) {
			for (final File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					cleanImageFiles(fileEntry);
				} else {
					long diff = new Date().getTime() - fileEntry.lastModified();
					if (diff > IMAGE_LIFETIME) {
						fileEntry.delete();
					}
				}
			}
		}

		@Override
		public void run() {
			final File folder = new File(
					servletContext.getRealPath("/resources/images"));
			// create an image folder if one doens't already exist
			if (!folder.exists()) {
				System.out
						.println("SBWebControler.java: creating temporay image directory"
								+ folder.toString());
				folder.mkdir();
			}
			cleanImageFiles(folder);
			System.out.println("Cleaning image folder.");
		}

	}

	// @InitBinder
	// public void initBinder(WebDataBinder dataBinder) {
	// dataBinder.setRequiredFields(new String[] {"sequence", "cellWidth",
	// "cellHeight", "maxBundleWidth", "tangL", "tangR", "horizontalExtent",
	// "offsetY", "conservationThreshold",
	// "minAlphaPerThread", "maxAlphaTotal"});
	//
	// dataBinder.registerCustomEditor(String.class, new
	// StringTrimmerEditor(false));
	//
	// }
}
