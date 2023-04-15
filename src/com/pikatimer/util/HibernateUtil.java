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
package com.pikatimer.util;

import com.pikatimer.Pikatimer;
import com.pikatimer.event.EventOptions;
import com.pikatimer.participant.CustomAttribute;
import com.pikatimer.participant.Participant;
import com.pikatimer.race.AgeGroupIncrement;
import com.pikatimer.race.AgeGroups;
import com.pikatimer.race.AwardCategory;
import com.pikatimer.race.AwardDepth;
import com.pikatimer.race.AwardFilter;
import com.pikatimer.race.CourseRecord;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceAwards;
import com.pikatimer.race.SexCode;
import com.pikatimer.race.SexGroups;
import com.pikatimer.race.Wave;
import com.pikatimer.results.ReportDestination;
import com.pikatimer.results.RaceOutputTarget;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.Result;
import com.pikatimer.timing.Bib2ChipMap;
import com.pikatimer.timing.CookedTimeData;
import com.pikatimer.timing.RawTimeData;
import com.pikatimer.timing.Segment;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimeOverride;
import com.pikatimer.timing.TimingLocation;
import com.pikatimer.timing.TimingLocationInput;
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
        cfg.addAnnotatedClass(RaceAwards.class);
        cfg.addAnnotatedClass(AwardCategory.class);
        cfg.addAnnotatedClass(AwardDepth.class);
        cfg.addAnnotatedClass(AwardFilter.class);
        cfg.addAnnotatedClass(AgeGroups.class);
        cfg.addAnnotatedClass(AgeGroupIncrement.class);
        cfg.addAnnotatedClass(Split.class);
        cfg.addAnnotatedClass(Segment.class);
        cfg.addAnnotatedClass(TimingLocation.class);
        cfg.addAnnotatedClass(TimingLocationInput.class);
        cfg.addAnnotatedClass(Wave.class);
        cfg.addAnnotatedClass(RawTimeData.class);
        cfg.addAnnotatedClass(CookedTimeData.class);
        cfg.addAnnotatedClass(Bib2ChipMap.class);
        cfg.addAnnotatedClass(Result.class);
        cfg.addAnnotatedClass(TimeOverride.class);
        cfg.addAnnotatedClass(ReportDestination.class);
        cfg.addAnnotatedClass(RaceReport.class);
        cfg.addAnnotatedClass(RaceOutputTarget.class);
        cfg.addAnnotatedClass(EventOptions.class);
        cfg.addAnnotatedClass(CustomAttribute.class);
        cfg.addAnnotatedClass(CourseRecord.class);
        cfg.addAnnotatedClass(SexGroups.class);
        cfg.addAnnotatedClass(SexCode.class);
        
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url",Pikatimer.getJDBCUrl());
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.show_sql", "true");
        cfg.setProperty("hibernate.jdbc.batch_size","100"); 
        
        cfg.setProperty("hibernate.enable_lazy_load_no_trans", "true");
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
