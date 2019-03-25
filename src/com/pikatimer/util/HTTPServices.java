/*
 * Copyright (C) 2019 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.util;

import com.pikatimer.PikaPreferences;
import com.pikatimer.Pikatimer;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import io.javalin.websocket.WsSession;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class HTTPServices {
    
    private static final List<WsSession> wsSessionList = new ArrayList();
    private Integer port = 8080;
    private final Javalin server = Javalin.create();
    private String url = "Not Available";
    
    public HTTPServices() {
        Boolean bound = false; 
        port = 8080;
        
        InetAddress localhost; 
        try {
            localhost = InetAddress.getLocalHost();
            System.out.println("System IP Address : " + (localhost.getHostAddress()).trim()); 
            url = "http://" + (localhost.getHostAddress()).trim();
        } catch (UnknownHostException ex) {
            Logger.getLogger(HTTPServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        // Lets start at 8080 and just walk up from there until we find a free port
        // but call it quits at 9,000 
        while(bound.equals(false) && port < 9000) {
            try {
                server.port(port).start();
                bound = true;
                url += ":" + port;
            } catch (Exception e) {
                port++;
            }
        }
        
        System.out.println("Web server listening on " + url); 
        
        // Setup the routes
        server.routes ( () -> {
                
                // Participant data
                path("/participants", () -> {
                    get( cx -> {
                        JSONArray p = new JSONArray();
                        JSONObject o = new JSONObject();

                        ParticipantDAO.getInstance().listParticipants().forEach(part -> {p.put(part.getJSONObject());});
                        o.put("Participants", p);
                        cx.result( o.toString(4));
                    });
                    path(":id", () -> {
                        get( cx -> {
                            cx.pathParamMap().keySet().forEach(k -> {
                                System.out.println("pathParam: " + k + " -> " + cx.pathParam(k));
                            });
                            System.out.println("Requesting participant with bib " + cx.pathParam("id"));
                            Participant p = ParticipantDAO.getInstance().getParticipantByBib(cx.pathParam("id"));
                            if (p==null) {
                                System.out.println("No Participant found!");
                                cx.status(404);
                                cx.result("NOT_FOUND");
                            }
                            else {
                                cx.result(p.getJSONObject().toString(4));
                            }
                            
                        });  
                    });
                });    
        }); 
        
        
        // Setup a network discovery listener so others can find us
        // Borrowed from https://michieldemey.be/blog/network-discovery-using-udp-broadcast/
        Task discoveryThread = new Task<Void>() {
            @Override public Void call() {
                try {

                  //Keep a socket open to listen to all the UDP trafic that is destined for this port
                  DatagramSocket socket = new DatagramSocket(8080, InetAddress.getByName("0.0.0.0"));
                  socket.setBroadcast(true);

                    while (true) {
                        System.out.println(getClass().getName() + ">>>Ready to receive broadcast packets!");
                        //Receive a packet
                        byte[] recvBuf = new byte[15000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        socket.receive(packet);

                        //Packet received
                        System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                        System.out.println(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()).trim());
                        //See if the packet holds the right command (message)
                        String message = new String(packet.getData()).trim();
                        if (message.equals("DISCOVER_PIKA_REQUEST") && PikaPreferences.getInstance().getDBLoaded() ) {
                          byte[] sendData = url.getBytes();
                          DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                          socket.send(sendPacket);
                          System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                        }
                    }
                } catch (IOException ex) {
                    System.out.println(ex.getStackTrace());
                }
                return null;
            }

        
        };
        Thread discovery = new Thread(discoveryThread);
        discovery.setDaemon(true);
        discovery.setName("HTTPD Discovery Thread");
        discovery.start();
    
    }
    
    public Integer port(){
        return port;
    }
    
    public String getUrl(){
        return url;
    }
    
    public void stopHTTPService(){
        server.stop();
    }
    
    
    
    
}
