$(function() {
    'use strict';
    var HI_RES_DPI = 72 * 1.0;

    function renderHiResInfo(otherClass, text, enableCreateBundleButton) {
        var status = $('#renderHiResStatus');
        status.show();
        status.removeClass();
        status.addClass('upload-status ' + otherClass);
        status.html(text);
        status.show();
    }

    function renderHiResImageStatusPoll(filename, imagePath) {
        $.post("upload/seq/status", {
            filename: filename
        }).done(
        function(data) {
            var progress = parseInt(data.value / data.max * 100, 10);

            var progressBar = $('#renderHiResProgress .bar').show();
            progressBar.css('width', progress + '%');
            if (data['isFinished'] === false) {
                    // continue to get status information
                    setTimeout(function() {
                        var d = new Date();
                        renderHiResImageStatusPoll(filename, imagePath);
                    }, 500);
                } else {
                    // image has finish rendering
                    // remove rendering progress listener
                    $.post("upload/seq/remove", {
                        filename: filename
                    });
                    var link = $('<a/>').attr('href', imagePath).attr(
                        'download', filename);
                    renderHiResInfo('upload-status-success',
                        'Rendering Complete', true);
                    window.open(imagePath, 'Download');
                }
            }).error(function(e) {
                var err = JSON.stringify(e, null, 4);
                Utils.debug("error loading jobStatus:" + "\n" + err);
            });
        }

        function renderHiResImage(data) {
        // sequence should already be in form so similar function to paste
        var url = "/upload/paste";

        // display Rendering
        renderHiResInfo('upload-status-validate', "Rendering ...", false);

        // start render this will return meta data on sequence
        // the rendering will continue as a worker thread on the server
        var posting = $.post(url, data);
        posting
        .done(function(data) {
                // handle possible errors in data
                if (data.errorMessage && data.errorMessage.length > 0) {
                    var errorMessage = '';
                    if (data.errorMessage.contains("1000")) {
                        errorMessage = "FASTA format is valid, but your data is too large (it has " + data.sequenceCount + " sequences, each " + data.sequenceBases + " positions long).";
                    } else {
                        errorMessage = $('<div/>')
                        .html(
                            "FASTA format not valid. Learn more about the FASTA format <a href='' target='_blank'>here</a>. <br/> " + data.errorMessage);
                    }
                    renderHiResInfo('upload-status-error', errorMessage,
                        false);
                    return;
                }
                var d = new Date();
                var wp = data.webPath + "?" + d.getTime();
                var filename = Utils.getFilename(wp);

                renderHiResImageStatusPoll(filename, wp);
            });
}

$('#downloadPNG').click(function() {
    // var data = Utils.createData({
    //     alignmentType: $('#alignmentTypePaste').val(),
    //     dpi: HI_RES_DPI
    // });
    // renderHiResImage(data);
});

$('#tweetButton')
.click(
    function(e) {
        e.stopPropagation();

        var pathname = window.location;
        var tweetText = encodeURIComponent("Try the #SequenceBundles web tool by @sciencepractice ");
                // Create the twitter URL
                var tweetUrl = 'http://twitter.com/share?&text=' + tweetText;

                var link = $('<a href="' + tweetUrl + '" />');
                link.attr('target', '_blank');
                window.open(link.attr('href'));
            });
});