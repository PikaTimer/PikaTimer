/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
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

import com.pikatimer.participant.Participant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */

@Entity
@DynamicUpdate
@Table(name="race_sex_groups")
public class SexGroups {
    private Integer raceID;
    
    private Race race;
    
    private SexHandling sexHandling = SexHandling.OFI; 
    private final ObjectProperty<SexHandling> sexHandlingProperty = new SimpleObjectProperty(sexHandling);
    
    private final ObservableList<SexCode> sexCodeObservableList = FXCollections.observableArrayList(SexCode.extractor());
    private List<SexCode> sexCodeList;
    private final Map<String,String> sexCodeMap = new HashMap();

    public SexGroups() {
        sexCodeObservableList.addListener((ListChangeListener<SexCode>) c -> {rebuildSexCodeMap(); });
    
        
    }
    
    @Id
    @GenericGenerator(name = "sex_generator", strategy = "foreign", 
	parameters = @Parameter(name = "property", value = "race"))
    @GeneratedValue(generator = "sex_generator")
    @Column(name = "race_id", unique = true, nullable = false)
    public Integer getRaceID() {
        return raceID; 
    }
    public void setRaceID(Integer r) {
        raceID = r;
    }
    
    @OneToOne(mappedBy = "sexGroups")
    @MapsId
    @JoinColumn(name="race_id")  
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
   
    @Enumerated(EnumType.STRING)
    @Column(name="handling_method")
    public SexHandling getHandling() {
        return sexHandling;
    }
    public void setHandling(SexHandling s) {
        
        if (s != null && (sexHandling == null || ! sexHandling.equals(s)) ){
            
            sexHandling = s;
            sexHandlingProperty.set(sexHandling);
        }
    }
    public ObjectProperty<SexHandling> statusProperty(){
        return sexHandlingProperty;
    }

    public void rebuildSexCodeMap() {
        sexCodeMap.clear();
        sexCodeObservableList.forEach(sc -> {
            sexCodeMap.put(sc.getCode(), sc.getLabel());
        });
    }
    
    public String getLabelFromCode(String c){
        return sexCodeMap.containsKey(c)?sexCodeMap.get(c):c;
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
          name="race_sex_group_code_map",
          joinColumns=@JoinColumn(name="race_id")
    )
    protected List<SexCode> getCustomIncrementsList(){
        return sexCodeList;
    }
    protected void setCustomIncrementsList(List<SexCode> l){
        sexCodeList = l;
    }
    
    public ObservableList<SexCode> sexCodeListProperty(){
        if (sexCodeObservableList.isEmpty() && sexCodeList != null && ! sexCodeList.isEmpty() ) {
            sexCodeObservableList.addAll(sexCodeList);
            rebuildSexCodeMap();
        }
        return sexCodeObservableList;
    }
    
    public void addSexCode(SexCode sc){
        System.out.println("SexGroups:addSexCode() called");
        sexCodeObservableList.add(sc);
        sexCodeList = sexCodeObservableList;
    }
    
    public void removeSexCode(SexCode sc){
        System.out.println("SexGroups:removeSexCode() called");
        sexCodeObservableList.remove(sc);
        sexCodeList = sexCodeObservableList;
    }
    
    List<String> listSexGroups(Participant p){
        List<String> s = new ArrayList();
        switch(sexHandling) {
            case OFI: //"Open/Female (Inclusive)")
                s.add("Open");
                if (p.getSex().startsWith("F")) s.add(getLabelFromCode(p.getSex()));
                break;
            case OFE: // "Open/Female (Exclusive)"),
                if (p.getSex().startsWith("F")) s.add(getLabelFromCode(p.getSex()));
                else s.add("Open");
                break; 
            case ALL: // "Awards for each value"),
                if (! p.getSex().isEmpty()) s.add(getLabelFromCode(p.getSex()));
                break; 
            case MF: // "M/F Only");              
                if (p.getSex().startsWith("M") || p.getSex().startsWith("F")) s.add(getLabelFromCode(p.getSex()));
                break; 
        }
        return s;
    }

    Boolean eligibilityFilter(Participant p) {
        switch(sexHandling) {
            case OFI: //"Open/Female (Inclusive)")
            case OFE: // "Open/Female (Exclusive)"),
                return true;
            case ALL: // "Awards for each value"),
                if (p.getSex().isEmpty()) return false;
                return true;
            case MF: // "M/F Only");              
                if (p.getSex().startsWith("M") || p.getSex().startsWith("F")) return true;
                return false; 
        }
        return true;
    }
    
}