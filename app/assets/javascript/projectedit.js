    $(document).ready(function(){

         $('#btnAddezbsection').click(function () {
         createField("projectsection", "textarea", "project_section", "", "section_set")
        });
        $('#btnAddezbsummary').click(function () {
         createField("projectsummary", "textarea", "project_summary", "", "section_set")
        });

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
    });

    function overlay() {
        el = document.getElementById("overlay");
        el.style.visibility = (el.style.visibility == "visible") ? "hidden" : "visible";
    }
