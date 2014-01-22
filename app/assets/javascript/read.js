//Requires utils.js

function setReaderHeight(){
    calcMaxHeight('#reader');
}

    $(document).ready(function(){
        MaSha.instance = new MaSha({
                            'selectable': 'reader',
                            //'select_message': 'quote-saved-msg',
                            'validate': true,
                            'marker': 'marker-bar'});

        setReaderHeight();

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
        $('.zoom-in').click(function(e){
            var selected = $(this).text().trim();
            var selectedRange = $(this).attr("data-range");
console.log("zooming " + selectedRange);

            $('#level-tabs a:first').tab('show');

            if(selectedRange && MaSha.instance){
                var quoteRange = MaSha.instance.deserializeRange(selectedRange);
console.log("quote: " + quoteRange);
                MaSha.instance.addSelection(quoteRange);
                var elem = $(".user_selection_true").get(0);
                scrollTo('#reader', elem);
            }


//            var selected = $(this).text().replace(/\[[^]\]|\«|\»/g,'').split('\n');
//            var quoteId = $(this).attr('id');
//
//            for (var i = 0; i < selected.length; i++) {
//                selected[i] = selected[i].trim();
//            }
//
//            $('#level-tabs a:first').tab('show');
//            $('body p').highlight(selected, {element: 'a', className: 'zoom-out'});
//            $('a.zoom-out').attr('data-origin',quoteId);
//            $('a.zoom-out').attr('tabindex',-1).focus();
//            $('a.zoom-out').click(zoomIn);
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

        $(window).resize(setReaderHeight);

    });
