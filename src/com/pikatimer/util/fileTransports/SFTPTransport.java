/*
 * Copyright (C) 2017 jcgarner
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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.pikatimer.results.ReportDestination;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.FileTransport;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;


/**
 *
 * @author jcgarner
 */
public class SFTPTransport implements FileTransport{
    
    String basePath;
    ReportDestination parent;
    Boolean stripAccents = false;

    Thread transferThread;
    JSch sshClient;
    Session sshSession;
    ChannelSftp sftpChannel;
    
    private static final BlockingQueue<String> transferQueue = new ArrayBlockingQueue(100000);

    private static final Map<String,String> transferMap = new ConcurrentHashMap();
    
    StringProperty transferStatus = new SimpleStringProperty("Idle");
    
    String hostname;
    String username;
    String password;

    Boolean fatalError = false;
    Boolean needConfigRefresh = true;
    Long lastTransferTimestamp = 0L;

    public SFTPTransport() {
        
        Task transferTask = new Task<Void>() {

                @Override 
                public Void call() {
                   

                    System.out.println("SFTPTransport: new result processing thread started");
                    String filename = null;
                    while(true) {
                        try {
                            System.out.println("SFTPTransport Thread: Waiting for the first file...");
                            if (filename == null) filename = transferQueue.take();
                            
                            while(true) {
                                System.out.println("SFTPTransport Thread: Waiting for a file...");
                                //filename = transferQueue.poll(60, TimeUnit.SECONDS);
                                if (sshClient == null || sshSession == null || !sshSession.isConnected()) Platform.runLater(() -> {transferStatus.set("Idle");});
                                else {
                                    sshSession.sendKeepAliveMsg();
                                    
                                    Platform.runLater(() -> {
                                        transferStatus.set("Connected");
                                    });
                                }
                                
                                //filename = transferQueue.take(); // blocks until
                                if (filename == null) filename = transferQueue.poll(15, TimeUnit.SECONDS);
                                if (filename == null) {
                                    // If we have been idle for more than 2 minutes, be nice and drop the connection
                                    if (TimeUnit.NANOSECONDS.toSeconds((System.nanoTime()-lastTransferTimestamp))> 120 ) break;
                                    else continue;
                                }

                                System.out.println("SFTPTransport Thread: Transfering " + filename);
                                String contents = transferMap.get(filename);
                                
                                while (fatalError || sftpChannel == null || !sftpChannel.isConnected()) {
                                    if (!fatalError) openConnection();
                                    if (fatalError || !sftpChannel.isConnected()) {
                                        System.out.println("SFTPTransport Thread: Still not connected, sleeping for 10 seconds...");
                                        Thread.sleep(10000);
                                    }
                                    
                                }
                                



                                //InputStream data = IOUtils.toInputStream(contents, "UTF-8");
                                InputStream data = IOUtils.toInputStream(contents);
                                String fn = filename;
                                Platform.runLater(() -> { 
                                    transferStatus.set("Transfering " + fn);
                                });
                                long startTime = System.nanoTime();
                                
                                
                                try {
                                    sftpChannel.put(data, filename);

                                    long endTime = System.nanoTime();
                                    lastTransferTimestamp = endTime;

                                    data.close();
                                    transferMap.remove(filename, contents); 
                                    System.out.println("SFTPTransport Thread: transfer of " + filename + " done in " + DurationFormatter.durationToString(Duration.ofNanos(endTime-startTime), 3, false, RoundingMode.HALF_EVEN));
                                    filename = null;
                                } catch (SftpException ex) {
                                    throw new IOException(ex);
                                } 
                            }

                        } catch (InterruptedException ex) {
                            System.out.println("SFTPTransport Thread: InterruptedException thrown");
                            //if (filename!= null) transferQueue.put(filename);

                            //Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.out.println("SFTPTransport Thread: IOException thrown");
                            //if (filename!= null) transferQueue.put(filename);
                            //Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            System.out.println("SFTPTransport Thread: Exception tossed: " + ex.getMessage());
                        } finally {
                            if (sshSession.isConnected()) {
                                System.out.println("SFTPTransport Thread: calling ftpClient.disconnect()"); // do nothing
                                sshSession.disconnect();
                                Platform.runLater(() -> {transferStatus.set("Disconnected");});
                            }
                        }
                    }
                    
                }
            };
            transferThread = new Thread(transferTask);
            transferThread.setName("Thread-SFTP-Transfer");
            transferThread.setDaemon(true);
            transferThread.start();
        
    }
    
    private void openConnection(){
            
        if (needConfigRefresh) refreshConfig();
        System.out.println("SFTP Not connected, connecting...");
        Platform.runLater(() -> {
            transferStatus.set("Connecting SFTP...");
        });
        // connect to the remote server

        sshClient = new JSch();

        // only for public key authentication
        try {
            sshSession = sshClient.getSession(username, hostname);
            sshSession.setTimeout(10000); // 10 seconds

            // we only support password authentication for now
            sshSession.setPassword(password);

            Platform.runLater(() -> {transferStatus.set("Loging in...");});
            sshSession.setConfig("StrictHostKeyChecking", "no");
            sshSession.connect();

            Platform.runLater(() -> {transferStatus.set("Connected");});

            sftpChannel = (ChannelSftp) sshSession.openChannel("sftp");
            sftpChannel.connect();
            try {
                sftpChannel.cd(basePath);
                fatalError=false;
            } catch (SftpException ex) {
                try {
                    sftpChannel.mkdir(basePath);
                    sftpChannel.cd(basePath);
                    fatalError=false;
                } catch (SftpException ex1) {
                    Platform.runLater(() -> {transferStatus.set("Error: Unabe to make target directory");});
                    fatalError=true;
                    Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex1);
                }
                Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (Exception ex) {
            Platform.runLater(() -> {transferStatus.set("Error: " + ex.getLocalizedMessage());});
            System.out.println(ex.getLocalizedMessage());
            fatalError=true;
            Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
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
        System.out.println("SFTPTransport.save() called for " + filename);
        if (stripAccents) contents = StringUtils.stripAccents(contents);
        transferMap.put(filename,contents);
        if (! transferQueue.contains(filename)) transferQueue.add(filename);
    }

    @Override
    public void setOutputPortal(ReportDestination op) {
        parent=op;
    }

    @Override
    public void refreshConfig() {
        
        // Get the hostname, username, password, basePath
        password=parent.getPassword();
        username=parent.getUsername();
        hostname=parent.getServer();
        basePath=parent.getBasePath();
        if (sshSession != null && sshSession.isConnected()) {
            System.out.println("SFTPTransport::refreshConfig: calling ftpClient.disconnect()"); // do nothing
            sshSession.disconnect();
        }
        
        stripAccents = parent.getStripAccents();
        

                    
        fatalError=false;
        needConfigRefresh = false;
    
    }    
}
