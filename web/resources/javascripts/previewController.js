/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
var PreviewController = {
    init: function () {
        $("#previewForm").submit(function (event) {
            // Stop form from submitting normally
            event.preventDefault();
            // Get some values from elements on the page:
            var $form = $(this),
                    url = $form.attr("action");
            var data = $('#visualSettingsForm').serializeArray();
            // sanitize start index less than 1
            if ($('#startIndex').val() < 1) {
                $('#startIndex').val(1);
            }
            data.push({name: "startIndex", value: $('#startIndex').val()});
            // Send the data using post
            var posting = $.post(url, data);
            // Put the results in a div
            posting.done(function (data) {
                var d = new Date();
                var wp = data["webPath"] + "?" + d.getTime();
                var filename = Utils.getFilename(wp);
                Utils.jobStatusPoll(filename, wp);
            });
        });


        $('#n-terminus').click(function () {
            $('#startIndex').val("0");
            PreviewController.renderImage();
        });

        $('#c-terminus').click(function () {
            $('#startIndex').val($('#visualSettingsForm #lastIndex').val());
            PreviewController.renderImage();
        });





        // setup conservation intial value
        $("#conservationThresholdLable").val("0");

        // setup slider
        $("#sliderResidueConservation").slider({
            min: 0,
            max: 1,
            step: .01,
            value: 0,
            slide: function (event, ui) {
                //$("#horizontalExtentLable").val(sizesLabel[ui.value]);
                $("#conservationThreshold").val(ui.value);
                $("#conservationThresholdLable").val(ui.value);
            }
        });


    },
    oldSliderValue: 1,
    initSequenceSlider: function (start, max, step) {
        console.log("init sequence slider - max: " + max + "step: " + step);
        PreviewController.oldSliderValue = $("#startIndex").val();

        $("#sliderSequence").slider({
            min: 1,
            max: max,
            step: step,
            value: start,
            slide: function (event, ui) {
                //$("#horizontalExtentLable").val(sizesLabel[ui.value]);
                $("#startIndex").val(ui.value);

            },
            change: function (event, ui) {
                // update image only if slider value changes
                if (ui.value !== PreviewController.oldSliderValue) {
                    console.log('PreviewController.oldSliderValue: ' + PreviewController.oldSliderValue);
                    console.log('ui.value: ' + ui.value);
                    PreviewController.oldSliderValue = ui.value;
                    PreviewController.renderImage();
                }
            }
        });
    },
    renderImage: function () {
        var posting = $.post("/upload/seq2", Utils.createData());

        // Put the results in a div
        posting.done(function (data) {
            PreviewController.renderProgress(data);
        });
    },
    renderProgress: function (data) {
        var d = new Date();
        var wp = data.webPath + "?" + d.getTime();
        var filename = Utils.getFilename(wp);
        Utils.jobStatusPoll(filename, wp);
    }
};

$(function () {
    'use strict';
    PreviewController.init();
});