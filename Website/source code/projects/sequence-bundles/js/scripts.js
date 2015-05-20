//REMOVING GREY HIGHLIGHTS IN MOBILE
document.addEventListener("touchstart", function(){}, true);






//EXPANDER-1
$(document).ready(function() {
  var expander1Trigger = document.getElementById("js-expander1-trigger");
  var expander1Content = document.getElementById("js-expander1-content");

  $('#js-expander1-trigger').click(function(){
    $(this).toggleClass("expander1-hidden");
  });
});

//EXPANDER-2
$(document).ready(function() {
  var expander2Trigger = document.getElementById("js-expander2-trigger");
  var expander2Content = document.getElementById("js-expander2-content");

  $('#js-expander2-trigger').click(function(){
    $(this).toggleClass("expander2-hidden");
  });
});

//EXPANDER-3
$(document).ready(function() {
  var expander3Trigger = document.getElementById("js-expander3-trigger");
  var expander3Content = document.getElementById("js-expander3-content");

  $('#js-expander3-trigger').click(function(){
    $(this).toggleClass("expander3-hidden");
  });
});






//STEP-1
$(document).ready(function() {
  var step1Trigger = document.getElementById("js-step1-trigger");
  var step1Content = document.getElementById("js-step1-content");

  $('#js-step1-trigger').click(function(){
    $(this).toggleClass("step1-hidden");
  });
});

//STEP-2
$(document).ready(function() {
  var step2Trigger = document.getElementById("js-step2-trigger");
  var step2Content = document.getElementById("js-step2-content");

  $('#js-step2-trigger').click(function(){
    $(this).toggleClass("step2-hidden");
  });
});

//STEP-3
$(document).ready(function() {
  var step3Trigger = document.getElementById("js-step3-trigger");
  var step3Content = document.getElementById("js-step3-content");

  $('#js-step3-trigger').click(function(){
    $(this).toggleClass("step3-hidden");
  });
});






//MODAL
$(function() {
  $("#modal-1").on("change", function() {
    if ($(this).is(":checked")) {
      $("body").addClass("modal-open");
    } else {
      $("body").removeClass("modal-open");
    }
  });

  $(".modal-fade-screen, .modal-close").on("click", function() {
    $(".modal-state:checked").prop("checked", false).change();
  });

  $(".modal-inner").on("click", function(e) {
    e.stopPropagation();
  });
});






//TABS
$(document).ready(function () {
  $('.accordion-tabs').each(function() {
    $(this).children('li').first().children('a').addClass('is-active').next().addClass('is-open').show();
  });
  $('.accordion-tabs').on('click', 'li > a.tab-link', function(event) {
    if (!$(this).hasClass('is-active')) {
      event.preventDefault();
      var accordionTabs = $(this).closest('.accordion-tabs');
      accordionTabs.find('.is-open').removeClass('is-open').hide();

      $(this).next().toggleClass('is-open').toggle();
      accordionTabs.find('.is-active').removeClass('is-active');
      $(this).addClass('is-active');
    } else {
      event.preventDefault();
    }
  });
});




