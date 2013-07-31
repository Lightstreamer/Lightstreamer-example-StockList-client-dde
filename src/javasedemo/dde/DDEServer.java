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
import com.neva.*;

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

public class DDEServer implements HandyTableListener {

	// DdeServer parameters
	private com.neva.DdeServer ddeServer = null;
    private static final int CF_TEXT = 1;
    private static final int XL_TABLE_FORMAT = DdeServer.ddeRegisterClipboardFormat("XlTable");
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

	public DDEServer(String[] items, String[] fields, String[] fieldNames) {
		if (fields.length != fieldNames.length) {
			throw new RuntimeException(
				"fields and fieldNames must have the same length");
		}
		this.items = items;
		this.fields = fields;
		this.fieldNames = fieldNames;
	}

	class DdeConnectionListener implements DdeServerConnectionEventListener {
		
		@Override
		public void onUnregister(DdeServerConnectionEvent arg0) { /* do nothing */ }
		
		@Override
		public void onRegister(DdeServerConnectionEvent arg0) { /* do nothing */ }
		
		@Override
		public void onException(DdeServerConnectionEvent arg0) { /* do nothing */ }
		
		@Override
		public void onDisconnect(DdeServerConnectionEvent arg0) { /* do nothing */ }
		
		@Override
		public void onConnectConfirm(DdeServerConnectionEvent arg0) { /* do nothing */ }
		
		@Override
		public void onConnect(DdeServerConnectionEvent arg0)
				throws DdeServerConnectionRejectedException {

            // Make sure the client is asking for the right service
            if (!arg0.getService().equalsIgnoreCase(SERVER_NAME)) {
                throw new DdeServerConnectionRejectedException();
            }
            // Make sure the client is asking for the right topic
            if (!arg0.getTopic().equalsIgnoreCase(QUOTE_TOPIC_NAME)) {
                throw new DdeServerConnectionRejectedException();
            }
            // informational output, uncomment for debugging
            //System.out.printf("onConnect: server: %s | topic: %s\n", SERVER_NAME,
            // 		QUOTE_TOPIC_NAME);
		}
	}

	class DdeTransactionListener implements DdeServerTransactionEventListener {

		private String getUpdateValue(DdeServerTransactionEvent arg0) {
            String itemName = arg0.getItem();
            int index = itemName.indexOf("@");

            if (index < 0) {
                return null;
            }
            String attribute = itemName.substring(index + 1);
            String key = itemName.substring(0, index);
            synchronized(DDEServer.this.itemCache) {
	            UpdateInfo update = DDEServer.this.itemCache.get(key);
	            if (update == null) {
	            	// NOTE: data not yet available in local cache
	            	return null;
	            }
	            return update.getNewValue(attribute);
            }
		}

	    private byte[] buildXLTable(String s) {
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
	    }

		@Override
		public void onRequest(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {

	        String value = this.getUpdateValue(arg0);
	        if (value == null) {
	            value = "";
	        }
	        // informational output, uncomment for debugging
			//System.out.printf("onRequest: item: %s | topic: %s | LSvalue: %s\n", arg0.getItem(),
			//		arg0.getTopic(), value);

	        if (arg0.getFormat() == XL_TABLE_FORMAT) {
	            byte[] xldata = this.buildXLTable(value);
	            arg0.setRequestedData(xldata);
	            return;
	        } else if (arg0.getFormat() == CF_TEXT) {
	        	arg0.setRequestedData(new String(value).getBytes());
	            return;
	        }
	        throw new DdeServerTransactionRejectedException();
		}
		
		@Override
		public void onPoke(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {
			/* do nothing */ 
		}
		
		@Override
		public void onExecute(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {
			/* do nothing */
		}
		
		@Override
		public void onException(DdeServerTransactionEvent arg0) {
			/* do nothing */
		}
		
		@Override
		public void onAdvStop(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {
			/* do nothing */
		}
		
		@Override
		public void onAdvStart(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {
            // Make sure the client is asking for the right topic
            if (!arg0.getTopic().equalsIgnoreCase(QUOTE_TOPIC_NAME)) {
                throw new DdeServerTransactionRejectedException();
            }
            // informational output, uncomment for debugging
            //System.out.printf("onAdvStart: topic: %s| dde item: %s\n",
            //		QUOTE_TOPIC_NAME, arg0.getItem());
		}
		
		@Override
		public void onAdvReq(DdeServerTransactionEvent arg0)
				throws DdeServerTransactionRejectedException {
			// informational output, uncomment for debugging
			//System.out.println("onAdvReq: " + arg0);
			this.onRequest(arg0);
		}
	}

	public synchronized void start() throws TooManyListenersException, DdeException {
		if (ddeServer != null) {
			this.stop();
		}
		com.neva.DdeServer server = new com.neva.DdeServer();
		server.setService(SERVER_NAME);
		server.addDdeServerConnectionEventListener(
				new DdeConnectionListener());
		server.addDdeServerTransactionEventListener(
				new DdeTransactionListener());
		server.start();
		this.ddeServer = server;
	}

	public synchronized void stop() {
		if (this.ddeServer == null) {
			return;
		}
		DdeServer server = this.ddeServer;
		this.ddeServer = null;
		server.stop();
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

	public void toggleFeeding() {
		this.feedingToggle = !this.feedingToggle;
		if (this.feedingToggle) {
			// tell Excel to re-fetch everything
			Iterator<String> iter = this.itemCache.keySet().iterator();
			while (iter.hasNext()) {
				String itemName = iter.next();
				for (int i = 0; i < this.fields.length; i++) {
					String ddeItem = itemName + "@" + this.fields[i];
					this.ddeServer.postAdvise(QUOTE_TOPIC_NAME, ddeItem);					
				}
			}
		}
	}

	public void onLightstreamerUpdate(int itemPos, String itemName, UpdateInfo update) {
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
		    		this.ddeServer.postAdvise(QUOTE_TOPIC_NAME, ddeItem);
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
		this.onLightstreamerUpdate(itemPos, itemName, update);
	}

}
