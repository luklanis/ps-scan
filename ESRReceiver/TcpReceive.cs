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
        private TcpClient client;

        private string ipAddress;
		private int port;

        private Thread receivingThread;
        private bool stopThread;
        private NetworkStream stream;

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

            if (this.receivingThread == null)
            {
                this.ipAddress = ipAddress;
				this.port = port;

                this.receivingThread = new Thread(this.ReceiveThread);

                this.receivingThread.Start();

                this.ConnectionStateChanged(ConnectionState.Connecting);

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
        public bool StopReceiving()
        {
            bool stopped = false;

            try
            {
                if (this.receivingThread == null)
                {
                    this.ConnectionStateChanged(ConnectionState.Disconnected);
                    return true;
                }

                this.stopThread = true;
                stopped = this.receivingThread.Join(500);

                if (!stopped)
                {
                    this.receivingThread.Abort();
                    stopped = this.receivingThread.Join(500);
                }

                this.receivingThread = null;

                if (this.stream != null)
                {
                    this.stream.Close();
                    this.stream = null;
                }

                if (this.client != null)
                {
                    this.client.Close();
                    this.client = null;
                }
            }
            finally
            {
                if (stopped || this.client == null)
                {
                    this.ConnectionStateChanged(ConnectionState.Disconnected);
                }
            }

            return stopped;
        }

        #endregion

        #region private Methods

        private NetworkStream Connect(string server)
        {
            try
            {
                // Create a TcpClient.
                // Note, for this client to work you need to have a TcpServer 
                // connected to the same address as specified by the server, port
                // combination.
                this.client = new TcpClient(server, Port);

                this.ConnectionStateChanged(ConnectionState.Connected);

                return this.client.GetStream();
            }
            catch (ArgumentNullException e)
            {
                Trace.WriteLine("ArgumentNullException: " + e);
                return null;
            }
            catch (SocketException e)
            {
                Trace.WriteLine("SocketException: " + e);
                return null;
            }
        }

        private void ReceiveThread()
        {
            // NetworkStream stream;
            byte[] data = new byte[256];
            string responseData;

            // int lostKa;
            this.stream = null;

            while (this.stream == null)
            {
                if (this.stopThread)
                {
                    return;
                }

                this.stream = this.Connect(this.ipAddress);

                //// lostKa = 0;

                while (this.stream != null)
                {
                    if (this.stopThread)
                    {
                        return;
                    }

                    // String to store the response ASCII representation.
                    responseData = String.Empty;

                    try
                    {
                        // Read the first batch of the TcpServer response bytes.
                        int bytes = this.stream.Read(data, 0, data.Length);
                        responseData = Encoding.UTF8.GetString(data, 2, bytes);
                    }
                    catch (System.IO.IOException)
                    {
                        this.ConnectionStateChanged(ConnectionState.Connecting);
                        this.stream.Close();
                        this.stream = null;
                        this.client.Close();
                        this.client = null;
						
                        break;
                    }

                    if (responseData.Length > 0)
                    {
                        this.DataReceived(responseData);
                    }

                    Thread.Sleep(50);
                }

                Thread.Sleep(500);
            }
        }

        #endregion
    }
}