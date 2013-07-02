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

   function contribDiv(){ return $("<div>").attr("class", "contrib"); }
     var dav = contribDiv();
     dav.append(newField);
     dav.append($("<input>")
                .attr("type","button")
                .attr("value","Delete")
                .click(function(){
             dav.remove()
            })
                
            );
     $("#"+set).append(dav);
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

    //var par_nav = ["part_contribs",["contrib_type","contrib_content"]];
    //var ezl_nav = ["ezoomlayer_contribs",["contrib_content","part_title","contrib_type",par_nav]];

    var sum_nav = ["ezoomlayer_contribs","contrib_content"];  //[ezl_nav[0],ezl_nav[1][0]];
    var ezl_contrib_type_nav = ["ezoomlayer_contribs","contrib_type"];
    var part_titl_nav = ["ezoomlayer_contribs","part_title"]; //[ezl_nav[0],ezl_nav[1][1]];
    var part_contrib_cont_nav = ["ezoomlayer_contribs","part_contribs","contrib_content"];
    var part_contrib_type_nav = ["ezoomlayer_contribs","part_contribs","contrib_type"];

//ezoomlayer_contribs_1__part_contribs_0__contrib_content

    function contribDiv(){ return $("<div>").attr("class", "contrib"); }

    var contrib = (function(){if (ctype == "summary"){ //If contrib.type is Summary
            var div = contribDiv();
            div.append(contribField("textarea","contrib_summary", sum_nav, [i]));
            div.append(contribField("input","", ezl_contrib_type_nav, [i])
                        .attr("type", "hidden")
                        .attr("value","contrib.Summary"));
             div.append($("<input>")
                .attr("type","button")
                .attr("value","Delete")
                .click(function(){
              div.remove()
            })
                
               
            );
            return div;
        }else{     //Otherwise contrib.type is Part
            var div = contribDiv();
            var fieldset = $("<fieldset>")
                .attr("id", "part_fieldset_"+i)
                .attr("class", "part_field");

            fieldset.append(contribField("input","",ezl_contrib_type_nav,[i])
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
                                
                                
                                
                                
                                
                                 var dev = contribDiv();
                    var j = $("#part_fieldset_"+i).children(".quote").length;
                    dev.append(contribField("textarea","quote",part_contrib_cont_nav,[i,j]));
                    dev.append(contribField("input","quote",part_contrib_type_nav,[i,j])
                                                    .attr("type","hidden"));
                    
              dev.append($("<input>")
                .attr("type","button")
                .attr("value","Delete")
                .click(function(){
             dev.remove();
            
            })  
            );
              $("#part_fieldset_"+i).append(dev);
                })
            )
            
              fieldset.append($("<input>")
                .attr("type","button")
                .attr("value","Delete")
                .click(function(){
               fieldset.remove()
            })  
            );
          
            
            div.append(fieldset);
            return div;
        }
    })();
    $("#contribs_set").append(contrib);
}
