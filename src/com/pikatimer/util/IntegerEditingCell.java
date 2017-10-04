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

import java.util.function.UnaryOperator;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

/**
 *
 * @author John Garner
 * 
 * Most of this is boilerplate code snagged from the dozen examples
 * on various web sites out there. If only there was an officially supported
 * convenience method that came with the JDK for common data types... :-/ 
 * 
*/
public class IntegerEditingCell<T> extends TableCell<T,Integer> {

    private final TextField textField = new TextField();;
    private final TextFormatter<Integer> textFormatter ;
    private Boolean negOK = true;
    
    public IntegerEditingCell(Boolean permitNegatives){
        this();
        negOK= permitNegatives;
    }

    public IntegerEditingCell() {
        
        
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String newText = c.getControlNewText() ;
            if (newText.isEmpty()) { // always allow deleting all characters
                return c ;
            } else if (negOK && ! newText.matches("-?\\d*")) { 
                return null;
            } else if (! newText.matches("\\d+")) {// otherwise, must have all digits:
                return null ;
            }
            return c;
        };
        
        // Custom string converter to deal with blanks and such
        StringConverter<Integer> sc = new StringConverter<Integer>() {

            @Override
            public String toString(Integer i) {
                if (i == null) return "";
                return i.toString() ;
            }

            @Override
            public Integer fromString(String s) {
                if (s != null && s.matches("-?\\d+")) {
                    return Integer.valueOf(s);
                } else {
                    // fall back to what the value used to be 
                    // (in case they blanked it out)
                    return getItem() ;
                }
            }
        };
        
        textFormatter = new TextFormatter<>(sc, 0, filter) ;
        textField.setTextFormatter(textFormatter);

        textField.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        
        textField.setOnAction(e -> commitEdit(sc.fromString(textField.getText())));
        
        // This may work. I hope... 
        textField.focusedProperty().addListener((e, o, n) -> {if (!n) commitEdit(sc.fromString(textField.getText()));});

        textProperty().bind(Bindings
                .when(emptyProperty())
                .then("")
                .otherwise(itemProperty().asString()));

        setGraphic(textField);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }


    // Override this so we can get the selectAll function
    @Override
    public void startEdit() {
        super.startEdit();
        textFormatter.setValue(getItem());
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        textField.requestFocus();
        textField.selectAll();
    }

    @Override
    public void commitEdit(Integer newValue) {
        super.commitEdit(newValue);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }


}
