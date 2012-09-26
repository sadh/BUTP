/** This is an implementation of BUTP packet.
 * It can encapsulate the BUTP header and BUTP payload. 
 * 
 */
package edu.saib10;

import java.nio.ByteBuffer;

/**
 *
 * @author Sadh Ibna Rahmat
 * @version 1.0
 * @since 2012-03-02
 */
public class BUTPPacket implements BUTPPacketBuilder{
    static final int PAYLOAD = 1420;
    static final int BUTP_HEADER = 16;
    private static final int SEQ_POSITION = 0;
    private static final int ACK_POSITION = 4;
    private static final int CHECK_POSITION = 8;
    private static final int OPT_POSITION = 10;
    private static final int WIND_POSITION = 12;
    private static final int PAYLOAD_POSITION = 16;
    private int sequence;
    private int acknowledge;
    private  short checksum;
    private short options;
    private byte flag;
    private  int window;
    private byte[] payload;
    private byte[] BUTPByteArray;
    private ByteBuffer buffer;
    
    public BUTPPacket(){
        BUTPByteArray = new byte[PAYLOAD+BUTP_HEADER];
        buffer = ByteBuffer.wrap(BUTPByteArray);
    }
    
    public BUTPPacket(int window,byte flag){
        this.window = window;
        this.flag = flag;
        options = (short)this.flag;
        sequence = 0;
        acknowledge = 0;
        BUTPByteArray = new byte[BUTP_HEADER];
        buffer = ByteBuffer.wrap(BUTPByteArray);
        buffer.putInt(SEQ_POSITION,0);
        buffer.putInt(ACK_POSITION,0);
        buffer.putShort(OPT_POSITION, options);
        buffer.putInt(WIND_POSITION, window);
               
        
    }
    public BUTPPacket(int acknowledge,int window,byte flag){
        sequence = 0;
        this.acknowledge = acknowledge;
        this.window = window;
        this.flag = flag;
        options = (short)this.flag;
        BUTPByteArray = new byte[BUTP_HEADER];
        buffer = ByteBuffer.wrap(BUTPByteArray);
        buffer.putInt(SEQ_POSITION,0);
        buffer.putInt(ACK_POSITION,this.acknowledge);
        buffer.putShort(OPT_POSITION, options);
        buffer.putInt(WIND_POSITION, window);
    }
    
    public BUTPPacket(byte[] payload ,int sequence,int window,byte flag ){
        
        BUTPByteArray = new byte[PAYLOAD+BUTP_HEADER];
        buffer = ByteBuffer.wrap(BUTPByteArray);
        if(payload != null && payload.length <= PAYLOAD){
            this.payload = payload;
            this.sequence = sequence;
            this.window = window;
            this.flag = flag;
            options = (short)flag;
            buffer.putInt(SEQ_POSITION,sequence);
            buffer.putInt(ACK_POSITION,0);
            buffer.putShort(OPT_POSITION, options);
            buffer.putInt(WIND_POSITION, this.window);
            buffer.position(PAYLOAD_POSITION);
            buffer.put(payload, 0, payload.length);
            setCheckSum(computeCheckSum());
            
        }
        
    }

    @Override
    public void setBUTPPayload(byte[] payload) {
        this.payload = payload;
        if(payload != null && payload.length <= PAYLOAD){
            buffer.position(PAYLOAD_POSITION);
            buffer.put(payload, 0, payload.length);
            setCheckSum(computeCheckSum());
        }
        
    }

    @Override
    public void setBUTPByteArray(byte[] BUTPByteArray) {
        if(BUTPByteArray != null && BUTPByteArray.length <= (BUTP_HEADER + PAYLOAD)){
            buffer.put(BUTPByteArray);
            sequence = buffer.getInt(SEQ_POSITION);
            acknowledge = buffer.getInt(ACK_POSITION);
            options = buffer.getShort(OPT_POSITION);
            flag = (byte)options;
            window = buffer.getInt(WIND_POSITION);
            checksum = buffer.getShort(CHECK_POSITION);
            if(BUTPByteArray.length > BUTP_HEADER){
                buffer.position(PAYLOAD_POSITION);
                payload = new byte[BUTPByteArray.length - BUTP_HEADER];
                buffer.get(payload, 0,payload.length);
            }
            
        }
        
    }

    @Override
    public void setSequenceNumber(int sequence) {
        this.sequence = sequence;
        buffer.putInt(SEQ_POSITION, this.sequence);
    }

    @Override
    public void setAcknowledgeNumber(int acknowledge) {
        this.acknowledge = acknowledge;
        buffer.putInt(ACK_POSITION, this.acknowledge);
    }

    private void setCheckSum(short checksum) {
        this.checksum = checksum;
        buffer.putShort(CHECK_POSITION, this.checksum);
    }

    @Override
    public void setOptions(short options) {
        this.options = options;
        buffer.putShort(OPT_POSITION, this.options);
    }

    @Override
    public void setWindowSize(int window) {
        this.window = window;
        buffer.putInt(WIND_POSITION, this.window);
    }

    @Override
    public void setFlags(byte flag) {
        this.flag = flag;
        options =  (short)this.flag;
        buffer.putShort(OPT_POSITION, options);
    }

    @Override
    public byte[] getBUTPPayload() {
        return payload;
    }

    @Override
    public byte[] getBUTPByteArray() {
        return BUTPByteArray;
    }

    @Override
    public int getSequenceNumber() {
        return sequence;
    }

    @Override
    public int getAcknowledgeNumber() {
        return acknowledge;
    }

    @Override
    public short getCheckSum() {
        return checksum;
    }

    @Override
    public short getOptions() {
        return options;
    }

    @Override
    public int getWindowSize() {
        return window;
    }

    @Override
    public byte getFlags() {
        return flag;
    }

    @Override
    public final short computeCheckSum() {
        int sum = 0;
        buffer.rewind();
        buffer.position(PAYLOAD_POSITION);
	while (buffer.hasRemaining()) {
	    if ((sum & 1) != 0)
		sum = (sum >> 1) + 0x800;
	    else
		sum >>= 1;
	    sum += buffer.get() & 0xff;
	    sum &= 0xffff;
	}
	return (short)sum;
    }
}
