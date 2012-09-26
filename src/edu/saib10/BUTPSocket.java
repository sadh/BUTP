/**
 * This class is the implementation of BUTP socket.
 * 
 */
package edu.saib10;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BUTP socket class
 * @author Sadh Ibna Rahmat
 * @version 1.0
 * @since 2012-03-05
 */
public class BUTPSocket {

    private DatagramSocket socket;
    private DatagramPacket send_packet, recv_packet;
    private byte[] recv, send, BUTPSegment;
    private int cwnd = 0, rwnd = 0, sequence, acknowledge;
    private short checksum;
    private BUTPPacket send_pkt, recv_pkt;
    private short src_port, dst_port;
    private InetAddress dst_addr;
    private double RTTm, RTTs, RTTd;
    private long RTO = 3000;
    private static final long RTO_TIMEOUT = 60000, CONNECTIONTIMEOUT = 180000;
    private static final int MAXWINDOWSIZE = 11360, INITIALWINDOW = 2840;
    private byte flags, retransmission = 0;
    private long send_time, recv_time;
    private static final byte SYN_PSH = 0x00;
    private static final byte SYN = 0x01;
    private static final byte ACK = 0x02;
    private static final byte SYN_ACK = 0x03;
    private static final byte RST = 0x04;
    private static final byte PSH = 0x08;
    private static final byte FIN = 0x10;
    private static final byte PSH_FIN = 0x18;
    private static final int SEGMENT_SIZE = 1420;
    private static boolean slowstart, congestion;
    private static final Logger logger = Logger.getLogger(BUTPSocket.class.getName());
    private static ExecutorService service;

    /**
     * Initiate client socket
     * @param address Internet address of the receiver
     * @param port Listening port address of the receiver
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IOException 
     */
    public BUTPSocket(String address, short port) throws SocketException, UnknownHostException, IOException {
        dst_port = port;
        Random rnd = new Random();
        src_port = (short) (rnd.nextInt(9999) + 1);
        dst_addr = InetAddress.getByName(address);
        socket = new DatagramSocket(src_port);
        service = Executors.newSingleThreadExecutor();
        sequence = 0;
        acknowledge = 0;
        flags = SYN;
        logger.log(Level.INFO,
                "Initiating BUTP client socket-- Src:{0}:Dst:{1}:{2}",
                new Object[]{src_port, dst_addr, dst_port});

    }

    /**
     * Initiate server socket
     * @param port Listening port of the receiver
     * @throws SocketException
     * @throws IOException 
     */
    public BUTPSocket(short port) throws SocketException {
        src_port = port;
        socket = new DatagramSocket(src_port);
        service = Executors.newSingleThreadExecutor();
        sequence = 0;
        acknowledge = 0;
        logger.log(Level.INFO, "Initiating server socket:Src:{0}", new Object[]{src_port});

    }

    /**
     * Connect BUTP socket
     */
    public void connect() throws IOException {
        send(flags);
        logger.log(Level.INFO, "Sending connection request");
        Future future = service.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    receive(false);
                } catch (IOException ex) {
                    Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Socket Error.", ex);
                }
            }
        });
        try {
            future.get(CONNECTIONTIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Interrupted.", ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Execution failed.", ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Connection timeout.");
            future.cancel(true);
            close();
            System.exit(1);
        }
        flags = recv_pkt.getFlags();
        if (flags == SYN_ACK) {
            cwnd = recv_pkt.getWindowSize();
            flags = SYN_PSH;
            logger.log(Level.INFO,
                    "connection established with: {0}:{1}, window size: {2}",
                    new Object[]{dst_addr, dst_port, MAXWINDOWSIZE});
            send(flags);
        }
    }

    /**
     * Accept incoming BUTP connection
     */
    public void accept() throws IOException {
        logger.info("waiting for connection.");
        receive(false);
        flags = recv_pkt.getFlags();
        logger.log(Level.INFO, "Connection request recieved : flags:{0}", flags);
        if (flags == SYN) {
            rwnd = recv_pkt.getWindowSize();
            cwnd = Math.min(cwnd, rwnd);
            dst_addr = recv_packet.getAddress();
            dst_port = (short) recv_packet.getPort();
            logger.log(Level.INFO, "Request recieved from : {0}:{1}", new Object[]{dst_addr, dst_port});
            flags = SYN_ACK;
            send(flags);
            Future future = service.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        receive(false);
                    } catch (IOException ex) {
                        Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Receive failed.", ex);
                    }
                }
            });
            try {
                future.get(RTO, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Interrupted", ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Execution failed.", ex);
            } catch (TimeoutException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Connection timeout");
                future.cancel(true);
                close();
                System.exit(1);
            }

            RTTm = (recv_time - send_time) * 2;
            RTTs = RTTm;
            RTTd = RTTm / 2;
            flags = recv_pkt.getFlags();
            if (flags == SYN_PSH) {
                logger.log(Level.INFO, "Connection ready for transmition , RTT: {0}ms", RTTs);
            }

        }
    }

    /**
     * Send flag packet SYN or FIN etc.
     * @param flags for sending flag bytes
     * @throws IOException
     */
    private void send(byte flags) throws IOException {
        this.flags = flags;
        send_pkt = new BUTPPacket(cwnd, flags);
        send = send_pkt.getBUTPByteArray();
        send_packet = new DatagramPacket(send, send.length, dst_addr, dst_port);
        send_time = System.currentTimeMillis();
        logger.log(Level.INFO, "Sending flags at: {0}, flags: {1}", new Object[]{new Date(send_time), flags});
        socket.send(send_packet);
    }

    /**
     * Send acknowledgment packet
     * @param acknowledge requesting next segment
     * @param flags acknowledgment flag byte
     * @throws IOException
     */
    private void send(int acknowledge, byte flags) throws IOException {

        this.acknowledge = acknowledge;
        send_pkt = new BUTPPacket(acknowledge, cwnd, flags);
        send = send_pkt.getBUTPByteArray();
        send_packet = new DatagramPacket(send, send.length, dst_addr, dst_port);
        send_time = System.currentTimeMillis();
        logger.log(Level.INFO, "Sending acknowledge at {0}", new Date(send_time));
        socket.send(send_packet);
    }

    /**
     * Send BUTP segments.
     * @param BUTP_segment BUTP segment.
     * @param flag flag byte.
     */
    private void send(byte[] BUTP_segment, byte flag) throws IOException {
        send_pkt = new BUTPPacket(BUTP_segment, sequence, cwnd, flag);
        send = send_pkt.getBUTPByteArray();
        send_packet = new DatagramPacket(send, send.length, dst_addr, dst_port);
        logger.log(Level.INFO, "Sending packet sequence: {0}", sequence);
        socket.send(send_packet);
    }

    /**
     * Send users data
     * @param send_data Senders data byte array
     * @throws IOException 
     */
    public final void send(byte[] send_data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(send_data);
        sequence = 0;
        acknowledge = 0;
        flags = PSH;
        cwnd = MAXWINDOWSIZE;
        slowstart = true;
        congestion = false;
        byte[] BUTP_segment;
        int sent = 0;
        while (buffer.hasRemaining()) {
            if (SEGMENT_SIZE > buffer.remaining()) {
                BUTP_segment = new byte[buffer.remaining()];
                sent = sent + buffer.remaining();
                flags = PSH_FIN;
            } else {
                BUTP_segment = new byte[SEGMENT_SIZE];
                sent = sent + SEGMENT_SIZE;
            }
            logger.log(Level.INFO, "Sent bytes: {0} bytes, window size: {1}", new Object[]{sent, cwnd});
            sequence = buffer.position();
            buffer.get(BUTP_segment);
            send(BUTP_segment, flags);
            if (sent == cwnd) {
                logger.log(Level.INFO, "Waiting for acknowledgement.");

                Future future = service.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            receive(false);
                            calculate_RTO();
                            acknowledge = recv_pkt.getAcknowledgeNumber();
                            logger.log(Level.INFO, "Acknowledgement received: {0}", new Object[]{acknowledge});

                        } catch (IOException ex) {
                            Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Socket error", ex);
                        }
                    }
                });
                try {
                    future.get(RTO, TimeUnit.MILLISECONDS);
                    /*if (retransmission == 0 && cwnd < MAXWINDOWSIZE && slowstart) {
                        cwnd *= 2;

                    } else if (retransmission == 0 && cwnd < MAXWINDOWSIZE && congestion) {
                        cwnd += (SEGMENT_SIZE * 10);
                    } else if (retransmission == 3) {
                        slowstart = true;
                        congestion = false;
                        cwnd = INITIALWINDOW;
                    }
                    retransmission = 0;*/
                } catch (InterruptedException ex) {
                    Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Interrupted", ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Execution failed.", ex);
                } catch (TimeoutException ex) {
                    Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Connection timeout");
                    future.cancel(true);
                    RTO = RTO * 2;
                    if (retransmission < 3) {
                        slowstart = false;
                        congestion = true;
                        retransmission++;
                    } else {
                        future.cancel(true);
                        close();
                        System.exit(1);
                    }
                    int current = buffer.position();
                    buffer.position(current - sent);
                    sent = 0;

                }

            }

        }
    }

    /**
     * Receiving single packet
     * @param rto RTO enabled
     * @throws IOException 
     */
    private void receive(boolean payload) throws IOException {
        if (payload) {
            recv = new byte[BUTPPacket.PAYLOAD + BUTPPacket.BUTP_HEADER];
        } else {
            recv = new byte[BUTPPacket.BUTP_HEADER];
        }
        recv_packet = new DatagramPacket(recv, recv.length);
        socket.receive(recv_packet);
        recv_time = System.currentTimeMillis();
        recv = recv_packet.getData();
        recv_pkt = new BUTPPacket();
        recv_pkt.setBUTPByteArray(recv);

    }

    /**
     * Receive users data
     * @param receive_data the data byte array
     * @throws IOException 
     */
    public final void receive(byte[] receive_data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(receive_data);
        buffer.clear();
        sequence = 0;
        acknowledge = 0;
        logger.log(Level.INFO, "Start receiving data..");
        int received = 0;
        while ((flags == PSH) || (flags != PSH_FIN)) {
            Future future = service.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        receive(true);
                    } catch (IOException ex) {
                        Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Socket Error.", ex);
                    }
                }
            });
            try {
                future.get(CONNECTIONTIMEOUT, TimeUnit.MILLISECONDS);
                checksum = recv_pkt.computeCheckSum();
                logger.log(Level.INFO, "Computed checksum:{0} : Header checksum: {1} ", new Object[]{checksum, recv_pkt.getCheckSum()});
                if (checksum == recv_pkt.getCheckSum()) {
                    sequence = recv_pkt.getSequenceNumber();
                    flags = recv_pkt.getFlags();
                    rwnd = recv_pkt.getWindowSize();
                    logger.log(Level.INFO,
                            "Receiving seqence: {0}, Acknowledged:{1}, Flags: {2}",
                            new Object[]{sequence, acknowledge, flags});
                    if ((flags == PSH || flags == PSH_FIN) && (sequence == acknowledge)) {
                        BUTPSegment = recv_pkt.getBUTPPayload();
                        received = received + BUTPSegment.length;
                        buffer.position(sequence);
                        int length = BUTPSegment.length;
                        if (buffer.remaining() < BUTPSegment.length) {
                            length = buffer.remaining();
                        }
                        buffer.put(BUTPSegment, 0, length);
                        acknowledge = buffer.position();
                        logger.log(Level.INFO, "Received bytes: {0} bytes, window size:{1}", new Object[]{received, rwnd});
                        if (received == rwnd) {
                            send(acknowledge, ACK);
                            logger.log(Level.INFO, "Sending acknowledge:- {0}", acknowledge);
                            received = 0;
                        }
                    } else {
                        logger.log(Level.INFO,
                            "Dicarded : received: {0}, Acknowledged:{1}, Flags: {2}",
                            new Object[]{sequence, acknowledge, flags});
                        acknowledge = acknowledge - received;
                        received = 0;
                    }
                } else {
                    logger.log(Level.INFO, "Packet corrupted.");
                    acknowledge = acknowledge - received;
                    received = 0;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Interrupted.", ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "receive failed.", ex);
            } catch (TimeoutException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "Connection timeout.");
                future.cancel(true);
                close();
                System.exit(1);
            }


        }
    }

    /**
     * Implementation of Karn's Algorithm for RTO calculation
     */
    private void calculate_RTO() {

        RTTm = (recv_time - send_time) * 2;
        RTTs = (0.875 * RTTs) + (0.125 * RTTm);
        RTTd = ((0.75 * RTTd) + (0.25 * (double) Math.abs(RTTs - RTTm)));
        RTO = (long) (RTTs + 4 * RTTd);
        if (RTO < 1000) {
            RTO = 1000;
        }if(RTO > RTO_TIMEOUT){
            try {
                close();
                return;
            } catch (IOException ex) {
                Logger.getLogger(BUTPSocket.class.getName()).log(Level.SEVERE, "RTO too long.", ex);
            }
        }
        logger.log(Level.INFO, "Current RTT: {0}ms, Retransmition timeout: {1}ms, RTT variance: {2}ms", new Object[]{RTTs, RTO, RTTd});
    }

    /**
     * Close the BUTP connection
     * @throws IOException 
     */
    public void close() throws IOException {
        logger.log(Level.INFO, "Closing connection.");
        service.shutdownNow();
        socket.close();
    }
}
