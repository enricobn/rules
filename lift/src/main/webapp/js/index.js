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

function addContentListener(listener) {
    $.contentListeners.push(listener);
}

function removeContentListener(listener) {
    $.contentListeners = $.contentListeners.filter(
        function(item) {
            if (item !== listener) {
                return item;
            }
        }
    );
}

/* returns true if content can be changed */
function fireBeforeContentChange() {
    var result = true;
    $.contentListeners.forEach(function(item) {
        result &= item.beforeContentChange();
    });
    /* TODO I think it's ugly, but for now it works */
    /* I remove the listeners */
    if (result) {
        $.contentListeners = new Array();
    }
    return result;
}

/*
.deleted : Array of ids (string)
.changed : Object key = id, value =
*/
function editChanges(container) {
    var result = new Object();
    result.changed = new Object();
    result.deleted = container.deleted;
    for (var id in container.changed) {
        result.changed[id] = container.cache[id];
    }
    return result;
}

/*
init the container for editing properties
*/
function editInit(container) {
    container.cache = new Object();
    container.changed = new Object();
    container.deleted = new Array();
    container.activeId = undefined;
    container.editingActive = false;
    container.changeListener = function() {
     if (container.editingActive && $.jsonEditor.isEnabled()) {
       if (typeof container.activeId != 'undefined') {
         container.cache[container.activeId] = $.jsonEditor.getValue();
         container.changed[container.activeId] = container.activeId;
         console.log("jsonEditor.changed");
       }
     }
   };
}

function editAfterSave(container) {
    container.changed = new Object();
    container.deleted = new Array();
}

$(document).ready(function () {
    $.contentListeners = new Array();
    $('[data-toggle="tooltip"]').tooltip();
    $(window).resize(pack);
    pack();
});
