package tcp;

import java.nio.ByteBuffer;
import java.lang.*;
import java.io.*;

public class TCPv2 {
  public static final int SYN = 4;
  public static final int FIN = 2;
  public static final int ACK = 1;
  public static final int FIN_ACK = 3;
  public static final int SYN_ACK = 5;
  protected int sequence;
  protected int ack;
  protected long time;
  protected int length;
  protected int flag;
  protected int lenAndFlag;
  protected short checksum;
  protected byte[] payload;
  protected int timer;
  private int numRetransmits;

  // return byte sequence
  public int getSequence() {
    return this.sequence;
  }

  // set byte sequence
  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  // return acknowledge (next expected byte sequence)
  public int getAck() {
    return this.ack;
  }

  // set ack
  public void setAck(int ack) {
    this.ack = ack;
  }

  // return checksum
  public short getChecksum() {
    return checksum;
  }

  public void resetChecksum() {
    this.checksum = 0;
  }

  public short calculateChecksum(){
    short tempCheck;
    int accumulation = 0;

    // checksum is just the 1s compliment of the bytesequence
    accumulation += ((sequence >> 16) & 0xffff) + (sequence & 0xffff);
    accumulation = ((accumulation >> 16) & 0xffff) + (accumulation & 0xffff);
    tempCheck = (short) (~accumulation & 0xffff);
    return tempCheck;
  }

  // return long for time
  public long getTime() {
    return time;
  }

  public void setTime(){
    time = System.nanoTime();
  }

  public void setTime(long time){
    this.time = time;
  }

  public int getLength() {  
    return length;
  }

  public void setLength(int length) {
    this.length = length ;
  }

  public int getFlag(){
    return this.flag;
  }

  public void setFlag(int flag){
    this.flag = flag;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public void incrementRetransmits(){
    numRetransmits++;
  }

  public int getNumRetransmits(){
    return numRetransmits;
  }

  // to calculate checksum call serialize
  public byte[] serialize() {
    int tempLength = 24;  // length in bytes = 6 * 4
    if (payload != null) {
      tempLength += this.length;
    }
    byte[] data = new byte[tempLength];
    ByteBuffer bb = ByteBuffer.wrap(data);

    bb.putInt(this.sequence);
    bb.putInt(this.ack);
    bb.putLong(this.time);

    int tempLenAndFlag = (length << 3) & 0xFFFFFFF8;
    //this.lenAndFlag = (length << 3) & 0xFFFFFFF8;
    //lenAndFlag = lenAndFlag << 3;
    //lenAndFlag = lenAndFlag & 0xFFFFFFF8;
    int tempFlag = flag & 0x00000007;
    //flag = flag & 0x00000007;
    //lenAndFlag += flag;
    this.lenAndFlag = tempLenAndFlag | tempFlag;
    //lenAndFlag += flag;
    bb.putInt(this.lenAndFlag);
    bb.putShort((short)0x0000);

    // EDIT: always compute checksum even if it's not 0.
    // computing checksum, if needed
    //if (this.checksum == 0) {
    int accumulation = 0;

    // checksum is just the 1s compliment of the bytesequence
    accumulation += ((sequence >> 16) & 0xffff) + (sequence & 0xffff);
    accumulation = ((accumulation >> 16) & 0xffff) + (accumulation & 0xffff);
    this.checksum = (short) (~accumulation & 0xffff);
    //}
    bb.putShort(checksum);

    if(payload != null) {
      bb.put(payload);
    }

    return data;
  }

  // TODO: write deserialize function
  public TCPv2 deserialize(byte[] data) {
    ByteBuffer bb = ByteBuffer.wrap(data);

    this.sequence = bb.getInt();
    this.ack = bb.getInt();
    this.time = bb.getLong();
    this.lenAndFlag = bb.getInt();
    //this.flag = lenAndFlag;
    this.flag = lenAndFlag & 0x00000007;
    //this.flag = this.flag & 0x00000007;
    //this.length = (lenAndFlag >> 3);
    //this.length = this.length & 0x1FFFFFFF;
    this.length = (lenAndFlag>>3) & 0x1FFFFFFF;

    // discarding 16 zeros before checksum
    short zero = bb.getShort();
    this.checksum = bb.getShort();

    // data.length = total length of data passed to deserialze (i.e. byte[]
    // representation of TCPv2) and we've already read 24 bytes (TCPv2 header
    // length = 24 bytes)
    if((this.length) == 0) {
      this.payload = null;
    } else {
      try {
        this.payload = new byte[this.length];
        bb.get(this.payload, 0, this.length);
      } catch (IndexOutOfBoundsException e) {
        this.payload = null;
      }
    }
    return this;
  }
}

