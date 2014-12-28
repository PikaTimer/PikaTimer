/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Session;

/**
 *
 * @author jcgarner
 */
public class RaceDAO {
            private static final ObservableList<Race> raceList =FXCollections.observableArrayList();

    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final RaceDAO INSTANCE = new RaceDAO();
    }

    public static RaceDAO getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public void addRace(Race r) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(r);
        s.getTransaction().commit();
        raceList.add(r);
    }
    
    public void addWave(Wave w) {
        Race r = w.getRace();
        r.addWave(w);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(w);
        //s.save(r);
        s.getTransaction().commit();
        
        System.out.println("Adding Wave id: " + w.getID() + "to" + w.getRace().getRaceName());
    }
    
    public void refreshRaceList() { 

        List<Race> list = new ArrayList<>();
        

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("RacedAO.refreshRaceList() Starting the query");
        
        try {  
            list=s.createQuery("from Race").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        System.out.println("Returning the list");
        if(!raceList.isEmpty())
            raceList.clear();
        raceList.addAll(list);
    }     
    
    public ObservableList<Race> listRaces() { 
        if(raceList.size() < 1)  refreshRaceList();
        return raceList;
        //return list;
    }      

    public void removeRace(Race tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(tl);
        s.getTransaction().commit(); 
        raceList.remove(tl);
    }      
    
    public void removeWave(Wave w) {
        Race r = w.getRace(); 
        r.removeWave(w);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(w);
        //s.update(r);
        s.getTransaction().commit();
        
    }
 
    public void clearAll() {
        removeRaces(raceList);
        addRace(new Race()); 
    }
    public void removeRaces(ObservableList<Race> removeList) {

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<Race> deleteMeIterator = removeList.iterator();
        while (deleteMeIterator.hasNext()) {
            Race p = deleteMeIterator.next();
            s.delete(p); 
            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }

        }
        s.getTransaction().commit(); 

        //Platform.runLater(() -> {
                refreshRaceList();
        //    });
    }  
    
    public void updateRace(Race tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(tl);
        s.getTransaction().commit();
     }
    public void updateWave (Wave w) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(w);
        s.getTransaction().commit();
     }
}
