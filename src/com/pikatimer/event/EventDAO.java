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
package com.pikatimer.event;


import com.pikatimer.util.HibernateUtil;
import java.time.LocalDate;
import java.util.ArrayList;
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
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final EventDAO INSTANCE = new EventDAO();
    }

    public static EventDAO getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
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
        
        public EventOptions getEventOptions(){
            EventOptions eo;
            // Run a select
            List<EventOptions> list = new ArrayList<>();
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            //System.out.println("RacedAO.refreshRaceList() Starting the query");

            try {  
                list=s.createQuery("from EventOptions").list();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } 
            s.getTransaction().commit(); 
                    
            // if we don't have any, create one and save it
            if (list.isEmpty()) {
                eo= new EventOptions();
                eo.setEventID(1);
                saveEventOptions(eo);
            } else {
                eo = list.get(0);
            }
                
            // return what we have
            
            return eo;
        }
        
        public void saveEventOptions(EventOptions e){
            Session s=HibernateUtil.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.saveOrUpdate(e);
            s.getTransaction().commit();
        }
        
}