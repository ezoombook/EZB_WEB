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
    var id2 = (_.isEmpty(name2) ? "" : "_"+name2);
    var n2 = (_.isEmpty(name2) ? "" : "."+name2 );
    var newField = $("<" + ftype + ">")
                .attr("id", name+"_"+i+"_"+id2)
                .attr("name", name+"["+i+"]"+n2)
                .attr("class", fclass);

    $("#"+set).append(newField);
}

function contribField(ftype, fclass, nav_names, nav_index){
    return $("<"+ ftype +">")
        .attr("id", genName(nav_names, nav_index, "_", "__"))
        .attr("name", genName(nav_names, nav_index, "[", "]."))
        .attr("class", fclass);
}

function genName(names, indexes, left, right){
    var result = "";
    var i = 0;
    for(var n in names){
        result += names[n] + (i >= indexes.length ? "" : left + indexes[i] + right);
        i++;
    }
    return result;
}

function addContrib(ctype){
    var i = $(".contrib").length;
    var div = $("<div>").attr("class", "contrib");
    var sum_nav = ["ezoomlayer_contribs","contrib_content"];
    var part_sum_nav = ["ezoomlayer_contribs","part_contribs","contrib_content"];
    var part_titl_nav = ["ezoomlayer_contribs","part_title"];

    var contrib = (function(){if (ctype == "summary"){
            return contribField("textarea","contrib_summary", sum_nav, [i]);
        }else{
            var fieldset = $("<fieldset>")
                .attr("id", "part_fieldset_"+i)
                .attr("class", "part_field");

            fieldset.append(
                contribField("input","part_title",part_titl_nav,[i])
                    .attr("type", "text")
                    .attr("value", "Type a title...")
                    .bind({
                        click: function(){$("#btnAddQuote_"+i).css("visibility","visible")},
                        focus: function(){$(this).attr("value","")}
                    })
            );

            fieldset.append($("<input>")
                .attr("type","button")
                .attr("class","addquote")
                .attr("id","btnAddQuote_"+i)
                .attr("value","+")
                .click(function(){
                    var j = $("#part_fieldset_"+i).children(".quote").length;
                    $("#part_fieldset_"+i).append(contribField("textarea","quote",part_sum_nav,[i,j]));
                })
            )
            return fieldset;
        }
    })();
    div.append(contrib)
    $("#contribs_set").append(div);
}
