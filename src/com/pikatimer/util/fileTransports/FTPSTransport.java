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
import com.pikatimer.results.Result;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.FileTransport;
import com.pikatimer.util.HibernateUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.hibernate.Session;

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
        
        if (transferThread == null || ftpClient == null || !ftpClient.isConnected() ) transferFile(); // kicks off the thread
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

    private void transferFile() {
        
        Task transferTask = new Task<Void>() {

                @Override 
                public Void call() {
                   
                    refreshConfig();

                    System.out.println("FTPSTransport: new result processing thread started");
                    String filename;
                    
                    // connect to the remote server
                    ftpClient = new FTPSClient(false);
                    ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
                    
                    try {
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
                            }
                        } else {
                          System.out.println("FTP connect to host failed");
                        }
                    } catch (IOException ioe) {
                      System.out.println("FTP client received network error");
                    }
                    
                    
                    // now let's process files
                    
                    while(ftpClient.isConnected()) {
                        try {
                            System.out.println("FTPSTransport Thread: Waiting for a file to ...");
                            filename = transferQueue.poll(60, TimeUnit.SECONDS);
                            String file = transferMap.get(filename);
                           
                            InputStream data = IOUtils.toInputStream(file, "UTF-8");
                            
                            System.out.println("FTPSTransport Thread: The wait is over...");

                            ftpClient.storeFile(filename, data);
                            
                            data.close();
                            transferMap.remove(filename, file);
                            
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ResultsDAO.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(FTPSTransport.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            if (ftpClient.isConnected()) {
                                try {
                                    ftpClient.disconnect();
                                } catch (IOException f) {
                                    // do nothing
                                }
                            }
                        }
                    }
                    return null;
                }
            };
            transferThread = new Thread(transferTask);
            transferThread.setName("Thread-FTPS-");
            transferThread.setDaemon(true);
            transferThread.start();
        
    }
}
