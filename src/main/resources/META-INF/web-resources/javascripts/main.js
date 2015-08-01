$(document).ready(function() {
    var debug = false;
    // clean state with no seq data or start index
    $('#sequence').val('');
    $('#startIndex').val('');

    // for all task buttons return false
    // except for download and file upload buttons they have special funtions
    $('.task-button').click(function() {
        if (this.id !== 'downloadPNGButton' && this.id !== "fileUploadButton") {
            return false;
        }
    });

    // only allow step 2,3 to work if data is ready
    $('.step3-trigger, .step2-trigger').click(function() {
        if (Utils.isDataNotReady()) {
            Utils.animateUploadSequence();
        }
    });

});

var Utils = {
    animateShowImage : function() {
        Utils.showLoadingImage();
        document.getElementById("tab-2").checked = false;
        document.getElementById("tab-1").checked = true;
        // location.href = "#js-step2-trigger";
    },
    showLoadingImage : function() {
        $('#sequenceBundle').addClass('loading');
        $('#loading').show();
        $('#renderProgress').fadeIn();
    },
    debug : function(message) {
        if (debug) {
            console.log(message);
        }
    },
    animateDowloadImage : function() {
        if (Utils.isDataNotReady()) {
            return false;
        }
        $('#js-step1-trigger').toggleClass('step1-hidden', true);
        $('#js-step2-trigger').toggleClass('step2-hidden', true);
        $('#js-step3-trigger').toggleClass('step3-hidden', false);
        // location.href = "#js-step3-trigger";

    },
    animatePreviewImage : function() {
        if (Utils.isDataNotReady()) {
            return false;
        }
        $('#js-step1-trigger').toggleClass('step1-hidden', true);
        $('#js-step2-trigger').toggleClass('step2-hidden', false);
        $('#js-step3-trigger').toggleClass('step3-hidden', true);
        // location.href = "#js-step2-trigger";
    },
    animateUploadSequence : function() {
        $('#js-step1-trigger').toggleClass('step1-hidden', false);
        $('#js-step2-trigger').toggleClass('step2-hidden', true);
        $('#js-step3-trigger').toggleClass('step3-hidden', true);
        // location.href = "#js-step1-trigger";

    },
    setActiveSVG : function(otherImages, activeImage) {
        // turn all images to disable png
        for (var i = 0; i < otherImages.length; i++) {
            var image = $(otherImages[i]);
            // remove active from source
            var deactivatedSrc = image.attr('src').replace('_active', '');
            image.attr('src', deactivatedSrc);

        }
        // turn on only active tab
        var activatedSrc = activeImage.attr('src').split('.')[0]
                + "_active.svg";
        activeImage.attr('src', activatedSrc);

    },
    getFilename : function(wp) {
        return wp.substring(wp.lastIndexOf('/') + 1, wp.lastIndexOf('?'));
    },
    createData : function(opt) {
        // gets data from visualSettingsForm
        // and puts it into a JSON
        if (!opt) {
            opt = {};
        }
        var data = $('#visualSettingsForm').serializeArray();
        var startIndex = $('#startIndex').val();
        if (startIndex === "" || startIndex < 1) {
            startIndex = 1;
            $('#startIndex').val(1);
        }
        // converts data into an associated array
        // where the name of the form element name is the key
        // and the form value is the value of key map
        var dataKeyMap = {};
        for (i = 0; i < data.length; i++) {
            var key = data[i].name;
            dataKeyMap[key] = {
                value : data[i].value,
                index : i
            };
        }

        if (dataKeyMap.startIndex) {
            data[dataKeyMap.startIndex].value = startIndex;
        } else {
            data.push({
                name : "startIndex",
                value : startIndex
            });
        }

        // add check box value if not selected
        if (!dataKeyMap.showingVerticalLines) {
            data.push({
                name : "showingVerticalLines",
                value : false
            });
        }

        // if user sends over opts then use those values in the data that will
        // be sent to the server
        // see if opt has any values to use
        if (Object.keys(opt).length > 0) {
            // loop over the keys in opt
            for (key in opt) {
                if (opt.hasOwnProperty(key)) {
                    var value = opt[key];
                    // if the attribute is already in data then update it
                    if (dataKeyMap[key]) {
                        data[dataKeyMap[key].index].value = value;
                    } else {
                        // else push the option onto the data object
                        // attribute may not in the data form but user wants to
                        // have it sent with the request
                        data.push({
                            name : key,
                            value : value
                        });
                    }
                    // update form visualization data
                    $('#visualSettingsForm ' + '#' + key).val(value);
                }
            }
        }
        return data;
    },
    alertWarning : function(message, attachAfterId) {
        if ($(attachAfterId).siblings('.alert').length > 0) {
            return;
        }
        var alertHTML = '<div class="alert alert-warning alert-dismissible" role="alert">\
        <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>\
        '
                + message + '</div>';
        $(attachAfterId).after(alertHTML);
    },
    jobStatusPoll : function(filename, imagePath) {
        // location.href = "#js-step1-trigger";
        // polls the server for status of the image that is rendering
        // Utils.animateShowImage();
        $.post("upload/seq/status", {
            filename : filename
        }).done(
                function(data) {

                    if (data['isFinished'] === false) {
                        // continue to get status information
                        setTimeout(function() {
                            var d = new Date();
                            Utils.jobStatusPoll(filename, imagePath);
                        }, 500);
                    } else {
                        // image has finish rendering
                        Utils.debug("main.js: image has finish rendering");
                        $('#loading').hide();
                        $('#sequenceBundle').removeClass("loading");

                        // remove rendering progress listener
                        $.post("upload/seq/remove", {
                            filename : filename
                        });

                        $('#renderProgress').fadeOut(300, function() {
                            $(this).hide();
                        });

                        // add image if there was none before otherwise
                        // update source for newly loaded image
                        var image = $('#sequenceBundle #sequenceBundleImage');
                        if (image.size() > 0) {
                            image.attr('src', imagePath);
                        } else {

                            $('#sequenceBundle').prepend(
                            '<img class="vis-box" id="sequenceBundleImage" src="' + imagePath + '" />').fadeIn();
                        }

                        $('#sequenceBundleImage').bind(
                                'error',
                                function(e) {
                                    var err = JSON.stringify(e, null, 4);
                            Utils.debug("error loading image:" + imagePath + "\n" + err);
                                    image.attr('src', imagePath);
                                });
                        $('#downloadPNGButton').attr('href', imagePath);
                        $('#downloadPNGButton').attr('download', filename);


                    // hide sequence nav container if the number of column is >= number of bases
                    if (parseInt($('#columnCount').val(),10) >=  parseInt($('#lastIndex').val(),10)) {
                            $('.seq-nav-container').addClass('hide');
                        } else {
                            $('.seq-nav-container').removeClass('hide');
                        }

                        // hide render status
                        $('#renderHiResStatus').hide();
                    }
                }).error(function(e) {
            var err = JSON.stringify(e, null, 4);
            Utils.debug("error loading jobStatus:" + "\n" + err);
        });
    },
    isDataNotReady : function() {
        Utils.debug("data ready? : " + JSON.parse($('#dataReady').val()));
        return !JSON.parse($('#dataReady').val());
    }
};
{
}