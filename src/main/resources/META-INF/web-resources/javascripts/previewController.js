/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 var PreviewController = {
 	init : function() {
 		$('#previewForm')
 		.submit(
 			function(event) {
							// Stop form from submitting normally
							event.preventDefault();

							var startIndex = $('#startIndex');
							if (isNaN(startIndex.val())) {
								Utils.alertWarning(
									"Enter valid index, please.",
									'#previewForm');
								return;
							}

							// Get some values from elements on the page:
							var $form = $(this), url = $form.attr('action');
							var data = $('#visualSettingsForm')
							.serializeArray();

							// sanitize start index less than 1
							if (startIndex.val() < 1) {
								startIndex.val(1);
							}
							if (startIndex.val() > $(
								'#visualSettingsForm #lastIndex').val()) {
								startIndex
							.val($('#visualSettingsForm #lastIndex')
								.val());
						}
						data.push({
							name : 'startIndex',
							value : startIndex.val()
						});
							// Send the data using post
							var posting = $.post(url, data);
							// Put the results in a div
							posting.done(function(data) {
								var d = new Date();
								var wp = data['webPath'] + '?' + d.getTime();
								var filename = Utils.getFilename(wp);
								Utils.jobStatusPoll(filename, wp);
								Utils.showImage();
							});
							PreviewController
							.updateSequenceNavigationControls(startIndex
								.val());
						});

$('#n-terminus').click(function() {
	PreviewController.updateSequenceNavigationControls(1);
});

$('#c-terminus').click(
	function() {
		PreviewController.updateSequenceNavigationControls($(
			'#visualSettingsForm #lastIndex').val());
	});

		// controls for tab icons changing from active <-> inactive states
		$('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
			// turn all tabs to disable png
			var tabImages = $('#tabs li.nav img');
			var activeImage = $($('#tabs li.nav.active img')[0]);
			Utils.setActiveSVG(tabImages, activeImage);
		});

		$("#downloadButton").click(function() {
			Utils.animateDowloadImage();
		});

		$("#restartButton").click(function() {
			Utils.animateUploadSequence();
		});

	},
	oldSliderValue : 1,
	initSequenceSlider : function(start, max, step) {
		PreviewController.oldSliderValue = $('#startIndex').val();
		var sequenceSlider = $('#sliderSequence');
		sequenceSlider.slider({
			min : 1,
			max : max,
			step : step,
			value : start,
			slide : function(event, ui) {
				PreviewController.updateSequenceNavigationControls(ui.value);
			},
			change : function(event, ui) {
				// update image only if slider value changes
				if (ui.value !== PreviewController.oldSliderValue) {
					PreviewController.oldSliderValue = ui.value;
					PreviewController
					.updateSequenceNavigationControls(ui.value);
					PreviewController.renderImage();
				}
			}
		});

		sequenceSlider.css('margin-top', '25px');
		sequenceSlider.css('margin-bottom', '25px');
		sequenceSlider.css('margin-right', '5px');

		$(window).resize(function() {
			console.log('resize happening');
			PreviewController.updateSliderWidth();
		});

		PreviewController
		.updateSequenceNavigationControls(PreviewController.oldSliderValue);
	},
	updateSliderWidth : function() {
		var sequenceSlider = $('#sliderSequence');
		var newWidth = 0.75 * (sequenceSlider.parent().parent().parent()
			.width() - ($('#n-terminus').width() + $('#c-terminus').width() + $(
				'#previewForm').width()));
		sequenceSlider.width(newWidth + 'px');
	},
	renderImage : function() {
		var posting = $.post('/upload/paste', Utils.createData());
		// Put the results in a div
		posting.done(function(data) {
			PreviewController.renderProgress(data);
		});
	},
	renderProgress : function(data) {
		var d = new Date();
		var wp = data.webPath + '?' + d.getTime();
		var filename = Utils.getFilename(wp);
		Utils.jobStatusPoll(filename, wp);
		Utils.showImage();
	},
	updateSequenceNavigationControls : function(value) {
		// search field
		$('#startIndex').val(value);
		// index label
		$('#sequenceIndexLabel').text(
			value + " out of " + $('#lastIndex').val());
		// slider
		$('#sliderSequence').slider('value', value);
	}
};

$(function() {
	'use strict';
	PreviewController.init();



});