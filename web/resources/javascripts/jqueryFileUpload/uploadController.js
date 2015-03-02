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
    function renderImage(url, data) {
        // Send the data using post
        var posting = $.post(url, data);
        // Put the results in a div
        posting.done(function (data) {
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

        updateSequenceMetaData(data.alignmentType, data.sequenceBases, data.sequenceCount);
        var d = new Date();
        var wp = data.webPath + "?" + d.getTime();
        var filename = Utils.getFilename(wp);
        // init rendering progress info 
        Utils.jobStatusPoll(filename, wp);

        // distribute meta data to visualiztion form
        $("#visualSettingsForm #sequence").val(data.sequences);
        $('#visualSettingsForm #lastIndex').val(data.sequenceBases);
        $('#visualSettingsForm #columnCount').val(data.numberOfColumns);
        PreviewController.initSequenceSlider($('#previewForm #startIndex').val(), data.sequenceBases, data.numberOfColumns);


    }
//    var frm = $(document.visualSettingsForm);
//    var data = JSON.stringify(frm.serializeObject());


    $('#fu').fileupload({
        dataType: 'json',
        maxFileSize: 5 * 1024 * 1024,
        acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
        done: function (e, data) {
            renderProgress(data.result);
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
                Utils.debug(currentFile.error);
                $('<p/>').text("ERROR: " + currentFile.error + " " + currentFile.name).appendTo("#messages").addClass("text-danger");
            }
        },
        add: function (e, data) {
            data.formData = Utils.createData(
                    {alignmentType: $('#alignmentTypeFile').val()});
            data.submit();
        }

    });
    $("#pasteSequenceForm").submit(function (event) {
        // Stop form from submitting normally
        event.preventDefault();
        // Get some values from elements on the page:
        var $form = $(this),
                url = $form.attr("action");
        var data = Utils.createData(
                {alignmentType: $('#alignmentTypePaste').val(),
                    sequence: $('#pasteSequence').val()
                });

        renderImage(url, data);
    });

    $("#useExampleForm").submit(function (event) {
        // Stop form from submitting normally
        event.preventDefault();
        // Get some values from elements on the page:
        var $form = $(this),
                url = $form.attr("action");
        var data = Utils.createData(
                {alignmentType: 'AMINOACIDS'});
        renderImage(url, data);
    });

    $("#createBundleButton").click(function () {
        Utils.animatePreviewImage();
    });
    $("#downloadButton").click(function () {
        Utils.animateDowloadImage();
    });
});
