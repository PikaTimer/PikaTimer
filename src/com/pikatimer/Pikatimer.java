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
package com.pikatimer;

import com.pikatimer.util.HTTPServices;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


/**
 *
 * @author jcgarner
 */
public class Pikatimer extends Application {
    
    //private final Event event = Event.getInstance(); 
    private static Stage mainStage;
    private static String jdbcURL; // Holds the jdbcURL for the open db
    private static HTTPServices webServer;
    
    public static final String VERSION = "1.6";
    
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
        
        mainStage.setWidth(600);
        mainStage.setHeight(400);
        
        // Start the WebServices javalin process
        webServer = HTTPServices.getInstance();
              
        Pane myPane = (Pane)FXMLLoader.load(getClass().getResource("FXMLopenEvent.fxml"));
        Scene myScene = new Scene(myPane);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();  
  
        //set Stage boundaries so that the main screen is centered.                
        primaryStage.setX((primaryScreenBounds.getWidth() - primaryStage.getWidth())/2);  
        primaryStage.setY((primaryScreenBounds.getHeight() - primaryStage.getHeight())/2);  
 
        // F11 to toggle fullscreen mode
        myScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), () -> {
            mainStage.setFullScreen(mainStage.fullScreenProperty().not().get());
        });
        
        // Icons
        String[] sizes = {"256","128","64","48","32"};
        for(String s: sizes){
            primaryStage.getIcons().add(new Image("resources/icons/Pika_"+s+".ico"));
            primaryStage.getIcons().add(new Image("resources/icons/Pika_"+s+".png"));
        }
        
        primaryStage.setScene(myScene);
        primaryStage.show();
        
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {webServer.stopHTTPService();});
        
       primaryStage.setOnCloseRequest((WindowEvent t) -> {
            Platform.exit();
            System.exit(0);
        });
        
        System.out.println("Exiting Pikatimer.start()");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
