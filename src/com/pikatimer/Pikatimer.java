/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.pikatimer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 *
 * @author jcgarner
 */
public class Pikatimer extends Application {
    
    //private final Event event = Event.getInstance(); 
    private static Stage mainStage;
    private static String jdbcURL; // Holds the jdbcURL for the open db
    
    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            private static final Pikatimer INSTANCE = new Pikatimer();
    }

    public static Pikatimer getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    public static void setJdbcUrl (String url) {
        jdbcURL=url; 
    }
    
    public static String getJDBCUrl () {
        return jdbcURL;         
    }
    
      
    public static Stage getPrimaryStage() {
        return mainStage;
    }    
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        //stash the primaryStage in the event object
        mainStage=primaryStage;
        
        primaryStage.setTitle("PikaTimer");
        
        Pane myPane = (Pane)FXMLLoader.load(getClass().getResource("FXMLopenEvent.fxml"));
        Scene myScene = new Scene(myPane);
        
        primaryStage.setScene(myScene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
