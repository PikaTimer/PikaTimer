/* 
 * Copyright (C) 2017 John Garner
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
package com.pikatimer.timing;

import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
//import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author jcgarner
 */
public class TimingDAO {
        private static final ObservableList<TimingLocation> timingLocationList =FXCollections.observableArrayList(TimingLocation.extractor());
        private static final BlockingQueue<RawTimeData> rawTimeQueue = new ArrayBlockingQueue(100000);
        private static final BlockingQueue<CookedTimeData> cookedTimeQueue = new ArrayBlockingQueue(100000);
        private static final ResultsDAO resultsDAO = ResultsDAO.getInstance();
        private static final BlockingQueue<String> resultsQueue = ResultsDAO.getInstance().getResultsQueue();
        
        private static ObservableList<CookedTimeData> cookedTimeList;
        private static final Map<String,List<CookedTimeData>> cookedTimeBibMap = new ConcurrentHashMap();
        private Bib2ChipMap bib2ChipMap;
        
        private static ObservableList<TimeOverride> overrideList;
        private  Map<String,List<TimeOverride>> overrideMap = new ConcurrentHashMap(); 
        
        Semaphore cookedTimeProcessorSemaphore = new Semaphore(1);
        private static final CountDownLatch locationsLoadedLatch = new CountDownLatch(1);

    public List<CookedTimeData> getCookedTimesByBib(String bib) {
        if (cookedTimeBibMap.containsKey(bib))
            return cookedTimeBibMap.get(bib); 
        return new ArrayList();
    }
        
        
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final TimingDAO INSTANCE = new TimingDAO();
    }

    public static TimingDAO getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public BlockingQueue<RawTimeData> getRawTimeQueue () {
        return rawTimeQueue; 
    }
    public Collection getRawTimes(TimingLocationInput tli) {
        List<RawTimeData> list = new ArrayList<>();
        Set<RawTimeData> set = new HashSet<>(); 

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the getRawTimes Query for " + tli.getLocationName());
        
        try {  
            list=s.createQuery("from RawTimeData where timingLocationInputId = :tli_id").setParameter("tli_id", tli.getID()).list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        return list; 
    }
    
      
    public void saveRawTimes(RawTimeData r) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(r);
        s.getTransaction().commit();
    }
    
    public void clearRawTimes(TimingLocationInput tli) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Deleting all raw times for " + tli.getLocationName());
        
        try {  
            s.createQuery("delete from RawTimeData where timingLocationInputId = :tli_id").setParameter("tli_id", tli.getID()).executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        tli.clearLocalReads();
        // Now go and clean up the cooked times too
        clearCookedTimes(tli); 
    }
    
    public void clearRawTimes(TimingLocation tl) {
        if (tl == null || tl.inputsProperty().isEmpty()) return;
        tl.getInputs().stream().forEach(tli -> {
            clearRawTimes(tli);
        });
    }
    
    
    
    public ObservableList<CookedTimeData> getCookedTimes() {
        
        // if the cooked time list is null, then let's create one,
        // fetch fetch existing datapoints from the DB,
        // and add them to the cookedTimesList;
        
        if (cookedTimeList == null) {
            cookedTimeList =FXCollections.observableArrayList(CookedTimeData.extractor());
            
            Task fetchCookedFromDB = new Task<Void>() {
                
                @Override public Void call() {
                   final List<CookedTimeData> cookedTimes = new ArrayList(); 
                   
                   Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                   s.beginTransaction();
                   System.out.println("Runing the getCookedTimes querry");

                    try {  
                        cookedTimes.addAll(s.createQuery("from CookedTimeData").list());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    } 
                    s.getTransaction().commit();  
                    
                    Platform.runLater(() -> {
                        cookedTimeList.addAll(cookedTimes);
                    });
                    cookedTimes.stream().forEach(c -> {
                        if (!cookedTimeBibMap.containsKey(c.getBib())) cookedTimeBibMap.put(c.getBib(), new ArrayList());
                        if (!c.ignoreTimeProperty().get()) cookedTimeBibMap.get(c.getBib()).add(c);
                    });
                    
                    return null; 
                }
            }; 
            new Thread(fetchCookedFromDB).start();
            
            
        
            
            
            Task processNewCooked = new Task<Void>() {
                @Override public Void call() {
                    
                    while(true) {
                        List<CookedTimeData> pending = new ArrayList();
                        List<CookedTimeData> newTimes = new ArrayList();
                        try {
                            cookedTimeProcessorSemaphore.acquire();
                            pending.add(cookedTimeQueue.take());
                        
                            //cookedTimeQueue.drainTo(pending,499);  // 500 total
                            Thread.sleep(10); // Times rarely come in 1 at a time
                            cookedTimeQueue.drainTo(pending);
                            System.out.println("ProcessNewCooked Thread: Processing: " + pending.size());

                            int i=1;
                            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                            s.beginTransaction();
                            int count = 0;
                            Iterator<CookedTimeData> addIterator = pending.iterator();
                            while (addIterator.hasNext()) {
                                CookedTimeData c = addIterator.next();
//                                if (c.getID() != null ) // previously saved
//                                    Platform.runLater(() -> {
//                                        cookedTimeList.remove(c); // remove for now to prevent duplicate entries
//                                    });
                                if (c.getID() == null) newTimes.add(c);
                                
                                s.saveOrUpdate(c); 
                                
                                if ( ++count % 20 == 0 ) {
                                    //flush a batch of updates and release memory:
                                    s.flush();
                                    s.clear();
                                }

                            }
                            s.getTransaction().commit(); 

                            Platform.runLater(() -> {
                                cookedTimeList.addAll(newTimes);
                            });
                            pending.stream().forEach(c -> {
                                if (!cookedTimeBibMap.containsKey(c.getBib())) cookedTimeBibMap.put(c.getBib(), new ArrayList());
                                if (c.ignoreTimeProperty().get()) cookedTimeBibMap.get(c.getBib()).remove(c);
                                else cookedTimeBibMap.get(c.getBib()).add(c);
                                resultsQueue.add(c.getBib());
                            });
                            
                            Thread.sleep(100); // This will limit us to being able to process about 5000 times / second
                        } catch (InterruptedException ex) {
                            Logger.getLogger(TimingDAO.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        cookedTimeProcessorSemaphore.release();
                    }

                }
            };
            Thread processNewCookedThread = new Thread(processNewCooked);
            processNewCookedThread.setName("Thread-ProcessNewCookedThread");
            processNewCookedThread.setDaemon(true);
            processNewCookedThread.setPriority(1);
            processNewCookedThread.start();
        }
    
        return cookedTimeList;
        
        
    }
    
    public void saveCookedTime(CookedTimeData c) {
        cookedTimeQueue.add(c); 
    }
    
    public void clearAllCookedTimes() {
        Task clearTimes = new Task<Void>() {
                
                @Override public Void call() {
                    try {
                        cookedTimeProcessorSemaphore.acquire();
                    
                        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                        s.beginTransaction();
                        System.out.println("Deleting all cooked times");

                        try {  
                            s.createQuery("delete from CookedTimeData").executeUpdate();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        } 
                        s.getTransaction().commit(); 

                        // Now go and clean up the cooked times observable list
                        Platform.runLater(() -> {
                            cookedTimeList.clear();
                        });

                        cookedTimeBibMap.clear();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TimingDAO.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    cookedTimeProcessorSemaphore.release();
                    resultsDAO.reprocessAllResults();
                    return null;
                }
        };
        Thread clearAllCookedThread = new Thread(clearTimes);
        clearAllCookedThread.setName("Thread-clearAllCookedThread");
        clearAllCookedThread.setPriority(1);
        clearAllCookedThread.start();
                    
    }
    
    public void clearCookedTimes(TimingLocation tl) {
        tl.getInputs().stream().forEach(tli -> {
            clearCookedTimes(tli);
        });
    }
    
    void blockingClearCookedTimes(TimingLocationInput tli) {
        if (Platform.isFxApplicationThread()) System.out.println("clockingClearCookedTimes called on FxApplicationThread!!!");
        List<CookedTimeData> toRemoveList = new ArrayList<>();
        cookedTimeList.stream().forEach(c -> { 
            if (Objects.equals(c.getTimingLocationInputId(), tli.getID()) ) {
                toRemoveList.add(c);
                if (cookedTimeBibMap.get(c.getBib()) != null) cookedTimeBibMap.get(c.getBib()).remove(c);
            }
         });
        Platform.runLater(() -> {
            cookedTimeList.removeAll(toRemoveList); 
        });
        toRemoveList.stream().forEach(c -> {
            if (!cookedTimeBibMap.containsKey(c.getBib())) cookedTimeBibMap.get(c.getBib()).remove(c);
            resultsQueue.add(c.getBib());
        });

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Deleting all cooked times for " + tli.getLocationName());

        try {  
            s.createQuery("delete from CookedTimeData where timingLocationInputId = :tli_id").setParameter("tli_id", tli.getID()).executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        System.out.println("Done deleting all cooked times for " + tli.getLocationName());

    
    }
    
    void clearCookedTimes(TimingLocationInput tli) {
        Task clearTimes = new Task<Void>() {
                
            @Override public Void call() {
                // Go and clean up the cooked times observable list
                blockingClearCookedTimes(tli);
                return null;
            }
        };
        Thread clearCookedThread = new Thread(clearTimes);
        clearCookedThread.setPriority(1);
        clearCookedThread.start();
    }
    
    public Map<String,List<CookedTimeData>> getCookedTimesMap() {
        return cookedTimeBibMap;
    }
    
    public BlockingQueue<CookedTimeData> getCookedTimeQueue() {
        return cookedTimeQueue; 
    }
    
    public void addTimingLocation(TimingLocation tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(tl);
        s.getTransaction().commit();
        //Platform.runLater(() -> {
            timingLocationList.add(tl);
        //});
        
    }
    
    public void createDefaultTimingLocations() {
            
            blockingClearAll();
            
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            // sql to set the name and date
            Query query = s.createSQLQuery("INSERT into TIMING_LOCATION (TIMING_LOCATION_ID, TIMING_LOCATION_NAME) values (:id, :name)");
            query.setParameter("id", 1);
            query.setParameter("name", "Start");
            query.executeUpdate();
            query.setParameter("id", 2);
            query.setParameter("name", "Finish");
            query.executeUpdate();            
            s.getTransaction().commit();
            
            // Thread.dumpStack(); // who called this?
            refreshTimingLocationList();
        }
    
    private void refreshTimingLocationList() { 

        List<TimingLocation> list = new ArrayList<>();
        

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the refreshTimingLocationList Query");
        
        try {  
            list=s.createQuery("from TimingLocation order by id").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        System.out.println("Returning the refreshTimingLocationList list");
        if(!timingLocationList.isEmpty())
            timingLocationList.clear();
        timingLocationList.addAll(list);
        locationsLoadedLatch.countDown();
    }     
    
    public ObservableList<TimingLocation> listTimingLocations() { 

        if (timingLocationList.isEmpty()) refreshTimingLocationList();
        return timingLocationList;
        //return list;
    }     
    
    public TimingLocation getTimingLocationByID(Integer id) {
        try {
            //System.out.println("Looking for a timingLocation with id " + id);
            // This is ugly. Setup a map for faster lookups
            locationsLoadedLatch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(TimingDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        Optional<TimingLocation> result = timingLocationList.stream()
                    .filter(t -> Objects.equals(t.getID(), id))
                    .findFirst();
        if (result.isPresent()) {
            //System.out.println("Found " + result.get().LocationNameProperty());
            return result.get();
        } 
        
        return null;
    }

    public void removeTimingLocation(TimingLocation tl) {
        
        clearRawTimes(tl);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(tl);
        s.getTransaction().commit(); 
        timingLocationList.remove(tl);
    }      
    
    public void clearAllRawTimes() {
//        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
//        s.beginTransaction();
//        System.out.println("Deleting all Raw times");
//        
//        try {  
//            s.createQuery("delete from RawTimeData").executeUpdate();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        } 
//        s.getTransaction().commit();
        
        //Clearing the raw times also clears the associated cooked times
        timingLocationList.forEach(t -> clearRawTimes(t));
        
    }
    
    public void clearAllTimes() {
        clearAllRawTimes();
        //clearAllCookedTimes();
    }
    
    public void reprocessAllRawTimes(){
        timingLocationList.forEach(t -> {
            System.out.println("TimingDAO::reprocessAllRawTimes: " + t.getLocationName());
            t.reprocessReads();
        });
    }
    
//    public void clearAll() {
//        clearAllTimes(); 
//        removeTimingLocations(timingLocationList);
//    }
//    private void removeTimingLocations(ObservableList<TimingLocation> removeList) {
//        
//        Task task;
//        task = new Task<Void>() {
//            @Override public Void call() {
//                int max = removeList.size();
//                int i=1;
//                Session s=HibernateUtil.getSessionFactory().getCurrentSession();
//                s.beginTransaction();
//                int count = 0;
//                Iterator<TimingLocation> deleteMeIterator = removeList.iterator();
//                while (deleteMeIterator.hasNext()) {
//                    TimingLocation p = deleteMeIterator.next();
//                    
//                    s.delete(p); 
//                    if ( ++count % 20 == 0 ) {
//                        //flush a batch of updates and release memory:
//                        s.flush();
//                        s.clear();
//                    }
//                    updateProgress(i++, max);
//                }
//                s.getTransaction().commit(); 
//                
//                Platform.runLater(() -> {
//                    refreshTimingLocationList();
//                });
//                
//                return null;
//            }
//        };
//        new Thread(task).start();
//    }   
    public void blockingClearAll() {
        clearAllTimes(); 
        blockingRemoveTimingLocations(timingLocationList);
    }
    public void blockingRemoveTimingLocations(ObservableList<TimingLocation> removeList) {
        int max = removeList.size();
        int i=1;
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<TimingLocation> deleteMeIterator = removeList.iterator();
        while (deleteMeIterator.hasNext()) {
            TimingLocation p = deleteMeIterator.next();
            
            s.delete(p); 
            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }

        }
        s.getTransaction().commit(); 

        //Platform.runLater(() -> {
                refreshTimingLocationList();
        //    });
                

    }  
    
    public void updateTimingLocation(TimingLocation tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(tl);
        s.getTransaction().commit();
     } 
    
    public void addTimingLocationInput(TimingLocationInput t) {
        if (t.getTimingLocation() != null) t.getTimingLocation().addInput(t);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(t);
        s.getTransaction().commit();
      
    }
    public void updateTimingLocationInput(TimingLocationInput t) {
        
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(t);
        //s.update(t);
        s.getTransaction().commit();
      
    }
    public void removeTimingLocationInput(TimingLocationInput t) {
        // clear out any raw times
        clearRawTimes(t); 
        // clear out any cooked times
        clearCookedTimes(t); 
        // unlink it from the timing location
        if (t.getTimingLocation() != null) t.getTimingLocation().removeInput(t);
        // get it out of the db
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(t);
        s.getTransaction().commit();
      
    }
    
//    public void timingLocationInputPauseReader(TimingLocationInput tl) {
//        //Set a flag so that the 
//    }
//    public void timingLocationInputStartReader(TimingLocationInput tl) {
//        
//        
//    }   
    
    public String getBibFromChip(String c) {
        if (bib2ChipMap == null) {
            bib2ChipMap = getBib2ChipMap();
        }
        return bib2ChipMap.getBibFromChip(c);
    }
    
    public Bib2ChipMap getBib2ChipMap() {
        if (bib2ChipMap != null) return bib2ChipMap;
        
        List<Bib2ChipMap> mapList = new ArrayList();
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the getBib2ChipMap querry");

         try {  
             mapList = s.createQuery("from Bib2ChipMap").list();
         } catch (Exception e) {
             System.out.println(e.getMessage());
         } 
         s.getTransaction().commit();  
         if(mapList.size()>0) {
             bib2ChipMap = mapList.get(0);
         } else {
             bib2ChipMap = new Bib2ChipMap();
         }
         return bib2ChipMap;
    }
    
    public void saveBib2ChipMap(Bib2ChipMap b) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(b);
        s.getTransaction().commit();
    }
    public void updateBib2ChipMap(Bib2ChipMap b) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.update(b);
        s.getTransaction().commit();
    }
    
    
    
    
    
    public ObservableList<TimeOverride> getOverrides() {
        
        // if the cooked time list is null, then let's create one,
        // fetch fetch existing datapoints from the DB,
        // and add them to the cookedTimesList;
        
        if (overrideList == null) {
            overrideList =FXCollections.observableArrayList(TimeOverride.extractor());
            
            Task fetchOverrideFromDB = new Task<Void>() {
                
                @Override public Void call() {
                   final List<TimeOverride> dbOverrides = new ArrayList(); 
                   
                   Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                   s.beginTransaction();
                   System.out.println("Runing the getOverrides querry");

                    try {  
                        dbOverrides.addAll(s.createQuery("from TimeOverride").list());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    } 
                    s.getTransaction().commit();  
                    
                    Platform.runLater(() -> {
                        overrideList.addAll(dbOverrides);
                    });
                    dbOverrides.stream().forEach(c -> {
                        if (!overrideMap.containsKey(c.getBib())) overrideMap.put(c.getBib(), new ArrayList());
                        overrideMap.get(c.getBib()).add(c);
                    });
                    
                    return null; 
                }
            }; 
            new Thread(fetchOverrideFromDB).start();
            
            
        }
    
        return overrideList;
        
        
    }
    
    public Optional<List<TimeOverride>> getOverridesByBib(String bib) {
        if(overrideMap.containsKey(bib) && ! overrideMap.get(bib).isEmpty() ){
            return Optional.of(overrideMap.get(bib));
        } else return Optional.empty();
        
    }
    
    public void saveOverride(TimeOverride o) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(o);
        s.getTransaction().commit();
        
        // Just in case.... This will make sure we initialized the overrideList
        getOverrides();
        
        
        
        if (!overrideMap.containsKey(o.getBib())) overrideMap.put(o.getBib(), new ArrayList()); 
        
        
        // If this is an edit, don't add it again
        if (!overrideMap.get(o.getBib()).contains(o)) {
            // if this is an overwrite, remove the old one first
            try {
                TimeOverride old = overrideMap.get(o.getBib()).stream().filter(e -> e.getSplitId().equals(o.getSplitId())).findFirst().get();
                deleteOverride(old);
            }
            catch (Exception e) {

            }
            overrideMap.get(o.getBib()).add(o);
        }
        
        if (!overrideList.contains(o)) Platform.runLater(() -> {
             overrideList.add(o);
        });
        
        // Check to see if this one replaces another
        
        
        resultsQueue.add(o.getBib());
    }
    
    public void reprocessBib(String bib){
        resultsQueue.add(bib);
    }
    
    public void deleteOverride(TimeOverride o) {
        // Just in case.... This will make sure we initialized the overrideList
        getOverrides();
        
        Platform.runLater(() -> {
            overrideList.remove(o);
        });
        overrideMap.get(o.getBib()).remove(o);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(o);
        s.getTransaction().commit();
        resultsQueue.add(o.getBib());
    }
    
    public void clearAllOverrides(){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Deleting all overrides...");
        overrideList.forEach(o -> resultsQueue.add(o.getBib()));
        try {  
            s.createQuery("delete from TimeOverride").executeUpdate();
            Platform.runLater(() -> {
                overrideList.clear();
            });
            overrideMap = new ConcurrentHashMap();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit();
    }
}
