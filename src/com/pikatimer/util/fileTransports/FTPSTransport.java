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
import javafx.concurrent.Task;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 *
 * @author jcgarner
 */
public class FTPSTransport implements FileTransport{
    
    String basePath;
    OutputPortal parent;
    
    Thread transferThread;
    FTPSClient ftpClient;
    
    private static final BlockingQueue<String> transferQueue = new ArrayBlockingQueue(100000);

    private static final Map<String,String> transferMap = new ConcurrentHashMap();
    
    String hostname;
    String username;
    String password;

    @Override
    public boolean isOK() {
        if (password.isEmpty() || username.isEmpty() || hostname.isEmpty() || basePath.isEmpty()) return false;
        return true;
    }

    @Override
    public void save(String filename, String contents) {
        System.out.println("FTPSTransport.save() called for " + filename);
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
    
    }    

    public FTPSTransport() {
        
        Task transferTask = new Task<Void>() {

                @Override 
                public Void call() {
                   

                    System.out.println("FTPSTransport: new result processing thread started");
                    String filename;
                    
                    // connect to the remote server
                    ftpClient = new FTPSClient(false);
                    ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
                    
                    
                    
                    
                    // now let's process files
                    
                    while(true) {
                        
                        
                        try {
                            while(true) {
                                System.out.println("FTPSTransport Thread: Waiting for a file to ...");
                                //filename = transferQueue.poll(60, TimeUnit.SECONDS);
                                filename = transferQueue.take(); // blocks until

                                System.out.println("FTPSTransport Thread: Transfering " + filename);
                                String contents = transferMap.get(filename);
                                    
                                
                                if (!ftpClient.isConnected()) openConnection();

                                //InputStream data = IOUtils.toInputStream(contents, "UTF-8");
                                InputStream data = IOUtils.toInputStream(contents);
                                
                                long start_time = System.nanoTime();
                                ftpClient.storeFile(filename, data);
                                long end_tiome = System.nanoTime();

                                data.close();
                                transferMap.remove(filename, contents); 
                                System.out.println("FTPSTransport Thread: transfer of " + filename + " done in " + DurationFormatter.durationToString(Duration.ofNanos(end_tiome-start_time), 3, false, RoundingMode.HALF_EVEN));
                            }

                        } catch (InterruptedException ex) {
                            System.out.println("FTPSTransport Thread: InterruptedException thrown");

                            //Logger.getLogger(FTPSTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.out.println("FTPSTransport Thread: IOException thrown");
                            //Logger.getLogger(FTPSTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            if (ftpClient.isConnected()) {
                                try {
                                    System.out.println("FTPSTransport Thread: calling ftpClient.disconnect()");

                                    ftpClient.disconnect();
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
            
            refreshConfig();
            System.out.println("FTPS Not connected, connecting...");
            // Connect to host
            ftpClient.connect(hostname);
            int reply = ftpClient.getReplyCode();
            if (FTPReply.isPositiveCompletion(reply)) {
                
                
                // Login
                if (ftpClient.login(username, password)) {

                    // Set protection buffer size
                    ftpClient.execPBSZ(0);
                    // Set data channel protection to private
                    ftpClient.execPROT("P");
                    // Enter local passive mode
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
                    //ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                    if(!ftpClient.changeWorkingDirectory(basePath)) {
                        reply = ftpClient.mkd(basePath);
                        if (!FTPReply.isPositiveCompletion(reply)) {
                            System.out.println("Unable to make remote dir " + basePath);
                            ftpClient.disconnect();
                        } else {
                            ftpClient.changeWorkingDirectory(basePath);
                        }
                    }
                } else {
                  System.out.println("FTP login failed");
                  ftpClient.disconnect();
                }
            } else {
              System.out.println("FTP connect to host failed");
              ftpClient.disconnect();
            }
        } catch (IOException ioe) {
            System.out.println("FTP client received network error");
            
        }
    }
}
