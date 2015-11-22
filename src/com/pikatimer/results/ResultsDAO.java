/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.results;

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.timing.CookedTimeData;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.HibernateUtil;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 *
 * @author jcgarner
 */
public class ResultsDAO {
    
    private static final BlockingQueue<String> resultsQueue = new ArrayBlockingQueue(100000);
    private Map<Integer,ObservableList<Result>> raceResultsMap;
    private static final Map<String,Map<Integer,Result>> resultsMap = new ConcurrentHashMap<>(); 
    private static final TimingDAO timingDAO = TimingDAO.getInstance();
    private static final ParticipantDAO participantDAO = ParticipantDAO.getInstance();
    private static final RaceDAO raceDAO = RaceDAO.getInstance();
        
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final ResultsDAO INSTANCE = new ResultsDAO();
    }

    public static ResultsDAO getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public BlockingQueue<String> getResultsQueue(){
        return resultsQueue;
    }
    
    public ObservableList<Result> getResults(Integer raceID) {
        
        // if the results list is null, then let's create one,
        // fetch fetch existing datapoints from the DB,
        // and add them to the resultsList;
        
        if (raceResultsMap == null) {
            raceResultsMap = new ConcurrentHashMap();
            
            if(!raceResultsMap.containsKey(raceID)) raceResultsMap.put(raceID, FXCollections.observableArrayList(Result.extractor()));

            Task processNewResult = new Task<Void>() {

                @Override public Void call() {
                   final List<Result> results = new ArrayList(); 

                   Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                   s.beginTransaction();
                   System.out.println("ResultsDAO: Runing the getResults querry");

                    try {  
                        results.addAll(s.createQuery("from Result").list());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    } 
                    s.getTransaction().commit();  
                    
                    results.stream().forEach(r -> {
                        //resultsMap.put(r.getBib() + " " + r.getRaceID(), r);
                        if (!resultsMap.containsKey(r.getBib())) resultsMap.put(r.getBib(), new HashMap());
                        resultsMap.get(r.getBib()).put(r.getRaceID(),r);
                        
                        if(!raceResultsMap.containsKey(r.getRaceID())) raceResultsMap.put(r.getRaceID(), FXCollections.observableArrayList(Result.extractor()));
                        
                        
                        Platform.runLater(() -> {
                            //raceResultsMap.get(r.getRaceID()).remove(r);
                            if(!r.isEmpty()){
                                raceResultsMap.get(r.getRaceID()).add(r);
                                System.out.println("ResultsDAO read from DB added " + r.getBib() + " from race " + r.getRaceID() + " new total " + raceResultsMap.get(r.getRaceID()).size() );
                            }
                        });

                    });

                    System.out.println("ResultsDAO: new result processing thread started");
                    while(true) {
                        Set<String> pendingBibs = new HashSet();
                        try {
                            
                            System.out.println("ResultsDAO ProcessNewResult Thread: Waiting for more bibs to process...");
                            pendingBibs.add(resultsQueue.take());
                            System.out.println("ResultsDAO ProcessNewResult Thread: The wait is over...");

                            resultsQueue.drainTo(pendingBibs,199);  // 200 total
                            
                            System.out.println("ProcessNewResult Thread: Processing: " + pendingBibs.size());

                            List<Result> pending = new ArrayList();
                            try {
                                pendingBibs.stream().forEach(pb -> {
                                    processBib(pb);
                                    if (!resultsMap.get(pb).keySet().isEmpty()) pending.addAll(resultsMap.get(pb).values());
                                    
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            } 
                            
                            try {
                                s = HibernateUtil.getSessionFactory().getCurrentSession();
                                s.beginTransaction();
                                int count = 0;
                                Iterator<Result> addIterator = pending.iterator();
                                while (addIterator.hasNext()) {
                                    Result c = addIterator.next();
                                    if (c.isEmpty() && c.getID() != null) {                                        
                                        s.delete(c);    
                                        c.setID(null);
                                    } else {                                        
                                        s.saveOrUpdate(c);                                        
                                    }
                                    if (++count % 20 == 0) {
                                        //flush a batch of updates and release memory:
                                        s.flush();
                                        s.clear();
                                    }
                                }
                                s.getTransaction().commit();                                
                            } catch (Exception e) {
                                e.printStackTrace();
                            } 

                            pending.stream().forEach(r -> {
                                
                                if(!raceResultsMap.containsKey(r.getRaceID())) raceResultsMap.put(r.getRaceID(), FXCollections.observableArrayList(Result.extractor()));

                                Platform.runLater(() -> {
                                    
                                    //if (r.isEmpty() && raceResultsMap.get(r.getRaceID()).contains(r)) raceResultsMap.get(r.getRaceID()).remove(r);
                                    
                                    if(!r.isEmpty()){
                                        if (!raceResultsMap.get(r.getRaceID()).contains(r)) {
                                            raceResultsMap.get(r.getRaceID()).add(r);
                                            System.out.println("ResultsDAO new/updated result added " + r.getBib() + " from race " + r.getRaceID() + " new total " + raceResultsMap.get(r.getRaceID()).size() );
                                        }// else r.setUpdated();
                                    }
                                    
                                    r.setUpdated();
                                });

                            });
                            
                            
                            Thread.sleep(100); // This will limit us to being able to process about 2000 results / second
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ResultsDAO.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        
                    }

                }
            };
            Thread processNewResultThread = new Thread(processNewResult);
            processNewResultThread.setDaemon(true);
            processNewResultThread.start();
            
     
        }
    
        
        if(!raceResultsMap.containsKey(raceID)) raceResultsMap.put(raceID, FXCollections.observableArrayList(Result.extractor()));

        //System.out.println("ResultsDAO.getResults for race ID " + raceID + " returning " + raceResultsMap.get(raceID).size());
        return raceResultsMap.get(raceID);
        
        
    }
    
    public void clearResultsByParticipant(Participant p){
        
    }
        
        
    public void reprocessAll(){
        resultsQueue.addAll(resultsMap.keySet());
    }    
    
    public void reprocessAll(Wave w) {
        participantDAO.listParticipants().stream().filter(p -> p.getWaveIDs().contains(w.getID())).forEach(p2 -> resultsQueue.add(p2.getBib()));
    }
    
    // This is absolutely ugly. I hope it works... 
    private void processBib(String bib){
        System.out.println("ResultsDAO.processBib: " + bib);
        //List<Result> resultsList = new ArrayList<>();
        
        if (!resultsMap.containsKey(bib)) {
            resultsMap.put(bib,new HashMap());
        } else {
            resultsMap.get(bib).keySet().forEach(k -> {
                resultsMap.get(bib).get(k).clearTimes();
            });
        }
        
        Participant p = participantDAO.getParticipantByBib(bib);
        if (p == null) return;
        System.out.println("Processing " + p.fullNameProperty());
        
        Set<Integer> waves = p.getWaveIDs();
        if (waves.isEmpty()) return;
        
        if (timingDAO.getCookedTimesByBib(bib) == null) return;
        
        List<CookedTimeData> timesList = new ArrayList(timingDAO.getCookedTimesByBib(bib));
        timesList.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
        
        //System.out.println("ResultsDAO.processBib: " + bib + " we have " + timesList.size() + " times");
        
        waves.forEach(i -> {
            System.out.println("Processing waveID " + i); 
            
            Result r = resultsMap.get(bib).get(i);
            
            if (r == null ) {
                r = new Result();
                r.setBib(bib);
                r.setRaceID(raceDAO.getWaveByID(i).getRace().getID());
                resultsMap.get(bib).put(i, r);
            }
            
            
            
            Duration waveStart = Duration.between(LocalTime.MIDNIGHT, RaceDAO.getInstance().getWaveByID(i).waveStartProperty());
            Duration maxWaveStart = waveStart.plus(Duration.ofHours(1)); 
            //System.out.println("ResultsDAO.processBib: " + r.getBib() + " waveStart: " + waveStart + " maxWaveStart" + maxWaveStart);
            List<Split> splits = raceDAO.getWaveByID(i).getRace().getSplits();
            
            r.setStartWaveStartDuration(waveStart);
            
            Iterator<CookedTimeData> times = timesList.iterator();
            Split[] splitArray = splits.toArray(new Split[splits.size()]); 
            Integer splitIndex = 0;
            CookedTimeData ctd = null;
            if (times.hasNext()) ctd = times.next();
            
            while(ctd != null) {
                //System.out.println("ResultsDAO.processBib: Looking at: " + r.getBib() + " " + ctd.getTimestamp());
                if (ctd.getTimestamp().compareTo(waveStart) < 0 ) {
                    if (times.hasNext()) ctd = times.next();
                    else ctd = null;
                    //System.out.println("ResultsDAO.processBib: tossing, before wave start");
                } else if (Objects.equals(ctd.getTimingLocationId(), splitArray[splitIndex].getTimingLocationID())) {
                    //System.out.println("ResultsDAO.processBib: timing Location ID's match!");

                    if (splitIndex == 0) { // start line

                         // consume times, record the last one up to maxWaveStart 
                         // or we find a different locationID
                        do { 
                            //System.out.println("ResultsDAO.processBib: start time: " + ctd.getTimestamp());

                            r.setStartDuration(ctd.getTimestamp());
                            if (times.hasNext()) ctd = times.next();
                            else ctd = null;
                        } while (ctd != null && (ctd.getTimingLocationId() == splitArray[splitIndex].getTimingLocationID() && ctd.getTimestamp().compareTo(maxWaveStart) < 0) );
                    } else if (splitIndex == splits.size() -1 ) { // finish line
                        //System.out.println("We made it to the finish line!");
                        r.setFinishDuration(ctd.getTimestamp());
                        break; // we are done!
                    } else {
                        // we matched a split. 
                        //System.out.println("We are at split " + splitIndex + " in " + ctd.getTimestamp());
                        r.setSplitTime(splitArray[splitIndex].getPosition(), ctd.getTimestamp());
                        
                        // TODO: Fix this 
                        Duration splitMax = ctd.getTimestamp().plusMinutes(10); 
                        
                        // now consume the rest of the hits at this split until we 
                        // either hit another location or hit the max
                        do { 
                            if (times.hasNext()) ctd = times.next();
                            else ctd = null;
                        } while (ctd != null && ctd.getTimingLocationId() == splitArray[splitIndex].getTimingLocationID() && ctd.getTimestamp().compareTo(splitMax) < 0 );
                        splitIndex++;
                    }
                } else { // walk the splitArray until we get a match
                    //System.out.println("   Bumping the split index up from " + splitIndex);
                    while (splitIndex < splits.size() && ctd.getTimingLocationId() != splitArray[splitIndex].getTimingLocationID()) splitIndex++;
                    //System.out.println("      to " + splitIndex);
                }
                
                if (splitIndex >= splits.size()) break; // failsafe to make sure we didn't run out of splits
                
            }
            
            //System.out.println("ResultsDAO.processBib: Final Result: " + r.getBib() + " " + r.getStartDuration() + " -> " + r.getFinishDuration());
            //resultsList.add(r); 
            //resultsMap.put(bib + " " + r.getRaceID(), r);
            
        });
        
        //return resultsList;
    }
}
