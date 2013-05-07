//Depends on libraries underscore-min.js and utils.js

function cloner(divName) {
    var j = $('.' + divName).length;
    var i = j - 1;

    if (i >= 0) {
        var newDiv = $('#' + divName + i).clone().attr('id', divName + j);
        newDiv.children(':first').attr('id', divName + '_' + j + '_field');
        $(newDiv).find('input').attr('id', divName + '_' + j + '_').attr('name', divName + '[' + j + ']').val('');

        $('#' + divName + i).after(newDiv);
    }
}

function createField(fclass, ftype, name, name2, set){
    var i = $("."+fclass).length;
    var id2 = (_.isEmpty(name2) ? "" : "_"+name2)
    var n2 = (_.isEmpty(name2) ? "" : "."+name2 )
    var newField = $("<" + ftype + ">")
                .attr("id", name+"_"+i+"_"+id2)
                .attr("name", name+"["+i+"]"+n2)
                .attr("class", fclass)

    $("#"+set).append(newField)
}

ezoomlayer_contribs[1].part_contribs[0].contrib_content