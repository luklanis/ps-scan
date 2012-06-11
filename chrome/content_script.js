chrome.extension.onRequest.addListener(function(request, sender, sendResponse) {
    if (request.coderow) {
        console.log(request.coderow);
        document.activeElement.value = request.coderow;
        sendResponse({result: "set"});
    }
});
