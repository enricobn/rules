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
function editChanges(viewId) {
    var view = $.liftViews[viewId];
    var result = new Object();
    result.changed = new Object();
    result.deleted = view.deleted;
    for (var id in view.changed) {
        result.changed[id] = view.cache[id];
    }
    return result;
}

/*
initializes the view
*/
function editInit(viewId, schema, onChange) {
    if (typeof $.liftViews == 'undefined') {
        $.liftViews = new Object();
    }

    if (typeof $.jsonEditor != 'undefined') {
      $.jsonEditor.destroy();
    }

    JSONEditor.defaults.options.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'bootstrap3';
    JSONEditor.defaults.options.disable_edit_json = true;
    JSONEditor.defaults.options.disable_properties = true;
    JSONEditor.defaults.options.disable_collapse = true;

    $("#detail-editor").empty();

    $("#detail-editor").hide();

    $.jsonEditor = new JSONEditor(document.getElementById("detail-editor"), schema);

    // to hide the title of the editor
    $( "span:contains('hide-me')" ).parent().hide();

    var view = new Object();
    $.liftViews[viewId] = view;

    view.cache = new Object();
    view.changed = new Object();
    view.deleted = new Array();
    view.activeId = undefined;
    view.editingActive = false;

    view.changeListener = function() {
     if (view.editingActive && $.jsonEditor.isEnabled()) {
       if (typeof view.activeId != 'undefined') {
         var newValue = $.jsonEditor.getValue();
         onChange(view.cache[view.activeId], newValue);
         view.cache[view.activeId] = newValue;
         view.changed[view.activeId] = view.activeId;
         console.log("jsonEditor.changed");
       }
     }
    };

    view.updateEditor = function(v) {
        console.log('updating editor');
        view.cache[v.id] = v;
        $.jsonEditor.setValue(v);
        view.activeId = v.id;
        $("#detail-editor").show();
        window.requestAnimationFrame(function() {
          $.jsonEditor.enable();
          $.jsonEditor.on('change', view.changeListener);
          view.editingActive = true;
          /* to enable bootstrap's tooltip style in json editor, but it does not work! */
          $('[data-toggle="tooltip"]').tooltip();
        });
    };
/*
    view.onError = function() {
        console.log("error");
        // TODO do it better
        alert("Error");
    };
*/
    view.contentListener = new Object();
    view.contentListener.beforeContentChange = function() {
        if (Object.keys(view.changed).length > 0 || view.deleted.length > 0) {
            return confirm("Do you want to loose changes?");
        } else {
           return true;
        }
    };
   addContentListener(view.contentListener);
}

function editAfterSave(viewId) {
    var view = $.liftViews[viewId];
    view.changed = new Object();
    view.deleted = new Array();
}

$(document).ready(function () {
    $.contentListeners = new Array();
    $('[data-toggle="tooltip"]').tooltip();
    $(window).resize(pack);
    pack();
});
