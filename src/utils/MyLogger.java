/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author chandra
 */
public class MyLogger {
    static private FileHandler fileTxt;
    static private SimpleFormatter formatterTxt;
    
    static Logger rootLogger = Logger.getLogger("");
    static public void setup() throws IOException {    
        // suppress the logging output to the console
        Handler[] handlers = rootLogger.getHandlers();
        //if (handlers[0] instanceof ConsoleHandler) {
        //  rootLogger.removeHandler(handlers[0]);
        //}

        fileTxt = new FileHandler("AgentLog");
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);

        rootLogger.addHandler(fileTxt);
        rootLogger.setLevel(Level.WARNING);

    }
    
    static public void setLevel(int level){
        switch (level){
            case 1:
                rootLogger.setLevel(Level.SEVERE);
                break;
            case 2:
                rootLogger.setLevel(Level.WARNING);
                break;
            case 3:
                rootLogger.setLevel(Level.ALL);
                break;
            case 4:
                rootLogger.setLevel(Level.FINER);
                break;
            default:
                rootLogger.setLevel(Level.WARNING);
                break;
        }
    }
    
}
