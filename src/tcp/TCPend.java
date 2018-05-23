package tcp;

import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.nio.charset.*;

public class TCPend {
  static final int TIMER = 100;
  static final int DEFAULT_PORT = 8888;
  static final int DEFAULT_MAX_RETRANSMIT = 16;
  static final int SYN = 4;
  static final int SYN_ACK = 5;
  static final int FIN = 2;
  static final int FIN_ACK = 3;
  static final int ACK = 1;
  static final int BUF_SIZE = 1472;
  static final double ALPHA = 0.875;
  static final double BETA = 0.75;
  static final int HEADER_SIZE = 24;

  static private int remotePort;
  static private int port;
  static private InetAddress addrs;
  static private DatagramSocket send;
  static private int mtu;
  static private int sws;
  static private String serverFilename;

  static Client cl;
  static Server sr;
  static Receiver listener;
  static Buffer buffer;
  static TimeOut retransmitter;

  public static void main(String[] args) {


    int bytesInFlight = 0;
    int clientLength = 12;
    int serverLength = 6;
    long sData;
    long rData;
    int seqPacket;
    int chkPacket;
    int retransmit;
    int dupAck;
    String remoteIP = "";
    String filename = "";
    int ssthresh = 65536;
    boolean client = false;
    boolean server = false;

    if(args.length == clientLength) client = true;
    if(args.length == serverLength) server = true;
    if(!client && !server){
      usage();
      return;
    }

    for(int i = 0; i < args.length; i++){
      String arg = args[i];
      if(arg.equals("-p"))
      { port = Integer.parseInt(args[++i]); }
      else if (arg.equals("-s"))
      { remoteIP = args[++i]; }
      else if (arg.equals("-a"))
      { remotePort = Integer.parseInt(args[++i]); }
      else if (arg.equals("-f"))
      { filename = new String(args[++i]); }
      else if (arg.equals("-m"))
      { mtu = Integer.parseInt(args[++i]); }
      else if (arg.equals("-c"))
      { sws = Integer.parseInt(args[++i]); }
    }

    if(client){
      cl = new Client();
      cl.setAdvertWindow(sws * mtu);
      buffer = new Buffer(Buffer.CLIENT, 2 * mtu * sws);

      ClientWriter writer = new ClientWriter(buffer, filename);
      try{
        addrs = InetAddress.getByName(remoteIP);
      } catch (UnknownHostException e){
        return;
      }
      DatagramSocket receive;
      try{
        receive = new DatagramSocket(port);
      } catch (SocketException e){
        return;
      }
      try{
        send = new DatagramSocket();
      } catch (SocketException e){
        return;
      }

      retransmitter = new TimeOut(cl);

      initializeTCP(receive);


      listener = new Receiver(cl, Buffer.CLIENT, receive);

      //sendFilename(filename, receive);

      while(true){
        if(buffer.reachedEndOfFile() && 
            (buffer.getLastByteWritten() - buffer.getLastByteSent() <= mtu - HEADER_SIZE)){
          if(buffer.getLastByteWritten() - buffer.getLastByteSent() == 0) break;
          while(!sendData(buffer.getLastByteWritten() - buffer.getLastByteSent())){
            try{
              Thread.sleep(1000);
            } catch (InterruptedException e){
              break;
            }
          }
          break;
            }
        while(!sendData(mtu-HEADER_SIZE)){
          try{
            Thread.sleep(1000);
          } catch (InterruptedException e){
            break;
          }
        }
      }

      while(true){
        try{ 
          if(cl.isEmpty()) { break;}
          Thread.sleep(TIMER);
        } catch (InterruptedException e) { 
          break; 
        }
      }

      clientFin();
      try{ 
        Thread.sleep(5000);
      } catch (InterruptedException e) { 
        return; 
      }

      printClient();
      receive.close();
      System.exit(0);
    } else{   // server runs
      sr = new Server(); 
      buffer = new Buffer(Buffer.SERVER, 2 * mtu * sws);
      try{
        send = new DatagramSocket();
      } catch (SocketException e){
        return;
      }
      DatagramSocket receive;
      try{
        receive = new DatagramSocket(port);
      } catch (SocketException e){
        return;
      }
      retransmitter = new TimeOut(sr);
      initialListen(receive);
      //receiveFilename(receive);

      File tmp = new File("results.txt");
      //if (!tmp.getParentFile().exists()) {
      //  tmp.getParentFile().mkdirs();
      //}
      //System.out.println("Trying to create /results/results.txt");
      //try{
      //  if(tmp.createNewFile()){
      //    System.out.println("/results/results.txt was created!");
      //  } else{
      //    System.out.println("/results/results.txt already exists");
      //  }

      //} catch (IOException e) {
      //  e.printStackTrace();
      //}

      listener = new Receiver(sr, Buffer.SERVER, receive, buffer); 
      try{ 
        Thread.sleep(TIMER);
      } catch (InterruptedException e) { 
        return; 
      }
      ServerReader reader = new ServerReader(buffer, tmp, sr);

      while(true){
        try{ 
          if(sr.isEmpty() && sr.getEndOfFile() ) {break;}
          Thread.sleep(TIMER);
        } catch (InterruptedException e) { 
          return; 
        }
      }
      try{ 
        Thread.sleep(5000);
      } catch (InterruptedException e) { 
        return; 
      }

      receive.close();
      printServer();
      System.exit(0);

    }
  }

  protected static void printPacketData(boolean type, long time, int flag, int sequence, int length, int ack){
    // type = True for sending data and False for receiving data

    String output1;
    if(type){ output1 = "snd ";}
    else { output1 = "rcv ";}
    double decimalTime = (double)time / ((double)1E9);

    String output2 = String.format(output1 + "%.3f ", decimalTime);
    String output3;
    switch(flag) {
      case 0: output3 = "---D ";
              break;
      case 1: output3 = "-A-- ";
              break;
      case 2: output3 = "--F- ";
              break;
      case 3: output3 = "-AF- ";
              break;
      case 4: output3 = "S--- ";
              break;
      case 5: output3 = "SA-- ";
              break;
      default: output3 = "---- ";
               break;
    }

    System.out.println(output2 + output3 + sequence + " " + length + " " + ack);


  }
  private static void printClient(){
    System.out.println("Amount of Data Transferred: " + cl.getAmountData());
    System.out.println("No of Packets Sent: " + cl.getPacketsSent() + " / Received: " + cl.getPacketsRec());
    System.out.println("No of Retransmission: " + cl.getRetrans());
    System.out.println("No of Duplicate Acknowledgements: " + cl.getDupAcks());
  }

  private static void printServer(){
    System.out.println("Amount of Data Transferred: " + sr.getAmountData());
    System.out.println("No of Packets Sent: " + sr.getPacketsSent() + " / Received: " + sr.getPacketsRec());
    System.out.println("No of Packets discarded (out of sequence): " + sr.getPacketsOutOfSeq());
    System.out.println("No of Packets discarded (wrong checksum): " + sr.getPacketsWrongCheck());
    System.out.println("No of Retransmission: " + sr.getRetrans());
  }
  private static boolean sendData(int numBytes){
    // EDIT: we don't need to use congestionwindow, just sliding window
    if(cl.getAdvertWindow() < numBytes){
      return false;
    }
    TCPv2 packet = new TCPv2();
    int byteSeq = cl.getLastByteSent() + 1;
    packet.setSequence(byteSeq);
    //packet.setAck(cl.getNextExpectedByte() + 1);
    packet.setAck(1);
    byte[] temp = buffer.cRead(numBytes);
    while(temp == null){
      try{
        Thread.sleep(100);
        temp = buffer.cRead(numBytes);
      } catch (InterruptedException e){
        return false;
      }
    }
    cl.incrementLastByteSent(numBytes);
    packet.setLength(numBytes);
    packet.setPayload(temp);
    packet.setFlag(0);
    packet.setTime();
    long packetTime = packet.getTime();
    byte[] temp2 = packet.serialize();
    cl.addPacket(packet);
    DatagramPacket sPacket = new DatagramPacket(temp2, temp2.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    cl.incrementData(numBytes);
    cl.setAdvertWindow(cl.getAdvertWindow() - numBytes);
    cl.incrementPacketsSent();
    printPacketData(true, packetTime, 0, byteSeq, numBytes, 1);
    return true;
  }

  protected static void resendPacket(Client cl, TCPv2 packet){
    // EDIT: Don't need to synchronize because calling Thread already has
    // synchronized on packet list 
    byte[] temp;
    int packetLength;
    packet.setTime();
    long packetTime = packet.getTime();
    int byteSeq = packet.getSequence();
    temp = packet.serialize();
    packetLength = packet.getLength();
    DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    cl.incrementData(packetLength);
    cl.incrementRetrans();
    cl.incrementPacketsSent();
    printPacketData(true, packet.getTime(), packet.getFlag(), packet.getSequence(), packet.getLength(), packet.getAck());
  }

  protected static void resendPacket(Server sr, TCPv2 packet){
    // Don't need to synchronize because calling method does.
    byte[] temp;
      packet.setTime();
      temp = packet.serialize();
    DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    sr.incrementRetrans();
    sr.incrementPacketsSent();
    printPacketData(true, packet.getTime(), packet.getFlag(), packet.getSequence(), packet.getLength(), packet.getAck());
  }

  protected static void sendServerAck(int packetSeq, int length, long time){
    TCPv2 ackPacket = new TCPv2();
    ackPacket.setSequence(0);

    // Setting ACK sequence based on sequence and length of original packet
    // or what was handed to method (currently buffer.getNextExpecetd byte
    // and 0)
    ackPacket.setAck(packetSeq+length);
    ackPacket.setLength(0);
    ackPacket.setFlag(TCPv2.ACK);
    ackPacket.setTime(time);
    byte[] temp = ackPacket.serialize();
    try{
      DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
      send.send(sPacket);
    } catch(IOException e){
      return;
    }
    printPacketData(true, ackPacket.getTime(), TCPv2.ACK, 0, 0, packetSeq + length);
  }

  private static long getRTT(long time){
    return System.nanoTime() - time;
  }


  private static void sendFilename(String filename, DatagramSocket receive){
    TCPv2 fname = new TCPv2();
    fname.setSequence(cl.getLastByteSent() + 1);
    fname.setAck(cl.getNextExpectedByte() + 1);
    byte[] temp = filename.getBytes();
    int filenameSize = temp.length;
    cl.setFileNameSize(filenameSize);
    cl.incrementLastByteSent(filenameSize);
    fname.setLength(filenameSize);
    fname.setFlag(0);
    fname.setPayload(temp);
    fname.setTime();
    byte[] temp2 = fname.serialize();
    cl.addPacket(fname);
    DatagramPacket sPacket = new DatagramPacket(temp2, temp2.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    cl.incrementPacketsSent();
    cl.incrementData(filenameSize);
    cl.setAdvertWindow(cl.getAdvertWindow() - filenameSize);
    byte[] rBuffer = new byte[BUF_SIZE];
    DatagramPacket rData = new DatagramPacket(rBuffer, rBuffer.length);
    try{
      receive.receive(rData);
    } catch (IOException e){
      System.exit(1);
    }
    TCPv2 serverAck = new TCPv2();
    serverAck.deserialize(rData.getData());
    updateTO(serverAck.getTime());
  }

  private static void receiveFilename(DatagramSocket receive){
    byte[] rBuffer = new byte[BUF_SIZE];
    DatagramPacket rData = new DatagramPacket (rBuffer, rBuffer.length);
    try{
      receive.receive(rData);
    } catch (IOException e){
      System.exit(1);
    }
    sr.incrementPacketsRec();
    TCPv2 ackPacket = new TCPv2();
    byte[] packet = rData.getData();
    TCPv2 fname = new TCPv2();
    fname.deserialize(packet);
    serverFilename = new String(fname.getPayload());
    ackPacket.setSequence(0);
    sr.incrementNextExpectedByte(fname.getLength());
    ackPacket.setAck(sr.getNextExpectedByte() + 1);
    ackPacket.setLength(0);
    ackPacket.setFlag(ACK);
    ackPacket.setTime();
    byte[] temp = ackPacket.serialize();
    try{
      DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
  }

  private static void initialListen(DatagramSocket receive){
    byte[] rBuffer = new byte[BUF_SIZE];
    DatagramPacket rData = new DatagramPacket (rBuffer, rBuffer.length);
    try{
      receive.receive(rData);
    } catch (IOException e){
      System.exit(1);
    }
    sr.incrementPacketsRec();
    addrs = rData.getAddress();
    remotePort = port;
    TCPv2 synAckPacket = new TCPv2();
    byte[] packet = rData.getData();
    TCPv2 synPacket = new TCPv2();

    synPacket.deserialize(packet);

    printPacketData(false, synPacket.getTime(), synPacket.getFlag(), synPacket.getSequence(), synPacket.getLength(), synPacket.getAck());
    synAckPacket.setSequence(0);
    int ackSeq = synPacket.getSequence() + 1;
    synAckPacket.setAck(ackSeq);
    synAckPacket.setFlag(TCPv2.SYN_ACK);
    synAckPacket.setLength(0);
    synAckPacket.setTime();
    long packetTime = synAckPacket.getTime();
    byte[] temp = synAckPacket.serialize();
    sr.addPacket(synAckPacket);
    try{
      DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }

    printPacketData(true, packetTime, TCPv2.SYN_ACK, 0, 0, ackSeq);
    sr.incrementPacketsSent();
    TCPv2 serverAck = new TCPv2();
    while(true){
      byte[] rBuffer2 = new byte[BUF_SIZE];

      DatagramPacket rData2 = new DatagramPacket(rBuffer2, rBuffer2.length);
      try{
        receive.receive(rData2);
      } catch (IOException e){
        System.exit(1);
      }
      serverAck.deserialize(rData2.getData());
      if(serverAck.getFlag() == TCPv2.ACK) { break;}
      else { 
        sr.incrementPacketsRec();
        printPacketData(false, serverAck.getTime(), serverAck.getFlag(), serverAck.getSequence(), serverAck.getLength(), serverAck.getAck());
      }

    }
    updateTOServer(serverAck.getTime());
    removeSynAckOrFinAckPacket(serverAck.getAck());
    printPacketData(false, serverAck.getTime(), serverAck.getFlag(), serverAck.getSequence(), serverAck.getLength(), serverAck.getAck());
  }

  protected static void tearDownServer(Server sr, TCPv2 finPacket, DatagramSocket socket){
    TCPv2 finAckPacket = new TCPv2();
    finAckPacket.setSequence(0);  
    int ackSeq = finPacket.getSequence() + 1;
    finAckPacket.setAck(ackSeq);
    finAckPacket.setLength(0);
    finAckPacket.setFlag(FIN_ACK);
    finAckPacket.setTime();
    long packetTime = finAckPacket.getTime();
    byte[] packet = finAckPacket.serialize();
    sr.addPacket(finAckPacket);
    DatagramPacket sPacket = new DatagramPacket(packet, packet.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    sr.incrementPacketsSent();

    printPacketData(true, packetTime, TCPv2.FIN_ACK, 0, 0, ackSeq);
    TCPv2 ackPacket = new TCPv2();
    while(true){
    byte[] rData = new byte[BUF_SIZE];
    DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
    try{
      socket.receive(rPacket);
    } catch(IOException e){
      return;
    }
    byte[] temp = rPacket.getData();
    ackPacket.deserialize(temp);
    if(ackPacket.getFlag() == TCPv2.ACK){
      break;
    } else {
      sr.incrementPacketsRec();}
      printPacketData(false, ackPacket.getTime(), ackPacket.getFlag(), ackPacket.getSequence(), ackPacket.getLength(), ackPacket.getAck());
    }
    // Don't need line below i guess
    //updateTOServer(ackPacket.getTime());
    removeSynAckOrFinAckPacket(ackPacket.getAck());
    printPacketData(false, ackPacket.getTime(), ackPacket.getFlag(), ackPacket.getSequence(), ackPacket.getLength(), ackPacket.getAck());
  }

  private static void clientFin(){
    TCPv2 serverAck = new TCPv2();
    TCPv2 finPacket = new TCPv2();
    int byteSeq = cl.getLastByteSent();
    finPacket.setSequence(byteSeq);
    int ackSeq = cl.getNextExpectedByte() + 1;
    finPacket.setAck(ackSeq);
    finPacket.setLength(0);
    finPacket.setFlag(FIN);
    finPacket.setTime();
    long packetTime = finPacket.getTime();
    byte[] packet = finPacket.serialize();
    cl.addPacket(finPacket);
    DatagramPacket sPacket = new DatagramPacket(packet, packet.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }
    cl.incrementPacketsSent();

    printPacketData(true, packetTime, TCPv2.FIN, byteSeq, 0, ackSeq);
  }

  protected static void tearDownClient(Client cl, TCPv2 finAckPacket){
    removeSynOrFinPacket(finAckPacket.getAck());
    TCPv2 ackPacket = new TCPv2();
    int byteSeq = cl.getLastByteSent();
    ackPacket.setSequence(byteSeq);
    int ackSeq = finAckPacket.getSequence() + 1;
    ackPacket.setAck(ackSeq);
    ackPacket.setLength(0);
    ackPacket.setFlag(ACK);
    long ackTime = finAckPacket.getTime();
    ackPacket.setTime(ackTime);
    byte[] packet = ackPacket.serialize();
    DatagramPacket sPacket = new DatagramPacket(packet, packet.length, addrs, remotePort);
    try{
      send.send(sPacket);
    } catch (IOException e){
      return; 
    }
    printPacketData(true, ackTime, TCPv2.ACK, byteSeq, 0, ackSeq);
  }

  private static void usage(){
    System.out.println("Client Usage: ");
    System.out.println("TCPend -p <port> -s <remote-IP> -a <remote-port> -f <filename> -m <mtu> -c <sws>\n");
    System.out.println("Server Usage: ");
    System.out.println("TCPend -p <port> -m <mtu> -c <sws>");
  }

  //returns the next byte sequence to send
  private static void initializeTCP(DatagramSocket receive){
    TCPv2 synPacket = new TCPv2();
    byte[] temp;
    synPacket.setSequence(0);
    synPacket.setAck(0); 
    synPacket.setLength(0);
    synPacket.setFlag(TCPv2.SYN);
    synPacket.setTime();
    long synPacketTime = synPacket.getTime();
    temp = synPacket.serialize();
    cl.addPacket(synPacket);
    DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);

    try{
      send.send(sPacket);
    } catch (IOException e){
      System.exit(1); 
    }

    printPacketData(true, synPacketTime, TCPv2.SYN, 0, 0, 0);

    cl.incrementPacketsSent();
    byte[] temp2 = new byte[BUF_SIZE];
    DatagramPacket rPacket = new DatagramPacket(temp2, temp2.length);
    try{
      receive.receive(rPacket);
    } catch (IOException e){
      System.exit(1);
    }
    cl.incrementPacketsRec();
    byte[] packet = rPacket.getData();
    TCPv2 serverSynAck = new TCPv2();
    serverSynAck.deserialize(packet);
    cl.seteRTT(getRTT(synPacketTime));
    int filenameSize;
    cl.setTimeout(2 * cl.geteRTT());

    long packetTime = serverSynAck.getTime();
    printPacketData(false, packetTime, serverSynAck.getFlag(), serverSynAck.getSequence(), serverSynAck.getLength(), serverSynAck.getAck());

    TCPv2 ackPacket = new TCPv2();
    ackPacket.setSequence(0);
    ackPacket.setAck(1);
    ackPacket.setLength(0);
    ackPacket.setFlag(TCPv2.ACK);
    ackPacket.setTime(packetTime);
    byte[] temp3 = ackPacket.serialize();
    DatagramPacket sPacket2 = new DatagramPacket(temp3, temp3.length, addrs, remotePort);
    try{
      send.send(sPacket2);
    } catch (IOException e){
      System.exit(1); 
    }
    removeSynOrFinPacket(serverSynAck.getAck());

    printPacketData(true, packetTime, TCPv2.ACK, 0, 0, 1);
  }

  private static void removeSynOrFinPacket(int ackSeq){
    LinkedList<TCPv2> packets = cl.getPacketList();
    synchronized(packets){
      Iterator<TCPv2> it = packets.iterator();
      while(it.hasNext()){
        TCPv2 current = it.next();
        if(current.getSequence() == (ackSeq-1)){
          // ackSeq should be 1 more than byteSeq sent in SYN or FIN packet
          it.remove();
          return;
        }
      }
    }
  }

  private static void removeSynAckOrFinAckPacket(int ackSeq){
    int i=0;
    LinkedList<TCPv2> packets = sr.getPacketList();
    synchronized(packets){
      Iterator<TCPv2> it = packets.iterator();
      while(it.hasNext()){
        i++;
        TCPv2 current = it.next();
        if(current.getSequence() == (ackSeq-1)){
          // ackSeq should be 1 more than byteSeq sent in SYN_ACK or FIN_ACK packet
          it.remove();
          return;
        }
      }
    }
  }

  private static void updateTO(long time){
    double eRTT = cl.geteRTT();
    double eDEV = cl.geteDEV();
    double sRTT = (double) (System.nanoTime() - time);
    double sDEV = Math.abs(sRTT - eRTT);
    cl.seteRTT(ALPHA * eRTT + (1 - ALPHA) * sRTT);
    cl.seteDEV(BETA * eDEV + (1 - BETA) * sDEV);
    cl.setTimeout(eRTT + 4 * eDEV);
  }

  private static void updateTOServer(long time){
    double eRTT = sr.geteRTT();
    double eDEV = sr.geteDEV();
    double sRTT = (double) (System.nanoTime() - time);
    double sDEV = Math.abs(sRTT - eRTT);
    sr.seteRTT(ALPHA * eRTT + (1 - ALPHA) * sRTT);
    sr.seteDEV(BETA * eDEV + (1 - BETA) * sDEV);
    sr.setTimeout(eRTT + 4 * eDEV);
  }

  protected static void updateWindow(Client cl, int seq){
    // buffer.updateLastByteAcked(seq - 1 - cl.getFileNameSize());
    buffer.updateLastByteAcked(seq-1);
    cl.setAdvertWindow(sws * mtu - (cl.getLastByteSent() - (seq - 1)));
  }
}

//  public void incrementWindow(){
//    advertWindow++;
//  }
//  public void updateSlowStart(boolean status){
//    slowStart = status;
//  }
//  public void updateFastRecovery(){
//    congestionWindow /= 2; 
//  }
// public void updateSsthresh(){
//    ssthresh = congestionWindow / 2;
//  }
// EDIT: we don't need to do slowstart or fastrecovery
//  private static double getTransmissionWindow() {
//    if(listener.getCongestionWindow() < cl.getAdvertWindow()) {
//      return listener.getCongestionWindow();
//    } else {
//      return cl.getAdvertWindow();
//    }
//  }
//After slowStart timeout
//  public void updateCongestion(double cwnd){
//    if(slowStart){
//When sender receives an ACK
//      congestWindow += 1;
//    } else (dupAck){
//Need specific conditions for each window updates
//On each new ack
//      congestWindow = cwnd; 
//    }
//congestWindow = congestionWindow + 1/congestionWindow; 
//One each additional dupack
//congestWindow += 1;
//On triple dupack
//fastRetransmit();
//On new ack
//congestWindow = SSTHRESH;
//  }
//When receives 3 duplicate ACKs (Ex. ACK:2 ACK:2 ACK:2 (Dup) ACK:2 (Tri)) =
//4 same ACKs
//  private void fastRetransmit(int seq){ //seq = missing/out-of-order seq
//    TCPv2 packet = new TCPv2();
//    packet.setSequence(seq);
//    packet.setAck(0);
//    packet.setLength(0);
//    packet.setFlag(0);
//   packet.setTime();
//    byte[] temp = packet.serialize();
//    DatagramPacket sPacket = new DatagramPacket(temp, temp.length, addrs, remotePort);
//    send.send(sPacket);
//    congestionWindow += 3;
//  }
//  private fastRecovery(){
//
//  }
//  private retransitTO(){

//  }
// True: data sent successfully
// False: data wasn't sent because window was too small
//private boolean sendData(DatagramSocket send){
//if(getTransmissionWindow() < mtu){
//return false;
//} else {
//byte[] temp = buffer.cRead(mtu);
//if(temp == null) return false; //null = data < mtu
//TCPv2 data = new TCPv2();
//data.setSequence(x + 1);
//x += temp.length;
//data.setLength(temp.length);
//data.setFlag(0);
//data.setAck(y + 1);
//data.setPayload(temp);
//data.setTime();
//synchronized(packets){
//packets.add(data);
//}
//byte[] temp2 = data.serialize();
//DatagramPacket packet = new DatagramPacket(temp2, temp2.length, addrs, remotePort);

//return true;
//}
//}
//  public void updateTO(long time){
//    long sRTT = System.nanoTime() - time;
//    long sDev = Math.abs(sRTT - eRTT);
//    eRTT = ALPHA * eRTT + (1 - ALPHA) * sRTT;
//    eDev = BETA * eDev + (1 - BETA) * sDev;
//Synchronize
//    timeout = eRTT + 4 * eDev;
//  }
//  /x
//
