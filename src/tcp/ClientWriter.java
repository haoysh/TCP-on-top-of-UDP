package tcp;

import java.util.*;
import java.lang.*;
import java.io.*;

public class ClientWriter implements Runnable {
  static final int TIMER = 100;

  private Thread writer;
  private Buffer buff;
  private FileInputStream inputStream;

  public ClientWriter (Buffer buff, String filename){
    this.buff = buff;
    try {
      inputStream = new FileInputStream(filename);
    } catch (FileNotFoundException ex){
      System.out.println("Unable to open file " + filename);
      System.exit(1);
    } catch (SecurityException ex){
      System.out.println("Permission denied. Could not open file " + filename);
      System.exit(1);
    }
    
    this.writer = new Thread(this);
    this.writer.start();
  }

  public void run(){
    synchronized(buff){
      while(true){
        int b= -2;  // -2 to is a sentinel value and b should never be this
        try {
          b = inputStream.read();
        } catch (IOException ex){
          System.out.println("Error reading file");
          System.exit(1);
        }
        if(b == -1){
          buff.setEndOfFile();
          break;
        }
        while(buff.cWrite((byte)b) == false){
          try{ 
            //Thread.sleep(10*TIMER);
            Thread.sleep(5*TIMER);
          }
          catch (InterruptedException e)
          { break; }
        }
      }
    }
    try{
      inputStream.close();
    } catch(IOException e) {}      
  }
}
