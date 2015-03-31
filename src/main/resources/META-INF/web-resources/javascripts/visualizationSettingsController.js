/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(function() {
	'use strict';

	$("#visualSettingsForm").submit(function(event) {
		// Stop form from submitting normally
		event.preventDefault();
		// Get some values from elements on the page:
		var $form = $(this), url = $form.attr("action");
		var data = Utils.createData({
			alignmentType : $('#alignmentType').val()
		});

		// Send the data using post
		var posting = $.post(url, data);
		// Put the results in a div
		posting.done(function(data) {
			var d = new Date();
			var wp = data["webPath"] + "?" + d.getTime();
			var filename = Utils.getFilename(wp);
			Utils.jobStatusPoll(filename, wp);
			Utils.animateShowImage();
			// number of columns may have been updated due to new column width
			$('#visualSettingsForm #columnCount').val(data.numberOfColumns);
		});
	});

	// use images that active/deactivate instead of radio buttons
	$('input[name=gapRendering]', '#visualSettingsForm').each(
			function() {
				console.log(this);
				$(this).click(
						function() {
							var otherImages = $('img', $('#gapRadioGroup',
									'#visualSettingsForm'))
							var activeImage = $(
									'input[name=gapRendering]:checked',
									'#visualSettingsForm').siblings();
							Utils.setActiveSVG(otherImages, activeImage);
						})
			})
})