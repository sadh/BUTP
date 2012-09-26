/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.saib10;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sadh
 */
public class BUTPPacketTest {
    
    public BUTPPacketTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setBUTPPayload method, of class BUTPPacket.
     */
    @Test
    public void testSetBUTPPayload() {
        System.out.println("setBUTPPayload");
        byte[] payload = {7,7,7,7,7,7,7,7,7,7};
        BUTPPacket instance = new BUTPPacket(payload, 0, 8192, (byte)0);
        short chk_pre = instance.getCheckSum();
        instance.setBUTPPayload(payload);
        // TODO review the generated test code and remove the default call to fail.
        assertArrayEquals(payload, instance.getBUTPPayload());
    }

    /**
     * Test of setBUTPByteArray method, of class BUTPPacket.
     */
    @Test
    public void testSetBUTPByteArray() {
        System.out.println("setBUTPByteArray");
        BUTPPacket packet = new BUTPPacket(new byte[]{1,2,3,4,5,6,7,8,9}, 0, 8192, (byte)0);
        byte[] BUTPByteArray = packet.getBUTPByteArray();
        BUTPPacket instance = new BUTPPacket();
        instance.setBUTPByteArray(BUTPByteArray);
        // TODO review the generated test code and remove the default call to fail.
        assertArrayEquals(BUTPByteArray, instance.getBUTPByteArray());
    }

    /**
     * Test of setSequenceNumber method, of class BUTPPacket.
     */
    @Test
    public void testSetSequenceNumber() {
        System.out.println("setSequenceNumber");
        int sequence = 0;
        BUTPPacket instance = new BUTPPacket(new byte[1024], sequence, 0, (byte)0);
        instance.setSequenceNumber(sequence);
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(sequence, instance.getSequenceNumber());
    }

    /**
     * Test of setAcknowledgeNumber method, of class BUTPPacket.
     */
    @Test
    public void testSetAcknowledgeNumber() {
        System.out.println("setAcknowledgeNumber");
        int acknowledge = 0;
        BUTPPacket instance = new BUTPPacket(11, 8192, (byte)0);
        instance.setAcknowledgeNumber(acknowledge);
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(acknowledge, instance.getAcknowledgeNumber());
    }

    /**
     * Test of setCheckSum method, of class BUTPPacket.
     */
    @Test
    public void testSetCheckSum() {
        System.out.println("setCheckSum");
        BUTPPacket instance = new BUTPPacket(new byte[]{1,2,3,4,5,6,7,8,9}, 0, 8192, (byte)0);
        short checksum = instance.computeCheckSum();
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(checksum, instance.getCheckSum());
    }

    /**
     * Test of setOptions method, of class BUTPPacket.
     */
    @Test
    public void testSetOptions() {
        System.out.println("setOptions");
        short options = 0;
        BUTPPacket instance = new BUTPPacket();
        instance.setOptions(options);
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(options, instance.getOptions());
    }

    /**
     * Test of setWindowSize method, of class BUTPPacket.
     */
    @Test
    public void testSetWindowSize() {
        System.out.println("setWindowSize");
        int window = 8192;
        BUTPPacket instance = new BUTPPacket();
        instance.setWindowSize(window);
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(window, instance.getWindowSize());
    }

    /**
     * Test of setFlags method, of class BUTPPacket.
     */
    @Test
    public void testSetFlags() {
        System.out.println("setFlags");
        byte flag = 0x01;
        BUTPPacket instance = new BUTPPacket();
        instance.setFlags(flag);
        // TODO review the generated test code and remove the default call to fail.
        assertEquals(flag, instance.getFlags());
    }

}
