/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.results;

import com.pikatimer.util.FileTransferTypes;
import com.pikatimer.util.FileTransport;
import static java.lang.Boolean.TRUE;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author jcgarner
 */
@Entity
@DynamicUpdate
@Table(name="report_destinations")
public class ReportDestination {

    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final StringProperty nameProperty= new SimpleStringProperty();
    private final StringProperty protocolProperty = new SimpleStringProperty();
    private final StringProperty serverProperty = new SimpleStringProperty();
    private final StringProperty basePathProperty = new SimpleStringProperty();
    private final StringProperty usernameProperty = new SimpleStringProperty();
    private final StringProperty passwordProperty = new SimpleStringProperty();
    private final StringProperty privateKeyProperty = new SimpleStringProperty();
    private final StringProperty remoteCertProperty = new SimpleStringProperty();
    private final BooleanProperty checkCertProperty = new SimpleBooleanProperty();
    private final BooleanProperty enabledProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty stripAccentsProperty = new SimpleBooleanProperty(false);

    private final StringProperty transferStatusProperty = new SimpleStringProperty();
    
    private FileTransport fileTransport;
    private FileTransferTypes outputProtocol = FileTransferTypes.LOCAL;
    
    public ReportDestination(){
        // nothing to do here for now
        //System.out.println("ReportDestination construtor called...");
    }
    
    //    id int primary key, 
    @Id
    @GenericGenerator(name="output_portal_id" , strategy="increment")
    @GeneratedValue(generator="output_portal_id")
    @Column(name="ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
 
    //    uuid varchar,
    @Column(name="uuid")
    public String getUUID() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return uuidProperty.getValue(); 
    }
    public void setUUID(String  uuid) {
        uuidProperty.setValue(uuid);
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty uuidProperty() {
        return uuidProperty; 
    }
    
//    target_name varchar
    @Column(name="target_name")
    public String getName() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return nameProperty.getValue(); 
    }
    public void setName(String  n) {
        nameProperty.setValue(n);
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty nameProperty() {
        return nameProperty; 
    }
    
//    protocol varchar,
    @Enumerated(EnumType.STRING)
    @Column(name="protocol")
    public FileTransferTypes getOutputProtocol() {
        return outputProtocol;
    }
    public void setOutputProtocol(FileTransferTypes t) {
        
        if (t != null && (outputProtocol == null || fileTransport == null || ! outputProtocol.equals(t)) ){
            
            
                               
            fileTransport = t.getNewTransport();
            fileTransport.setOutputPortal(this);
            
            System.out.println("Binding fileTransport.statusProperty() to transferStatusProperty");
            transferStatusProperty.bind(fileTransport.statusProperty());
            transferStatusProperty.addListener((ob, oldStatus, newStatus) -> {
                System.out.println("FileTransport.statusProperty() is now " + newStatus);
            });
            
            outputProtocol = t;
            protocolProperty.setValue(outputProtocol.toString());
        }
    }
//    server varchar,
    @Column(name="server")
    public String getServer() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return serverProperty.getValue(); 
    }
    public void setServer(String  s) {
        serverProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty serverProperty() {
        return serverProperty; 
    }
    
//    base_path varchar,
    @Column(name="base_path")
    public String getBasePath() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return basePathProperty.getValue(); 
        
    }
    public void setBasePath(String  s) {
        basePathProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty basePathProperty() {
        return basePathProperty; 
    }
    
//    username varchar,
    @Column(name="username")
    public String getUsername() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return usernameProperty.getValue(); 
    }
    public void setUsername(String  s) {
        usernameProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty usernameProperty() {
        return usernameProperty; 
    }
    
//    password varchar,
    @Column(name="password")
    public String getPassword() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return passwordProperty.getValue(); 
    }
    public void setPassword(String  s) {
        passwordProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty passwordProperty() {
        return passwordProperty; 
    }
    
//    private_key varchar,
    @Column(name="private_key")
    public String getPrivateKey() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return privateKeyProperty.getValue(); 
    }
    public void setPrivateKey(String  s) {
        privateKeyProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty privateKeyProperty() {
        return privateKeyProperty; 
    }
    
//    remote_cert varchar,
    @Column(name="remote_cert")
    public String getRemoteCert() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return remoteCertProperty.getValue(); 
    }
    public void setRemoteCert(String  s) {
        remoteCertProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty remoteCertProperty() {
        return remoteCertProperty; 
    }
    
//    permit_any boolean
    @Column(name="permit_any")
    public Boolean getCheckCert() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return checkCertProperty.get();
    }
    public void setCheckCert(Boolean  s) {
        if (s == null) return;
        checkCertProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public BooleanProperty checkCertProperty() {
        return checkCertProperty; 
    }
    
    //    strip Accents boolean
    @Column(name="stripAccents")
    public Boolean getStripAccents() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return stripAccentsProperty.getValue();
    }
    public void setStripAccents(Boolean  s) {
        if (s == null) return;
        stripAccentsProperty.setValue(s);
        if(fileTransport != null) fileTransport.refreshConfig();
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public BooleanProperty stripAccentsProperty() {
        return stripAccentsProperty; 
    }
    
    public BooleanProperty enabledProperty(){
        return enabledProperty;
    }
    
    public StringProperty transferStatusProperty(){
        return transferStatusProperty;
    }
    
    public StringProperty protocolProperty(){
        return protocolProperty;
    }
    public Boolean testTransfer(){
        return TRUE;
    }
    
    public static Callback<ReportDestination, Observable[]> extractor() {
        return (ReportDestination p) -> new Observable[]{p.nameProperty(),p.transferStatusProperty(),p.protocolProperty(),p.basePathProperty()};
    }
    
    @Override
    public String toString(){
        if (FileTransferTypes.LOCAL.equals(outputProtocol)) {
            return protocolProperty.getValueSafe() + ": " + basePathProperty.getValueSafe();
        }
        return protocolProperty.getValueSafe() + ": " + serverProperty.getValueSafe();
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.uuidProperty.getValue());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.uuidProperty.getValue(),((ReportDestination)obj).uuidProperty.getValue());
    }

    void save(String filename, String contents) {
        System.out.println("OutputPortal.save() called for " + filename);
        if (fileTransport != null && enabledProperty.get()) {
            if (fileTransport.isOK())
                fileTransport.save(filename,contents);
        }
    }
}
