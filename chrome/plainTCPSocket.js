window.tcpSocket = {

    connect: function(address, port, connected, onRead) {
        this.address = address;
        this.port = port;
        this.connected = connected;
        this.onRead = onRead;

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
        var self = tcpSocket;

        if (!result) {
            console.log("couldn't connect");

            setTimeout(function() {
                self._connectSocket();
            }, 5000);

            // chrome.experimental.socket.destroy(socketId);
            return;
        }

        if (self.connected) {
            self.connected();
        }

        chrome.experimental.socket.read(self.socketId, self._onRead);
    },

    _onRead: function(readInfo) {
        var self = tcpSocket;
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
