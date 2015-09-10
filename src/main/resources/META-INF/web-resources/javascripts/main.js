$(document).ready(function() {
    // clean state with no sequence data nor start index
    $('#sequence').val('');
    $('#startIndex').val('');

    // for all task buttons return false
    // except for download and file upload buttons they have special functions
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
