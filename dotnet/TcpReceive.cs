// --------------------------------------------------------------------------------------------------------------------
// <copyright file="TcpReceive.cs" company="KABA AG Access Management">
//   Copyright (c) KABA AG Access Management. All rights reserved.
// </copyright>
// <summary>
//   The tcp receive.
// </summary>
// --------------------------------------------------------------------------------------------------------------------

namespace ESRReceiver
{
    using System;
    using System.Diagnostics;
    using System.Diagnostics.CodeAnalysis;
    using System.Net.Sockets;
    using System.Text;
    using System.Threading;
    using System.IO;

    /// <summary>
    /// The tcp receive.
    /// </summary>
    internal class TcpReceive
    {
        #region private Attributes

        //// 15 * 50ms = 750ms
        //private const int Timeout = 15;
        // 300 * 50ms = 15000ms
        private const int Timeout = 300;

        private string ipAddress;
        private int port;

        private Thread receivingThread;
        private bool stopThread;

        private TcpClient client = null;

        #endregion

        #region public Attributes

        #region Delegates

        /// <summary>
        /// The connection state changed event handler.
        /// </summary>
        /// <param name="state">
        /// The state.
        /// </param>
        public delegate void ConnectionStateChangedEventHandler(ConnectionState state);

        /// <summary>
        /// The data received from net event handler.
        /// </summary>
        /// <param name="text">
        /// The received text.
        /// </param>
        public delegate void DataReceivedFromNetEventHandler(string text);

        #endregion

        /// <summary>
        /// The connection state changed.
        /// </summary>
        public event ConnectionStateChangedEventHandler ConnectionStateChanged;

        /// <summary>
        /// The data received.
        /// </summary>
        public event DataReceivedFromNetEventHandler DataReceived;

        #endregion

        #region enums

        /// <summary>
        /// The connection state.
        /// </summary>
        public enum ConnectionState
        {
            /// <summary>
            /// The disconnected.
            /// </summary>
            Disconnected,

            /// <summary>
            /// The connected.
            /// </summary>
            Connected,

            /// <summary>
            /// The connecting.
            /// </summary>
            Connecting
        }

        #endregion

        #region public Methods

        /// <summary>
        /// Gets IpAddress.
        /// </summary>
        public string IpAddress
        {
            get { return this.ipAddress; }
        }

        /// <summary>
        /// Gets Port.
        /// </summary>
        public int Port
        {
            get { return this.port; }
        }

        /// <summary>
        /// The start receiving.
        /// </summary>
        /// <param name="ipAddress">
        /// The ip address.
        /// </param>
        /// <returns>
        /// <c>True</c> if starts successful; otherwise <c>false</c>
        /// </returns>
        public bool StartReceiving(string ipAddress)
        {
            return this.StartReceiving(ipAddress, 8765);
        }

        /// <summary>
        /// The start receiving.
        /// </summary>
        /// <param name="ipAddress">
        /// The ip address.
        /// </param>
        /// <param name="port">
        /// The ip address.
        /// </param>
        /// <returns>
        /// <c>True</c> if starts successful; otherwise <c>false</c>
        /// </returns>
        public bool StartReceiving(string ipAddress, int port)
        {
            this.stopThread = false;

            if (this.receivingThread == null && this.client == null)
            {
                this.ipAddress = ipAddress;
                this.port = port;

                this.ConnectionStateChanged(ConnectionState.Connecting);

                this.Connect();

                return true;
            }

            return false;
        }

        /// <summary>
        /// The stop receiving.
        /// </summary>
        /// <returns>
        /// Stops the thread to receiving messages.
        /// </returns>
        public void StopReceiving()
        {
            bool stopped = false;

            try
            {
                if (this.receivingThread == null && this.client == null)
                {
                    this.stopThread = true;
                    return;
                }

                if (this.receivingThread != null)
                {
                    this.stopThread = true;
                    stopped = this.receivingThread.Join(500);

                    if (!stopped)
                    {
                        this.receivingThread.Abort();
                        stopped = this.receivingThread.Join(500);
                    }
                }

                if (this.client != null && this.client.Connected)
                {
                    this.client.Close();
                }

                this.client = null;
                this.receivingThread = null;
            }
            finally
            {
                this.ConnectionStateChanged(ConnectionState.Disconnected);
            }
        }

        #endregion

        #region private Methods

        private void Receiving(IAsyncResult result)
        {
            var client = (TcpClient)result.AsyncState;

            if (client.Connected && this.receivingThread == null)
            {
                this.client = client;

                this.receivingThread = new Thread(this.ReceiveThread);

                this.receivingThread.Start();

                this.ConnectionStateChanged(ConnectionState.Connected);
            }
            else if (this.receivingThread == null && !this.stopThread)
            {
                Thread.Sleep(500);
                this.Connect();
            }
        }

        private void Connect()
        {
            try
            {
                var client = new TcpClient();
                client.LingerState = new LingerOption(true, 0);
                //client.ReceiveTimeout = 1000;

                client.BeginConnect(this.ipAddress, this.port, Receiving, client);
                //client.Connect(server, Port);

                //this.ConnectionStateChanged(ConnectionState.Connected);

                //return client;
            }
            catch (ArgumentNullException e)
            {
                Trace.WriteLine("ArgumentNullException: " + e);
            }
            catch (SocketException e)
            {
                Trace.WriteLine("SocketException: " + e);
            }
        }

        private void ReceiveThread()
        {
            // NetworkStream stream;
            byte[] data = new byte[256];
            string responseData;
            NetworkStream stream = null;
            bool shouldReconnect = false;

            try
            {
                while (this.client != null && this.client.Connected)
                {
                    if (this.stopThread)
                    {
                        if (stream != null)
                        {
                            stream.Close();
                        }

                        return;
                    }

                    // String to store the response ASCII representation.
                    responseData = String.Empty;

                    if (stream == null)
                    {
                        stream = client.GetStream();
                    }

                    // Read the first batch of the TcpServer response bytes.
                    int bytes = stream.Read(data, 0, data.Length);

                    if (bytes <= 2)
                    {
                        this.ConnectionStateChanged(ConnectionState.Connecting);

                        shouldReconnect = true;

                        break;
                    }

                    responseData = Encoding.UTF8.GetString(data, 2, bytes - 2);

                    this.DataReceived(responseData);

                    Thread.Sleep(50);
                }
            }
            catch (Exception)
            {
            }
            finally
            {
                if (stream != null)
                {
                    stream.Close();
                }

                if (this.client != null)
                {
                    this.client.Close();
                    this.client = null;
                }

                this.receivingThread = null;

                if (shouldReconnect)
                {
                    this.Connect();
                }
            }
        }

        #endregion
    }
}