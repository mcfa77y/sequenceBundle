$(document).ready(function() {
	setUpAccordionBehaviors();
});

function setUpAccordionBehaviors() {
	$('#collapseOne').collapse();
	$('#collapseTwo').collapse();
	$('#collapseThree').collapse();

	$('#collapseOne').on('show.bs.collapse', function() {
		$('#collapseTwo').collapse('hide');
		$('#collapseThree').collapse('hide');
	});

	$('#collapseTwo').on('show.bs.collapse', function() {
		$('#collapseOne').collapse('hide');
		$('#collapseThree').collapse('hide');
		PreviewController.updateSliderWidth();

	});

	$('#collapseTwo').on('shown.bs.collapse', function() {
		PreviewController.updateSliderWidth();
	});

	$('#collapseThree').on('show.bs.collapse', function() {
		console.log("dl seq");
		$('#collapseOne').collapse('hide');
		$('#collapseTwo').collapse('hide');
	});
}

var Utils = {
	animateShowImage : function() {
		// navigates to second tab and opens first tab
		$('#collapseOne').collapse('hide');
		$('#collapseTwo').collapse('show');
		Utils.showImage();
		$('#tabs a:first').tab('show');
	},
	showImage : function() {
		$('#sequenceBundle').addClass('loading');
		$('#loading').show();
		$('#renderProgress').fadeIn();
	},
	debug : function(message) {
		if (debug) {
			console.log(message);
		}
	},
	animateDowloadImage : function() {
		$('#collapseOne').collapse('hide');
		$('#collapseTwo').collapse('hide');
		$('#collapseThree').collapse('show');
	},
	animatePreviewImage : function() {
		$('#collapseOne').collapse('hide');
		$('#collapseTwo').collapse('show');
		$('#collapseThree').collapse('hide');
	},
	animateUploadSequence : function() {
		$('#collapseOne').collapse('show');
		$('#collapseTwo').collapse('hide');
		$('#collapseThree').collapse('hide');
	},
	setActiveSVG : function(otherImages, activeImage) {
		// turn all images to disable png
		for (var i = 0; i < otherImages.length; i++) {
			var image = $(otherImages[i]);
			// remove active from source
			var deactivatedSrc = image.attr('src').replace('_active', '');
			image.attr('src', deactivatedSrc);

		}
		// turn on only active tab
		var activatedSrc = activeImage.attr('src').split('.')[0]
				+ "_active.svg";
		activeImage.attr('src', activatedSrc);

	},
	getFilename : function(wp) {
		return wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
	},
	createData : function(opt) {
		// gets data from visualSettingsForm
		// and puts it into a JSON
		if (!opt) {
			opt = {};
		}
		var data = $('#visualSettingsForm').serializeArray();
		var startIndex = $('#startIndex').val();
		if (startIndex === "" || startIndex < 1) {
			startIndex = 1;
			$('#startIndex').val(1);
		}
		// converts data into an associated array
		// where the name of the form element name is the key
		// and the form value is the value of key map
		var dataKeyMap = {};
		for (i = 0; i < data.length; i++) {
			var key = data[i].name;
			dataKeyMap[key] = {
				value : data[i].value,
				index : i
			};
		}

		if (dataKeyMap.startIndex) {
			data[dataKeyMap.startIndex].value = startIndex;
			dataKeyMap.startIndex;
		} else {
			data.push({
				name : "startIndex",
				value : startIndex
			});
		}

		// if user sends over opts then use those values in the data that will
		// be sent to the server
		// see if opt has any values to use
		if (Object.keys(opt).length > 0) {
			// loop over the keys in opt
			for (key in opt) {
				if (opt.hasOwnProperty(key)) {
					var value = opt[key];
					// if the attribute is already in data then update it
					if (dataKeyMap[key]) {
						data[dataKeyMap[key].index].value = value;
					} else {
						// else push the option onto the data object
						// attribute may not in the data form but user wants to
						// have it sent with the request
						data.push({
							name : key,
							value : value
						});
					}
					// update form visualization data
					$('#visualSettingsForm ' + '#' + key).val(value);
				}
			}
		}
		return data;
	},
	alertWarning : function(message, attachAfterId) {
		if ($(attachAfterId).siblings('.alert').length > 0) {
			return;
		}
		var alertHTML = '<div class="alert alert-warning alert-dismissible" role="alert">\
		<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
		'
				+ message + '</div>';
		$(attachAfterId).after(alertHTML);
	},
	jobStatusPoll : function(filename, imagePath) {
		// polls the server for status of the image that is rendering
		// Utils.animateShowImage();
		$.post("upload/seq/status", {
			filename : filename
		}).done(
				function(data) {
					var progress = parseInt(data.value / data.max * 100, 10);

					var progressBar = $('#renderProgress .progress-bar');
					progressBar.css('width', progress + '%');
					if (data['isFinished'] === false) {
						// continue to get status information
						setTimeout(function() {
							var d = new Date();
							Utils.jobStatusPoll(filename, imagePath);
						}, 500);
					} else {
						// image has finish rendering
						$('#loading').hide();
						$('#sequenceBundle').removeClass("loading");

						// remove rendering progress listener
						$.post("upload/seq/remove", {
							filename : filename
						});
						// hundo the progress bar and fade out
						progressBar.css('width', 100 + '%');
						$('#renderProgress').fadeOut(300, function() {
							$(this).hide();
						});

						// add image if there was none before otherwise
						// update source for newly loaded image
						var image = $('#sequenceBundle #sequenceBundleImage');
						if (image.size() > 0) {
							image.attr('src', imagePath);
						} else {

							$('#sequenceBundle').prepend(
									'<img class="image-all-width" id="sequenceBundleImage" src="'
											+ imagePath + '" />').fadeIn();
						}

						$('#sequenceBundleImage').bind(
								'error',
								function(e) {
									var err = JSON.stringify(e, null, 4);
									Utils.debug("error loading image:"
											+ imagePath + "\n" + err);
									image.attr('src', imagePath);
								});
						$('#downloadPNGLink').attr('href', imagePath);
						$('#downloadPNGLink').attr('download', filename);

						// hide render status
						$('#renderHiResStatus').hide();
					}
				}).error(function(e) {
			var err = JSON.stringify(e, null, 4);
			Utils.debug("error loading jobStatus:" + "\n" + err);
		});
	}
};