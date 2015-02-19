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

})