/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class TimingLocationDAO {
        private static final ObservableList<TimingLocation> timingLocationList =FXCollections.observableArrayList(TimingLocation.extractor());

    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final TimingLocationDAO INSTANCE = new TimingLocationDAO();
    }

    public static TimingLocationDAO getInstance() {
            return SingletonHolder.INSTANCE;
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
        System.out.println("Runing the Query");
        
        try {  
            list=s.createQuery("from TimingLocation").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        System.out.println("Returning the list");
        if(!timingLocationList.isEmpty())
            timingLocationList.clear();
        timingLocationList.addAll(list);
    }     
    
    public ObservableList<TimingLocation> listTimingLocations() { 

        refreshTimingLocationList();
        return timingLocationList;
        //return list;
    }      

    public void removeTimingLocation(TimingLocation tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(tl);
        s.getTransaction().commit(); 
        timingLocationList.remove(tl);
    }      
    
    public void clearAll() {
        removeTimingLocations(timingLocationList);
    }
    public void removeTimingLocations(ObservableList<TimingLocation> removeList) {
        
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
                
                //Platform.runLater(() -> {
                refreshTimingLocationList();
                //    });
                
                return null;
            }
        };
        new Thread(task).start();
    }   
    public void blockingClearAll() {
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
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(t);
        s.getTransaction().commit();
      
    }
}
