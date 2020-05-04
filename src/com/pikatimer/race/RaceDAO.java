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
package com.pikatimer.race;

import com.pikatimer.timing.Segment;
import com.pikatimer.timing.Split;
import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Session;

/**
 *
 * @author jcgarner
 */
public class RaceDAO {
    private static final ObservableList<Race> raceList =FXCollections.observableArrayList( e -> new Observable[] {e.raceNameProperty()});
    private static final ObservableList<Wave> waveList =FXCollections.observableArrayList(Wave.extractor());
    private static final Map<Integer,Wave> waveMap = new HashMap();
    private Map<Integer,Split> splitMap = new HashMap();
    
    // This is mostly precautionary just in case we put the initial race
    // loading into a background thread for whatever reason
    final CountDownLatch racesLoadedLatch = new CountDownLatch(1);

    public Split getSplitByID(Integer splitID) {
        return splitMap.get(splitID);
    }
    
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
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(w);
        s.getTransaction().commit();
        r.addSplit(w);
        //System.out.println("Adding Split id: " + w.getID() + "to" + w.getRace().getRaceName());
        updateSplitOrder(r);
        splitMap.put(w.getID(), w);
    }

    
    private void refreshRaceList() { 
        List<Race> list = new ArrayList<>();
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("RacedAO.refreshRaceList() Starting the query");
        
        try {  
            list=s.createQuery("from Race order by ID").list();
            if (list != null) list.forEach(r -> {
                AgeGroups ag = r.getAgeGroups();
                if (ag != null) ag.getCustomIncrementsList();
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        //System.out.println("RaceDAO::refreshRaceList() Returning the list");
        if(!raceList.isEmpty())
            raceList.clear();
        raceList.addAll(list);
        splitMap = new HashMap();
        raceList.forEach(r -> r.getSplits().forEach(sp -> splitMap.put(sp.getID(),sp)));
        
        racesLoadedLatch.countDown();
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
        //System.out.println("Adding Wave id: " + w.getID() + "to" + w.getRace().getRaceName());
        waveList.add(w); 
        waveMap.put(w.getID(), w);
    }
    
    public void removeWave(Wave w) {
        
        w.getRace().removeWave(w); 
        //refreshWaveList();         
        //System.out.println("removeWaves before: waveList.size()= " + waveList.size());
        //System.out.println("Wave: " + w.idProperty());
        waveList.forEach(e -> {System.out.println("Possible: " + e.idProperty() + " " + e.equals(w));});
        Boolean res = waveList.remove(w);
        Wave remove = waveMap.remove(w.getID());
        //System.out.println("removeWaves after: waveList.size()= " + waveList.size() + " result: " + res);

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
        //System.out.println("getWaveByID: racesLoadedLatch is now " + racesLoadedLatch.getCount());
        try {
            racesLoadedLatch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(RaceDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (waveMap.isEmpty()) refreshWaveList(); 
        return waveMap.get(id); 
    }
    
    public Race getRaceByID (int id){
        ObjectProperty<Race> result = new SimpleObjectProperty();
        listRaces().forEach(r -> {
            if (r.getID().intValue() == id) result.set(r);
        });
        return result.get();
    }
    
    
    public void refreshWaveList() { 
        
        if (waveMap.isEmpty()) {
            waveList.clear();
            waveMap.clear();
            raceList.forEach( r -> {
                waveList.addAll(r.getWaves()); 
            });
            waveList.forEach(w -> {
                    waveMap.put(w.getID(), w);
            });
        }
    }     
    
    public ObservableList<Wave> listWaves() { 
        if(waveList.isEmpty())  refreshWaveList();
        //System.out.println("ListWaves for " + waveList.size());
        //refreshWaveList(); 
        
        return waveList; 
    } 
    
    public void removeRace(Race r) {
        raceList.remove(r);
        waveList.removeAll(r.getWaves());
        r.getWaves().forEach(w -> waveMap.remove(w.getID()));
        r.getSplits().forEach(sp -> splitMap.remove(sp.getID()));
        
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(r);
        s.getTransaction().commit(); 
        
    }      
    

    
    public void removeSplit(Split w) {
        splitMap.remove(w.getID());
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
    
    public void updateAwardCategory(AwardCategory a){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.saveOrUpdate(a);
        s.getTransaction().commit();
    }
    
    public void removeAwardCategory(AwardCategory a){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.delete(a);
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
    
    public void updateSegment(Segment seg){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.saveOrUpdate(seg);
        s.getTransaction().commit();
    }
    public void removeSegment (Segment seg) {
        
        Race r = seg.getRace(); 
        r.removeRaceSegment(seg);
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(seg);
        s.getTransaction().commit();
        updateSplitOrder(r);
    }
    
    public void upsertCourseRecord(CourseRecord cr){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.saveOrUpdate(cr);
        s.getTransaction().commit();
    }
    public void clearCourseRecords(Race race) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<CourseRecord> deleteMeIterator = race.getCourseRecords().iterator();
        while (deleteMeIterator.hasNext()) {
            CourseRecord p = deleteMeIterator.next();
            s.delete(p); 
            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }
        }
        s.getTransaction().commit(); 
    }
}
