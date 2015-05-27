/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

 $(function() {
    'use strict';

    $("#applyChangesButton").click(function(event) {
        // Stop form from submitting normally
        event.preventDefault();
        // Get some values from elements on the page:
        var url = '/upload/paste';
        var data = Utils.createData();

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
        return false;
    });

    // use images that active/deactivate instead of radio buttons
    $('input[name=gapRendering]', '#visualSettingsForm').each(
        function() {
            $(this).click(
                function() {
                    var otherImages = $('img', $('#gapRadioGroup',
                        '#visualSettingsForm'));
                    var activeImage = $(
                        'input[name=gapRendering]:checked',
                        '#visualSettingsForm').siblings();
                    Utils.setActiveSVG(otherImages, activeImage);
                });
        });

    // jump conservation to 0, 50, 100
    $('.conservationJump').each(function() {
        var self = $(this);
        self.click(function() {
            updateConservationThreshholdControls(self.attr('value'));
        });
    });

    // setup conservation initial value
    $("#conservationThresholdLabel").val("0");

    // setup slider
    $("#sliderResidueConservation").on('input', function(event) {
        // console.log(this.value);
        updateConservationThreshholdControls(this.value);
    });

    function updateConservationThreshholdControls(value) {
        // slider
        $('#sliderResidueConservation').val(value);
        // form value
        $("#conservationThreshold").val(parseInt(value, 10) / 100);
        // label
        $("#conservationThresholdLabel").text(value + '%');

    }
});