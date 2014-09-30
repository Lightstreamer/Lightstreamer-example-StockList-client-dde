/*
* Copyright 2013 Weswit Srl
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package javasedemo.dde;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import java.awt.Toolkit;
import java.util.TooManyListenersException;

import javax.swing.SwingUtilities;

import com.lightstreamer.ls_client.ConnectionInfo;
import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.SubscrException;
import com.lightstreamer.ls_client.UpdateInfo;

import com.pretty_tools.dde.DDEException;

public class StockListDemo extends JFrame implements LightstreamerConnectionHandler.StatusListener {

	private static final long serialVersionUID = 1L;

	/*
	 * Lightstreamer server URL to connect to.
	 */
	private final static String PUSH_SERVER_URL = "http://push.lightstreamer.com";
	private final static String CONNECTED_MSG = "Stop Lightstreamer";
	private final static String DISCONNECTED_MSG = "Start Lightstreamer";
	private boolean connected = false;

    /*
     * List of items available on the Lightsteamer server.
     */
    private final static String[] items = {"item1", "item2", "item3",
        "item4", "item5", "item6", "item7", "item8", "item9", "item10",
        "item11", "item12", "item13", "item14", "item15", "item16",
        "item17", "item18", "item19", "item20", "item21", "item22",
        "item23", "item24", "item25", "item26", "item27", "item28",
        "item29", "item30"};

    /*
     * Lightstreamer field names available on the Lightstreamer server for given
     * items.
     */
    private final static String[] fields = {"stock_name", "last_price", "time", "pct_change", "bid_quantity", "bid", "ask", "ask_quantity", 
            "min", "max", "ref_price", "open_price"};

    /*
     * UI names associated with field names.
     */
    private static final  String[] fieldNames = {"Name", "Last price", "Time", "Change", "Bid Size", "Bid", "Ask", "Ask Size", 
        "Min", "Max","Ref","Open"};
    private LightstreamerConnectionHandler ls;

	private static final String TITLE = "Lightstreamer :: Stock-List Demo :: Java/DDE";
    private static final ImageIcon LOGO = new ImageIcon(StockListDemo.class.getResource("/images/logo.png"));
    private static final ImageIcon disconnectedIcon = new ImageIcon(StockListDemo.class.getResource("/images/status_disconnected.png") );
    private static final ImageIcon pollingIcon = new ImageIcon(StockListDemo.class.getResource("/images/status_connected_polling.png") );
    private static final ImageIcon stalledIcon = new ImageIcon(StockListDemo.class.getResource("/images/status_stalled.png") );  
    private static final ImageIcon streamingIcon = new ImageIcon(StockListDemo.class.getResource("/images/status_connected_streaming.png") );
    private final JLabel statusLabel = new JLabel("", disconnectedIcon, JLabel.LEFT);
	private final JButton connectButton = new JButton(DISCONNECTED_MSG);
	private final JButton copyClipboardButton = new JButton("Copy Excel data to clipboard");
	private final JToggleButton toggleFeedingButton = new JToggleButton("Toggle data feeding to Excel");
	private final boolean showUiUpdatesCounter = true;
	private int updatesCounter = 0;
	private final JLabel updatesLabel = new JLabel("No Lightstreamer updates");
	private final LSDDEServer ddeServer;

	class DdeButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			if (e.getActionCommand().equals("connect")) {

				if (StockListDemo.this.connected) {
					new Thread("ServerStop") {
						public void run() {
						    try {
						        StockListDemo.this.ddeServer.stop();
						        StockListDemo.this.ls.stop();
						    }  catch (DDEException e) {
                                e.printStackTrace();
                            }
						}
					}.start();

				} else {

					new Thread("ServerStart") {
						public void run() {
							StockListDemo.this.ls.start();
							try {
								StockListDemo.this.ddeServer.start();
							} catch (TooManyListenersException e) {
								e.printStackTrace();
							} catch (DDEException e) {
								e.printStackTrace();
							}
						}
					}.start();

				}
			} else if (e.getActionCommand().equals("toggle")) {
				try {
                    StockListDemo.this.ddeServer.toggleFeeding();
                } catch (DDEException ddee) {
                    ddee.printStackTrace();
                }
			} else {

				// copy to clipboard action
				String str = StockListDemo.this.ddeServer.generateClipboardString();
				StringSelection stringSelection = new StringSelection(str);
			    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection, new ClipboardOwner() {

					@Override
					public void lostOwnership(Clipboard arg0, Transferable arg1) {
						/* do nothing */
					}
			    	
			    });
			}
		}

	}
    
	class DdePanel extends JPanel {
		private static final long serialVersionUID = 2L;

        private void setup() {
        	this.setBackground(Color.white);
	        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	        
	        //we create a JPanel containing the status indicator, our logo and the demo name
	        //we'll use this JPanel as first element on the main JPanel
	        JPanel firstLine = new JPanel();
	        firstLine.setBackground(Color.white);
	        firstLine.setLayout(new BoxLayout(firstLine, BoxLayout.X_AXIS));
	        firstLine.add(statusLabel);
	        JLabel label = new JLabel("", LOGO, JLabel.LEFT);
	        firstLine.add(label);

	        DdeButtonListener buttonListener = new DdeButtonListener();
	        connectButton.setActionCommand("connect");
        	connectButton.addActionListener(buttonListener);
	        firstLine.add(connectButton);

	        firstLine.add(Box.createHorizontalGlue());
	        this.add(firstLine);

	        JPanel secondLine = new JPanel();
	        secondLine.setBackground(Color.white);
	        //secondLine.setLayout(new FlowLayout());
	        copyClipboardButton.setActionCommand("clipboard");
	        copyClipboardButton.setEnabled(false);
	        copyClipboardButton.addActionListener(buttonListener);
	        secondLine.add(copyClipboardButton);
	        toggleFeedingButton.setActionCommand("toggle");
	        toggleFeedingButton.setEnabled(false);
	        toggleFeedingButton.addActionListener(buttonListener);
	        secondLine.add(toggleFeedingButton);
	        this.add(secondLine);
	        if (showUiUpdatesCounter) {
	        	JPanel thirdLine = new JPanel();
	        	thirdLine.setLayout(new FlowLayout());
	        	thirdLine.setBackground(Color.white);
	        	thirdLine.add(updatesLabel);
	        	this.add(thirdLine);
	        }
		}
	}

	private void setupLightstreamerClient(LSDDEServer ddeServer) {
        // configure the connection 
        ConnectionInfo cInfo = new ConnectionInfo();
        cInfo.pushServerUrl = StockListDemo.PUSH_SERVER_URL;
        cInfo.adapter = "DEMO";
        
        // this will handle the connection with Lightstreamer
        this.ls = new LightstreamerConnectionHandler(cInfo, this);
        
        // this represents our subscription
        ExtendedTableInfo table = null;
        try {
            table = new ExtendedTableInfo(StockListDemo.items,
            		"MERGE", StockListDemo.fields, true);
            table.setDataAdapter("QUOTE_ADAPTER");
        } catch (SubscrException e) { /* do nothing */ }
        if (table != null) {
        	// Lightstreamer RT data updates are directly pushed to
        	// the DDE Server
        	this.ls.addTable(table, ddeServer);
        }
	}

	public StockListDemo() {
		// DDE variables
		this.ddeServer = new LSDDEServer(items, fields, fieldNames);
		this.setupLightstreamerClient(this.ddeServer);

		DdePanel panel = new DdePanel();
		panel.setup();
		// setup frame and show application
		this.setContentPane(panel);
		this.setSize(370, 200);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle(TITLE);
		this.pack();
		this.setVisible(true);

		// setup a listener into the newly created DDEServer object.
		// This will handle the UI updates.
		try {
			this.ddeServer.addServerListener(new DDEServerListener() {
				
				@Override
				public void onLightstreamerUpdate(int itemPos, String itemName,
						UpdateInfo update) {

					// call Swing UI updates in event loop thread
					// the following UI calls are not thread-safe
					SwingUtilities.invokeLater(new Runnable() {

						UpdateInfo update;

						@Override
						public void run() {
							updatesLabel.setText(new Integer(++updatesCounter).toString());
							// first thing the server sends, is a snapshot of the data
							// so allow Excel to use the DDE server, after this happens.
							// This means, enable the copy to clipboard button now.
							if (update.isSnapshot()) {
								copyClipboardButton.setEnabled(true);
							}
							
						}

						public Runnable setData(UpdateInfo update) {
							this.update = update;
							return this;
						}

					}.setData(update));
					
				}
			});
		} catch (TooManyListenersException e) { /* cannot happen */ }

	}

    @Override
    public void onStatusChange(int status) {
        String statusTxt = null;
        ImageIcon icon = null;
        switch(status) {
            case LightstreamerConnectionHandler.DISCONNECTED:
                statusTxt = "Disconnected";
                icon = disconnectedIcon;
                this.connected = false;
                break;
            case LightstreamerConnectionHandler.CONNECTING:
                statusTxt = "Connecting to server";
                icon = disconnectedIcon;
                this.connected = false;
                break;
            case LightstreamerConnectionHandler.STREAMING:
                statusTxt = "Session started in streaming";
                icon = streamingIcon;
                this.connected = true;
                break;
            case LightstreamerConnectionHandler.POLLING:
                statusTxt = "Session started in smart polling";
                icon = pollingIcon;
                this.connected = true;
                break;
            case LightstreamerConnectionHandler.STALLED:
                statusTxt = "Connection stalled";
                icon = stalledIcon;
                this.connected = false;
                break;
            default:
                statusTxt = "Disconnected";
                icon = disconnectedIcon;
                this.connected = false;
        }

        // Call swing from the event loop, UI calls are
        // not thread-safe.
        SwingUtilities.invokeLater(new Runnable() {

        	boolean connected;
        	String text;
        	ImageIcon icon;

			@Override
			public void run() {
				if (connected) {
					connectButton.setText(CONNECTED_MSG);
					toggleFeedingButton.setEnabled(true);
				} else {
					connectButton.setText(DISCONNECTED_MSG);
					toggleFeedingButton.setEnabled(false);
					copyClipboardButton.setEnabled(false);
				}
				statusLabel.setToolTipText(text);
				statusLabel.setIcon(icon);
				
			}

			private Runnable setData(boolean connected, String text, ImageIcon icon) {
				this.connected = connected;
				this.text = text;
				this.icon = icon;
				return this;
			}
        	
        }.setData(connected, statusTxt, icon));
    }

	public static void main(String args[]) {
		new StockListDemo();
	}

}
