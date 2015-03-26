/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
						// hundo the progresss bar and fade out
						progressBar.css('width', 100 + '%');
						$('#renderProgress').fadeOut(300, function() {
							$(this).hide();
						});
						var image = $('#sequenceBundle #sequenceBundleImage');
						if (image.size() > 0) {
							image.attr('src', imagePath);
						} else {

							$('#sequenceBundle').prepend(
									'<img class="image-sm" id="sequenceBundleImage" src="'
											+ imagePath + '" />').fadeIn();
							// image.attr('src', imagePath)
						}
						// $('#sequenceBundleImage')
						// .bind(
						// 'load',
						// function() {
						// var sequenceBundle = $('#sequenceBundle');
						// sequenceBundle.hide();
						// // resize image with height
						// // as 500
						// // and proportional width
						// var img = $('#sequenceBundle #sequenceBundleImage');
						// var sw = Math
						// .min(
						// document
						// .getElementById('sequenceBundleImage').naturalWidth,
						// 1100);
						// Utils
						// .debug("bind load image width: "
						// + sw);
						// var sh = 500;
						// img
						// .css("height", sh
						// + "px");
						// img.css("width", sw + "px");
						//
						// sequenceBundle.show();
						//
						// });
						// if there is an error try reloading the image
						$('#sequenceBundleImage').bind(
								'error',
								function(e) {
									var err = JSON.stringify(e, null, 4);
									Utils.debug("error loading image:"
											+ imagePath + "\n" + err);
									image.attr('src', imagePath);
								});
						$('#downloadPNG').attr('href', imagePath);
						$('#downloadPNG').attr('download', filename);
					}
				}).error(function(e) {
			var err = JSON.stringify(e, null, 4);
			Utils.debug("error loading jobStatus:" + "\n" + err);
		});
	}
};