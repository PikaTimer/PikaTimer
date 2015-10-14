/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.timing.Split;
import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Session;

/**
 *
 * @author jcgarner
 */
public class RaceDAO {
    private static final ObservableList<Race> raceList =FXCollections.observableArrayList();
    private static final ObservableList<Wave> waveList =FXCollections.observableArrayList();
    private static final Map<Integer,Wave> waveMap = new HashMap();
    
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
    

    
    public void addSplit (Split w) {
        Race r = w.getRace();
        r.addSplit(w);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(w);
        s.getTransaction().commit();
        System.out.println("Adding Split id: " + w.getID() + "to" + w.getRace().getRaceName());
        updateSplitOrder(r);
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
        
        System.out.println("RaceDAO::refreshRaceList() Returning the list");
        if(!raceList.isEmpty())
            raceList.clear();
        raceList.addAll(list);
    }     
    
    public ObservableList<Race> listRaces() { 
        if(raceList.size() < 1)  refreshRaceList();
        return raceList;
    }      
    
    
    public void addWave(Wave w) {
        if (w.getRace() != null) w.getRace().addWave(w);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(w);
        s.getTransaction().commit();
        System.out.println("Adding Wave id: " + w.getID() + "to" + w.getRace().getRaceName());
        waveList.add(w); 
        waveMap.put(w.getID(), w);
    }
    
    public void removeWave(Wave w) {
        
        w.getRace().removeWave(w); 
        //refreshWaveList();         
        System.out.println("removeWaves before: waveList.size()= " + waveList.size());
        System.out.println("Wave: " + w.idProperty());
        waveList.forEach(e -> {System.out.println("Possible: " + e.idProperty() + " " + e.equals(w));});
        Boolean res = waveList.remove(w);
        Wave remove = waveMap.remove(w.getID());
        System.out.println("removeWaves after: waveList.size()= " + waveList.size() + " result: " + res);

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(w);
        s.getTransaction().commit();
    }
    
    public void updateWave (Wave w) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(w);
        s.getTransaction().commit();
                //refreshWaveList(); 

    } 
    
    public Wave getWaveByID(int id) {
        return waveMap.get(id); 
    }
    
    
    public void refreshWaveList() { 

        waveList.clear();
        waveMap.clear();
        raceList.forEach( r -> {
            waveList.addAll(r.getWaves()); 
        });
        waveList.forEach(w -> {
                waveMap.put(w.getID(), w);
        });
    }     
    
    public ObservableList<Wave> listWaves() { 
        if(waveList.size() < 1)  refreshWaveList();
        System.out.println("ListWaves for " + waveList.size());
        //refreshWaveList(); 
        
        return waveList; //.sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString()));
    } 
    
    public void removeRace(Race tl) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(tl);
        s.getTransaction().commit(); 
        raceList.remove(tl);
    }      
    

    
    public void removeSplit(Split w) {
        Race r = w.getRace(); 
        r.removeSplit(w);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(w);
        s.getTransaction().commit();
        updateSplitOrder(r);
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

    
    public void updateSplit (Split sp) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(sp);
        s.getTransaction().commit();
     }
    
    public void updateSplitOrder(Race r) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        r.splitsProperty().stream().forEach((item) -> {
            //System.out.println(self.getRaceName() + " has " + item.getSplitName() + " at " + raceSplits.indexOf(item));
            item.splitPositionProperty().set(r.splitsProperty().indexOf(item)+1);
            s.update(item);
        });
        s.getTransaction().commit();
    }
}
