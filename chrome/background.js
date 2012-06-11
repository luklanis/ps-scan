

chrome.browserAction.setBadgeBackgroundColor({color:[0, 200, 0, 100]});

chrome.browserAction.onClicked.addListener(function(tab) {
    var ipAddress = localStorage.ipAddress;

    if (ipAddress && !this.connecting) {
        this.connecting = true;
        tcpSocket.connect(ipAddress, 8765, connected, onReadSocket);
    }
});

function connected() {
    chrome.browserAction.setBadgeText({text:"connected"});
}

function arrayBuffer2String(buf, callback) {
    var BlobBuilder = window.BlobBuilder || window.WebKitBlobBuilder;
    var bb = new BlobBuilder();
    bb.append(buf);
    
    var f = new FileReader();
    f.onload = function(e) {
        callback(e.target.result);
    };

    f.readAsText(bb.getBlob());
}

function onReadSocket(readInfo) {
    arrayBuffer2String(readInfo.data, function(text) {
        chrome.tabs.getSelected(null, function(tab) {
            chrome.tabs.sendRequest(tab.id, {coderow: text}, function(response) {
                console.log(response);
            });
        });
/*        var message = document.createElement("p");

        message.innerHTML = text;
        document.body.appendChild(message);
        */
    });
}

