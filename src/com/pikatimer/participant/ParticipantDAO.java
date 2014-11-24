/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import java.util.List; 
import com.pikatimer.HibernateUtil;
import java.util.ArrayList;
import org.hibernate.HibernateException;
import org.hibernate.Session;
/**
 *
 * @author jcgarner
 */
public class ParticipantDAO {
    
    public void addParticipant(Participant p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        s.save(p);
        s.getTransaction().commit();
    }
    
    
    public List<Participant> listParticipants() { 

        List<Participant> list = new ArrayList<>();
        

        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        System.out.println("Runing the Query");
        
      try {  
        list=s.createQuery("from Participant").list();
        } catch (HibernateException e) {
            System.out.println(e.getMessage());
        }
        s.getTransaction().commit(); 
        System.out.println("Returning the list");
        return list;
    }      


    public void removeParticipant(Integer id) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction();
        Participant c=(Participant)s.load(Participant.class , id);
        s.delete(c);
        s.getTransaction().commit(); 
    }      

    
    public void updateParticipant(Participant p) {
        Session s=HibernateUtil.getSessionFactory().getCurrentSession();
        s.beginTransaction(); 
        s.update(p);
        s.getTransaction().commit();
        s.close();
     } 
} 

