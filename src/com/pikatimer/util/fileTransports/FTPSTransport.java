/*
 * Copyright (C) 2016 jcgarner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.util.fileTransports;

import com.pikatimer.results.OutputPortal;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.FileTransport;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 *
 * @author jcgarner
 */
public class FTPSTransport implements FileTransport{
    
    String basePath;
    OutputPortal parent;
    Boolean stripAccents = false;

    Thread transferThread;
    FTPSClient ftpClient;
    
    private static final BlockingQueue<String> transferQueue = new ArrayBlockingQueue(100000);

    private static final Map<String,String> transferMap = new ConcurrentHashMap();
    
    StringProperty transferStatus = new SimpleStringProperty("Idle");
    
    String hostname;
    String username;
    String password;

    Boolean fatalError = false;
    Boolean needConfigRefresh = true;
    Long lastTransferTimestamp = 0L;

    public FTPSTransport() {
        
        Task transferTask = new Task<Void>() {

                @Override 
                public Void call() {
                   

                    System.out.println("FTPSTransport: new result processing thread started");
                    String filename = null;
                    
                    // connect to the remote server
                    ftpClient = new FTPSClient(false);
                    ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
                    
                    
                    
                    // now let's process files
                    
                    while(true) {
                        try {
                            System.out.println("FTPSTransport Thread: Waiting for the first file...");
                            if (filename == null) filename = transferQueue.take();
                            
                            while(true) {
                                System.out.println("FTPSTransport Thread: Waiting for a file...");
                                //filename = transferQueue.poll(60, TimeUnit.SECONDS);
                                if (!ftpClient.isConnected()) Platform.runLater(() -> {transferStatus.set("Idle");});
                                else {
                                    ftpClient.sendNoOp();
                                    Platform.runLater(() -> {
                                        if (ftpClient.getEnabledProtocols() != null) 
                                        transferStatus.set("Connected FTPS");
                                        else transferStatus.set("Connected");
                                    });
                                }
                                
                                //filename = transferQueue.take(); // blocks until
                                if (filename == null) filename = transferQueue.poll(15, TimeUnit.SECONDS);
                                if (filename == null) {
                                    // If we have been idle for more than 2 minutes, be nice and drop the connection
                                    if (TimeUnit.NANOSECONDS.toSeconds((System.nanoTime()-lastTransferTimestamp))> 120 ) break;
                                    else continue;
                                }

                                System.out.println("FTPSTransport Thread: Transfering " + filename);
                                String contents = transferMap.get(filename);
                                
                                while (!ftpClient.isConnected()) {
                                    if (!fatalError) openConnection();
                                    if (!ftpClient.isConnected()) {
                                        System.out.println("FTPSTransport Thread: Still not connected, sleeping for 10 seconds...");
                                        Thread.sleep(10000);
                                    }
                                    
                                }

                                //InputStream data = IOUtils.toInputStream(contents, "UTF-8");
                                InputStream data = IOUtils.toInputStream(contents);
                                String fn = filename;
                                Platform.runLater(() -> {transferStatus.set("Transfering " + fn);});
                                long startTime = System.nanoTime();
                                ftpClient.storeFile(filename, data);
                                long endTime = System.nanoTime();
                                lastTransferTimestamp = endTime;

                                data.close();
                                transferMap.remove(filename, contents); 
                                
                                System.out.println("FTPSTransport Thread: transfer of " + filename + " done in " + DurationFormatter.durationToString(Duration.ofNanos(endTime-startTime), 3, false, RoundingMode.HALF_EVEN));
                                filename = null;
                            }

                        } catch (InterruptedException ex) {
                            System.out.println("FTPSTransport Thread: InterruptedException thrown");
                            //if (filename!= null) transferQueue.put(filename);

                            //Logger.getLogger(FTPSTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.out.println("FTPSTransport Thread: IOException thrown");
                            //if (filename!= null) transferQueue.put(filename);
                            //Logger.getLogger(FTPSTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            System.out.println("FTPSTransport Thread: Exception tossed: " + ex.getMessage());
                        } finally {
                            if (ftpClient.isConnected()) {
                                try {
                                    System.out.println("FTPSTransport Thread: calling ftpClient.disconnect()");
                                    ftpClient.disconnect();
                                    Platform.runLater(() -> {transferStatus.set("Disconnected");});
                                } catch (IOException f) {
                                    // do nothing
                                }
                            }
                        }
                    }
                    
                }
            };
            transferThread = new Thread(transferTask);
            transferThread.setName("Thread-FTPS-Transfer");
            transferThread.setDaemon(true);
            transferThread.start();
        
    }
    
    private void openConnection(){
        try {
            
            if (needConfigRefresh) refreshConfig();
            System.out.println("FTPS Not connected, connecting...");
            Platform.runLater(() -> {transferStatus.set("Connecting...");});
            // Connect to host
            ftpClient.setConnectTimeout(10000); // 10 seconds
            ftpClient.connect(hostname);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) {
                //ftpClient.feat();
                
                // Login
                Platform.runLater(() -> {transferStatus.set("Loging in...");});

                if (ftpClient.login(username, password)) {

                    // Set protection buffer size
                    ftpClient.execPBSZ(0);
                    // Set data channel protection to private
                    ftpClient.execPROT("P");
                    // Enter local passive mode
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
                    //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    
                    Platform.runLater(() -> {transferStatus.set("Changing Directories...");});
                    if(!ftpClient.changeWorkingDirectory(basePath)) {
                        reply = ftpClient.mkd(basePath);
                        if (!FTPReply.isPositiveCompletion(reply)) {
                            System.out.println("Unable to make remote dir " + basePath);
                            Platform.runLater(() -> {transferStatus.set("Error: Unabe to make target directory");});
                            fatalError=true;
                            ftpClient.disconnect();
                        } else {
                            ftpClient.changeWorkingDirectory(basePath);
                        }
                    }
                    Platform.runLater(() -> {transferStatus.set("Connected");});
//                    String[] enabledProtocols = ftpClient.getEnabledProtocols();
//                    for (int i = 0; i < enabledProtocols.length; i++){
//                        System.out.println("FTPSClient Enabled Protocols: " + enabledProtocols[i]);
//                    }
                    
                    
                } else {
                  System.out.println("FTP login failed");
                  Platform.runLater(() -> {transferStatus.set("Error: Login Failed");});
                  fatalError=true;
                  ftpClient.disconnect();
                }
            } else {
              System.out.println("FTP connect to host failed");
              Platform.runLater(() -> {transferStatus.set("Error: Unable to connect to host");});

              ftpClient.disconnect();
            }
        } catch (IOException ioe) {
            System.out.println("FTP client received network error");
            Platform.runLater(() -> {transferStatus.set("Error: Network Error");});

        }
    }

    @Override
    public StringProperty statusProperty() {
        return transferStatus;
    }
     @Override
    public boolean isOK() {
        if (password.isEmpty() || username.isEmpty() || hostname.isEmpty() || basePath.isEmpty()) return false;
        return true;
    }

    @Override
    public void save(String filename, String contents) {
        System.out.println("FTPSTransport.save() called for " + filename);
        if (stripAccents) contents = StringUtils.stripAccents(contents);
        transferMap.put(filename,contents);
        if (! transferQueue.contains(filename)) transferQueue.add(filename);
        
        //if (transferThread == null || ftpClient == null || !ftpClient.isConnected() ) transferFile(); // kicks off the thread
    }

    @Override
    public void setOutputPortal(OutputPortal op) {
        parent=op;
    }

    @Override
    public void refreshConfig() {
        
        // Get the hostname, username, password, basePath
        password=parent.getPassword();
        username=parent.getUsername();
        hostname=parent.getServer();
        basePath=parent.getBasePath();
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                System.out.println("FTPSTransport::refreshConfig: calling ftpClient.disconnect()");
                ftpClient.disconnect();
            } catch (IOException f) {
                // do nothing
            }
        }
        
        stripAccents = parent.getStripAccents();
                    
        fatalError=false;
        needConfigRefresh = false;
    
    }    
}
