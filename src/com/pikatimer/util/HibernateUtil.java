/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.util;

import com.pikatimer.Pikatimer;
import com.pikatimer.participant.Participant;
import com.pikatimer.race.Race;
import com.pikatimer.race.Wave;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingLocation;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;



/**
 * Hibernate Utility class with a convenient method to get Session Factory
 * object.
 *
 * @author jcgarner
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static ServiceRegistry serviceRegistry;
    
    
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            // loads configuration and mappings
            Configuration configuration = getConfiguration();
            serviceRegistry
                = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();
             
            // builds a session factory from the service registry
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);           
        }
         
        return sessionFactory;
    }
    
    public void close() throws Exception{
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if(serviceRegistry!= null) {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
}
    
    private static Configuration getConfiguration() {
        Configuration cfg = new Configuration();
        //cfg.addAnnotatedClass(Event.class );
        cfg.addAnnotatedClass(Participant.class);
        cfg.addAnnotatedClass(Race.class);
        cfg.addAnnotatedClass(TimingLocation.class);
        cfg.addAnnotatedClass(Wave.class);
        cfg.addAnnotatedClass(Split.class);
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url",Pikatimer.getJDBCUrl());
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.show_sql", "true");
        cfg.setProperty("hibernate.dialect","org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.cache.provider_class","org.hibernate.cache.NoCacheProvider");
        cfg.setProperty("hibernate.current_session_context_class", "thread");
        cfg.setProperty("hibernate.c3p0.max_size", "4");
        cfg.setProperty("hibernate.c3p0.min_size", "1");
        cfg.setProperty("hibernate.c3p0.timeout", "5000");
        cfg.setProperty("hibernate.c3p0.max_statements", "100");
        cfg.setProperty("hibernate.c3p0.idle_test_period", "300");
        cfg.setProperty("hibernate.c3p0.acquire_increment", "1");
        return cfg;
    } 
    
}
