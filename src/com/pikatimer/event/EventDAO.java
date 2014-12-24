/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;


import com.pikatimer.util.HibernateUtil;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.DateType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;



/**
 * There is only Event object, and it is a singleton. So no need for hibernate 
 * except for keeping everything in sync in terms with the connections to the 
 * db and such. We will use some hand tuned sql to get/set/retrieve the 
 * single line from the db and use that to setup/create/rehydrate the Event 
 * object. 
 */
public class EventDAO {
        private final Event event = Event.getInstance();
        
        public void updateEvent() {
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            // sql to set the name and date
            Query query = s.createSQLQuery("UPDATE EVENT set EVENT_NAME = :name, EVENT_DATE = :date WHERE ID = :id");
            query.setParameter("id", 1);
            query.setParameter("name", event.getEventName());
            //query.setParameter("date", event.getEventDate());
            query.setParameter("date", event.getLocalEventDate().toString());
            query.executeUpdate();
            s.getTransaction().commit();
            
            // Thread.dumpStack(); // who called this?
            
        }
        
        public void createEvent() {
            event.setEventName("New Event");
            event.setEventDate(LocalDate.now());
            
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            // sql to set the name and date
            Query query = s.createSQLQuery("INSERT into EVENT (ID, EVENT_NAME, EVENT_DATE) values (:id, :name, :date)");
            query.setParameter("id", 1);
            query.setParameter("name", event.getEventName());
            //query.setParameter("date", event.getEventDate());
            query.setParameter("date", event.getLocalEventDate().toString());
            query.executeUpdate();
            s.getTransaction().commit();
            
            // Thread.dumpStack(); // who called this?
            
        }
        
        public void getEvent() {
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            
            s.beginTransaction();
            Query query  = s.createSQLQuery("SELECT * FROM EVENT")
                .addScalar("ID", LongType.INSTANCE)
                .addScalar("EVENT_NAME", StringType.INSTANCE)
                .addScalar("EVENT_DATE", StringType.INSTANCE);
                //.addScalar("EVENT_DATE", DateType.INSTANCE);
            
            List<Object[]> results = query.list();
            s.getTransaction().commit();
            
            if (results.isEmpty()) {
                // nothing in the db, lets create an entry
                System.out.println("No event in DB, creating one...");
                createEvent();
            } else {
                // woot, we have data. :-) 
                for(Object[] row : results){
                    event.setEventName(row[1].toString());
                    event.setEventDate(row[2].toString());
                    System.out.println("Results: " + row[1].toString() + " Date:" + row[2].toString());
                }
            }
            
        }
}