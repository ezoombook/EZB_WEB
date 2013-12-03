    $(document).ready(function(){
        function zoomOut(e){
            var quote = $(this).text();
            var quoteId = $(this).attr('data-origin');

            $("body p").unhighlight({ element: 'a', className: 'zoom-out' });
            $('#level-tabs li:eq(1) a').tab('show');
            $('#'+quoteId).attr('tabindex',-1).focus();
        }

        $('#showall').click(function(){
            $('.summary').collapse('show');
            $('.quote').collapse('show');
        });
        $('#hidequotes').click(function(){
            $('.quote').collapse('hide');
            $('.summary').collapse('show');
        });
        $('#hidesummaries').click(function(){
            $('.summary').collapse('hide');
            $('.quote').collapse('show');
        });
        $('.quote-text').click(function(e){
            var selected = $(this).children('i').text().replace(/\[[^]\]|\«|\»/g,'').split('\n');
            var quoteId = $(this).attr('id');

            for (var i = 0; i < selected.length; i++) {
                selected[i] = selected[i].trim();
            }

            $('#level-tabs a:first').tab('show');
            $('body p').highlight(selected, {element: 'a', className: 'zoom-out'});
            $('a.zoom-out').attr('data-origin',quoteId);
            $('a.zoom-out').attr('tabindex',-1).focus();
            $('a.zoom-out').click(zoomOut);
        });
        $('.comment-btn').click(function(){
            console.log('coucou!');
            var contribId = $(this).attr('data-contrib-id');
            var currDate = new Date();
            console.log('today: ' + currDate.toString());
            $('#commentContrib').val(contribId);
            $('#contribAuthor').val($(this).attr('data-contrib-auth'));
            $('#contribType').val($(this).attr('data-contrib-type'));
            $('#commentLayer').val($(this).attr('data-contrib-layer'));
            //$('#commentPart').val();
        });
    });
