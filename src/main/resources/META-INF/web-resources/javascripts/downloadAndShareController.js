$(function() {
	'use strict';
	// Get the current URL
	var pathname = window.location;

	// Place the text on the page. Change body to wherever you want the button
	// placed.
	$('#tweetButton')
			.click(
					function(e) {
						e.stopPropagation();

						var pathname = window.location;
						var tweetText = encodeURIComponent("Try the #SequenceBundles web tool by @sciencepractice https://sequence-bundles.herokuapp.com/");
						// Create the twitter URL
						var tweetUrl = 'http://twitter.com/share?&text='
								+ tweetText;

						var link = $('<a href="' + tweetUrl + '" />');
						link.attr('target', '_blank');
						window.open(link.attr('href'));
					});
});