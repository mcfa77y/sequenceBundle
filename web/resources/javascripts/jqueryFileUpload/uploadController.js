var debug = true;
var sequence = "";
/*
 * jQuery File Upload Plugin JS Example 8.9.1
 * https://github.com/blueimp/jQuery-File-Upload
 *
 * Copyright 2010, Sebastian Tschan
 * https://blueimp.net
 *
 * Licensed under the MIT license:
 * http://www.opensource.org/licenses/MIT
 */

/* global $, window */
var utils = {
    animateShowImage: function () {
        $('#collapseOne').collapse('hide');
        $('#collapseTwo').collapse('show');
        $('#sequenceBundleImage img').hide();
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
    jobStatusPoll: function (filename, imagePath) {
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
                    utils.jobStatusPoll(filename, imagePath);
                }, 500);
            } else {
                // image has finish rendering
//                utils.debug("finished: " + data['isFinished']);
//
                // remove rendering progress listener
                $.post("upload/seq/remove", {filename: filename});
                // hundo the progresss bar and fade out
                progressBar.css(
                        'width',
                        100 + '%'
                        );
                $('#renderProgress').fadeOut(800, function () {
                    utils.debug("main hiding progressbar");
                    $(this).hide();
                });

                var image = $('#sequenceBundleImage img');
                if ($('#sequenceBundleImage img').size() > 0) {
                    //utils.debug('removing old image: ' + $('#sequenceBundleImage img').attr('src'));
                    //$("#sequenceBundleImage img").hide();
                    image.attr('src', imagePath);
                } else {
                    $('#sequenceBundleImage').prepend('<img class="image-sm" id="theImg" src="' + imagePath + '" />').fadeIn();
                    //image.attr('src', imagePath)
                }
                $('#theImg').bind('load', function () {
                    // resize image with height as 500
                    // and proportional width

                    var img = $('#sequenceBundleImage img');
                    img.hide();
                    var sw = document.getElementById('theImg').naturalWidth * 500 / 2250;
                    utils.debug("bind load image width: " + sw);
                    var sh = 500;
                    img.css("height", sh + "px");
                    img.css("width", sw + "px");
                    img.show();

                });
                $('#theImg').bind('error', function () {
                    utils.debug("error loading image:" + imagePath);
                    image.attr('src', imagePath);
                });

                $('#downloadPNG').attr('href', imagePath);
                $('#downloadPNG').attr('download', filename);

            }
        });
    }
};
$(function () {
    'use strict';

    function updateSequenceMetaData(alignmentType, sequenceBases, sequenceCount) {
        $('.upload-error').hide();
        $('.upload-summary').text("You have uploaded an " + alignmentType + " sequence with " + sequenceBases + " bases and " + sequenceCount + " sequences.");
        $('.upload-summary').show();
        $('#createBundleButton').prop('disabled', false);

    }

    function updateErrorSequenceMetaData(errorMessage) {
        $('.upload-summary').hide();
        $('.upload-error').text(errorMessage);
        $('.upload-error').show();
        $('#createBundleButton').prop('disabled', true);
    }

    function renderProgress(data) {
        if (data.errorMessage && data.errorMessage.length > 0) {
            updateErrorSequenceMetaData(data.errorMessage);
            return;
        }

        updateSequenceMetaData(data.alignmentType, data.sequenceBases, data.sequenceCount);
        var d = new Date();
        var wp = data.webPath + "?" + d.getTime();
        var filename = wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
        utils.jobStatusPoll(filename, wp);
        $("#visualSettingsForm #sequence").val(data.sequences);

    }
//    var frm = $(document.visualSettingsForm);
//    var data = JSON.stringify(frm.serializeObject());


    $('#fu').fileupload({
        dataType: 'json',
        maxFileSize: 5 * 1024 * 1024,
        acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
        done: function (e, data) {
            renderProgress(data.result);


//            utils.debug("removed cookie: " + $.removeCookie("sequence", {path: "/", json: true}));
//            $.cookie("sequence", data.result["sequences"], {path: "/", json: true});
//            utils.debug("wrote to cookie: " + data.result["sequences"])

        },
        progressall: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .progress-bar').css(
                    'width',
                    progress + '%'
                    );
        },
        processfail: function (e, data) {
            var currentFile = data.files[data.index];
            if (data.files.error && currentFile.error) {
                // there was an error, do something about it
                utils.debug(currentFile.error);
                $('<p/>').text("ERROR: " + currentFile.error + " " + currentFile.name).appendTo("#messages").addClass("text-danger");
            }
        },
        add: function (e, data) {
            data.formData = ($("#visualSettingsForm").serializeArray());
            data.formData.push({name: "alignmentType", value: $('#alignmentTypeFile').val()});
            $('#alignmentType').val($('#alignmentTypeFile').val());
            data.submit();
        }

    });



    $("#pasteSequenceForm").submit(function (event) {
// Stop form from submitting normally
        event.preventDefault();
// Get some values from elements on the page:
        var $form = $(this),
                url = $form.attr("action");
        $('#sequence').val($('#pasteSequence').val());
        var data = $('#visualSettingsForm').serializeArray();
        data.push({name: "alignmentType", value: $('#alignmentTypePaste').val()});
        $('alignmentType').val($('#alignmentTypePaste').val());

        for (var i = 0; i < data.length; i++) {
            var d = data[i];
            utils.debug(d);
//            if (d.name === "sequence") {
//                d.value = $form['paseSequence'];
//            }
        }
// Send the data using post
        var posting = $.post(url, data);
// Put the results in a div
        posting.done(function (data) {
            renderProgress(data);
        });
    });

    $("#useExampleButton").click(function (event) {
// Stop form from submitting normally
        event.preventDefault();
// Get some values from elements on the page:
        var url = "/upload/seq2";
        $('#sequence').val(
                ">SEQUENCE_A\r\n\
ATTTGCATGCAAGCATGC\r\n\
>SEQUENCE_B\r\n\
AT--GCATGCATGCATGC\r\n\
>SEQUENCE_C\r\n\
AT--TTATGCAGGCATGC\r\n\
>SEQUENCE_D\r\n\
ATTT--ATGCAGGCATG-\r\n\
");
        var data = $('#visualSettingsForm').serializeArray();
        $('alignmentType').val('AMINOACIDS');
        for (var i = 0; i < data.length; i++) {
            var d = data[i];
            utils.debug(d);
//            if (d.name === "sequence") {
//                d.value = $form['paseSequence'];
//            }
        }
// Send the data using post
        var posting = $.post(url, data);
// Put the results in a div
        posting.done(function (data) {
            renderProgress(data);
        });
    });

    $("#createBundleButton").click(function () {
        utils.animatePreviewImage();
    });

    $("#downloadButton").click(function () {
        utils.animateDowloadImage();
    });

});
