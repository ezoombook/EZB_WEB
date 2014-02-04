function calcMaxHeight(elemId){
    var reader = $(elemId);
    var parent = reader.parent();

    var parRect = parent.get(0).getBoundingClientRect();
    var maxHeight = $(window).innerHeight() - parRect.top;

    reader.css('height', maxHeight);
}

function scrollToPart(elemId, part){
    var reader = $(elemId);
    var partDiv = $('#'+part).get(0);
    if(partDiv){
        var topPos = partDiv.offsetTop;
        reader.get(0).scrollTop = topPos;
    }
}

function scrollTo(containerId, elem){
    var container = $(containerId).get(0);
    //var elem = $(elemId).get(0);
    if(elem && container){
        var topPos = elem.offsetTop;
        container.scrollTop = topPos;
    }
}
