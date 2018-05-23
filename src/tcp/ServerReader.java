package tcp;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.nio.file.*;

public class ServerReader implements Runnable{
  static final int TIMER = 100;

  private Thread reader;
  private Buffer buff;
  private FileOutputStream outputStream;
  private File file;
  private Server sr;

  public ServerReader(Buffer buff, File file, Server sr){
    this.buff = buff;
    this.file = file;
    this.sr = sr;
    //Path currentRelataivePath = Paths.get("");
    //String path = currentRelativePath.toAbsolutePath().toString();
    //String path = Paths.get(".").toAbsolutePath().normalize().toString();
    //String path = "result/";
    //String path = "";
    //String path = new File("").getAbsolutePath().toString();
    //path += "/result/";
    //path += "/S_";
    //path += filename;
    //path += "2";
    //String output = String.format("Server: About to open file named: %s", path);
    //System.out.println(output);
    //File file = new File(path);
    //File dir = new File("tmp");
    //dir.mkdirs();
    //File tmp = new File(dir, filename);
    // String output;
    //File tmp = new File("results" + File.separator + filename);
    //if (!tmp.getParentFile().exists()) {
    //  tmp.getParentFile().mkdirs();
    //}
    // File tmp = new File("S_" + filename);
    //if (!tmp.exists()){
    //  try{
    //file.createNewFile();
    //    tmp.createNewFile();
    //  } catch(IOException e) {
    //output = String.format("Unable to create file %s", path);
    //    System.out.println(e.getMessage());
    //    output = String.format("Unable to create file %s", tmp.toString());
    //    System.out.println(output);
    //    String path = tmp.getAbsolutePath().toString();
    //    output = String.format("Path of file was %s", path);
    //    System.out.println(output);
    //    System.exit(1);
    //  } catch(SecurityException e){
    //    System.out.println("Security exception didn't allow write access to file");
    //    System.exit(1);
    // }
    //}
    String output;
    try {
      outputStream = new FileOutputStream(file);
      //System.out.println(path);
    } catch (FileNotFoundException ex){
      output = String.format("Unable to open file %s", file.toString());
      System.out.println(output);
      System.exit(1);
    } catch (SecurityException ex){
      System.out.println("Permission denied. Could not open file " + file.toString());
      System.exit(1);
    }
    this.reader = new Thread(this);
    this.reader.start();
  }

  public void run(){
    while(true){
      byte b;
      while(true){
        int availableBytes;
        synchronized(buff){
          availableBytes = buff.getNextExpectedByte()-1 - buff.getLastByteRead();
        }
        if(availableBytes <= 0){
          if(sr.getEndOfFile()) {
            try{
              outputStream.close();
            } catch(IOException e) {
              return;
            }
            return;
          }
          try{ 
           // Thread.sleep(10*TIMER);
            Thread.sleep(5*TIMER);
          }
          catch (InterruptedException e)
          { break; }
        } else { break;}
      }

      synchronized(buff){
        b = buff.sRead();
      }
      try {
        outputStream.write(Byte.valueOf(b)); //was (int) b
      } catch (IOException ex){
        System.out.println("Error writng file");
        System.exit(1);
      }
    }
  }
}
