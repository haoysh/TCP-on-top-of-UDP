package tcp;

import java.util.*;

public class Client {
  private int amountData;
  private int numPacketsSent;
  private int numPacketsReceived;
  private int numRetransmission;
  private int lastByteSent;     // lastByteSent = x
  private int nextExpectedByte; // nextExpectedByte = y
  private int fileNameSize;
  private int advertisedWindow;
  private int numDupAcks;
  private double eDEV;
  private double eRTT;
  private double timeout;
  private LinkedList<TCPv2> packets;

  public Client(){
    amountData = 0;
    numPacketsSent = 0;
    numPacketsReceived = 0;
    numRetransmission = 0;
    lastByteSent = 0;
    nextExpectedByte = 0;
    eDEV = 0;
    eRTT = 0;
    timeout = 5E9;
    fileNameSize = 0;
    advertisedWindow = 0;
    packets = new LinkedList<TCPv2>();
    numDupAcks = 0;
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
  
  protected void incrementDupAcks(){
    numDupAcks++;
  }

  protected int getDupAcks(){
    return numDupAcks;
  }

  protected void incrementPacketsRec(){
    numPacketsReceived++;
  }

  protected int getPacketsRec(){
    return numPacketsReceived;
  }

  protected void incrementRetrans(){
    numRetransmission++;
  }

  protected int getRetrans(){
    return numRetransmission;
  }

  protected void incrementLastByteSent(int increment){
    lastByteSent += increment;
  }

  protected int getLastByteSent(){
    return lastByteSent;
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

  protected void setFileNameSize(int size) {
    fileNameSize = size;
  }

  protected int getFileNameSize(){
    return fileNameSize;
  }

  protected void setAdvertWindow(int size){
    advertisedWindow = size;
  }

  protected int getAdvertWindow(){
    return advertisedWindow;
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
