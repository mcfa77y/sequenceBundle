/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
var PreviewController = {
    init: function() {
        $('#startIndex').change(function(event) {
            $('#gotoPositionButton').removeClass();
            $('#gotoPositionButton').addClass('position-go-button');
        });
        $('#gotoPositionButton').click(function(event) {
            if (!Utils.isDataReady()) {
                return false;
            }
            if ($(this).hasClass('disabled')) {
                return false;
            }
            var startIndex = $('#startIndex');
            var startIndexValue = parseInt($('#startIndex').val(), 10);
            var lastIndexValue = parseInt($('#lastIndex').val(), 10);

            if (isNaN(startIndexValue)) {
                return false;
            }

            // Get some values from elements on the page:
            var url = './upload/paste';
            var data = $('#visualSettingsForm').serializeArray();
            // sanitize start index less than 1
            if (startIndexValue < 1) {
                startIndex.val(1);
            }
            if (startIndexValue > lastIndexValue) {
                startIndex.val(lastIndexValue);
            }

            PreviewController.updateSequenceNavigationControls(startIndex.val());
            PreviewController.renderImage();
            return false;
        });

        $('#n-terminus').click(function() {
            if (!Utils.isDataReady()) {
                return false;
            }
            PreviewController.updateSequenceNavigationControls(1);
            PreviewController.renderImage();
            return false;
        });

        $('#c-terminus').click(function() {
            if (!Utils.isDataReady()) {
                return false;
            }
            var lastIndex = $('#visualSettingsForm #lastIndex').val() - $('#visualSettingsForm #columnCount').val() + 1;
            PreviewController.updateSequenceNavigationControls(lastIndex);
            PreviewController.renderImage();
            return false;
        });

        // // controls for tab icons changing from active <-> inactive states
        // $('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
        // // turn all tabs to disable png
        // var tabImages = $('#tabs li.nav img');
        // var activeImage = $($('#tabs li.nav.active img')[0]);
        // Utils.setActiveSVG(tabImages, activeImage);
        // });
        $("#downloadButton").click(function() {
            Utils.animateDowloadImage();
            return false;
        });

    },
    oldSliderValue: 1,
    initSequenceSlider: function(max, step) {
        PreviewController.oldSliderValue = $('#startIndex').val();
        var sliderSequence = $('#sliderSequence');
        sliderSequence.attr({
            min: 1,
            max: max,
            step: step,
            value: 1
        });
        sliderSequence.on('input', function(event) {
            PreviewController.updateSequenceNavigationControls(this.value);
        });
        sliderSequence.on('change', function(event) {
            var value = this.value;
            // update image only if slider value changes
            if (value !== PreviewController.oldSliderValue) {
                PreviewController.oldSliderValue = value;
                PreviewController.updateSequenceNavigationControls(value);
                PreviewController.renderImage();
            }
        });

        PreviewController.updateSequenceNavigationControls(1);
    },
    // updateSliderWidth: function() {
    // var sequenceSlider = $('#sliderSequence');
    // var newWidth = 0.75 * (sequenceSlider.parent().parent().parent()
    // .width() - ($('#n-terminus').width() + $('#c-terminus').width() + $(
    // '#previewForm').width()));
    // sequenceSlider.width('100%');
    // },
    renderImage: function() {
        Utils.showLoadingImage();
        var posting = $.post('./upload/paste', Utils.createData());
        // Put the results in a div
        posting.done(function(data) {
            PreviewController.renderProgress(data);
        });
    },
    renderProgress: function(data) {
        var d = new Date();
        var wp = data.webPath + '?' + d.getTime();
        var filename = Utils.getFilename(wp);
        // init rendering progress info
        Utils.jobStatusPoll(filename, wp);
    },
    updateSequenceNavigationControls: function(value) {
        var lastValue = parseInt($('#lastIndex').val(), 10);
        var range = parseInt($("#columnCount").val(), 10);
        value = parseInt(value, 10);
        displayValue = Math.min(value, lastValue - range + 1);
        displayValue = Math.max(displayValue, 1);
        // search field
        $('#startIndex').val(value);
        var endRange = Math.min(value + range - 1, lastValue);
        // index label
        $('#sequenceIndexLabel').text(displayValue + " - " + endRange + " OUT OF " + lastValue + " POSITIONS");
        // slider
        $('#sliderSequence').attr('max', lastValue - range + 1);
        $('#sliderSequence')[0].value = value;
    }
};

$(function() {
    'use strict';
    PreviewController.init();

});

