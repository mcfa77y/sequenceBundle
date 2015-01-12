

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
    jobStatusPoll: function (filename, imagePath) {
        $.post("upload/seq/status", {filename: filename}).done(function (data) {
            var progress = parseInt(data.value / data.max * 100, 10);
            console.log("render progress: " + data['value'] + "/" + data['max'] + " = " + progress);
            console.log("isFinished: " + data['isFinished']);
            $('#renderProgress .progress-bar').css(
                    'width',
                    progress + '%'
                    );

            if (data['isFinished'] === false) {
                console.log("not finished");

                setTimeout(function () {
                    var d = new Date();
                    console.log("date: " + d);
                    utils.jobStatusPoll(filename, imagePath);
                }, 500);
            } else {
                console.log("finished: " + data['isFinished']);
                $.post("upload/seq/remove", {filename: filename});
                $('#renderProgress').show();
                $('#renderProgress .progress-bar').css(
                        'width',
                        100 + '%'
                        );
                $('#renderProgress').fadeOut(400, function () {
                    console.log("main hiding")
                    $(this).hide();
                });
                if ($('#sequenceBundleImage img').size() > 0) {
                    console.log('removing image');
                    $("#sequenceBundleImage img").remove();
                }
                $('#sequenceBundleImage').prepend('<img class="image-sm" id="theImg" src="' + imagePath + '" />').fadeIn();
                $('#theImg').bind('load', function () {
                    var img = $('#sequenceBundleImage img');
                    var sw = img.width() * 500 / 2250;
                    var sh = 500;
                    img.css("height", sh + "px");
                    img.css("width", sw + "px");
                });


            }
        });
    }
};
$(function () {
    'use strict';


//    var frm = $(document.visualSettingsForm);
//    var data = JSON.stringify(frm.serializeObject());


    $('#fileupload').fileupload({
        dataType: 'json',
        maxFileSize: 5 * 1024 * 1024,
        acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
        done: function (e, data) {
            $('#collapseOne').collapse('hide');
            $('#collapseTwo').collapse('show');
            console.log("webPath: " + data.result["webPath"]);
            var wp = data.result["webPath"];
            var filename = wp.substring(wp.lastIndexOf('/') + 1, wp.length);
            utils.jobStatusPoll(filename, wp);
            $.cookie("sequence", data.result["sequences"], {path: "/", json: true});
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
                console.log(currentFile.error);
                $('<p/>').text("ERROR: " + currentFile.error + " " + currentFile.name).appendTo("#messages").addClass("text-danger");
            }
        },
        add: function (e, data) {
            data.formData = ($("#visualSettingsForm").serializeArray());
            data.submit();
        }

    });

});
