

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



    $.fn.serializeObject = function () {
        var o = {};
//    var a = this.serializeArray();
        $(this).find('input[type="hidden"], input[type="text"], input[type="password"], input[type="checkbox"]:checked, input[type="radio"]:checked, select').each(function () {
            if ($(this).attr('type') == 'hidden') { //if checkbox is checked do not take the hidden field
                var $parent = $(this).parent();
                var $chb = $parent.find('input[type="checkbox"][name="' + this.name.replace(/\[/g, '\[').replace(/\]/g, '\]') + '"]');
                if ($chb != null) {
                    if ($chb.prop('checked'))
                        return;
                }
            }
            if (this.name === null || this.name === undefined || this.name === '')
                return;
            var elemValue = null;
            if ($(this).is('select'))
                elemValue = $(this).find('option:selected').val();
            else
                elemValue = this.value;
            if (o[this.name] !== undefined) {
                if (!o[this.name].push) {
                    o[this.name] = [o[this.name]];
                }
                o[this.name].push(elemValue || '');
            } else {
                o[this.name] = elemValue || '';
            }
        });
        return o;
    };

    var frm = $(document.visualSettingsForm);
    var data = JSON.stringify(frm.serializeObject());

    $('#fileupload').fileupload({
        dataType: 'json',
        maxFileSize: 5 * 1024 * 1024,
        acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
        done: function (e, data) {
            $('#collapseOne').collapse('hide');
            $('#collapseTwo').collapse('show');
            console.log("webPath: " + data.result["webPath"]);

            $('#sequenceBundleImage').prepend('<img class="image-sm" id="theImg" src="' + data.result["webPath"] + '" />')
            $('#theImg').bind('load', function () {
                $('#sequenceBundleImage').imagefit()
            });
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
            data.formData = ($(document.visualSettingsForm).serializeArray());
            //data.formData = {example: 'test'};
            data.submit();
        }

    });

});
