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

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lightstreamer.ls_client.ConnectionInfo;
import com.lightstreamer.ls_client.ConnectionListener;
import com.lightstreamer.ls_client.ExtendedTableInfo;
import com.lightstreamer.ls_client.HandyTableListener;
import com.lightstreamer.ls_client.LSClient;
import com.lightstreamer.ls_client.PushConnException;
import com.lightstreamer.ls_client.PushServerException;
import com.lightstreamer.ls_client.PushUserException;
import com.lightstreamer.ls_client.SubscrException;

/**
 * this class wraps the LSClient class to enhance it with automatic re-connections and automatic
 * re-subscriptions; when start is called from outside, until stop is called, this class will continuously
 * try to connect to a Lightstreamer server. 
 * If while connected the connection is dropped the class will try to connect again.
 * Moreover this class keeps a list of table to be subscribed as soon as a connection is established.
 */
public class LightstreamerConnectionHandler {
    
    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int STREAMING = 2;
    public static final int POLLING = 3;
    public static final int STALLED = 4;
    
    final private LSClient client = new LSClient();
    private StatusListener statusListener;
    private ConnectionInfo info;
    private LinkedList<TableAndListener> tables = new LinkedList<TableAndListener>();
    private ExecutorService connectionThread;
    private int status = DISCONNECTED;
  
    //  the phase will change on each connection effort so that calls from older StatusListener will be ignored
    private int phase = 0;
    
    public LightstreamerConnectionHandler(ConnectionInfo info, StatusListener statusListener) {
        this.statusListener = statusListener;
        this.info = info;
        
        //  prepare an ExecutorService that will handle our connection efforts
        connectionThread = Executors.newSingleThreadExecutor();
    }
    
    public void addTable(ExtendedTableInfo table, HandyTableListener listener) {
        synchronized(tables) {
            TableAndListener toSub = new TableAndListener(table,listener);
            tables.add(toSub);
            
            int addPhase;
            synchronized(this) {
               addPhase = this.phase; 
               if (this.status == DISCONNECTED || this.status == CONNECTING) {
                   return;
               }
            }
            
            // if we're already connected the table will be subscribed immediately
            this.subscribeOne(toSub, addPhase);
        }
    }
    
    private synchronized void start(int ph) {
        if (ph != this.phase) {
            // we ignore old calls
            return;
        }
        this.start();
    }
    
    public synchronized void start() {
        // this method starts a connection effort
        this.phase++;
        this.changeStatus(this.phase,CONNECTING);
        connectionThread.execute(new ConnectionThread(this.phase));
    }
    
    public synchronized void stop() {
        // this method closes the connection and automatic re-connections
        this.phase++;
        this.client.closeConnection();
        this.changeStatus(this.phase,DISCONNECTED);
    }
    
    private synchronized void changeStatus(final int ph, final int status) {
        if (ph != this.phase) {
            // we ignore old calls
            return;
        }
        this.status = status;
        // this method passes the current status to a simple listener
        if (this.statusListener != null) {
            this.statusListener.onStatusChange(status);
        }
    }
    
    private void execute(int ph) {
        synchronized(this) {
            if (ph != this.phase) {
                return;
            }
            this.phase++;
        }
       
        // we connect first
        ph = this.connect(this.phase); 
        if (ph > -1) {
            // and then subscribe
            this.subscribe(ph);
        } // else someone called stop while we were connecting 
        
    }
    
    private int connect(int connPhase) {
        boolean connected = false;
        int pause = 2000;
        
        // this method will not exit until the openConnection returns without throwing an exception 
        // that means until we obtain a session from a Lightstreamer server 
        while (!connected) {
            synchronized(this) {
                if (connPhase != this.phase) {
                    //something changed the phase (maybe a stop call) while we were waiting; exit now
                    return -1;
                }
                
                connPhase = ++this.phase;
                
                try {
                    client.openConnection(this.info, new LSConnectionListener(this.phase));
                    connected = true;
                } catch (PushConnException e) {
                } catch (PushServerException e) {
                } catch (PushUserException e) {
                    // wrong user password notifications will pass from here
                }
            }    
            if (!connected) {
                // if we fail to connect we make a pause before the next effort
                try {
                    Thread.sleep(pause);
                    
                } catch (InterruptedException e) {
                }
            }
            // each time a connection fails the pause will grow
            pause*=2;
         }
        
        return connPhase;
    }
    
    private void subscribe(int ph) {
        for (TableAndListener toSub : this.tables) {
            if (!this.subscribeOne(toSub, ph)) {
                return;
            }
        }
    }
    
    private boolean subscribeOne(TableAndListener toSub, int subscriptionPhase) {
        // most exceptions will be caused by problems that will let the entire connection to the server to fail
        // this means that probably, after an exception, we'll soon find a different session (and the subscription will be then reissued)
        // other exceptions are "static" exceptions that means that should not arise after the development phase is closed
        // (note that our metadata adapter does not deny anything to anyone)
        
        synchronized(this) {
            if (subscriptionPhase != this.phase) {
                //something changed the phase (maybe a stop call) while we were waiting; exit
                return false;
            }
            
            try {
                client.subscribeTable(toSub.table, toSub.listener, false);
                return true;
            } catch (SubscrException e) {
            } catch (PushServerException e) {
            } catch (PushUserException e) {
            } catch (PushConnException e) {
            }
        }

        return false;
    }
    
    
    /**
     * this class will receive the notifications on the status of the connection. 
     * Such information will be used to automatically reconnect and to send the status 
     * to a listener
     */
    class LSConnectionListener implements ConnectionListener {
        
        private boolean isPolling;
        private int ph = 0;        

        public LSConnectionListener(int phase) {
            super();
            this.ph = phase;
        }

        private void onConnection() {
            if (this.isPolling) {
                changeStatus(this.ph, POLLING);
            } else {
                changeStatus(this.ph, STREAMING);
            }
        }
        
        private void onDisconnection() {
            changeStatus(this.ph,DISCONNECTED);
            start(this.ph);
        }

        @Override
        public void onActivityWarning(boolean warningOn) {
            if (warningOn) {
                changeStatus(this.ph, STALLED);
            } else {
                this.onConnection();
            }
        }

        @Override
        public void onClose() {
            this.onDisconnection();
        }
        
        @Override
        public void onConnectionEstablished() {   
        }
        
        @Override
        public void onDataError(PushServerException e) {
        }
        
        @Override
        public void onEnd(int cause) {
            this.onDisconnection();
        }

        @Override
        public void onFailure(PushServerException e) {
            this.onDisconnection();
        }

        @Override
        public void onFailure(PushConnException e) {
            this.onDisconnection();
        }
        
        @Override
        public void onNewBytes(long b) {
        }
        
        @Override
        public void onSessionStarted(boolean isPolling) {
            this.isPolling = isPolling;
            this.onConnection();
        }

    }
    
    private class ConnectionThread extends Thread {
        private final int ph;

        public ConnectionThread(int ph) {
            this.ph = ph;
        }

        public void run() {
            execute(this.ph);
        }
    }
    
    class TableAndListener {
        ExtendedTableInfo table;
        HandyTableListener listener;
        
        public TableAndListener(ExtendedTableInfo table,
                HandyTableListener listener) {
           this.table = table;
           this.listener = listener;
        }
    }
    
    public interface StatusListener {
        public void onStatusChange(int status);
    }
}


