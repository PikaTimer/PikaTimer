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
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ResultsDAO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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
    private static final BlockingQueue<String> eventQueue = new ArrayBlockingQueue(100000);
    
    private static Map<WsSession,Set<String>> announcerDupeCheckHash = new HashMap();
    private static Set<String> announcerDupeCheckSet = new HashSet();
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final HTTPServices INSTANCE = new HTTPServices();
    }

    public static HTTPServices getInstance() {
        
            return SingletonHolder.INSTANCE;
    }
    
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
        
        //server.enableCaseSensitiveUrls();
        
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
        
        setupHTTPDRoutes();
        startDiscoveryListener();
        startEventQueueProcessor();
    
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
    
    
    
    private void startDiscoveryListener(){
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
        discovery.setName("Marmot Discovery Listener Thread");
        discovery.start();
    }
    
    public void publishEvent(String category, JSONObject event){
        System.out.println("WebSocket Publish Event: " + category + ":" + event);
        
        eventQueue.add(new JSONObject().put(category, event).toString());
    }
    
    public void publishEvent(String category, String event){
        
        System.out.println("WebSocket Publish Event: " + category + ":" + event);
        
        eventQueue.add(new JSONObject().put(category, event).toString());
        
    }
    
     private void startEventQueueProcessor(){

        Task eventThread = new Task<Void>() {
            @Override public Void call() {
                while(true) {
                    String save = "";
                    try {
                        while(true) {
                            System.out.println("HTTPServices: Waiting for events to publish");
                            String m = eventQueue.poll(20, TimeUnit.SECONDS);
                            if (m == null) {
                                m = "{\"KEEPALIVE\":\"" + System.currentTimeMillis() + "\"}";
                            }
                            String message = m;
                            System.out.println("HTTPServices: Publishing Event");
                            wsSessionList.stream().forEach(session -> {
                                if(message.contains("PARTICIPANT") || message.contains("KEEPALIVE") || ! announcerDupeCheckHash.get(session).contains(message)) {
                                    announcerDupeCheckHash.get(session).add(message);
                                    try {
                                        System.out.println(" HTTPServices: Publishing Event  to " + session.getId() + " " + session.host());
                                        session.send(message);
                                        System.out.println(" HTTPServices: Successfuly published to " + session.getId() + " " + session.host());
                                    } catch (Exception e){
                                        eventQueue.add(message);
                                        System.out.println("Event Processor Exception: " + e.getMessage());
                                    }     
                                }
                            });
                        }
                    } catch (Exception ex) {
                        System.out.println("Event Processor Outer Exception: " + ex.getMessage());
                    }
                    
                    System.out.println("Marmot Event Processor Thread Ended!!!");
                    if (!save.isEmpty()) eventQueue.add(save);
                }
            }
        };
        Thread eventProcessor = new Thread(eventThread);
        eventProcessor.setDaemon(true);
        eventProcessor.setName("Marmot Event Processor Thread");
        eventProcessor.start();
    }

    private void setupHTTPDRoutes() {
        
        // Event Websocket
        server.ws("/eventsocket/", ws -> {
            ws.onConnect(session -> {

                session.setIdleTimeout(61000); // 61 second timeout
                
                wsSessionList.add(session);
                announcerDupeCheckHash.put(session, new HashSet());
                
                System.out.println("WebSocket Connected: " + session.host() + " Size: " + wsSessionList.size());
                
            });
            ws.onMessage((session, message) -> {
                System.out.println("Received: " + message);
                session.getRemote().sendString("Echo: " + message);
            });
            ws.onClose((session, statusCode, reason) -> {
                System.out.println("WebSocket: websocket session disconnected: " + session.host());
                if (wsSessionList.contains(session) ) {
                    wsSessionList.remove(session);
                } else {
                    System.out.println("WebSocket: Unknown websocket session disconnected: " + session.host());
                }
            });
            ws.onError((session, throwable) -> {
                if (wsSessionList.contains(session) ) {
                    System.out.println("WebSocket: websocket session disconnected: " + session.host());
                    wsSessionList.remove(session);
                } else {
                    System.out.println("WebSocket: Unknown websocket session disconnected: " + session.host());
                }
                System.out.println("WebSocket Error: " + session.host());
            });
        });
        
        // Setup the routes
        server.routes ( () -> {
                
                // Participant data
                path("/participants", () -> {
                    get( cx -> {
                        JSONArray p = new JSONArray();
                        JSONObject o = new JSONObject();

                        ParticipantDAO.getInstance().listParticipants().forEach(part -> {p.put(part.getJSONObject());});
                        o.put("Participants", p);
                        cx.contentType("application/json; charset=utf-8");
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
                // Result Data
                path("/results", () -> {
                    get( cx -> {
                        JSONArray p = new JSONArray();
                        JSONObject o = new JSONObject();
                        RaceDAO.getInstance().listRaces().forEach(r -> {
                            ResultsDAO.getInstance().getResults(r.getID()).forEach(res -> {
                                
                                if (!res.isEmpty() && res.getFinish()>0) {
                                    String race = "";
                                    if (RaceDAO.getInstance().listRaces().size() > 1) 
                                        race = RaceDAO.getInstance().getRaceByID(res.getRaceID()).getRaceName();
                                    String bib = res.getBib();
                                    String time = DurationFormatter.durationToString(res.getFinishDuration().minus(res.getStartDuration()), "[HH:]MM:SS");
                                    JSONObject json = new JSONObject();
                                    json.put("Bib", bib);
                                    json.put("Race", race);
                                    json.put("Time", time);
                                    //System.out.println("/ Results -> " + bib + " -> " + time);
                                    p.put(json);
                                }
                            });
                            o.put("Results", p);
                        });
                        cx.result( o.toString(4));
                    });
                });
                
              
                                
        }); 
    }
    
    public Javalin getServer(){
        return server;
    }
}
