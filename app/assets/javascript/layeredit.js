    function onDeleteContrib(e){
        var divId = $(this).attr("data-contribid");
        $('#'+divId).remove();
    }

    function onPartIdChange(e){
        var selectedPart = $(this).find("option[value='"+$(this).val()+"']").text();
        var partTitleId = $(this).attr('data-titlefield');
        $('#'+partTitleId).val(selectedPart);
    }

    function onDeleteHoverIn(){$(this).css("opacity", "1");}
    function onDeleteHoverOut(){$(this).css("opacity", "0.2");}

    function addPart(){
        var partsCount = $("#contribs_set > .part_field" ).length;
        var sumCount = $("#contribs_set > .summary_div" ).length;
        var contribTotal = partsCount+sumCount;

        var newId = "part_field_"+(partsCount);
        var newPartField = $("#part_field_999").clone().attr("id", newId);

        newPartField.find(".part_id:first")
             .attr("id", "ezoomlayer_contribs_"+contribTotal+"__part_id")
             .attr("name", "ezoomlayer_contribs["+contribTotal+"].part_id");

        newPartField.find(".part_title:first")
            .attr("id", "ezoomlayer_contribs_"+contribTotal+"__part_title")
            .attr("name", "ezoomlayer_contribs["+contribTotal+"].part_title");

        newPartField.find("#ezoomlayer_contribs_contrib_type:first")
            .attr("id", "ezoomlayer_contribs_"+contribTotal+"__contrib_type")
            .attr("name", "ezoomlayer_contribs["+contribTotal+"].contrib_type")
            .attr("value","contrib.Part");

        //Change buttons' id
        newPartField.find("#btnAddQuote_999").attr("id","btnAddQuote_"+contribTotal)
            .click(function(e){
                var divId = e.target.id.split("_")[1];
                addAtomicContrib(divId, "contrib.Quote");
            });
        newPartField.find("#btnAddSummary_999").attr("id","btnAddSummary_"+contribTotal)
            .click(function(e){
                var divId = e.target.id.split("_")[1];
                addAtomicContrib(divId, "contrib.Summary");
            });
        newPartField.find(".deletecontrib").attr("data-contribid",newId)
            .click(onDeleteContrib);
        newPartField.find(".part_id")
            .attr("data-titlefield","ezoomlayer_contribs_"+contribTotal+"__part_title")
            .change(onPartIdChange);

        newPartField.removeClass("hide");

        $("#contribs_set").append(newPartField);
    }
/*
    function addSummary(){
        var partsCount = $("#contribs_set > .part_field" ).length;
        var sumCount = $("#contribs_set > .summary_div" ).length;
        var newId = "summary_div_"+sumCount;
        var contribTotal = partsCount+sumCount;

        var newSummaryDiv = $("#summary_div___999_").clone().attr("id", newId);

        newSummaryDiv.find("#ezoomlayer_contribs_contrib_type:first")
            .attr("id", "ezoomlayer_contribs_"+contribTotal+"__contrib_type")
            .attr("name", "ezoomlayer_contribs["+contribTotal+"].contrib_type")
            .attr("value", "contrib.Summary");

        newSummaryDiv.find("#ezoomlayer_contribs_contrib_content:first")
            .attr("id", "ezoomlayer_contribs_"+contribTotal+"__contrib_content")
            .attr("name", "ezoomlayer_contribs["+contribTotal+"].contrib_content");

        newSummaryDiv.find(".deletecontrib").attr("data-contribid",newId)
            .click(onDeleteContrib);

        newSummaryDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        newSummaryDiv.removeClass("hide");
        newSummaryDiv.addClass("hl_contrib");

        $("#contribs_set").append(newSummaryDiv);
    }
*/
    function addAtomicContrib(parentId, contribType){

        var parent = $("#part_field_"+parentId);

        var numContrs = parent.children(".quote_div").length +
                        parent.children(".summary_div").length;

        var newId =  contribType == "contrib.Quote" ?
                        "quote_div_" +parentId+"_"+numContrs+"_" :
                        "summary_div_" +parentId+"_"+numContrs+"_"
        var contribDiv = contribType == "contrib.Quote" ?
                            $("#quote_div___999_").clone().attr("id",newId) :
                            $("#summary_div___999_").clone().attr("id",newId);

        contribDiv.find("#ezoomlayer_contribs_contrib_type:first")
            .attr("id", "ezoomlayer_contribs_"+parentId+"__part_contribs_"+numContrs+"__contrib_type")
            .attr("name", "ezoomlayer_contribs["+parentId+"].part_contribs["+numContrs+"].contrib_type")
            .attr("value", contribType);

        contribDiv.find("#ezoomlayer_contribs_contrib_content:first")
            .attr("id", "ezoomlayer_contribs_"+parentId+"__part_contribs_"+numContrs+"__contrib_content")
            .attr("name", "ezoomlayer_contribs["+parentId+"].part_contribs["+numContrs+"].contrib_content");

        contribDiv.find(".deletecontrib").attr("data-contribid",newId)
            .click(onDeleteContrib);

        contribDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        contribDiv.removeClass("hide");

        parent.append(contribDiv);
    }

    $(document).ready(function(){
        $('#btnAddezbSummary').click(function (e) {
            var sumSet = $("#summaries_set");
            var numSums = $(".ezb_summary").length - 1;

            var sumDiv = $("#ezb_summary_999").clone().attr("id", "ezb_summary_"+numSums);

            sumDiv.find(".ezb_summary")
                .attr("id","ezoomlayer_summaries_"+numSums+"_")
                .attr("name","ezoomlayer_summaries["+numSums+"]");

            sumDiv.find(".deletecontrib").attr("data-contribid","ezb_summary_"+numSums)
                .click(onDeleteContrib);

            sumDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

            sumDiv.removeClass("hide");

            sumSet.append(sumDiv);
        });
        $('#btnAddPart').click(function () {
            addPart();
        });
        $('.btnAddQuote').click(function (e){
            var divId = e.target.id.split("_")[1];
            if(divId){
                addAtomicContrib(divId, "contrib.Quote");
            }
        });
        $('.btnAddPartSummary').click(function (e) {
            var divId = e.target.id.split("_")[1];
            if(divId){
                addAtomicContrib(divId, "contrib.Summary");
            }
        });
        $('#btnAddSummary').click(function (e){
            addSummary();
        });

        $(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        $(".deletecontrib").click(onDeleteContrib);

        $("#ezoomlayer_locked_btn").click(function(){
            var lkdButton = $("#ezoomlayer_locked");
            //Toggle
            if (lkdButton.attr("value") == "true"){
                $("#ezoomlayer_locked").attr("value","false")
            }else{
                $("#ezoomlayer_locked").attr("value","true")
            }
        });
        $("#ezoomlayer_status_btn").click(function(){
            var lkdButton = $("#ezoomlayer_status");
            //Toggle
            if (lkdButton.attr("value") == "@Status.workInProgress"){
                $("#ezoomlayer_status").attr("value","@Status.published")
            }else{
                $("#ezoomlayer_status").attr("value","@Status.workInProgress")
            }
        });

        $(".part_id").change(onPartIdChange);
   });
