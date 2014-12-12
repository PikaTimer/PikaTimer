/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import java.util.List; 
import com.pikatimer.util.HibernateUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;
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
    private static final ObservableList<Participant> participantsList =FXCollections.observableArrayList();;
    Semaphore semaphore = new Semaphore(1);
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final ParticipantDAO INSTANCE = new ParticipantDAO();
    }

    public static ParticipantDAO getInstance() {
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
        
    }
    
    public void refreshParticipantsList() { 

        List<Participant> list = new ArrayList<>();
        

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the Query");
        
        try {  
            list=s.createQuery("from Participant").list();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } 
        s.getTransaction().commit(); 
        
        System.out.println("Returning the list");
        if(!participantsList.isEmpty())
            participantsList.clear();
        participantsList.addAll(list);
    }     
    
    public ObservableList<Participant> listParticipants() { 

        refreshParticipantsList();
        return participantsList;
        //return list;
    }      

    public void removeParticipant(Participant p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.delete(p);
        s.getTransaction().commit(); 
        participantsList.remove(p);
    }      
    
    public void clearAll() {
        removeParticipants(participantsList);
    }
    public void removeParticipants(ObservableList<Participant> removeList) {
        
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
                        refreshParticipantsList();
                    });
                
                return null;
            }
        };
        new Thread(task).start();
    }   
    public void blockingClearAll() {
        blockingRemoveParticipants(participantsList);
    }
    public void blockingRemoveParticipants(ObservableList<Participant> removeList) {
        int max = removeList.size();
        int i=1;
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        int count = 0;
        Iterator<Participant> deleteMeIterator = removeList.iterator();
        while (deleteMeIterator.hasNext()) {
            Participant p = deleteMeIterator.next();
            s.delete(p); 
            if ( ++count % 20 == 0 ) {
                //flush a batch of updates and release memory:
                s.flush();
                s.clear();
            }

        }
        s.getTransaction().commit(); 

        Platform.runLater(() -> {
                refreshParticipantsList();
            });
                

    }  
    
    public void updateParticipant(Participant p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(p);
        s.getTransaction().commit();
     } 
    
    
} 

