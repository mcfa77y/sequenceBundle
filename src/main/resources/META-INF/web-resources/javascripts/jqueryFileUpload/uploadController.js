var debug = false;
var sequence = "";
/*
 * jQuery File Upload Plugin JS Example 8.9.1
 * https://github.com/blueimp/jQuery-File-Upload
 *
 * Copyright 2010, Sebastian Tschan https://blueimp.net
 *
 * Licensed under the MIT license: http://www.opensource.org/licenses/MIT
 */

 /* global $, window */

 $(function() {
 	'use strict';

 	function updateSequenceMetaData(alignmentType, sequenceBases, sequenceCount) {
 		$('.upload-error').hide();
 		$('.upload-summary').text(
 			"You have uploaded an " + alignmentType + " sequence with " + sequenceBases + " bases and " + sequenceCount + " sequences.");
 		$('.upload-summary').show();
 		$('#createBundleButton').prop('disabled', false);
 	}

 	function updateErrorSequenceMetaData(errorMessage) {
 		$('.upload-summary').hide();
 		$('.upload-error').text(errorMessage);
 		$('.upload-error').show();
 		$('#createBundleButton').prop('disabled', true);
 	}

 	function renderImage(url, data) {
        // display validating
        $('.upload-summary').addClass('uploadPanelInfo').text("Validating ...");
        // Send the data using post
        var posting = $.post(url, data);
        // Put the results in a div
        posting.done(function(data) {
        	$('.upload-summary').removeClass('uploadPanelInfo')
        	renderProgress(data);
        });
    }

    // meta data has been returned about the sequence
    // but the image still needs to be rendered hence renderProgress
    function renderProgress(data) {
    	if (data.errorMessage && data.errorMessage.length > 0) {
    		updateErrorSequenceMetaData(data.errorMessage);
    		return;
    	}

    	updateSequenceMetaData(data.alignmentType, data.sequenceBases,
    		data.sequenceCount);
    	var d = new Date();
    	var wp = data.webPath + "?" + d.getTime();
    	var filename = Utils.getFilename(wp);
        // init rendering progress info
        Utils.jobStatusPoll(filename, wp);

        // distribute meta data to visualiztion form
        $("#visualSettingsForm #sequence").val(data.sequences);
        $('#visualSettingsForm #lastIndex').val(data.sequenceBases);
        $('#visualSettingsForm #columnCount').val(data.numberOfColumns);
        PreviewController.initSequenceSlider($('#previewForm #startIndex')
        	.val(), data.sequenceBases, 1);


    }

    $('#fileUpload')
    .fileupload({
    	dataType: 'json',
    	maxFileSize: 5 * 1024 * 1024,
    	acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
    	done: function(e, data) {
    		renderProgress(data.result);
    	},
    	progressall: function(e, data) {
    		var progress = parseInt(data.loaded / data.total * 100, 10);
    		$('#progress .progress-bar').css('width',
    			progress + '%');
    	},
    	processfail: function(e, data) {
    		var currentFile = data.files[data.index];
    		if (data.files.error && currentFile.error) {
                    // there was an error, do something about it
                    Utils.debug(currentFile.error);
                    $('<p/>').text(
                    	"ERROR: " + currentFile.error + " " + currentFile.name).appendTo(
                    	"#messages").addClass("text-danger");
                    }
                },
                add: function(e, data) {
                	data.formData = Utils.createData({
                		alignmentType: $('#alignmentTypeFile').val()
                	});
                	data.submit();
                }

            });

$("#pasteSequenceForm").submit(function(event) {
        // Stop form from submitting normally
        event.preventDefault();
        // Get some values from elements on the page:
        var $form = $(this),
        url = $form.attr("action");
        var data = Utils.createData({
        	alignmentType: $('#alignmentTypePaste').val(),
        	sequence: $('#pasteSequence').val()
        });

        renderImage(url, data);
    });

$("#useExampleForm").submit(function(event) {
        // Stop form from submitting normally
        event.preventDefault();
        var filename = $('#useExampleFile').val();
        // Get some values from elements on the page:
        var $form = $(this),
        url = $form.attr("action");
        var data = Utils.createData({
        	alignmentType: 'AMINOACIDS',
        	filename: filename
        });
        renderImage(url, data);
    });

var descriptionMap = {
	'ABC_tran-(PF00005)_ATP-binding-domain-of-ABC-transporters.txt': 'In living organisms, ABC transporters are responsible for moving molecules across cellular membranes. To do this job, they require energy generated by their ATP-domains.\
	Source: Pfam (PF00005)',
	'ADH_N-(PF08240)_Alcohol-dehydrogenases-catalytic-domain.txt': 'Alcohol dehydrogenase is a family of enzymes which catalyse alcohols. In humans, its catalytic domain processes toxic ethanol with a turnover of about 1200 molecules per second.\
	Source: Pfam (PF08240)',
	'ADK_LID-Adenylate-kinase-lid-domain_mod-from-Magliery-Ray.txt': 'Adenylate kinase is an enzyme that plays an important role in cellular energy homeostasis. Its LID domain in stabilised by hydrogen bonds in Gram-negative bacteria, and by a metal ion in Gram-positives.\
	Source: Pfam (PF05191); Magliery & Ray Labs, Ohio State University',
	'Ank_2-(PF12796)_Ankyrin-repeat_seed.txt': 'The ankyrin repeat is a common structural motif in proteins. Proteins containing the ankyrin motif can be involved in cell signalling, muscle tissue repair, and have been linked to some cancers.\
	Source: Pfam (PF12796)',
	'CBM_5_12_2-(PF14600)_Cellulose-binding-domain_RP75.txt': 'This domain belongs to carbohydrate-binding modules associated with glycoside hydrolases — extremely common enzymes found in all kinds of living organisms, but also used in food and paper industry.\
	Source: Pfam (PF14600)',
	'DUF4596-(PF15363)_Domain-of-unknown-function.txt': 'This protein domain can be found in eucaryotes. \
	Its function has not yet been characterised.\
	Source: Pfam (PF15363).',
	'HATPase_c-(PF02518)_GHKL-domain_Seed.txt': 'This is an evolutionary conserved domain that can be found in several ATP-binding proteins, for example in Hsp90 protein which protects cells from elevated temperatures. \
	Source: Pfam (PF02518)',
	'Insulin-(PF00049)-RP55.txt': 'Insulin is a protein hormone produced in pancreas and involved in regulation of glucose levels in blood. Three cysteine pairs are responsible for the structure of insulin, hence they are very strongly conserved across species.\
	Source: Pfam (PF00049)',
	'Tox-WTIP-(PF15654)_Toxin-with-tryptophan-and-TIP-motif.txt': 'This is a predicted toxin domain with strongly conserved motifs. The domain is present in bacterial polymorphic toxin systems.\
	Source: Pfam (PF15654)',
	'zf-H2C2_2-(PF13465)-Zinc-finger-double-domain.txt': 'Zinc-fingers are small protein structural motifs. Most of them function as interaction modules binding to DNA, RNA, other proteins, or other molecules.\
	Source: Pfam (PF13465)'
};

    // update sequence description
    $('#upload-description').text(descriptionMap[$('#useExampleFile').val()]);

    $("#useExampleFile").on(
    	'change',
    	function() {
            // update description blerb
            $('#upload-description').text(
            	descriptionMap[$('#useExampleFile').val()]);
        });

    $("#createBundleButton").click(function() {
    	Utils.animatePreviewImage();
    });

    // minimize/maximize button for sequence upload methods
    $('.upload-minimize-button').each(function() {
    	$(this).click(function() {
    		var span = $('span', this);
    		if (Boolean(span.hasClass('glyphicon-minus'))) {
    			span.removeClass('glyphicon-minus').addClass('glyphicon-plus');
    		} else {
    			span.removeClass('glyphicon-plus').addClass('glyphicon-minus');

    		}
    	});
    })

    // readmore link that hide/shows info
    $('#readMoreInfo').on('show.bs.collapse', function() {
    	$('#readLessInfo').collapse('toggle');
    });
    $('#readMoreInfo').on('hide.bs.collapse', function() {
    	$('#readLessInfo').collapse('toggle');
    });
    $('#readMoreInfoPaste').on('show.bs.collapse', function() {
    	$('#readLessInfoPaste').collapse('toggle');
    });
    $('#readMoreInfoPaste').on('hide.bs.collapse', function() {
    	$('#readLessInfoPaste').collapse('toggle');
    });

});