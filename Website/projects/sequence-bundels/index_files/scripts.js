//Working with us cards javascript functions

var cardCopy1 = [
  "for tackling antibiotic resistance.",
  "for sustainable water desalination.",
  "for novel Computing technologies.",
  "to develop zero carbon flight."
];

var cardCopy2 = [
  "visualisation tools for bioinformatics that support research and discovery.",
  "prototyping tools for diagnostic development.",
  "analysis tools for understanding genomic data.",
  "tools for researchers conducting experiments in low resources settings."
];

var cardCopy3 = [
  "imagine the future of Synthetic Biology.",
  "imagine the impact of emerging technologies.",
  "imagine the future of government.",
  "imagine the ."
];

var cardCopy4 = [
  "methods and practical applications for Microfluidics.",
  "for biosensors that can be used in the field.",
  "something else here.",
  "something else here."
];

var cardCopy5 = [
  "between biologists and computer scientists.",
  "doctors and geneticists.",
  "something else here.",
  "something else here."
];

var captionLength = 0;
var counter = 0;

$(document).ready(function() {

	$('#slides').superslides({
		animation: 'fade',
		play: 4000
	});

	$('.carousel-link-container').hover(
		function() {
			$('#slides').superslides('stop');
			clearTimeout(timeout);
		}, function () {
			timeout = setTimeout(function() {
      			$('#slides').superslides('start');
			}, 2000);
		}
	);
	
	$('.capability-detailed').addClass("expander-hidden");
	
	$('#cap-button').click(function(){
    	$('.capability-detailed').toggleClass("expander-hidden");
    	//$(this).
    	return false;
  	});
	
	$('.update').prop('disabled', true);
    
    //could make similar function to handle all of these
    // $('#refresh1').click(refreshButton);
//     $('#refresh2').click(refreshButton);
//     $('#refresh3').click(refreshButton);
//     $('#refresh4').click(refreshButton);
//     $('#refresh5').click(refreshButton);
    
    $('.update').each(function (counter) {
    		$(this).click(refreshButton)
		});
    
    //variable to make sure typing does keep 
    var startedTyping = false;
    
    $(window).scroll( function(){
		if (startedTyping == false) {
        	$('#cards-typing').each( function(i){
            
            	var bottom_of_object = $(this).offset().top + $(this).outerHeight();
            	var bottom_of_window = $(window).scrollTop() + $(window).height();
            
            	if( bottom_of_window > bottom_of_object ){
                    
                	typingOnLoad('#card-caption1');
                  typingOnLoad('#card-caption2');
									typingOnLoad('#card-caption3');
									typingOnLoad('#card-caption4');
									typingOnLoad('#card-caption5');
                  startedTyping = true;
            	}
        	}); 
    	}
	});
});

function refreshButton() {

	var buttonPressed = $(this);
	$(buttonPressed).prop('disabled', true);
	var buttonNum = $(this).attr('id').replace(/refresh/, '');
  var captionElement = $("#card-caption" + buttonNum);
  var caption = captionElement.html();
  var captionLength = caption.length;
  var obj = eval("cardCopy" + buttonNum);
	var numberOfThings = obj.length;
  	
    var allDeleted = -1;
    var erasing = setInterval(function() {
    	if (allDeleted < captionLength) {
        	captionElement.html(caption.substr(0, captionLength--));
        } else {
        	clearTimeout(erasing);
        	setTimeout(function() {
				if (counter == numberOfThings) { counter = 0; }
				var caption = obj[counter];
				var arrayCapLength = caption.length;
    			var typing = setInterval(function() {
    				if (arrayCapLength >= captionLength) {
    					captionElement.html(caption.substr(0, captionLength++));
    				} else {
    					clearTimeout(typing);
    					$(buttonPressed).prop('disabled', false);
    				}
    			}, 50);
    			
			}, 1000);
        }
    }, 25);
        
	counter++;
}

function typingOnLoad(card) {
	
	var cardNum = $(card).attr('id').replace(/card-caption/, '');
	console.log(cardNum);
	var obj = eval("cardCopy" + cardNum);
	var caption = obj[0];
	var captionLength = 0;
	var correspondingButton = "#refresh" + cardNum;
	
	var typingLoad = setInterval(function() {
	     	if (caption.length >= captionLength) {
     		$(card).html(caption.substr(0, captionLength++));
     	} else {
     		clearTimeout(typingLoad);
     		$(correspondingButton).prop('disabled', false);
     	}
     }, 50);
}
