/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(function () {
    'use strict';

    $("#previewForm").submit(function (event) {
// Stop form from submitting normally
        event.preventDefault();
// Get some values from elements on the page:
        var $form = $(this),
                url = $form.attr("action");
        var data = $('#visualSettingsForm').serializeArray();
        data.push({name: "startIndex", value: $('#startIndex').val()});
// Send the data using post
        var posting = $.post(url, data);
// Put the results in a div
        posting.done(function (data) {
            var d = new Date();
            var wp = data["webPath"] + "?" + d.getTime();
            var filename = wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
            utils.animateShowImage();
            utils.jobStatusPoll(filename, wp);
        });
    });


    $('#n-terminus').click(function () {
        $('#startIndex').val("0");
        var posting = $.post("/upload/seq2", utils.createData());

        // Put the results in a div
        posting.done(function (data) {
            renderProgress(data);
        });
    });

    $('#c-terminus').click(function () {
        $('#startIndex').val($('#visualSettingsForm #lastIndex').val());
        var posting = $.post("/upload/seq2", utils.createData());
        // Put the results in a div
        posting.done(function (data) {
            renderProgress(data);
        });
    });

    function renderProgress(data) {
        var d = new Date();
        var wp = data.webPath + "?" + d.getTime();
        var filename = wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
        utils.jobStatusPoll(filename, wp);
    }

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

});