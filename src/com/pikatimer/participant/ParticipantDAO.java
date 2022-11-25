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
package com.pikatimer.participant;

import com.pikatimer.Pikatimer;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.race.WaveAssignment;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.HTTPServices;
import java.util.List; 
import com.pikatimer.util.HibernateUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.hibernate.Session;

/**
 * TODO: Figure out the locking so that mass updates/deletes/etc do not screw things up
 */

/**
 *
 * @author jcgarner
 */
public class ParticipantDAO {
    private static final ObservableList<Participant> participantsList = FXCollections.observableArrayList(Participant.extractor());
    private static final Map<String,Participant> Bib2ParticipantMap = new HashMap<>();
    private static final Map<Integer,Participant> ID2ParticipantMap = new HashMap<>(); 
    private static final Map<Participant,String> Participant2BibMap = new HashMap<>();
    private static final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    private static final BooleanProperty participantsListInitialized = new SimpleBooleanProperty(false);
    //Semaphore semaphore = new Semaphore(1);
    
    private static final CountDownLatch participantsLoadedLatch = new CountDownLatch(1);
    
    private static final ObservableList<CustomAttribute> customAttributeList = FXCollections.observableArrayList(CustomAttribute.extractor());
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final ParticipantDAO INSTANCE = new ParticipantDAO();
    }

    public static ParticipantDAO getInstance() { 
        synchronized(participantsListInitialized){
              if (!participantsListInitialized.getValue()) {
                  participantsListInitialized.set(true);
                  refreshParticipantsList();
              }
        }
        return SingletonHolder.INSTANCE; 
    }
    
    public void addParticipant(Participant p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(p);
        s.getTransaction().commit();
        Platform.runLater(() -> {
            participantsList.add(p);
        });
        Participant2BibMap.put(p, p.getBib()); 
        Bib2ParticipantMap.put(p.getBib(),p); 
        ID2ParticipantMap.put(p.getID(),p);
        resultsDAO.getResultsQueue().add(p.getBib()); 
        
        if (p.bibProperty().isEmpty().not().get()) HTTPServices.getInstance().publishEvent("PARTICIPANT", p.getJSONObject());
        
    }
    
    public void addParticipant(ObservableList newParticipantList) {
        int max = newParticipantList.size();
        int i=1;
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<Participant> addIterator = newParticipantList.iterator();
        while (addIterator.hasNext()) {
            Participant p = addIterator.next();
            s.save(p); 
            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }
            //updateProgress(i++, max);
            Participant2BibMap.put(p, p.getBib()); 
            Bib2ParticipantMap.put(p.getBib(),p); 
            ID2ParticipantMap.put(p.getID(),p);
            resultsDAO.getResultsQueue().add(p.getBib()); 
            
            if (p.bibProperty().isEmpty().not().get()) HTTPServices.getInstance().publishEvent("PARTICIPANT", p.getJSONObject());

        }
        s.getTransaction().commit(); 

        Platform.runLater(() -> {
            //refreshParticipantsList();
            participantsList.addAll(newParticipantList); 
        });
        
        
        
    }
    private static void refreshParticipantsList() { 

        Task fetchParticipants = new Task<Void>() {
                
            @Override 
            public Void call() {
                final List<Participant> list;// = new ArrayList<>();
                final List<CustomAttribute> attributeList;


                Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                s.beginTransaction();
                System.out.println("ParticipantDAO:: refreshParticipantsList Runing the Query");

                try {  
                    list=s.createQuery("from Participant").list();
                    attributeList=s.createQuery("from CustomAttribute").list();

                    System.out.println("ParticipantDAO::refreshParticipantsList found " + list.size() + " Participants");
                    //if(!participantsList.isEmpty()) participantsList.clear();

                    Platform.runLater(() -> {
                            participantsList.setAll(list);
                            customAttributeList.setAll(attributeList);
                    });
                    
                    
                    list.forEach(p -> {
                        Participant2BibMap.put(p, p.getBib()); 
                        Bib2ParticipantMap.put(p.getBib(),p); 
                        ID2ParticipantMap.put(p.getID(),p);
                    });
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } 
                s.getTransaction().commit();
                participantsLoadedLatch.countDown();
                return null;
            }
        }; 

        new Thread(fetchParticipants).start();
            
    }     
    
    public ObservableList<Participant> listParticipants() { 

        //if (participantsList.isEmpty() ) refreshParticipantsList();
        
        return participantsList;
        //return list;
    }      

    public void removeParticipant(Participant p) {
        participantsList.remove(p);
        Participant2BibMap.remove(p);
        Bib2ParticipantMap.remove(p.getBib()); 
        ID2ParticipantMap.remove(p.getID());
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(p);
        s.getTransaction().commit(); 
        resultsDAO.getResultsQueue().add(p.getBib()); 
    }      
    
    public void clearAll() {
        removeParticipants(participantsList);
    }
    public void removeParticipants(ObservableList<Participant> rl) {
        List<Participant> removeList = FXCollections.observableArrayList(rl);

        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                int max = removeList.size();
                int i=1;
                Session s=HibernateUtil.getSessionFactory().getCurrentSession();
                s.beginTransaction();
                int count = 0;
                Iterator<Participant> deleteMeIterator = removeList.iterator();
                while (deleteMeIterator.hasNext()) {
                    Participant p = deleteMeIterator.next();
                    Participant2BibMap.remove(p);
                    Bib2ParticipantMap.remove(p.getBib());
                    ID2ParticipantMap.remove(p.getID());
                    resultsDAO.getResultsQueue().add(p.getBib()); 
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
                        //refreshParticipantsList();
                        participantsList.removeAll(removeList);
                    });
                
                return null;
            }
        };
        new Thread(task).start();
    }   
    public void blockingClearAll() {
        blockingRemoveParticipants(participantsList);
    }
    public void blockingRemoveParticipants(ObservableList<Participant> rl) {
        List<Participant> removeList = FXCollections.observableArrayList(rl);
        int max = removeList.size();
        int i=1;
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<Participant> deleteMeIterator = removeList.iterator();
        while (deleteMeIterator.hasNext()) {
            Participant p = deleteMeIterator.next();
            Participant2BibMap.remove(p);
            Bib2ParticipantMap.remove(p.getBib());
            ID2ParticipantMap.remove(p.getID()); 
            resultsDAO.getResultsQueue().add(p.getBib()); 
            s.delete(p); 

            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }

        }
        s.getTransaction().commit(); 

        Platform.runLater(() -> {
                //refreshParticipantsList();
                participantsList.removeAll(removeList);
            });
    }  
    
    public void updateParticipant(Participant p) {
        if (p == null){
            System.out.println("Cant save NULL!!!");
            return;
        }
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(p);
        s.getTransaction().commit();
        if ( ! p.getBib().equals(Participant2BibMap.get(p))) {
            // bib number changed
            System.out.println("bib Number Change... "); 
            String oldBib = Participant2BibMap.get(p);
            Bib2ParticipantMap.remove(Participant2BibMap.get(p));
            Bib2ParticipantMap.put(p.getBib(), p);
            
            Participant2BibMap.replace(p,p.getBib()); 
            
            // Flush out any old results
            resultsDAO.getResultsQueue().add(oldBib);
        }
        resultsDAO.getResultsQueue().add(p.getBib()); 
        
        if (p.bibProperty().isEmpty().not().get()) HTTPServices.getInstance().publishEvent("PARTICIPANT", p.getJSONObject());
        
     } 
    
    public Participant getParticipantByBib(String b) {
        if (b == null || b.isEmpty()) return null;
        try {
            participantsLoadedLatch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(ParticipantDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Bib2ParticipantMap.get(b);
    }
    public Participant getParticipantByID(Integer id) {
        try {
            participantsLoadedLatch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(ParticipantDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ID2ParticipantMap.get(id); 
    }
    
    public ObservableList<CustomAttribute> getCustomAttributes() {
        return customAttributeList;
    }
    
    public void saveCustomAttribute(CustomAttribute cr){
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.saveOrUpdate(cr);
        s.getTransaction().commit();
        if (!customAttributeList.contains(cr)) customAttributeList.add(cr);
    }
    public void deleteCustomAttribute(CustomAttribute cr){
        if (customAttributeList.contains(cr)) {
            customAttributeList.remove(cr);
        
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.delete(cr);
            s.getTransaction().commit();
        }
        
    }
    
    public Set<Wave> getWaveByBib(String bib) {
        AlphanumericComparator comp = new AlphanumericComparator(); 
        
        Set<Wave> waves = new HashSet<>();
        Map raceMap = new HashMap(); 
        
        System.out.println("Only one wave to assign them to...");

        // If there is only one...
        if (RaceDAO.getInstance().listWaves().size() == 1) {
            System.out.println("Only one wave to assign them to...");
            waves.add(RaceDAO.getInstance().listWaves().get(0));
            return waves;
        }
        
        RaceDAO.getInstance().listWaves().forEach(i -> {
            if (i.getWaveAssignmentMethod() == WaveAssignment.BIB) {
                String start = i.getWaveAssignmentStart(); 
                String end = i.getWaveAssignmentEnd(); 
                if (!(start.isEmpty() && end.isEmpty()) && (comp.compare(start, bib) <= 0 || start.isEmpty()) && (comp.compare(end, bib) >= 0 || end.isEmpty())) {
                    if(!raceMap.containsKey(i.getRace())) {
                        //System.out.println("Bib " + bibTextField.getText() + " matched wave " + i.getWaveName() + " results: "+ comp.compare(start, bibTextField.getText()) + " and " + comp.compare(end, bibTextField.getText()) );
                        raceMap.put(i.getRace(), true); 
                        waves.add(i); 
                    }
                }
            }
        });
        
        return waves; 
    }
} 

