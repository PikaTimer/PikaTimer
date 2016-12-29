/* 
 * Copyright (C) 2016 John Garner
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
package com.pikatimer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 *
 * @author jcgarner
 */
public class Pikatimer extends Application {
    
    //private final Event event = Event.getInstance(); 
    private static Stage mainStage;
    private static String jdbcURL; // Holds the jdbcURL for the open db
    
    public static final String VERSION = "0.8 (Alpha)";
    
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
        
        primaryStage.setTitle("PikaTimer " + VERSION);
        
        Pane myPane = (Pane)FXMLLoader.load(getClass().getResource("FXMLopenEvent.fxml"));
        Scene myScene = new Scene(myPane);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();  
  
        //set Stage boundaries so that the main screen is centered.                
        primaryStage.setX((primaryScreenBounds.getWidth() - primaryStage.getWidth())/2);  
        primaryStage.setY((primaryScreenBounds.getHeight() - primaryStage.getHeight())/2);  
 
        primaryStage.setScene(myScene);
        primaryStage.show();
        
        System.out.println("Exiting Pikatimer.start()");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
