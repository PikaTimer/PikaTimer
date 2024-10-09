/*
 * Copyright (C) 2024 John Garner <segfaultcoredump@gmail.com>
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
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.ResultsDAO;
import io.javalin.Javalin;
//import static io.javalin.apibuilder.ApiBuilder.get;
//import static io.javalin.apibuilder.ApiBuilder.path;
import io.javalin.websocket.WsContext;
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
import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class HTTPServices {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HTTPServices.class);

    private static final List<WsContext> wsSessionList = new ArrayList();
    private Integer port = 8080;
    private final Javalin server = Javalin.create();
    private String url = "Not Available";
    private static final BlockingQueue<String> eventQueue = new ArrayBlockingQueue(100000);
    
    private static Map<WsContext,Set<String>> announcerDupeCheckHash = new HashMap();
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
            logger.info("System IP Address : " + (localhost.getHostAddress()).trim()); 
            url = "http://" + (localhost.getHostAddress()).trim();
        } catch (UnknownHostException ex) {
            logger.error("Error in InetAddress.getLocalHost()", ex);
        }
        
        //server.enableCaseSensitiveUrls();
        
        // Lets start at 8080 and just walk up from there until we find a free port
        // but call it quits at 9,000 
        while(bound.equals(false) && port < 9000) {
            try {
                //server.port(port).start();
                server.start(port);
                bound = true;
                url += ":" + port;
            } catch (Exception e) {
                port++;
            }
        }
        
        logger.info("Web server listening on " + url); 
        
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
                        logger.info("DiscoveryListener:  Ready to receive broadcast packets!");
                        //Receive a packet
                        byte[] recvBuf = new byte[15000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        socket.receive(packet);

                        //Packet received
                        logger.debug(">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                        logger.debug(">>>Packet received; data: " + new String(packet.getData()).trim());
                        //See if the packet holds the right command (message)
                        String message = new String(packet.getData()).trim();
                        if (message.equals("DISCOVER_PIKA_REQUEST") && PikaPreferences.getInstance().getDBLoaded() ) {
                          byte[] sendData = url.getBytes();
                          DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                          socket.send(sendPacket);
                          logger.debug(">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                        }
                    }
                } catch (IOException ex) {
                    logger.debug("Discovery Listener Exception",ex);
                }
                return null;
            }

        
        };
        Thread discovery = new Thread(discoveryThread);
        discovery.setDaemon(true);
        discovery.setName("PikaAnnouncer Discovery Listener Thread");
        discovery.start();
    }
    
    public void publishEvent(String category, JSONObject event){
        logger.debug("WebSocket Publish Event: " + category + ":" + event);
        
        eventQueue.add(new JSONObject().put(category, event).toString());
    }
    
    public void publishEvent(String category, String event){
        
        logger.debug("WebSocket Publish Event: " + category + ":" + event);
        
        eventQueue.add(new JSONObject().put(category, event).toString());
        
    }
    
     private void startEventQueueProcessor(){

        Task eventThread = new Task<Void>() {
            @Override public Void call() {
                while(true) {
                    String save = "";
                    try {
                        while(true) {
                            logger.debug("HTTPServices: Waiting for events to publish");
                            String m = eventQueue.poll(20, TimeUnit.SECONDS);
                            if (m == null) {
                                m = "{\"KEEPALIVE\":\"" + System.currentTimeMillis() + "\"}";
                            }
                            String message = m;
                            logger.debug("HTTPServices: Publishing Event");
                            wsSessionList.stream().forEach(ctx -> {
                                if(message.contains("PARTICIPANT") || message.contains("KEEPALIVE") || ! announcerDupeCheckHash.get(ctx).contains(message)) {
                                    announcerDupeCheckHash.get(ctx).add(message);
                                    try {
                                        logger.debug(" HTTPServices: Publishing Event  to " + ctx.sessionId() + " " + ctx.host());
                                        ctx.send(message);
                                        logger.trace(" HTTPServices: Successfuly published to " + ctx.sessionId() + " " + ctx.host());
                                    } catch (Exception e){
                                        eventQueue.add(message);
                                        logger.warn("Event Processor Exception: " + e.getMessage());
                                    }     
                                }
                            });
                        }
                    } catch (Exception ex) {
                        logger.warn("Event Processor Outer Exception: " + ex.getMessage());
                    }
                    
                    logger.info("Marmot Event Processor Thread Ended!!!");
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
            ws.onConnect(ctx -> {
                ctx.enableAutomaticPings();
                
                wsSessionList.add(ctx);
                announcerDupeCheckHash.put(ctx, new HashSet());
                
                logger.debug("WebSocket Connected: " + ctx.host() + " Size: " + wsSessionList.size());
                
            });
            ws.onClose(session -> {
                wsSessionList.remove(session);
            
            });
        });
        
        // Setup the routes
        server.get("/participants/{id}", ctx -> {

                            logger.debug("Requesting participant with bib " + ctx.pathParam("id"));
                            Participant p = ParticipantDAO.getInstance().getParticipantByBib(ctx.pathParam("id"));
                            if (p==null) {
                                logger.debug("No Participant found!");
                                ctx.status(404);
                                ctx.result("NOT_FOUND");
                            }
                            else {
                                ctx.json(p.getJSONObject());
                            }
        });
        server.get("/participants", ctx -> {
                        JSONArray p = new JSONArray();
                        JSONObject o = new JSONObject();

                        ParticipantDAO.getInstance().listParticipants().forEach(part -> {p.put(part.getJSONObject());});
                        o.put("Participants", p);
                        //ctx.contentType("application/json; charset=utf-8");
                        ctx.json(o);
        
        });
        
        server.get("/results",ctx -> {
            JSONArray p = new JSONArray();
            JSONObject o = new JSONObject();
            ResultsDAO resDAO = ResultsDAO.getInstance();
            RaceDAO.getInstance().listRaces().forEach(r -> {
                resDAO.getResults(r.getID()).forEach(res -> {

                    if (!res.isEmpty() && res.getFinish()>0) {
                        String race = "";
                        if (RaceDAO.getInstance().listRaces().size() > 1) 
                            race = RaceDAO.getInstance().getRaceByID(res.getRaceID()).getRaceName();
                        String bib = res.getBib();
                        //String time = DurationFormatter.durationToString(res.getFinishDuration().minus(res.getStartDuration()), "[HH:]MM:SS");
                        ProcessedResult pr = resDAO.processResult(res,r);
                        String time = DurationFormatter.durationToString(pr.getChipFinish(), "[HH:]MM:SS");

                        JSONObject json = new JSONObject();
                        json.put("Bib", bib);
                        json.put("Race", race);
                        json.put("Time", time);
                        //logger.debug("/ Results -> " + bib + " -> " + time);
                        p.put(json);
                    }
                });
                o.put("Results", p);
            });
            ctx.json(o);
        });
    }
    
    public Javalin getServer(){
        return server;
    }
}
