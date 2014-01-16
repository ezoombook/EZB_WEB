var QUOTER = function(){

    var my = {};

    my.url = "";

    //Add a function to Masha prototype for getting the last selected range
    MaSha.prototype.getLastRange = function(){
        for(var k in this.ranges) {}
        return this.ranges[k]; //this.deserializeRange(this.ranges[k]);
    };

    function saveQuote(user,bookId,ezbId,ezlId,partId){
        return (function(e){
            var selRange = MaSha.instance.getLastRange();
            if(selRange){
                var quote = MaSha.instance.deserializeRange(selRange).cloneContents().textContent;

                var quoteMsg = {"userid":user,
                    "bookid":bookId,
                    "ezbid":ezbId,
                    "layerid":ezlId,
                    "partid":partId,
                    "content":quote,
                    "range":selRange
                };

                $.ajax({
                    type:"POST",
                    url:my.url,
                    data:JSON.stringify(quoteMsg),
                    contentType:"application/json; charset=utf-8",
                    dataType: "json",
                    success: function(data){
                    console.log("Quote saved: " + data);
                    },
                    failure: function(xhr,ajaxOptions,trownErr){
                    console.log("Error: " + xhr.responseText);
                    }
                });
            }else{
                console.log('Ooops it`s an empty string');
            }

        });
    };

    my.init = function(user,bookId,ezbId,ezlId,partId,existingRange){

            MaSha.instance = new MaSha({
                'selectable': 'reader',
                //'select_message': 'quote-saved-msg',
                'validate': true,
                'onMark': saveQuote(user,bookId,ezbId,ezlId,partId),
                'marker': 'marker-bar'});

            existingRange.map(function(qr){
                if(qr.length > 0){
                    var quoteRange = MaSha.instance.deserializeRange(qr);
                    MaSha.instance.addSelection(quoteRange);
                }
            });

            console.log("QUOTED initiated!");
    };

    return my;

}();