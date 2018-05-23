package tcp;

import java.util.*;

public class Server {
  private int amountData;
  private int numPacketsSent;
  private int numPacketsReceived;
  private int numRetransmission;
  private int nextExpectedByte; // nextExpectedByte = y
  private int numPacketsOutSeq;
  private int numWrongCheck;
  private double eDEV;
  private double eRTT;
  private double timeout;
  private LinkedList<TCPv2> packets;
  private boolean endOfFile;

  public Server(){
    amountData = 0;
    numPacketsSent = 0;
    numPacketsReceived = 0;
    numRetransmission = 0;
    nextExpectedByte = 0;
    eDEV = 0;
    eRTT = 0;
    timeout = 5E9;
    packets = new LinkedList<TCPv2>();
    endOfFile = false;
    numPacketsOutSeq = 0;
    numWrongCheck = 0;
  }

  protected void incrementData(int increment){
    amountData += increment;
  }

  protected int getAmountData(){
    return amountData;
  }

  protected void incrementPacketsSent(){
    numPacketsSent++;
  }

  protected int getPacketsSent(){
    return numPacketsSent;
  }

  protected void incrementPacketsRec(){
    numPacketsReceived++;
  }

  protected int getPacketsRec(){
    return numPacketsReceived;
  }  
  
  protected void incrementPacketsOutOfSeq(){
    numPacketsOutSeq++;
  }

  protected int getPacketsOutOfSeq(){
    return numPacketsOutSeq;
  }
  
  protected void incrementPacketsWrongCheck(){
    numWrongCheck++;
  }

  protected int getPacketsWrongCheck(){
    return numWrongCheck;
  }

  protected void incrementRetrans(){
    numRetransmission++;
  }

  protected int getRetrans(){
    return numRetransmission;
  }

  protected void incrementNextExpectedByte(int increment){
    nextExpectedByte += increment;
  }

  protected int getNextExpectedByte(){
    return nextExpectedByte;
  }

  protected void seteRTT(double eRTT){
    this.eRTT = eRTT;
  }

  protected double geteRTT(){
    return eRTT;
  }

  protected void seteDEV(double eDEV){
    this.eDEV = eDEV;
  }

  protected double geteDEV(){
    return eDEV;
  }

  protected void setTimeout(double timeout){
    this.timeout = timeout;
  }

  protected double getTimeout(){
    return timeout;
  }

  protected void setEndOfFile(){
    endOfFile = true;
  }

  protected boolean getEndOfFile(){
    return endOfFile;
  }

  protected void addPacket(TCPv2 packet){
    synchronized(packets){
      packets.add(packet);
    }
  }

  protected LinkedList<TCPv2> getPacketList(){
    return packets;
  }
  
  protected boolean isEmpty(){
    int size;
    synchronized(packets){
       size = packets.size();
    }
    if(size == 0){
      return true;
    } else {
      return false;
    }
  }
}
