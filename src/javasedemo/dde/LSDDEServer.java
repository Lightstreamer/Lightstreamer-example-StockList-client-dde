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

import java.util.HashMap;
import java.util.Iterator;
import java.util.TooManyListenersException;

import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.UpdateInfo;
import com.pretty_tools.dde.server.*;
import com.pretty_tools.dde.ClipboardFormat;
import com.pretty_tools.dde.DDEException;

abstract interface DDEServerListener {

	/**
	 * Triggered whenever DDEServer object receives a Lightstreamer update.
	 * 
	 * @param itemPos Lightstreamer updated item position
	 * @param itemName name of the updated item
	 * @param update Lightstreamer UpdateInfo object
	 */
	public void onLightstreamerUpdate(int itemPos, String itemName, UpdateInfo update);

}

public class LSDDEServer implements HandyTableListener {

	// DdeServer parameters
	private DDEServer ddeServer = null;
    private static final int CF_TEXT = 1;
    // private static final com.pretty_tools.dde.ClipboardFormat XL_TABLE_FORMAT = ClipboardFormat.valueOf("XlTable");
	private static final String SERVER_NAME = "PT_DDE_SERVER";
	private static final String QUOTE_TOPIC_NAME = "PT_DDE_QUOTE_TOPIC";

	// Subscribed Lightstreamer data
	private final String[] items;
	private final String[] fields;
	private final String[] fieldNames;

	// Updates handling objects.
	private final HashMap<String, UpdateInfo> itemCache = new HashMap<String, UpdateInfo>();
	private boolean feedingToggle = true;
	private DDEServerListener listener = null;

    /* private byte[] buildXLTable(String s) {
	    // This method answers data in xltable format
	    byte[] arr = new byte[12 + 1 + s.length()];
	    Coroutine.setWORDAtOffset(arr, 16, 0);
	    Coroutine.setWORDAtOffset(arr, 4, 2);
	    Coroutine.setWORDAtOffset(arr, 1, 4);
	    Coroutine.setWORDAtOffset(arr, 1, 6);
	    Coroutine.setWORDAtOffset(arr, 2, 8);
	    Coroutine.setWORDAtOffset(arr, 1 + s.length(), 10);
	    Coroutine.setBYTEAtOffset(arr, s.length(), 12);
	    System.arraycopy(s.getBytes(), 0, arr, 13, s.length());
	    return arr;
	    } */
	
	public LSDDEServer(String[] items, String[] fields, String[] fieldNames) {
		if (fields.length != fieldNames.length) {
			throw new RuntimeException(
				"fields and fieldNames must have the same length");
		}
		this.items = items;
		this.fields = fields;
		this.fieldNames = fieldNames;
	}

	public synchronized void start() throws TooManyListenersException, DDEException {
		if (this.ddeServer != null) {
			// this.stop();
		    return ;
		}
		DDEServer server = new DDEServer(SERVER_NAME) {
            @Override
            protected boolean isTopicSupported(String topicName)
            {
                System.out.println("is supported " + topicName + "?");
                return QUOTE_TOPIC_NAME.equalsIgnoreCase(topicName);
            }

            @Override
            protected boolean isItemSupported(String topic, String item, int uFmt)
            {
                return this.isTopicSupported(topic) && (uFmt == ClipboardFormat.CF_TEXT.getNativeCode() || uFmt == ClipboardFormat.CF_UNICODETEXT.getNativeCode());
            }

            @Override
            protected boolean onExecute(String command)
            {
                System.out.println("onExecute(" + command + ")");

                if ("stop".equalsIgnoreCase(command))
                    return false;

                return true;
            }

            @Override
            protected boolean onPoke(String topic, String item, String data)
            {
                System.out.println("onPoke(" + topic + ", " + item + ", " + data + ")");

                return true;
            }

            @Override
            protected boolean onPoke(String topic, String item, byte[] data, int uFmt)
            {
                System.out.println("onPoke(" + topic + ", " + item + ", " + data + ", " + uFmt + ")");

                return true; // we do not support it
            }

            @Override
            protected String onRequest(String topic, String item)
            {
                System.out.println("onRequest(" + topic + ", " + item + ")");

                int index = item.indexOf("@");
                if (index < 0) {
                    return "";
                }
                String attribute = item.substring(index + 1);
                String key = item.substring(0, index);
                synchronized(LSDDEServer.this.itemCache) {
                    UpdateInfo update = LSDDEServer.this.itemCache.get(key);
                    if (update == null) {
                        // NOTE: data not yet available in local cache
                        return "";
                    }
                    if (attribute.equals("time")) {
                        return "'" + update.getNewValue(attribute);
                    } else {
                        return update.getNewValue(attribute);
                    }
                }
                    
            }

            @Override
            protected byte[] onRequest(String topic, String item, int uFmt)
            {
                System.out.println("onRequest(" + topic + ", " + item + ", " + uFmt + ")");
                
                int index = item.indexOf("@");
                if (index < 0) {
                    return null;
                }
                String attribute = item.substring(index + 1);
                String key = item.substring(0, index);
                synchronized(LSDDEServer.this.itemCache) {
                    UpdateInfo update = LSDDEServer.this.itemCache.get(key);
                    if (update == null) {
                        // NOTE: data not yet available in local cache
                        return "".getBytes();
                    }
                    if (attribute.equals("time")) {
                        return ("'" + update.getNewValue(attribute)).getBytes();
                    } else {
                        return update.getNewValue(attribute).getBytes();
                    }
                }
                
            }
        }; 

/*		server.setService(SERVER_NAME);
		server.addDdeServerConnectionEventListener(
				new DdeConnectionListener());
		server.addDdeServerTransactionEventListener(
				new DdeTransactionListener());  */
		server.start();
		System.out.println("Start ... ");
		this.ddeServer = server;
	}

	public synchronized void stop() throws DDEException {
		if (this.ddeServer == null) {
			return;
		}
		
        // Uncomment the code below to shut down the DDE Server in case of 
        // stop of the connection with the data source (Lightstreamer server).
		
		/* DDEServer server = this.ddeServer;
		System.out.println(" ... Stop.");
		this.ddeServer = null;
		server.stop();*/
	}

	public void addServerListener(DDEServerListener listener) throws TooManyListenersException {
		if (this.listener != null || listener == null) {
			throw new TooManyListenersException();
		}
		this.listener = listener;
	}

	public void removeServerListener() {
		this.listener = null;
	}

	public String generateClipboardString() {
		String str = "TECH " + this.items.length + "\n";
		for (int i=0; i < this.fieldNames.length; i++) {
			// setup columns
			if (i != (this.fieldNames.length-1))
				str += this.fieldNames[i] + "\t";
			else
				str += this.fieldNames[i] + "\n";
		}
		for (int i=0; i < this.items.length; i++) {
			String itemHeader = "=" + SERVER_NAME + "|" + QUOTE_TOPIC_NAME + "!'" 
				 + this.items[i] + "@";
			for (int y=0; y < this.fields.length; y++) {
				if (y != (this.fields.length-1))
					str += itemHeader + this.fields[y] + "'\t";
				else
					str += itemHeader + this.fields[y] + "'\r\n";
			}
		}
		return str;
	}

	public void toggleFeeding() throws DDEException {
		this.feedingToggle = !this.feedingToggle;
		if (this.feedingToggle) {
			// tell Excel to re-fetch everything
			Iterator<String> iter = this.itemCache.keySet().iterator();
			while (iter.hasNext()) {
				String itemName = iter.next();
				for (int i = 0; i < this.fields.length; i++) {
					String ddeItem = itemName + "@" + this.fields[i];
					this.ddeServer.notifyClients(QUOTE_TOPIC_NAME, ddeItem);					
				}
			}
		}
	}

	public void onLightstreamerUpdate(int itemPos, String itemName, UpdateInfo update) throws DDEException {
		if (this.ddeServer == null) {
			// server not started yet
			return;
		}
		
		synchronized(this.itemCache) {
			this.itemCache.put(itemName, update);
		}
		// inform DDE about data update
		if (feedingToggle) {
			for (int i = 0; i < this.fields.length; i++) {
				String field = this.fields[i];
		    	if (update.isValueChanged(field)) { 
		    		// generate DDE item string
		    		String ddeItem = itemName + "@" + field;
		    		this.ddeServer.notifyClients(QUOTE_TOPIC_NAME, ddeItem);
		    		
		    	}
			}
		}

		// now it is possible to inform the listener
		if (this.listener != null) {
			this.listener.onLightstreamerUpdate(itemPos, itemName, update);
		}

	}

	// ***************************************
	// Lightstreamer HandyTableListener events
	// ***************************************

    @Override
    public void onRawUpdatesLost(int itemPos, String itemName,
            int lostUpdates) {
    	/* do nothing */
    }

    @Override
    public void onSnapshotEnd(int itemPos, String itemName) {
    	/* do nothing */
    }

    @Override
    public void onUnsubscr(int itemPos, String itemName) {
    	/* do nothing */
    }

    @Override
    public void onUnsubscrAll() {
    	/* do nothing */
    }

	@Override
	public void onUpdate(int itemPos, String itemName, UpdateInfo update) {
	    try {
	        this.onLightstreamerUpdate(itemPos, itemName, update);
	    } catch (DDEException ddee) {
	        // Skip.
	    }
	}

}
