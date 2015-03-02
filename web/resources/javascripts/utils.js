/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var Utils = {
    animateShowImage: function () {
        $('#collapseOne').collapse('hide');
        $('#collapseTwo').collapse('show');
        $('#sequenceBundle').hide();
        $('#loading').show();
        $('#tabs a:first').tab('show');
        $('#renderProgress').fadeIn();
    },
    debug: function (message) {
        if (debug) {
            console.log(message);
        }
    },
    animateDowloadImage: function () {
        $('#collapseOne').collapse('hide');
        $('#collapseTwo').collapse('hide');
        $('#collapseThree').collapse('show');
    },
    animatePreviewImage: function () {
        $('#collapseOne').collapse('hide');
        $('#collapseTwo').collapse('show');
        $('#collapseThree').collapse('hide');
    },
    getFilename: function (wp) {
        return wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
    },
    createData: function (opt) {
        // gets data from visualSettingsForm 
        // and puts it into a JSON
        if (!opt) {
            opt = {};
        }
        var data = $('#visualSettingsForm').serializeArray();
        var startIndex = $('#startIndex').val();
        if (startIndex === "") {
            startIndex = 1;
        }

        var keyMap = {};
        for (i = 0; i < data.len; i++) {
            var key = data[i].name;
            keyMap[key] = i;
        }


        if (keyMap.startIndex) {
            data[keyMap.startIndex].value = startIndex;
        }
        else {
            data.push({name: "startIndex", value: startIndex});
        }


        var alignmentType = opt.alignmentType;
        if (alignmentType) {
            if (keyMap.alignmentType) {
                data[keyMap.alignmentType].value = alignmentType;
            }
            else {
                data.push({name: "alignmentType", value: alignmentType});
            }
            // update form visualization data 
            $('#visualSettingsForm #alignmentType').val(alignmentType);
        }
        
        var sequence = opt.sequence;
        if (sequence) {
            if (keyMap.sequence) {
                data[keyMap.sequence].value = sequence;
            }
            else {
                data.push({name: "sequence", value: sequence});
            }
            // update form visualization data 
            $("#visualSettingsForm #sequence").val(sequence);
        }
        return data;
    },
    jobStatusPoll: function (filename, imagePath) {
        // polls the server for status of the image that is rendering
        Utils.animateShowImage();
        $.post("upload/seq/status", {filename: filename}).done(function (data) {
            var progress = parseInt(data.value / data.max * 100, 10);
//            utils.debug("render progress: " + data['value'] + "/" + data['max'] + " = " + progress);
//            utils.debug("isFinished: " + data['isFinished']);

            var progressBar = $('#renderProgress .progress-bar');
            progressBar.css(
                    'width',
                    progress + '%'
                    );
            if (data['isFinished'] === false) {
//                utils.debug("not finished");

                setTimeout(function () {
                    var d = new Date();
//                    utils.debug("date: " + d);
                    Utils.jobStatusPoll(filename, imagePath);
                }, 500);
            } else {
                // image has finish rendering
                $('#loading').hide();
                // remove rendering progress listener
                $.post("upload/seq/remove", {filename: filename});
                // hundo the progresss bar and fade out
                progressBar.css(
                        'width',
                        100 + '%'
                        );
                $('#renderProgress').fadeOut(800, function () {
                    Utils.debug("main hiding progressbar");
                    $(this).hide();
                });
                var image = $('#sequenceBundle #sequenceBundleImage');
                if (image.size() > 0) {
                    image.attr('src', imagePath);
                } else {

                    $('#sequenceBundle').prepend('<img class="image-sm" id="sequenceBundleImage" src="' + imagePath + '" />').fadeIn();
                    //image.attr('src', imagePath)
                }
                $('#sequenceBundleImage').bind('load', function () {
                    var sequenceBundle = $('#sequenceBundle');
                    sequenceBundle.hide();
                    // resize image with height as 500
                    // and proportional width
                    var img = $('#sequenceBundle #sequenceBundleImage');
                    var sw = Math.min(document.getElementById('sequenceBundleImage').naturalWidth, 1100);
                    Utils.debug("bind load image width: " + sw);
                    var sh = 500;
                    img.css("height", sh + "px");
                    img.css("width", sw + "px");

                    sequenceBundle.show();

                });
                // if there is an error try reloading the image
                $('#sequenceBundleImage').bind('error', function (e) {
                    var err = JSON.stringify(e, null, 4);
                    Utils.debug("error loading image:" + imagePath + "\n" + err);
                    image.attr('src', imagePath);
                });
                $('#downloadPNG').attr('href', imagePath);
                $('#downloadPNG').attr('download', filename);
            }
        }).error(function (e) {
            var err = JSON.stringify(e, null, 4);
            Utils.debug("error loading jobStatus:" + "\n" + err);
        });
    }
};