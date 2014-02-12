    String.prototype.format = (function()
    {
        var replacer = function(context)
        {
            return function(s, name)
            {
                return context[name];
            };
        };

        return function(context)
        {
            return this.replace(/\{(\w+)\}/g, replacer(context));
        };
    })();

    function onDeleteContrib(e){
        var divId = $(this).attr("data-contribid");
        $('#'+divId).remove();
    }

    function onPartIdChange(e){
        var selectedPart = $(this).find("option[value='"+$(this).val()+"']").text();
        var partTitleId = $(this).attr('data-titlefield');
        $('#'+partTitleId).val(selectedPart);
        //Add "go to book" button
        var gotobutton = $(this).parents(".part_div").find(".readBtn:first");
        gotobutton.attr("href","/bookread/"+gotobutton.attr("data-bookId")+"/"+$(this).val());
        gotobutton.removeClass("hide");

    }

    function askLeaveConfirmation(){
        if($('#ezl_form').prop('changed')){
            return 'Are you sure you want to leave without saving?';
        }
    }

    function onDeleteHoverIn(){$(this).css("opacity", "1");}
    function onDeleteHoverOut(){$(this).css("opacity", "0.2");}

    function addAtomicContrib(parent, parentId, contribType){

        var numContrs = parent.find(".contrib_div").length;
        var newId = "contrib_div_"+parentId+"_"+numContrs+"_";

        var contribDiv = contribType == "contrib.Quote" ?
                            $('#contrib_div___quote_999_').clone().attr("id",newId) :
                            $('#contrib_div___summary_999_').clone().attr("id",newId);

        var fmt = {
            parentId: parentId,
            numContrs: numContrs
        }

        var contribTypeFieldId = "ezoomlayer_contribs_{parentId}__part_contribs_{numContrs}__contrib_contrib_type".format(fmt);
        var contribTypeFieldName = "ezoomlayer_contribs[{parentId}].part_contribs[{numContrs}].contrib.contrib_type".format(fmt);
        var contribContentFieldId = "ezoomlayer_contribs_{parentId}__part_contribs_{numContrs}__contrib_contrib_content".format(fmt);
        var contribContentFieldName = "ezoomlayer_contribs[{parentId}].part_contribs[{numContrs}].contrib.contrib_content".format(fmt);
        var contribIndexFieldId = "ezoomlayer_contribs_{parentId}__part_contribs_{numContrs}__contrib_index".format(fmt);
        var contribIndexFieldName = "ezoomlayer_contribs[{parentId}].part_contribs[{numContrs}].contrib_index".format(fmt);

        contribDiv.find('[data-field-key="contrib_type"]').first()
            .attr("id", contribTypeFieldId)
            .attr("name", contribTypeFieldName)
            .attr("value", contribType);

        contribDiv.find('[data-field-key="contrib_content"]').first()
            .attr("id", contribContentFieldId)
            .attr("name", contribContentFieldName);

        contribDiv.find('[data-field-key="contrib_index"]').first()
            .attr("id", contribIndexFieldId)
            .attr("name", contribIndexFieldName)
            .attr("value", numContrs);

        contribDiv.find(".deletecontrib").attr("data-contribid",newId)
            .click(onDeleteContrib);

        contribDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        contribDiv.find(".move-up")
            .attr("data-move-target",newId)
            .click(onMoveUp);

        contribDiv.removeClass("hide");

        parent.append(contribDiv);
    }

    function onAddQuote(e){
        var divId = $(this).attr("data-part-div-id");
        var parentDiv = $('#part_field_'+divId).find(".part-contribs");
        if(parentDiv){
            addAtomicContrib(parentDiv, divId, "contrib.Quote");
        }
    }

    function onAddPartSummary(e){
        var divId = $(this).attr("data-part-div-id");
        var parentDiv = $('#part_field_'+divId).find(".part-contribs");
        if(parentDiv){
            addAtomicContrib(parentDiv, divId, "contrib.Summary");
        }
    }

    function onAddPart(e){
        var partsCount = $("#contribs_set").find(".part_field").length;
        var sumCount = $("#contribs_set").find(".summary_div").length;
        var contribTotal = partsCount+sumCount;

        var newId = "part_field_"+(partsCount);
        var newPartField = $("#part_field_999").clone().attr("id", newId);

        var fmt = {
            contribTotal: partsCount+sumCount
        }

        var contribPartFieldId = "ezoomlayer_contribs_{contribTotal}__part_id".format(fmt);
        var contribPartFieldName = "ezoomlayer_contribs[{contribTotal}].part_id".format(fmt);
        var contribTitleFieldId = "ezoomlayer_contribs_{contribTotal}__part_title".format(fmt);
        var contribTitleFieldName = "ezoomlayer_contribs[{contribTotal}].part_title".format(fmt);
        var contribTypeFieldId = "ezoomlayer_contribs_{contribTotal}__contrib_type".format(fmt);
        var contribTypeFieldName = "ezoomlayer_contribs[{contribTotal}].contrib_type".format(fmt);
        var contribIndexFieldId = "ezoomlayer_contribs[{contribTotal}].contrib_index".format(fmt);
        var contribIndexFieldName = "ezoomlayer_contribs[{contribTotal}].contrib_index".format(fmt);

        newPartField.find(".part_id:first")
             .attr("id", contribPartFieldId)
             .attr("name", contribPartFieldName);

        newPartField.find(".part_title:first")
            .attr("id", contribTitleFieldId)
            .attr("name", contribTitleFieldName);

        newPartField.find("#ezoomlayer_contribs_contrib_type:first")
            .attr("id", contribTypeFieldId)
            .attr("name", contribTypeFieldName)
            .attr("value","contrib.Part");

        newPartField.find('[data-field-key="contrib_index"]').first()
            .attr("id", contribIndexFieldId)
            .attr("name", contribIndexFieldName)
            .val(contribTotal);

        //Change buttons target
        newPartField.find(".btnAddQuote").attr("data-part-div-id", partsCount)
            .click(onAddQuote);
        newPartField.find(".btnAddPartSummary").attr("data-part-div-id", partsCount)
            .click(onAddPartSummary);
        newPartField.find(".deletecontrib").attr("data-contribid", newId)
            .click(onDeleteContrib);
        newPartField.find(".part_id")
            .attr("data-titlefield",contribTitleFieldId)
            .change(onPartIdChange);

        newPartField.find(".move-up")
            .attr("data-move-target",newId)
            .click(onMoveUp);

        newPartField.removeClass("hide");

        $("#contribs_set").append(newPartField);
    }

    function addSummary(){
        var partsCount = $("#contribs_set > .part_field" ).length;
        var sumCount = $("#contribs_set > .summary_div" ).length;
        var newId = "summary_div_"+sumCount;
        //var contribTotal = partsCount+sumCount;

        var newSummaryDiv = $("#summary_div___999_").clone().attr("id", newId);

        var fmt = {
            contribTotal : partsCount+sumCount
        }

        var contribTypeFieldId = "ezoomlayer_contribs_{contribTotal}__contrib_type".format(fmt);
        var contribTypeFieldName = "ezoomlayer_contribs[{contribTotal}].contrib_type".format(fmt);
        var contribContentId = "ezoomlayer_contribs_{contribTotal}__contrib_content".format(fmt);
        var contribContentName = "ezoomlayer_contribs[{contribTotal}].contrib_content".format(fmt);

        newSummaryDiv.find("#ezoomlayer_contribs_contrib_type:first")
            .attr("id", contribTypeFieldId)
            .attr("name", contribTypeFieldName)
            .attr("value", "contrib.Summary");

        newSummaryDiv.find("#ezoomlayer_contribs_contrib_content:first")
            .attr("id", contribContentId)
            .attr("name", contribContentName);

        newSummaryDiv.find(".deletecontrib").attr("data-contribid",newId)
            .click(onDeleteContrib);

        newSummaryDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        newSummaryDiv.removeClass("hide");
        newSummaryDiv.addClass("hl_contrib");

        $("#contribs_set").append(newSummaryDiv);
    }

    function addEzbSummary(e){
        var sumSet = $("#summaries_set");
        //var numSums = $(".ezb_summary").length - 1;

        var fmt = {numSums: $(".ezb_summary").length - 1};

        var sumDiv = $("#ezb_summary_999").clone().attr("id", "ezb_summary_"+numSums);

        var summaryFieldId = "ezoomlayer_summaries_{numSums}_".format(fmt);
        var summaryFieldName = "ezoomlayer_summaries[{numSums}]".format(fmt);

        sumDiv.find(".ezb_summary")
            .attr("id",summaryFieldId)
            .attr("name",summaryFieldName);

        sumDiv.find(".deletecontrib").attr("data-contribid","ezb_summary_"+numSums)
            .click(onDeleteContrib);

        sumDiv.find(".delete-icon").hover(onDeleteHoverIn, onDeleteHoverOut);

        sumDiv.removeClass("hide");

        sumSet.append(sumDiv);
    }

    function sortContribs(contribParent){
        var contribs = contribParent.children('.contrib_div');
        var i = 0;
        contribs.each(function(){
            $(this).find('[data-field-key="contrib_index"]').first().val(i);
            i=i+1;
        });
    }

    function handleSort(event, ui){
        var parent = ui.item.parents(".sortable").first();
        sortContribs(parent);
    }

    function onMoveUp(e){
        var divId = $(this).attr("data-move-target");
        var div = $("#"+divId);
        var prev = div.prev();
        if(div.size() > 0 && prev.size() > 0){
console.log("swapping " + div.attr("id") + " and " + prev.attr("id"));
            prev.before(div);
        }
        sortContribs(div.parents(".sortable").first());
    }

    $(document).ready(function(){
        window.changed = false;

        $('#btnAddezbSummary').click(addEzbSummary);

        $('#btnAddPart').click(onAddPart);

        $('.btnAddQuote').click(onAddQuote);

        $('.btnAddPartSummary').click(onAddPartSummary);

        $('#btnAddSummary').click(addSummary);

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

        window.onbeforeunload = askLeaveConfirmation;

        $("#saveChangesBtn").click(function(){
            window.onbeforeunload = null;
        });

        $('#ezl_form > *').on("change", function(){
            $('#ezl_form').prop('changed',true);
        });

        $(".sortable").sortable({
            cancel:".title_box, .part_actions",
            handle:".drag-handle",
            stop: handleSort
        });

        $(".move-up").click(onMoveUp);
   });
