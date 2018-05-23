package tcp;

import java.util.*;
import java.lang.*;

public class Buffer {
  static final int CLIENT = 0;
  static final int SERVER = 1;
  //static final int SIZE = 4000; 
  protected byte[] data;
  protected boolean[] dataMap;
  protected int LastByteAcked;
  protected int LastByteSent;
  protected int LastByteWritten;
  protected int LastByteRead;
  protected int NextByteExpected;
  protected int LastByteRcvd;
  protected int size;
  protected boolean endOfFile;

  public Buffer(int type, int size){

    data = new byte[size];
    dataMap = new boolean[size];
    this.size = size;
    if(type == CLIENT){
      LastByteAcked = 0;
      LastByteSent = 0;
      LastByteWritten = 0;
    } else if (type == SERVER){
      Arrays.fill(dataMap, false);
      LastByteRead = 0;
      NextByteExpected = 1;
      LastByteRcvd = 0;
    }
    endOfFile = false;
  }

  public int getLastByteAcked(){
    return LastByteAcked;
  }

  public void updateLastByteAcked(int seq){
    LastByteAcked = seq;
  }
  public int getLastByteSent(){
    return LastByteSent;
  }

  public int getLastByteWritten(){
    return LastByteWritten;
  }

  public int getLastByteRead(){
    return LastByteRead;
  }

  public int getNextExpectedByte(){
    return NextByteExpected;
  }

  public int getLastByteRcvd(){
    return LastByteRcvd;
  }

  public boolean reachedEndOfFile(){
    return endOfFile;
  }

  public void setEndOfFile(){
    endOfFile = true;
  }

  // false - buffer is full
  // true - byte was successfully written
  public boolean cWrite(byte b){
    if(LastByteWritten - LastByteSent == size) return false;
    else {
      //LastByteWritten increase by 1 after the byte is written
      data[(LastByteWritten % size)] = b;
      LastByteWritten += 1;
      return true;
    }
  }
  public byte[] cRead(int numBytes){
    if(numBytes > LastByteWritten - LastByteSent) return null; // Not enough data to read the numBytes requested
    else {

      if (LastByteSent % size > (LastByteSent+numBytes)%size){
        byte[] temp = Arrays.copyOfRange(data, (LastByteSent) % size, 
            data.length);
        if((LastByteSent+numBytes)%size == 0){
          byte temp2 = data[0];
          byte[] temp3 = new byte[temp.length + 1];
          for(int i=0; i<temp.length; i++){
            temp3[i]=temp[i];
          }
          temp3[temp3.length-1] = temp2;
          LastByteSent += numBytes;
          return temp3;
        } else {
          byte[] temp2 = Arrays.copyOfRange(data, 0, 
            (LastByteSent + numBytes)%size);
          int i;
          byte[] temp3 = new byte[temp.length + temp2.length];
          for(i=0; i<temp.length; i++){
            temp3[i]=temp[i];
          }

          for(int j=0; j<temp2.length; j++){
            temp3[i]=temp2[j];
            i++;
          }
          LastByteSent += numBytes;
          return temp3;
        }
      } else {
        byte[] temp = Arrays.copyOfRange(data, (LastByteSent) % size, 
            (LastByteSent+numBytes) % size);
        LastByteSent += numBytes;
        return temp;
        //return temp;
      }
    }
  }

  public byte sRead(){
    //LastByteRead < NextByteExpected
    //LastByteRead is actually LastByteRead + 1
    byte temp = data[LastByteRead % size];
    dataMap[LastByteRead % size] = false; // Indicates index is empty
    LastByteRead++;
    return temp;
  }

  //return -1 when data is duplicate
  //return 0  when theres not enough space
  //else returns how many bytes it wrote
  public int sWrite(byte[] b, int seq, int length){
    if((seq > LastByteRcvd) && 
        (LastByteRcvd + length - LastByteRead > size)){ //window too small
      return 0;
    } else {
      if((seq-1) < LastByteRcvd){ //Missing/Repeat segment
        if(dataMap[(seq  - 1)% size] == true) return -1;
        else{
          fill(b, seq, length);
          if(NextByteExpected == seq) {
            NextByteExpected += length;
            if(NextByteExpected -1 != LastByteRcvd){
              updateNextExpectedByte();
            }
          }
          return length;
        }
      } else {
        fill(b, seq, length);
        LastByteRcvd = (seq-1) + length;
        //LastByteRcvd += length;
        if(NextByteExpected == seq) {
          NextByteExpected += length;
          if(NextByteExpected-1 != LastByteRcvd){
            updateNextExpectedByte();
          }
        }
        return length;
      }
    }
  }

  private void fill(byte[] b, int seq, int length){
    int start = (seq -1) % size;
    int j = 0;
    for(int i = start; i < start + length; i++){
      data[i%size] = b[j]; 
      dataMap[i%size] = true;
      j++;
    }
  }

  private void updateNextExpectedByte(){
    while(true){
      if(dataMap[(NextByteExpected-1) % size] == true){
        NextByteExpected++;
      } else { break; }
    }
  }
}

//return -1 when data is duplicate
//return 0  when theres not enough space
//else returns how many bytes it wrote
//  public int sWrite(byte[] b, int seq){
//    if((seq > LastByteRcvd) && 
//       (LastByteRcvd + b.length - LastByteRead > size)){ //window too small
//      return 0;
//    } else {
//      if(seq < LastByteRcvd){ //Missing/Repeat segment
//        if(dataMap[(seq  - 1)% size] == true) return -1;
//        else{
//          fill(b, seq);
//          if(NextByteExpected == seq) {
//            NextByteExpected += b.length;
//            updateNextExpectedByte();
//          }
//          return b.length;
//        }
//      } else {
//        fill(b, seq);
//        LastByteRcvd += b.length;
//        if(NextByteExpected == seq) {
//          NextByteExpected += b.length;
//         updateNextExpectedByte();
//        }
//        return b.length;
//      }
//    }
//  }
