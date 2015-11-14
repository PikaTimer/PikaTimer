/*
 *  Copyright 2014 John Garner. All rights reserved. 

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
//import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        private static final BlockingQueue<String> resultsQueue = ResultsDAO.getInstance().getResultsQueue();
        
        private static ObservableList<CookedTimeData> cookedTimeList;
        private static final Map<String,List<CookedTimeData>> cookedTimeBibMap = new ConcurrentHashMap();
        private Bib2ChipMap bib2ChipMap;

    public List<CookedTimeData> getCookedTimesByBib(String bib) {
        return cookedTimeBibMap.get(bib); 
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
        tl.getInputs().stream().forEach(tli -> {
            clearRawTimes(tli);
        });
    }
    
    
    
    public ObservableList<CookedTimeData> getCookedTimes() {
        
        // if the cooked time list is null, then let's create one,
        // fetch fetch existing datapoints from the DB,
        // and add them to the cookedTimesList;
        
        if (cookedTimeList == null) {
            cookedTimeList =FXCollections.observableArrayList();
            
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
                        cookedTimeBibMap.get(c.getBib()).add(c);
                    });
                    
                    return null; 
                }
            }; 
            new Thread(fetchCookedFromDB).start();
            
            
        
            
            
            Task processNewCooked = new Task<Void>() {
                @Override public Void call() {
                    
                    while(true) {
                        List<CookedTimeData> pending = new ArrayList();
                        try {
                            pending.add(cookedTimeQueue.take());
                        
                            cookedTimeQueue.drainTo(pending,99);  // 100 total
                            
                            System.out.println("ProcessNewCooked Thread: Processing: " + pending.size());

                            int i=1;
                            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                            s.beginTransaction();
                            int count = 0;
                            Iterator<CookedTimeData> addIterator = pending.iterator();
                            while (addIterator.hasNext()) {
                                CookedTimeData c = addIterator.next();
                                s.save(c); 
                                if ( ++count % 20 == 0 ) {
                                    //flush a batch of updates and release memory:
                                    s.flush();
                                    s.clear();
                                }

                            }
                            s.getTransaction().commit(); 

                            Platform.runLater(() -> {
                                cookedTimeList.addAll(pending);
                            });
                            pending.stream().forEach(c -> {
                                if (!cookedTimeBibMap.containsKey(c.getBib())) cookedTimeBibMap.put(c.getBib(), new ArrayList());
                                cookedTimeBibMap.get(c.getBib()).add(c);
                                resultsQueue.add(c.getBib());
                            });
                            
                            Thread.sleep(100); // This will limit us to being able to process about 1000 times / second
                        } catch (InterruptedException ex) {
                            Logger.getLogger(TimingDAO.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            };
            Thread processNewCookedThread = new Thread(processNewCooked);
            processNewCookedThread.setDaemon(true);
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
                    return null;
                }
        };
        Thread processNewCookedThread = new Thread(clearTimes);
        processNewCookedThread.start();
                    
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
    
    }
    
    void clearCookedTimes(TimingLocationInput tli) {
        Task clearTimes = new Task<Void>() {
                
            @Override public Void call() {
                // Go and clean up the cooked times observable list
                blockingClearCookedTimes(tli);
                return null;
            }
        };
        Thread processNewCookedThread = new Thread(clearTimes);
        processNewCookedThread.start();
    }
    
    public Map<String,List<CookedTimeData>> getCookedTimesMap() {
        return cookedTimeBibMap;
    }
    
    public BlockingQueue<CookedTimeData> getCookedTimeQueue () {
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
    
    public void refreshTimingLocationList() { 

        List<TimingLocation> list = new ArrayList<>();
        

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the refreshTimingLocationList Query");
        
        try {  
            list=s.createQuery("from TimingLocation").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        System.out.println("Returning the refreshTimingLocationList list");
        if(!timingLocationList.isEmpty())
            timingLocationList.clear();
        timingLocationList.addAll(list);
    }     
    
    public ObservableList<TimingLocation> listTimingLocations() { 

        if (timingLocationList.isEmpty()) refreshTimingLocationList();
        return timingLocationList;
        //return list;
    }     
    
    public TimingLocation getTimingLocationByID(Integer id) {
        //System.out.println("Looking for a timingLocation with id " + id);
        // This is ugly. Setup a map for faster lookups
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
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Deleting all Raw times");
        
        try {  
            s.createQuery("delete from RawTimeData").executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit();
        
    }
    
    public void clearAllTimes() {
        clearAllRawTimes();
        clearAllCookedTimes();
    }
    
    public void clearAll() {
        clearAllTimes(); 
        removeTimingLocations(timingLocationList);
    }
    private void removeTimingLocations(ObservableList<TimingLocation> removeList) {
        
        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
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
                    updateProgress(i++, max);
                }
                s.getTransaction().commit(); 
                
                Platform.runLater(() -> {
                    refreshTimingLocationList();
                });
                
                return null;
            }
        };
        new Thread(task).start();
    }   
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
    
    public void timingLocationInputPauseReader(TimingLocationInput tl) {
        //Set a flag so that the 
    }
    public void timingLocationInputStartReader(TimingLocationInput tl) {
        
        
    }   
    
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
}
