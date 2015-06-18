var debug = false;
var sequence = "";
/*
 * jQuery File Upload Plugin JS Example 8.9.1
 * https://github.com/blueimp/jQuery-File-Upload
 *
 * Copyright 2010, Sebastian Tschan https://blueimp.net
 *
 * Licensed under the MIT license: http://www.opensource.org/licenses/MIT
 */

 /* global $, window */

 $(function() {
    'use strict';
    enableCreateBundleButton(false);

    function uploadSequenceInfo(otherClass, text, isEnabled) {
        var status = $('#uploadStatus');
        $('[class^=valid-background]', status).removeClass().addClass('valid-background-' + otherClass);
        $('.valid-message-grey', status).removeClass().addClass('valid-message-' + otherClass);
        $('.valid-message-green', status).removeClass().addClass('valid-message-' + otherClass);
        $('.valid-message-red', status).removeClass().addClass('valid-message-' + otherClass);
        if (typeof text === "string") {
            $('h4', status).text(text);

        } else {
            $('h4', status).html(text);
        }
        status.show();
        enableCreateBundleButton(isEnabled);
    }

    function enableCreateBundleButton(isEnabled) {
        var createButton = $('.next-btn').removeClass();
        if (isEnabled) {
            createButton.addClass('next-btn action-button-active');
            $('#dataReady').val(true);
            $('h1.disabled').removeClass();
        } else {
            createButton.addClass('next-btn action-button-inactive');
            $('#dataReady').val(false);
        }
    }

    function renderImage(url, data, validatingText) {
        // display validating
        uploadSequenceInfo('grey', validatingText, false);

        var posting = $.post(url, data);
        // Put the results in a div
        posting.done(function(data) {
            $('#uploadStatus').removeClass('uploadPanelInfo');
            renderProgress(data);
        });
    }

    // meta data has been returned about the sequence
    // but the image still needs to be rendered hence renderProgress
    function renderProgress(data) {
        // handle possible errors in data
        if (data.errorMessage && data.errorMessage.length > 0) {
            var errorMessage = '';
            if (data.errorMessage.indexOf("1000") > 0) {
                errorMessage = "FASTA format is valid, but your data is too large (it has " + data.sequenceCount + " sequences, each " + data.sequenceBases + " positions long).";
                uploadSequenceInfo('red', errorMessage, false);
            } else {
                errorMessage = $('<div/>')
                .html(
                    "FASTA format not valid. Learn more about the <a href='http://en.wikipedia.org/wiki/FASTA_format'>FASTA</a> format here.<br/>");
                // errorMessage = "FASTA format not valid. Learn more about the <a href='http://en.wikipedia.org/wiki/FASTA_format/>FASTA</a> format here.<br/>" + data.errorMessage;
                uploadSequenceInfo('red', errorMessage, false);
            }
            return;
        }

        var text = "Your protein data contains " + data.sequenceCount + " sequences, each " + data.sequenceBases + " positions long.";
        uploadSequenceInfo('green', text, true);

        var d = new Date();
        var wp = data.webPath + "?" + d.getTime();
        var filename = Utils.getFilename(wp);
        // init rendering progress info
        Utils.jobStatusPoll(filename, wp);
        // add loading overlay for sequence
        Utils.showImage();

        // distribute meta data to visualiztion form
        $("#visualSettingsForm #sequence").val(data.sequences);
        $('#visualSettingsForm #lastIndex').val(data.sequenceBases);
        $('#visualSettingsForm #columnCount').val(data.numberOfColumns);
        PreviewController.initSequenceSlider(1, data.sequenceBases, 1);
    }

    $('#fileUpload').fileupload({
        dataType: 'json',
        maxFileSize: 5 * 1024 * 1024,
        acceptFileTypes: /(\.|\/)(txt|fasta)$/i,
        done: function(e, data) {
            renderProgress(data.result);
        },
        progressall: function(e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .progress-bar').css('width',
                progress + '%');
        },
        processfail: function(e, data) {
            var currentFile = data.files[data.index];
            if (data.files.error && currentFile.error) {
                // there was an error, do something about it
                Utils.debug(currentFile.error);
                $('<p/>').text(
                    "ERROR: " + currentFile.error + " " + currentFile.name).appendTo(
                    "#messages").addClass("text-danger");
                }
            },
            add: function(e, data) {
            // display validating
            uploadSequenceInfo('grey',
                "Uploading and Validating ...", false);
            // clear old sequence data
            $("#visualSettingsForm #sequence").val('');
            $('#startIndex').val(1);
            data.formData = Utils.createData();
            data.submit();
        }
    });

$("#pasteSequenceButton").click(
    function(event) {
            // clear old sequence data
            $("#visualSettingsForm #sequence").val('');
            $('#startIndex').val(1);
            var url = "/upload/paste";
            var data = Utils.createData({
                sequence: $('#pasteSequence').val()
            });
            renderImage(url, data, "Uploading and validating ...");
            return false;
        });

$("#useExampleButton").click(function(event) {
        // clear old sequence data
        $("#visualSettingsForm #sequence").val('');
        var filename = descriptionMap[$('#useExampleFile').val()].filename;
        // Get some values from elements on the page:
        var url = "/upload/example";
        $('#startIndex').val(1);
        var data = Utils.createData({
            filename: filename
        });
        renderImage(url, data, "Validating ...");
        return false;
    });

var descriptionMap = {
    '5': {
        description: 'In living organisms, ABC transporters are responsible for moving molecules across cellular membranes. To do this job, they require energy generated by their ATP-domains.\
        Source: Pfam (PF00005)',
        filename: 'ABC_tran-(PF00005)_ATP-binding-domain-of-ABC-transporters.txt'
    },
    '6': {
        description: 'Alcohol dehydrogenase is a family of enzymes which catalyse alcohols. In humans, its catalytic domain processes toxic ethanol with a turnover of about 1200 molecules per second.\
        Source: Pfam (PF08240)',
        filename: 'ADH_N-(PF08240)_Alcohol-dehydrogenases-catalytic-domain.txt'
    },
    '7': {
        description: 'Adenylate kinase is an enzyme that plays an important role in cellular energy homeostasis. Its LID domain in stabilised by hydrogen bonds in Gram-negative bacteria, and by a metal ion in Gram-positives.\
        Source: Pfam (PF05191); Magliery Lab and Ray Lab, Ohio State University',
        filename: 'ADK_LID-Adenylate-kinase-lid-domain_mod-from-Magliery-Ray.txt'
    },
    '9': {
        description: 'The ankyrin repeat is a common structural motif in proteins. Proteins containing the ankyrin motif can be involved in cell signalling, muscle tissue repair, and have been linked to some cancers.\
        Source: Pfam (PF12796)',
        filename: 'Ank_2-(PF12796)_Ankyrin-repeat_seed.txt'
    },
    '2': {
        description: 'This domain belongs to carbohydrate-binding modules associated with glycoside hydrolases — extremely common enzymes found in all kinds of living organisms, but also used in food and paper industry.\
        Source: Pfam (PF14600)',
        filename: 'CBM_5_12_2-(PF14600)_Cellulose-binding-domain_RP75.txt'
    },
    '3': {
        description: 'This protein domain can be found in eucaryotes. Its function has not yet been characterised.\
        Source: Pfam (PF15363).',
        filename: 'DUF4596-(PF15363)_Domain-of-unknown-function.txt'
    },
    '4': {
        description: 'This is an evolutionary conserved domain that can be found in several ATP-binding proteins, for example in Hsp90 protein which protects cells from elevated temperatures.\
        Source: Pfam (PF02518)',
        filename: 'HATPase_c-(PF02518)_GHKL-domain_Seed.txt'
    },
    '8': {
        description: 'Insulin is a protein hormone produced in pancreas and involved in regulation of glucose levels in blood. Three cysteine pairs are responsible for the structure of insulin, hence they are very strongly conserved across species.\
        Source: Pfam (PF00049)',
        filename: 'Insulin-(PF00049)-RP55.txt'
    },
    '1': {
        description: 'This is a predicted toxin domain with strongly conserved motifs. The domain is present in bacterial polymorphic toxin systems.\
        Source: Pfam (PF15654)',
        filename: 'Tox-WTIP-(PF15654)_Toxin-with-tryptophan-and-TIP-motif.txt'
    },
    '10': {
        description: 'Zinc-fingers are small protein structural motifs. Most of them function as interaction modules binding to DNA, RNA, other proteins, or other molecules.\
        Source: Pfam (PF13465)',
        filename: 'zf-H2C2_2-(PF13465)-Zinc-finger-double-domain.txt'
    }

};

    // hide status on load
    $('#uploadStatus').addClass('hide');
    // update sequence description
    $('#upload-description').text(descriptionMap[$('#useExampleFile').description]);

    $("#useExampleFile").on(
        'change',
        function() {
            // update description blerb
            $('#upload-description').text(
                descriptionMap[$('#useExampleFile').val()].description);
        });

    $("#createBundleButton").click(function() {
        Utils.animatePreviewImage();
        return false;
    });
});