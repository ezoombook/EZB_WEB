    function onPartChange(e){
        var selectedPart = $(this).val();
        var parent = $(this).parents('.assignment-list');
        if(selectedPart == "all-parts"){
            parent.find('#has_all_parts').val("true");
            parent.siblings().find('.add-assignment-btn').addClass('hide');
            parent.find('select').not('#assigned_parts_0_').remove();
        }else{
            parent.find('#has_all_parts').val("false");
            parent.siblings().find('.add-assignment-btn').removeClass('hide');
        }
    }

    function onAddAssignment(assListId){
        return function addAssignmentList(e){
            var assignmentsDiv = $(assListId); //$('#assigned_parts');
            var partsCount = assignmentsDiv.find('.assigment').length + 1;
            var newElem = $('#assigned_parts_0_').clone()
                .attr('id', "assigned_parts_"+partsCount+"_")
                .attr('name', "assigned_parts["+partsCount+"]");

            newElem.find('[value="all-parts"]').remove();

            assignmentsDiv.append(newElem);
        }
    }

    function updateRemoveForm(e){
        var memberId = $(this).attr('data-memberid');
        var removeForm = $('#remove_member_form');
        var formAction = removeForm.attr('action');
        removeForm.attr('action', formAction+memberId);
    }

    $(document).ready(function(){

        $('.changeAssignmentBtn').click(function (){
          var memberId = $(this).attr('data-memberid');
          var newLayer = $(this).attr('data-layer');
          $('#modifMemberForm #user_id').val(memberId);
          $('#modifMemberForm #assigned_layer').val(newLayer);
        });

        $('.showContribDetail').click(function(){
            var layerId = $(this).attr('data-layer');
            var contribId = $(this).attr('data-contrib');
            $.ajax({
                url:"/collab/contrib/"+layerId+"/"+contribId,
                type:"GET",
                dataType:"json"
            }).done(function(msg){
                $('#contribDetailModal .modal-body p.contrib-content')
                    .replaceWith('<p class="contrib-content">'+msg.contrib_content+'</p>');
                $('#contribDetailModal .modal-body span.contrib-type')
                    .replaceWith('<span class="contrib-type">'+msg.contrib_type+'<span>');
            });
        });

        $('#add-assignment-btn').click(onAddAssignment('#assigned_parts'));

        $('.part-list.first_list').change(onPartChange);

        $('#add-assignment-btn-modif').click(onAddAssignment('#assigned_parts-modif'));

        $('.remove_member_btn').click(updateRemoveForm);

    });

    function overlay() {
        el = document.getElementById("overlay");
        el.style.visibility = (el.style.visibility == "visible") ? "hidden" : "visible";
    }
