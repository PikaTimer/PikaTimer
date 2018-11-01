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
import com.jcraft.jsch.SftpProgressMonitor;
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
import java.util.concurrent.CountDownLatch;
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

                                System.out.println("SFTPTransport Thread: Preping for transfer of  " + filename);
                                String contents = transferMap.get(filename);
                                
                                while (fatalError || sshSession == null || !sshSession.isConnected() || sftpChannel == null || !sftpChannel.isConnected()) {
                                    if (!fatalError ) openConnection();
                                    if (fatalError || !sftpChannel.isConnected()) {
                                        System.out.println("SFTPTransport Thread: Still not connected, sleeping for 10 seconds...");
                                        Thread.sleep(10000);
                                    }
                                    
                                }
                                System.out.println("SFTPTransport Thread: Transfering " + filename);




                                InputStream data = IOUtils.toInputStream(contents, "UTF-8");
                                //InputStream data = IOUtils.toInputStream(contents);
                                String fn = filename;
                                Platform.runLater(() -> { 
                                    transferStatus.set("Transfering " + fn);
                                });
                                long startTime = System.nanoTime();
                                
                                
                                try {
                                    SFTPTransferMonitor monitor = new SFTPTransferMonitor();
                                    sftpChannel.put(data, filename, monitor);
                                    monitor.await();

                                    long endTime = System.nanoTime();
                                    lastTransferTimestamp = endTime;

                                    data.close();
                                    transferMap.remove(filename, contents); 
                                    System.out.println("SFTPTransport Thread: transfer of " + filename + " done in " + DurationFormatter.durationToString(Duration.ofNanos(endTime-startTime), 3, false, RoundingMode.HALF_EVEN));
                                    filename = null;
                                } catch (SftpException ex) {
                                    System.out.println("SftpException: " + ex.getLocalizedMessage());
                                    throw new IOException(ex.getLocalizedMessage());
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
                            System.out.println("SFTPTransport Thread: Generic Exception tossed: " );
                            ex.printStackTrace();
                            
                        } finally {
                            if (sftpChannel != null && sftpChannel.isConnected()) sftpChannel.disconnect();
                            if (sshSession != null && sshSession.isConnected()) {
                                System.out.println("SFTPTransport Thread: calling sshSession.disconnect()"); // do nothing
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
            if(hostname.contains(":")){
                System.out.println("Explicit Port Specified...");
                String[] h = hostname.split(":");
                int port = Integer.parseInt(h[1]);
                sshSession = sshClient.getSession(username, h[0],port);
            } else {
                sshSession = sshClient.getSession(username, hostname);
            }
            
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

    @Override
    public void test(ReportDestination parent, StringProperty output) {
        Task transferTask = new Task<Void>() {

                @Override 
                public Void call() {
                    
                    password=parent.getPassword();
                    username=parent.getUsername();
                    hostname=parent.getServer();
                    basePath=parent.getBasePath();

                    Platform.runLater(() -> output.set(output.getValueSafe() + "Connecting to " + hostname +"..." ));
                    sshClient = new JSch();

                    // only for public key authentication
                    try {
                        if(hostname.contains(":")){
                            System.out.println("Explicit Port Specified...");
                            String[] h = hostname.split(":");
                            int port = Integer.parseInt(h[1]);
                            sshSession = sshClient.getSession(username, h[0],port);
                        } else {
                            sshSession = sshClient.getSession(username, hostname);
                        }
                        sshSession.setTimeout(10000); // 10 seconds

                        // we only support password authentication for now
                        sshSession.setPassword(password);

                        Platform.runLater(() -> {transferStatus.set("Loging in...");});
                        sshSession.setConfig("StrictHostKeyChecking", "no");
                        sshSession.connect();

                        Platform.runLater(() -> output.set(output.getValueSafe() + "\nConnected" ));
                        Platform.runLater(() -> output.set(output.getValueSafe() + "\nOpening SFTP Channel..." ));

                        sftpChannel = (ChannelSftp) sshSession.openChannel("sftp");
                        sftpChannel.connect();
                        Platform.runLater(() -> output.set(output.getValueSafe() + "\nOpened" ));
                        try {
                            Platform.runLater(() -> output.set(output.getValueSafe() + "\nChanging Directories..." ));
                            sftpChannel.cd(basePath);
                            Platform.runLater(() -> output.set(output.getValueSafe() + "\n\nSuccess!" ));
                            sftpChannel.disconnect();

                        } catch (SftpException ex) {
                            Platform.runLater(() -> output.set(output.getValueSafe() + "\nDirectory does not exist!" ));
                            try {
                                Platform.runLater(() -> output.set(output.getValueSafe() + "\nAttempting to make the target directory..." ));

                                sftpChannel.mkdir(basePath);
                                
                                Platform.runLater(() -> output.set(output.getValueSafe() + "\nCreated target directory" ));
                                Platform.runLater(() -> output.set(output.getValueSafe() + "\nChanging Directories..." ));

                                sftpChannel.cd(basePath);
                                Platform.runLater(() -> output.set(output.getValueSafe() + "\n\nSuccess!" ));
                            } catch (SftpException ex1) {
                                Platform.runLater(() -> output.set(output.getValueSafe() + "\nError: " + ex.getLocalizedMessage()+"\n\nTest Failed!"));
                            }
                            sftpChannel.disconnect();
                        }

                    } catch (Exception ex) {
                        Platform.runLater(() -> output.set(output.getValueSafe() + "\nError: " + ex.getLocalizedMessage()+"\n\nTest Failed!"));
                        //System.out.println(ex.getLocalizedMessage());
                        //Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    sshSession.disconnect();
                    return null;
                }
        
        };
        transferThread = new Thread(transferTask);
        transferThread.setName("Thread-SFTP-Transfer-Test");
        transferThread.setDaemon(true);
        transferThread.start();
    
    }
    
    class SFTPTransferMonitor implements SftpProgressMonitor {
        CountDownLatch latch = new CountDownLatch(1);
        long transferedBytes = 0L;

        public SFTPTransferMonitor() {;}

        public void init(int op, String src, String dest, long max) 
        {
            System.out.println("SFTP Transfer Starting: "+op+" "+src+" -> "+dest+" total: "+max);
        }

        public boolean count(long bytes){
            transferedBytes = bytes;
            return(true);
        }

        public void end()
        {
            latch.countDown();
            System.out.println("\nSFTP Transfer: DONE!");
        }
        
        public void await() throws IOException{
            long counter = 0L;
            try {
                while (latch.getCount() > 0) {
                    latch.await(30, TimeUnit.SECONDS);
                    if (counter == transferedBytes) // timeout 
                        throw new IOException("SFTP Transfer Timeout");
                    else counter = transferedBytes;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(SFTPTransport.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
