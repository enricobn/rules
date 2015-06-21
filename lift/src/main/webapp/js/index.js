function pack() {
    //console.log('pack');
    var margin = 0;
    var height = $(this).height() - $('#header').outerHeight() - $('#footer').outerHeight() - 2 * margin;
    var width = $(this).width() - $('#nav').outerWidth();
    var left = $('#nav').outerWidth() + margin;
    var top = $('#header').outerHeight() + margin;
    //console.log($('#nav').outerWidth(true));
    console.log('height=' + height + ', left=' + left + ', top=' + top);
    $('#nav').height(height);
    $('#content').height(height);
    $('#content').width(width);
    $('#content').css({'top': top + 'px'});
    $('#content').css({'left': left + 'px'});
    $('#nav').css({'top': top + 'px'});
    //console.log($('#nav'));
}

$(document).ready(function () {
    $(window).resize(pack);
    pack();
});
