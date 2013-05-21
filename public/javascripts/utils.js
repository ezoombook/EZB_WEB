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
    var par_nav = ["part_contribs",["contrib_type","contrib_content"]];
    var ezl_nav = ["ezoomlayer_contribs",["contrib_content","part_title","contrib_type",par_nav]];

    var sum_nav = [ezl_nav[0],ezl_nav[1][0]];
    var part_sum_nav = [ezl_nav[0],par_nav[0],par_nav[1][1]];
    var part_titl_nav = [ezl_nav[0],ezl_nav[1][1]];

    function contribDiv(){ return $("<div>").attr("class", "contrib"); }

    var contrib = (function(){if (ctype == "summary"){
            var div = contribDiv();
            div.append(contribField("textarea","contrib_summary", sum_nav, [i]));
            div.append(contribField("input","", [ezl_nav[0],ezl_nav[1][2]], [i])
                        .attr("type", "hidden")
                        .attr("value","contrib.Summary"));
            return div;
        }else{
            var div = contribDiv();
            var fieldset = $("<fieldset>")
                .attr("id", "part_fieldset_"+i)
                .attr("class", "part_field");

            fieldset.append(contribField("input","",[ezl_nav[0],par_nav[1][0]],[i])
                        .attr("type","hidden")
                        .attr("value","contrib.Part"));

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
                    $("#part_fieldset_"+i).append(contribField("input","quote",[ezl_nav[0],par_nav[0],par_nav[1][0]])
                                                    .attr("type","hidden"))
                })
            )
            div.append(fieldset);
            return div;
        }
    })();
    $("#contribs_set").append(contrib);
}
