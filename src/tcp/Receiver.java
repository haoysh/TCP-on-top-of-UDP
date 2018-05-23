package tcp;

import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;

public class Receiver implements Runnable{
  private static final int TIMER = 100;
  private static final int CLIENT = 0;
  private static final int SERVER = 1;
  private static final int BUF_SIZE = 1472;
  private static final double ALPHA = 0.875;
  private static final double BETA = 0.75;

  //private int totalDupAcks = 0;
  private int sequentialDupAcks = 0;
  //private int amountDataReceived = 0;
  //private int numPackReceived = 0;
  //private int numOutOfSeq = 0;
  //private int numWrongCheck = 0;
  private int type;
  private int lastByteSeq = -1;
  private double timeout = 5;
  private double eRTT = 0;
  private double eDEV = 0;
  // EDIT: we don't need to do slowstart or fastrecovery
  // private double congestionWindow;
  // private double ssthresh;
  // private boolean slowstart = false;
  // private boolean fastrecovery = false;
  private Thread receiver;
  private DatagramSocket receive;
  private LinkedList<TCPv2> packets;
  private Client cl;
  private Server sr;
  private Buffer buffer;

  public Receiver(Client cl, int type, DatagramSocket receive){
    this.cl = cl;
    this.type = type;
    this.packets = cl.getPacketList();
    this.receive = receive;
    this.eRTT = cl.geteRTT();
    this.eDEV = cl.geteDEV();
    this.timeout = cl.getTimeout();
    // slowstart = true;
    //ssthresh = 65536;
    //congestionWindow = 1;

    this.receiver = new Thread(this);
    this.receiver.start();
  }

  public Receiver(Server sr, int type, DatagramSocket receive, Buffer buffer){
    this.type = type;
    this.receive = receive;
    this.buffer = buffer;

    this.sr = sr;
    this.packets = sr.getPacketList();
    this.receiver = new Thread(this);
    this.receiver.start();
  }

  protected double getTimeOut(){
    return timeout;
  }

  private void updateTO(long time){
    double sRTT = (double) (System.nanoTime() - time);
    double sDEV = Math.abs(sRTT - eRTT);
    eRTT = ALPHA * eRTT + (1 - ALPHA) * sRTT;
    eDEV = BETA * eDEV + (1 - BETA) * sDEV;
    cl.setTimeout(eRTT + 4 * eDEV);
  }

  public void run() {
    if(type == CLIENT){
      while(true){
        byte[] rData = new byte[BUF_SIZE];
        DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
        try{
          receive.receive(rPacket);
        } catch(IOException e) {
          break;
        }
        byte[] temp = rPacket.getData();
        TCPv2 packet = new TCPv2();
        packet.deserialize(temp);
        TCPend.printPacketData(false, packet.getTime(), packet.getFlag(), packet.getSequence(), packet.getLength(), packet.getAck());
        if(packet.getFlag() == TCPv2.FIN_ACK) {
          cl.incrementPacketsRec();
          TCPend.tearDownClient(cl, packet);
          return;
        } 
        int packetSeq = packet.getAck();
        if(packetSeq == lastByteSeq) {
          cl.incrementDupAcks();
          sequentialDupAcks++;
        } else {
          sequentialDupAcks = 0;
          TCPend.updateWindow(cl, packetSeq);
        }
        if(sequentialDupAcks == 3) {
          synchronized(packets) {
            Iterator<TCPv2> it = packets.iterator();
            while(it.hasNext()){
              TCPv2 current = it.next();
              if(current.getSequence() == packetSeq){
                TCPend.resendPacket(cl, current);
                break;
              }
            }
          }
        }
        lastByteSeq = packetSeq;
        synchronized(packets) {
          Iterator<TCPv2> it = packets.iterator();
          while(it.hasNext()){
            TCPv2 current = it.next();
            long packetTime = packet.getTime();
            if(current.getTime() == packetTime){
              updateTO(packetTime);
              it.remove();
              break; 
            }
          }
        }
      }
    } else{ // server runs
      while(true){
        byte[] rData = new byte[BUF_SIZE];
        DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
        try{
          receive.receive(rPacket);
        } catch(IOException e) {
          break;
        }
        byte[] temp = rPacket.getData();
        TCPv2 packet = new TCPv2();
        packet.deserialize(temp);

        //if(packet.getFlag() != TCPv2.ACK) {
        //  sr.incrementPacketsRec();
        //  continue;
       // }
        TCPend.printPacketData(false, packet.getTime(), packet.getFlag(), packet.getSequence(), packet.getLength(), packet.getAck());
        sr.incrementPacketsRec();
        if(packet.getFlag() == TCPv2.FIN) {
          sr.setEndOfFile();
          TCPend.tearDownServer(sr, packet, receive);
          return;
        }
        if(packet.getChecksum() != packet.calculateChecksum()){
          sr.incrementPacketsWrongCheck();
          continue;
        }

        int packetLength = packet.getLength();

        int packetSeq = packet.getSequence();
        long packetTime = packet.getTime();
        int numBytesWritten;
        byte[] payload = packet.getPayload();
        int nextExpectedByte;
        sr.incrementData(packetLength);
        while(true){
          synchronized(buffer) {
            numBytesWritten = buffer.sWrite(payload, packetSeq, packetLength);
            nextExpectedByte = buffer.getNextExpectedByte();
          }
          if (numBytesWritten == -1) {
            sr.incrementPacketsOutOfSeq();
            break;
          }
          if (numBytesWritten > 0) { break; }
          try{ Thread.sleep(100);}
          catch(InterruptedException e) { return; }
        }
        TCPend.sendServerAck(nextExpectedByte, 0, packetTime);
      }
    }
  }
}

