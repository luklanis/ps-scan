
var socketCommunication = {

    connect: function(address, port, onRead) {
        this.onRead = onRead;
        this.address = address;
        this.port = port;

        var self = this;

        console.log("creating socket...");

        chrome.experimental.socket.create('tcp', {}, function(socketInfo) {
            
            console.log(socketInfo);

            if (!socketInfo.socketId) {
                console.log("couldn't create socket");
                return;
            }

            self.socketId = socketInfo.socketId;

            self._connectSocket();
        });
    },

    _connectSocket: function() {
        console.log("connecting...");
                
        chrome.experimental.socket.connect(this.socketId, 
                this.address, this.port, this._onConnectSocket);
    },

    _onConnectSocket: function(result) {
        var self = socketCommunication;

        if (!result) {
            console.log("couldn't connect");

            setTimeout(function() {
                self._connectSocket();
            }, 5000);

            // chrome.experimental.socket.destroy(socketId);
            return;
        }

        chrome.experimental.socket.read(self.socketId, self._onRead);
    },

    _onRead: function(readInfo) {
        var self = socketCommunication;
        console.log(readInfo);

        if(readInfo.resultCode < 0) {
            // message.innerHTML = "Error occoured: " + readInfo.resultCode;
            setTimeout(function() { self._connectSocket(); }, 10000);
        } else if (self.onRead) {
            self.onRead(readInfo);

            chrome.experimental.socket.read(self.socketId, self._onRead);
        }

    }
};

socketCommunication.connect("192.168.1.25", 8765, onReadSocket);

function arrayBuffer2String(buf, callback) {
    var bb = new (window.BlobBuilder || window.WebKitBlobBuilder)();
    bb.append(buf);
    
    var f = new FileReader();
    f.onload = function(e) {
        callback(e.target.result);
    };

    f.readAsText(bb.getBlob());
}

function onReadSocket(readInfo) {
    arrayBuffer2String(readInfo.data, function(text) {
        var message = document.createElement("p");

        message.innerHTML = text;
        document.body.appendChild(message);
    });
}

/*

var req = new XMLHttpRequest();
req.open(
    "GET",
    "http://api.flickr.com/services/rest/?" +
        "method=flickr.photos.search&" +
        "api_key=90485e931f687a9b9c2a66bf58a3861a&" +
        "text=hello%20world&" +
        "safe_search=1&" +  // 1 is "safe"
        "content_type=1&" +  // 1 is "photos only"
        "sort=relevance&" +  // another good one is "interestingness-desc"
        "per_page=20",
    true);
req.onload = showPhotos;
req.send(null);

function showPhotos() {
  var photos = req.responseXML.getElementsByTagName("photo");

  for (var i = 0, photo; photo = photos[i]; i++) {
    var img = document.createElement("image");
    img.src = constructImageURL(photo);
    document.body.appendChild(img);
  }
}

// See: http://www.flickr.com/services/api/misc.urls.html
function constructImageURL(photo) {
  return "http://farm" + photo.getAttribute("farm") +
      ".static.flickr.com/" + photo.getAttribute("server") +
      "/" + photo.getAttribute("id") +
      "_" + photo.getAttribute("secret") +
      "_s.jpg";
}

*/
