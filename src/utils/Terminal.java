/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

//import interfaces.ShellExecutionCallback;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Ethan_Hunt
 */
public class Terminal {
    
    public static Process execute(String commands){
        StringBuffer output = new StringBuffer();
        
        //String[] cmd = { "cmd.exe", "/c",commands};
        String[] cmd = { "/bin/sh", "-c",commands};
        //String[] cmd = {commands};
        final Process p ;
        try {
                p = Runtime.getRuntime().exec(cmd);
                
                Runnable runnable = new Runnable() {
                    public void run() {
                        p.destroy();
                    }
                };
                Runtime.getRuntime().addShutdownHook(new Thread(runnable));
                
                //p.waitFor();
                BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
                BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));
                
                
                
                String line = "";			
                /*
                while ((line = stdInput.readLine())!= null) {
                        output.append(line + "\n");
                }
                */
                while ((line = stdError.readLine())!= null) {
                        output.append(line + "\n");
                }
                System.out.println(output.toString());
                
                return p;
                
        } catch (IOException e) {
                e.printStackTrace();
        }
        
        return null;
        
    }
    
    public static Process executeNoError(String commands){
        StringBuffer output = new StringBuffer();
        
        //String[] cmd = { "cmd.exe", "/c",commands};
        String[] cmd = { "/bin/sh", "-c",commands};
        //String[] cmd = {commands};
        final Process p ;
        try {
                p = Runtime.getRuntime().exec(cmd);
                
                Runnable runnable = new Runnable() {
                    public void run() {
                        p.destroy();
                    }
                };
                Runtime.getRuntime().addShutdownHook(new Thread(runnable));
                
                //p.waitFor();
                BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
                BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));
                
                
                
                String line = "";			
                /*
                while ((line = stdInput.readLine())!= null) {
                        output.append(line + "\n");
                }
                
                while ((line = stdError.readLine())!= null) {
                        output.append(line + "\n");
                }
                System.out.println(output.toString());
                */
                return p;
                
        } catch (IOException e) {
                e.printStackTrace();
        }
        
        return null;
        
    }


}
