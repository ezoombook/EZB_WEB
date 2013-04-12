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