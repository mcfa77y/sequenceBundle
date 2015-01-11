/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


$(function () {
    $("#visualSettingsForm").submit(function (event) {
// Stop form from submitting normally
        event.preventDefault();
// Get some values from elements on the page:
        var $form = $(this),
                url = $form.attr("action");
        var data = JSON.stringify($form.serializeObject());
// Send the data using post
        var posting = $.post(url, data);
// Put the results in a div
        posting.done(function (data) {
            console.log("webPath: " + data.result["webPath"]);

            $('#sequenceBundleImage').prepend('<img class="image-sm" id="theImg" src="' + data.result["webPath"] + '" />')
            $('#theImg').bind('load', function () {
                $('#sequenceBundleImage').imagefit()
            });
        });
    });

})