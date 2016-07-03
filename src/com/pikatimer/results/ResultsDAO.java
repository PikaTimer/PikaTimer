/* 
 * Copyright (C) 2016 John Garner
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
package com.pikatimer.results;

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.timing.CookedTimeData;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimeOverride;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.HibernateUtil;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
    private static final ObservableList<OutputPortal> outputPortalList = FXCollections.observableArrayList(OutputPortal.extractor());
    
    private static final BooleanProperty outputPortalListInitialized = new SimpleBooleanProperty(FALSE);
        
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

                @Override 
                public Void call() {
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
                                //System.out.println("ResultsDAO read from DB added " + r.getBib() + " from race " + r.getRaceID() + " new total " + raceResultsMap.get(r.getRaceID()).size() );
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
                                            //System.out.println("ResultsDAO new/updated result added " + r.getBib() + " from race " + r.getRaceID() + " new total " + raceResultsMap.get(r.getRaceID()).size() );
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
            processNewResultThread.setName("Thread-ProcessNewResultThread");
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
        //System.out.println("ResultsDAO.processBib: " + bib);
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
        //System.out.println("Processing " + p.fullNameProperty().getValueSafe());
        
        Set<Integer> waves = p.getWaveIDs();
        if (waves.isEmpty()) return;
        
        
        
        Optional<List<TimeOverride>> bibOverrides = timingDAO.getOverridesByBib(bib);
        if (timingDAO.getCookedTimesByBib(bib) == null && ! bibOverrides.isPresent()) {
            return;
        }
        
        Map<Integer,Duration> overrideMap = new HashMap();
        if (bibOverrides.isPresent()) {
            bibOverrides.get().forEach(to -> {
                if (!to.getRelative()) {
                    overrideMap.put(to.getSplitId(), to.getTimestamp());
                    //System.out.println("Override found for splitID of " + to.getSplitId() + " for " + to.getTimestamp());
                } else {
                    // we have to convert the relative -> actual time
                    //System.out.println("Relative override found for splitID of " + to.getSplitId() + " for " + to.getTimestamp());

                    overrideMap.put(to.getSplitId(), to.getTimestamp().negated());
                }
            });
        }
        
        List<CookedTimeData> timesList = new ArrayList(timingDAO.getCookedTimesByBib(bib));
        timesList.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
        
        //System.out.println("ResultsDAO.processBib: " + bib + " we have " + timesList.size() + " times");
        
        waves.forEach(i -> {
            //System.out.println("Processing waveID " + i); 
            
            Boolean hasOverrides = false;
            
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
            
            Duration[] overrides = new Duration[splits.size()];
            for (int o = 0; o  < splits.size(); o++) {
                overrides[o] = overrideMap.get(splitArray[o].getID());
                if (overrides[o] != null) {
                    //System.out.println("Found an override for split " + o + " of time " + overrides[o].toString());
                    hasOverrides = true;
                }
            }
            
            // do we have an override for the start?
            Boolean startTimeFound = false;
            if (hasOverrides && overrides[0] != null) {
                waveStart = overrides[0];
                r.setStartDuration(waveStart);
                r.setStartWaveStartDuration(waveStart);
                //System.out.println("Found start time override of " + overrides[0].toString());
                
                // Adjust all relative overrides to actual
                for (int o = 1; o < splits.size(); o++) {
                    if (overrides[o] != null && overrides[o].isNegative()) {
                        overrides[o] = waveStart.plus(overrides[o].negated());
                    }
                }
                
                startTimeFound = true;

                splitIndex = 1; 
            }
            
            // pre-fill any intermediate splits
//            if (hasOverrides) {
//                for (int o = 1; o < splits.size()-1; o++) {
//                    if (overrides[o] != null) {
//                        System.out.println("Found split time override of " + overrides[o].toString() + " for the " + o + " split");
//                        r.setSplitTime(o+1, overrides[o]);
//                    }
//                }
//            }
            
            // Finish overrides get taken care of after the main while loop.... 
            
            CookedTimeData ctd = null;
            if (times.hasNext()) ctd = times.next();
            
            
            
            while(ctd != null) {
                //System.out.println("ResultsDAO.processBib: Looking at: " + r.getBib() + " " + ctd.getTimestamp());
                
                // is there an override time for a future split that is before 
                // the time in the ctd? If so, advance to the split after that 
                // and consume the times
                if (hasOverrides) {
                    
                    if (!startTimeFound && splitIndex > 0) {
                        // Adjust all relative overrides to actual
                        
                        for (int o = 1; o < splits.size(); o++) {
                            if (overrides[o] != null && overrides[o].isNegative()) {
                                overrides[o] = r.getStartDuration().plus(overrides[o].negated());
                            }
                        }
                        startTimeFound = true;
                    }
                    
                    for (int ot = splitIndex; ot < splits.size(); ot++) {
                        if (overrides[ot] != null && !overrides[ot].isNegative() && overrides[ot].minusMinutes(10).compareTo(ctd.getTimestamp()) < 0) {
                            splitIndex= ot;
                            r.setSplitTime(splitArray[splitIndex].getPosition(), overrides[splitIndex]);
                            //System.out.println("Found an override for " + splitIndex + " that is too close to the current times");

                            // TODO: Fix this 
                            Duration splitMax = overrides[ot].plusMinutes(10); 

                            // now consume the rest of the hits at this split until we 
                            // hit the max
                            do { 
                                //System.out.println("Tossing ctd from " + ctd.getTimingLocationId() + " at " + ctd.getTimestamp());
                                if (times.hasNext()) ctd = times.next();
                                else ctd = null;
                            } while (ctd != null && ctd.getTimestamp().compareTo(splitMax) < 0 );
                            splitIndex++;
                            //System.out.println("splitIndex now set to " + splitIndex);
                        }
                    }
                } 
                
                
                if (ctd == null || splitIndex >= splits.size()) {
                    break; // we ate all of the cooked times
                } else if (hasOverrides && overrides[splitIndex] != null) {
                    //System.out.println("We have an override for " + splitIndex + " incrementing and moving on.");
                    r.setSplitTime(splitArray[splitIndex].getPosition(), overrides[splitIndex]);
                    splitIndex++; // we pre-filled the split times earlier
                } else if (ctd.getTimestamp().compareTo(waveStart) < 0 ) {
                    if (times.hasNext()) ctd = times.next();
                    else ctd = null;
                    //System.out.println("ResultsDAO.processBib: tossing ctd's that were before the wave start");
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
                        Duration splitMax = ctd.getTimestamp().plusMinutes(5); 
                        
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
                    Integer orgIndex  = splitIndex;
                    while (splitIndex < splits.size() && ctd.getTimingLocationId() != splitArray[splitIndex].getTimingLocationID()) splitIndex++;
                    //System.out.println("      to " + splitIndex);
                    
                    if (splitIndex == splits.size()) {
                        //System.out.println("oops, we hit the bottom, reset the splitIndex to " + orgIndex);
                        // Ok, so the current timing location is never used again. Odds are they just sat around
                        // there too long. Let's fix that
                        splitIndex = orgIndex;
                        Integer orgCTDLocation = ctd.getTimingLocationId() ;
                        do { 
                            if (times.hasNext()) ctd = times.next();
                            else ctd = null;
                        } while (ctd != null && orgCTDLocation == ctd.getTimingLocationId());
                    }
                    
                }
                
                if (splitIndex >= splits.size()) break; // failsafe to make sure we didn't run out of splits
                
            }
            
            // we processed every time so far
            // do we have an override for the finish?
            if (overrides[splits.size()-1] != null) {
                
                // Adjust all relative overrides to actual
                if (overrides[splits.size()-1].isNegative()) {
                    overrides[splits.size()-1] = r.getStartDuration().plus(overrides[splits.size()-1].negated());
                }
                //System.out.println("Found finish time override of " + overrides[splits.size()-1].toString());
                r.setFinishDuration(overrides[splits.size()-1]);
            }
           // System.out.println("ResultsDAO.processBib: Final Result: " + r.getBib() + " " + r.getStartDuration() + " -> " + r.getFinishDuration());
            //resultsList.add(r); 
            //resultsMap.put(bib + " " + r.getRaceID(), r);
            
        });
        
        //return resultsList;
    }
    
    
    
    
    public void saveRaceReport(RaceReport rr){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(rr);
        s.getTransaction().commit();
    }
            
    public void removeRaceReport(RaceReport rr){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(rr);
        s.getTransaction().commit(); 
    }
    
    public void saveOutputPortal(OutputPortal p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(p);
        s.getTransaction().commit();
        //Platform.runLater(() -> {
        if (!outputPortalList.contains(p)) outputPortalList.add(p);
        //});
        
    }
    
    public void refreshOutputPortalList() { 
        List<OutputPortal> list = new ArrayList<>();
        
        outputPortalListInitialized.setValue(TRUE);


        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the refreshOutputPortalList Query");

        try {  
            list=s.createQuery("from OutputPortal").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } 
        s.getTransaction().commit(); 

        System.out.println("Returning the refreshOutputPortalList list: " + list.size());
        outputPortalList.addAll(list);   

        

    }     
    
    public ObservableList<OutputPortal> listOutputPortals() { 

        if (!outputPortalListInitialized.get()) refreshOutputPortalList();
        return outputPortalList;
        //return list;
    }     
    
    public OutputPortal getOutputPortalByUUID(String id) {
        //System.out.println("Looking for a timingLocation with id " + id);
        // This is ugly. Setup a map for faster lookups
        if (!outputPortalListInitialized.get()) refreshOutputPortalList();
        Optional<OutputPortal> result = outputPortalList.stream()
                    .filter(t -> Objects.equals(t.getUUID(), id))
                    .findFirst();
        if (result.isPresent()) {
            //System.out.println("Found " + result.get().LocationNameProperty());
            return result.get();
        } 
        
        return null;
    }
    
    public OutputPortal getOutputPortalByID(Integer id) {
        //System.out.println("Looking for a timingLocation with id " + id);
        // This is ugly. Setup a map for faster lookups
        if (!outputPortalListInitialized.get()) refreshOutputPortalList();
        Optional<OutputPortal> result = outputPortalList.stream()
                    .filter(t -> Objects.equals(t.getID(), id))
                    .findFirst();
        if (result.isPresent()) {
            //System.out.println("Found " + result.get().LocationNameProperty());
            return result.get();
        } 
        
        return null;
    }

    public void removeOutputPortal(OutputPortal op) {
        
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(op);
        s.getTransaction().commit(); 
        outputPortalList.remove(op);
    }
    
    
    
    public void saveRaceReportOutputTarget(RaceOutputTarget t) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(t);
        s.getTransaction().commit(); 
    }

    public void removeRaceReportOutputTarget(RaceOutputTarget t) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(t);
        s.getTransaction().commit(); 
    }
    
    
    
    public void processAllReports(){
        raceDAO.listRaces().forEach(r -> {processReports(r);});
    }
    
    public void processReports(Race r){
        List<ProcessedResult> results = new ArrayList();
        
        Integer splitSize = r.getSplits().size();
        
        // get the current results list
        getResults(r.getID()).forEach(res -> {
            ProcessedResult pr = new ProcessedResult();
            
            // If there is no participant, then bail. 
            // TODO: Maybe add an option to create a participant on the fly, but
            // this could gete messy with all of the random RFID chips out there.
            // Either way, this would be handled on the timing tab, not here. 
            if(participantDAO.getParticipantByBib(res.getBib()) == null) return; 
            
            // Link in the participant
            pr.setParticipant(participantDAO.getParticipantByBib(res.getBib()));
            // Set the AG code (e.g. M30-34) (age and gender are set automagically)
            pr.setAge(pr.getParticipant().getAge());
            pr.setAGCode(r.getAgeGroups().ageToAGString(pr.getAge()));
            
            // set the start and wave start times
            Duration chipStartTime = res.getStartDuration();
            Duration waveStartTime = res.getStartWaveStartDuration();
            
            // Set the start duration
            pr.setChipStartTime(chipStartTime);
            pr.setWaveStartTime(waveStartTime);
            
            //if(chipStartTime.equals(waveStartTime)) System.out.println("Chip == Wave Start for " + res.getBib());
            
            // Set the finish times
            if(res.getFinishDuration() != null && ! res.getFinishDuration().isZero()){
                pr.setChipFinish(res.getFinishDuration().minus(chipStartTime));
                pr.setGunFinish(res.getFinishDuration().minus(waveStartTime));
            }
            
            // Set the splits
            if(r.getSplits().size() > 2) {
                for (int i = 2; i <  splitSize ; i++) {
                    //if (res.getSplitTime(i) != null) pr.setSplit(i,res.getSplitTime(i).minus(chipStartTime));
                    if (! res.getSplitTime(i).isZero()) pr.setSplit(i,res.getSplitTime(i).minus(chipStartTime));
                }
            }
            
            results.add(pr);
        });
        
        // sort it by finish, then last completed split
        results.sort(null); // ProcessedResult iplements the Comparable interface
        
        // calculate placement in Overall, Gender, AG
        Map<String,Integer> placementCounter = new HashMap();
        placementCounter.put("overall", 1);
        placementCounter.put("M",1);
        placementCounter.put("F",1);
        
        results.forEach(pr -> {
            pr.setOverall(placementCounter.get("overall"));
            placementCounter.put("overall", pr.getOverall()+1);
            
            pr.setSexPlace(placementCounter.get(pr.getSex()));
            placementCounter.put(pr.getSex(), pr.getSexPlace()+1);
            
            placementCounter.putIfAbsent(pr.getSex()+pr.getAGCode(), 1);
            pr.setAGPlace(placementCounter.get(pr.getSex()+pr.getAGCode()));
            placementCounter.put(pr.getSex()+pr.getAGCode(),pr.getAGPlace()+1);
            
//            System.out.println("Results: " + r.getRaceName() + ": "
//                    + pr.getParticipant().fullNameProperty().getValueSafe() 
//                    + "(" + pr.getSex() + pr.getAGCode() + "): " 
//                    + DurationFormatter.durationToString(pr.getChipFinish())
//                    + " O:" + pr.getOverall() + " S:" + pr.getSexPlace() 
//                    + " AG:" + pr.getAGPlace()
//            );
        
        });
        
        
        // for each report, feed it the results list
        r.raceReportsProperty().forEach(rr ->{
            rr.processResult(results);
        });
        
        
    }
    
    
    
}
