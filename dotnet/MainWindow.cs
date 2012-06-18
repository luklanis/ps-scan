namespace ESRReceiver
{
    using System;
    using System.Configuration;
    using System.Windows.Forms;
    using System.Net;

    using WindowsInput;

    public partial class MainWindow : Form
    {
        private TcpReceive tcpReceive;
        private TcpReceive.ConnectionState connState;

        private KeyboardSimulator simulator;

        public MainWindow()
        {
            this.InitializeComponent();

            this.tcpReceive = new TcpReceive();
            this.tcpReceive.DataReceived += new TcpReceive.DataReceivedFromNetEventHandler(this.tcpReceive_DataReceived);
            this.tcpReceive.ConnectionStateChanged += new TcpReceive.ConnectionStateChangedEventHandler(this.tcpReceive_ConnectionStateChanged);

            //this.textBox2.Text = (string)Registry.LocalMachine.GetValue(ConfigTraceIpAddress, string.Empty);
            this.textBox2.Text = Properties.Settings.Default.IpAddress;

            this.addCR.Checked = Properties.Settings.Default.AddCR;

            this.simulator = new KeyboardSimulator();
        }

        void tcpReceive_ConnectionStateChanged(TcpReceive.ConnectionState state)
        {
            // InvokeRequired required compares the thread ID of the
            // calling thread to the thread ID of the creating thread.
            // If these threads are different, it returns true.
            if (this.textBox2.InvokeRequired)
            {
                TcpReceive.ConnectionStateChangedEventHandler d =
                    new TcpReceive.ConnectionStateChangedEventHandler(this.tcpReceive_ConnectionStateChanged);
                this.Invoke(d, new object[] { state });
            }
            else
            {
                this.connState = state;

                switch (state)
                {
                    case TcpReceive.ConnectionState.Disconnected:
                        this.connectionState.Text = "Disconnected";
                        break;
                    case TcpReceive.ConnectionState.Connected:
                        this.connectionState.Text = "Connected";
                        break;
                    case TcpReceive.ConnectionState.Connecting:
                        this.connectionState.Text = "Connecting...";
                        break;
                    default:
                        break;
                }
            }
        }

        void tcpReceive_DataReceived(string text)
        {
            this.simulator.TextEntry(text);

            if (this.addCR.Checked)
            {
                this.simulator.KeyPress(WindowsInput.Native.VirtualKeyCode.RETURN);
            }
        }

        private void Form1_FormClosing(object sender, FormClosingEventArgs e)
        {
            this.tcpReceive.StopReceiving();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            if (this.connState == TcpReceive.ConnectionState.Disconnected)
            {
                try
                {
                    IPAddress.Parse(this.textBox2.Text);

                    if (Properties.Settings.Default.IpAddress != this.textBox2.Text)
                    {
                        Properties.Settings.Default.IpAddress = this.textBox2.Text;
                        Properties.Settings.Default.Save();
                    }

                    this.tcpReceive.StartReceiving(this.textBox2.Text);
                    this.button1.Text = "Disconnect";
                }
                catch
                {
                    MessageBox.Show("\"" + this.textBox2.Text + "\" is not a valid IP Address");
                }
            }
            else
            {
                this.tcpReceive.StopReceiving();
                this.button1.Text = "Connect";
            }
        }

        private void addCR_CheckedChanged(object sender, EventArgs e)
        {
            if (Properties.Settings.Default.AddCR != this.addCR.Checked)
            {
                Properties.Settings.Default.AddCR = this.addCR.Checked;
                Properties.Settings.Default.Save();
            }
        }
    }
}
