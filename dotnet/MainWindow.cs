namespace ESRReceiver
{
    using System;
    using System.Configuration;
    using System.Threading;
    using System.Windows.Forms;
    using System.Net;
    using System.IO;

    using WindowsInput;

    public partial class MainWindow : Form
    {
        private const string ConfigIpAddress = "IpAddress";
        private const string ConfigAddCR = "AddCR";

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
            this.textBox2.Text = ConfigurationManager.AppSettings[ConfigIpAddress] ?? string.Empty;

            this.addCR.Checked = (ConfigurationManager.AppSettings[ConfigAddCR] ?? string.Empty).ToLower() == "true";

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

                    string urlConfiguration = ConfigurationManager.AppSettings[ConfigIpAddress];

                    if (urlConfiguration != this.textBox2.Text)
                    {
                        Configuration config =
                            ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);

                        config.AppSettings.Settings.Remove(ConfigIpAddress);
                        config.AppSettings.Settings.Add(new KeyValueConfigurationElement(ConfigIpAddress, this.textBox2.Text));
                        config.Save(ConfigurationSaveMode.Modified);
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

            string addCRChecked = ConfigurationManager.AppSettings[ConfigAddCR];

            if (addCRChecked != this.addCR.Checked.ToString())
            {
                Configuration config =
                    ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);

                config.AppSettings.Settings.Remove(ConfigAddCR);
                config.AppSettings.Settings.Add(new KeyValueConfigurationElement(ConfigAddCR, this.addCR.Checked.ToString()));
                config.Save(ConfigurationSaveMode.Modified);
            }
        }
    }
}
