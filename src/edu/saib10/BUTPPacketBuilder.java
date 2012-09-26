/**
 * This interface is used to implement BUTP packet
 */
package edu.saib10;

/**
 * @author Sadh Ibna Rahmat
 * @version 1.0
 * @since 2012-03-04
 */
public interface BUTPPacketBuilder {
   
    void setBUTPPayload(byte[] payload);
    void setBUTPByteArray(byte[] BUTPByteArray);
    void setSequenceNumber(int sequence);
    void setAcknowledgeNumber(int acknowledge);
    void setOptions(short options);
    void setWindowSize( int window);
    void setFlags(byte flag);
    byte[] getBUTPPayload();
    byte[] getBUTPByteArray();
    int getSequenceNumber();
    int getAcknowledgeNumber();
    short getCheckSum();
    short getOptions();
    int getWindowSize();
    byte getFlags();
    short computeCheckSum();
}
