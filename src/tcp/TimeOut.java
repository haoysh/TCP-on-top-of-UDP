package tcp;

import java.util.*;
import java.lang.*;
import java.io.*;

public class TimeOut implements Runnable {
  static final int TIMER = 75;

  private Thread timeOut;
  private Client cl;
  private Server sr;
  private LinkedList<TCPv2> packets;
  private int numRetransmissions = 0;
  private int type;

  public TimeOut (Client cl){
    this.cl = cl;
    packets = cl.getPacketList();
    this.type = Buffer.CLIENT;
    this.timeOut = new Thread(this);
    this.timeOut.start();
  }

  public TimeOut (Server sr){
    this.sr = sr;
    packets = sr.getPacketList();
    this.type = Buffer.SERVER;
    this.timeOut = new Thread(this);
    this.timeOut.start();
  }

  public void run(){
    if(type == Buffer.CLIENT){
      while(true){
        try{ Thread.sleep(TIMER);}
        catch(InterruptedException E) { return; }
        double timeout = cl.getTimeout();
        synchronized(packets){
          Iterator<TCPv2> it = packets.iterator();
          while(it.hasNext()){
            TCPv2 current = it.next();
            if((double)(System.nanoTime() - current.getTime()) > timeout){
              if(current.getNumRetransmits() >= 16) {
                System.exit(1);
              }
              numRetransmissions++;
              TCPend.resendPacket(cl, current);
              current.incrementRetransmits();
            }
          }
        }
      }
    } else{   // server Runs
      double timeout = sr.getTimeout();
      while(true){
        try{ Thread.sleep(TIMER);}
        catch(InterruptedException E) { return; }
        synchronized(packets){
          Iterator<TCPv2> it = packets.iterator();
          while(it.hasNext()){
            TCPv2 current = it.next();
            if((double)(System.nanoTime() - current.getTime()) > timeout){
              if(current.getNumRetransmits() >= 16) {
                System.exit(1);
              }
              numRetransmissions++;
              TCPend.resendPacket(sr, current);
              current.incrementRetransmits();
            }
          }
        }
      }
    }
  }
}
