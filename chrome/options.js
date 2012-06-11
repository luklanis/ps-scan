
$(document).ready(function() {
    if (localStorage.ipAddress) {
        $('#ipAddress')[0].value = localStorage.ipAddress;
    }

    $('#saveButton').bind('click', save);
});

function save() {
    var ipAddress = $("#ipAddress")[0].value;

    if (ipAddress) {
        localStorage.ipAddress = ipAddress;
    }
}

