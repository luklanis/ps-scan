package ch.luklanis.esreceiver;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ch.luklanis.esreceiver.connectionstate.ConnectionState;
import ch.luklanis.esreceiver.connectionstate.ConnectionStateChangedEvent;
import ch.luklanis.esreceiver.connectionstate.OnConnectionStateChangeListener;
import ch.luklanis.esreceiver.datareceived.DataReceivedEvent;
import ch.luklanis.esreceiver.datareceived.OnDataReceivedListener;

import com.crs.toolkit.layout.SWTGridData;
import com.crs.toolkit.layout.SWTGridLayout;

public class AppFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1668274381664960966L;
	private JButton saveButton;
	private JLabel connectionState;
	private TcpReceive tcpReceive;
	private JTextField ipAddress;
	private JCheckBox addCRCheckBox;
	private Properties properties;
	private static final String propertiesFile = System.getProperty("user.dir") + "/ESRReceiver.properties";

	public AppFrame() {
		super("ESR Receiver");

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(280, 140);

		FileInputStream inputStream = null;
		String host = "";
		boolean addCR = false;
		properties = new Properties();
		
		try {
			inputStream = new FileInputStream(propertiesFile);
			properties.load(inputStream);        
			host = properties.getProperty("host");
			addCR = properties.getProperty("addCR").equalsIgnoreCase("true");
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
			}
		}

		JPanel body = new JPanel(new SWTGridLayout(2, false));
		getContentPane().add(body);

		body.add(new JLabel("IP Address:"));

		ipAddress = new JTextField();
		ipAddress.setText(host);

		SWTGridData data = new SWTGridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = SWTGridData.FILL;
		body.add(ipAddress, data);

		addCRCheckBox = new JCheckBox("Add ENTER-Key");
		addCRCheckBox.setSelected(addCR);
		addCRCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveProperty("addCR", String.valueOf(addCRCheckBox.isSelected()));
			}
		});

		body.add(addCRCheckBox);

		saveButton = new JButton("Connect");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (saveButton.getText().equalsIgnoreCase("connect")){
					saveProperty("host", ipAddress.getText());
					saveButton.setText("Disconnect");
					connectionState.setText(ConnectionState.Connecting.name());
					tcpReceive.connect(ipAddress.getText());
				} else {
					saveButton.setText("Connect");
					tcpReceive.close();
				}
			}
		});
		getRootPane().setDefaultButton(saveButton);
		body.add(saveButton);


		JPanel footer = new JPanel();
		getContentPane().add(footer, BorderLayout.SOUTH);
		footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));

		data = new SWTGridData();
		data.horizontalSpan = 4;
		data.horizontalAlignment = SWTGridData.HORIZONTAL_ALIGN_BEGINNING;
		//		body.add(address, data);
		connectionState = new JLabel(ConnectionState.Disconnected.name());
		footer.add(connectionState, data);

		this.tcpReceive = new TcpReceive();
		this.tcpReceive.setOnConnectionStateChangeListener(new OnConnectionStateChangeListener() {

			@Override
			public void connectionStateChanged(ConnectionStateChangedEvent event) {
				connectionState.setText(event.getConnectionState().name());
			}
		});
		this.tcpReceive.setOnDataReceivedListener(new OnDataReceivedListener() {

			@Override
			public void dataReceived(DataReceivedEvent event) {
				String codeRow = event.getData();

				Robot robot;
				try {
					robot = new Robot();
				} catch (AWTException e1) {
					e1.printStackTrace();
					return;
				}
				robot.setAutoDelay(5);

				for (int i = 0; i < codeRow.length(); i++) {
					char keycode = codeRow.charAt(i);
					if (((keycode - KeyEvent.VK_0) >= 0) 
							&& ((keycode - KeyEvent.VK_0) <= 9)
							|| (keycode == KeyEvent.VK_SPACE)) {
						robot.keyPress(keycode);
						robot.keyRelease(keycode);
					} else {
						if (keycode == '>') {
							robot.keyPress(KeyEvent.VK_SHIFT);
							robot.keyPress(KeyEvent.VK_LESS);
							robot.keyRelease(KeyEvent.VK_SHIFT);
							continue;
						}
						if (keycode == '+') {
							robot.keyPress(KeyEvent.VK_SHIFT); 
							robot.keyPress(KeyEvent.VK_1); 
							robot.keyRelease(KeyEvent.VK_SHIFT);
							continue;
						}
						
						System.out.println(String.format("Received: %d", (int)keycode));
					}
				}

				if (addCRCheckBox.isSelected()) {
					robot.keyPress(KeyEvent.VK_ENTER);
					robot.keyRelease(KeyEvent.VK_ENTER);
				}
			}
		});
	}

	protected void saveProperty(String property, String value) {
		FileOutputStream outputStream = null;
		try {  
			properties.setProperty(property, value);        
			outputStream = new FileOutputStream(propertiesFile);        
			properties.store(outputStream, "update " + property);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public static void main(String... args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException e) {
				} catch (InstantiationException e) {
				} catch (IllegalAccessException e) {
				} catch (UnsupportedLookAndFeelException e) {
				}

				new AppFrame().setVisible(true);
			}
		});
	}
}
