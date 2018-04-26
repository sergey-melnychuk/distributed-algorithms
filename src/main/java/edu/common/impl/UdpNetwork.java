package edu.common.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.membership.api.Member;
import edu.membership.api.Message;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpNetwork implements Network {
    private static final byte HI = 1;
    private static final byte LO = 0;

    private static final byte MEMBER = 1;
    private static final byte KEYVAL = 2;

    private static final int SO_TIMEOUT = 1000; // 1 second receive timeout from socket
    private static final int RECEIVE_BUFFER_SIZE = 1024;

    private static final int MAGIC = 0x0A0A0A0A;

    private Address readAddress(ByteBuffer buffer) {
        byte[] addr = new byte[4];
        buffer.get(addr, 0, 4);
        int port = buffer.getInt();
        return new RemoteAddress(addr, port);
    }

    private void writeAddress(ByteBuffer buffer, Address address) {
        byte [] addr = new byte[4];
        String bytes[] = address.host().split("\\.");
        for (int i=0; i<4; i++) {
            addr[i] = Byte.valueOf(bytes[i]);
        }
        buffer.put(addr,0, 4);
        buffer.putInt(address.port());
    }

    private Payload readKeyval(ByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IllegalStateException("Invalid magic int");
        }

        int typeIdx = buffer.getInt();
        if (typeIdx < 0 || typeIdx >= edu.kvstore.api.Message.Type.values().length) {
            throw new IllegalStateException("Invalid message type");
        }

        edu.kvstore.api.Message.Type type = edu.kvstore.api.Message.Type.values()[typeIdx];

        long seq = buffer.getLong();

        Address sender = readAddress(buffer);

        int keyLen = buffer.getInt();
        int valLen = buffer.getInt();

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<keyLen; i++) {
            sb.append(buffer.getChar());
        }
        String key = sb.toString();

        String value = null;
        if (valLen > 0) {
            sb = new StringBuilder();
            for (int i=0; i<valLen; i++) {
                sb.append(buffer.getChar());
            }
            value = sb.toString();
        }

        boolean replica = buffer.get() > 0;

        return Payload.of(new edu.kvstore.api.Message(type, seq, sender, key, value, replica));
    }

    private Payload readMember(ByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IllegalStateException("Invalid magic int");
        }

        int typeIdx = buffer.getInt();
        if (typeIdx < 0 || typeIdx >= Message.Type.values().length) {
            throw new IllegalStateException("Invalid message type");
        }
        Message.Type type = Message.Type.values()[typeIdx];

        Address address = readAddress(buffer);

        long ts = buffer.getLong();
        long hb = buffer.getLong();
        Member member = new Member(address, ts, hb);

        if (type == Message.Type.MEMBER_LIST) {
            List<Member> memberList = new ArrayList<>();
            int n = buffer.getInt();
            for (int i=0; i<n; i++) {
                Address a = readAddress(buffer);
                long t = buffer.getLong();
                long h = buffer.getLong();
                Member m = new Member(a, t, h);
                memberList.add(m);
            }
            return Payload.of(new Message(type, member, memberList));
        } else {
            return Payload.of(new Message(type, member, Collections.emptyList()));
        }
    }

    private Payload read(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == MEMBER) return readMember(buffer);
        else return readKeyval(buffer);
    }

    private ByteBuffer write(edu.kvstore.api.Message message) {
        ByteBuffer temp = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
        temp.put(KEYVAL);
        temp.putInt(MAGIC);
        temp.putInt(message.type.ordinal());
        temp.putLong(message.seqNr);
        writeAddress(temp, message.sender);
        temp.putInt(message.key.length());
        if (message.value != null) {
            temp.putInt(message.value.length());
        } else {
            temp.putInt(0);
        }
        char k[] = message.key.toCharArray();
        for (char c : k) {
            temp.putChar(c);
        }
        if (message.value != null) {
            char v[] = message.value.toCharArray();
            for (char c : v) {
                temp.putChar(c);
            }
        }
        temp.put(message.replica ? HI : LO);
        int size = temp.position();
        temp.flip();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(temp);
        return buffer;
    }

    private ByteBuffer write(edu.membership.api.Message message) {
        ByteBuffer temp = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
        temp.put(MEMBER);
        temp.putInt(MAGIC);
        temp.putInt(message.type.ordinal());
        writeAddress(temp, message.sender.address);
        temp.putLong(message.sender.timestamp);
        temp.putLong(message.sender.heartbeat);
        if (message.type == Message.Type.MEMBER_LIST) {
            int n = message.members.size();
            temp.putInt(n);
            for (int i = 0; i < n; i++) {
                writeAddress(temp, message.members.get(i).address);
                temp.putLong(message.members.get(i).timestamp);
                temp.putLong(message.members.get(i).heartbeat);
            }
        }
        int size = temp.position();
        temp.flip();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(temp);
        return buffer;
    }

    private ByteBuffer write(Payload payload) {
        if (payload.keyval != null) return write(payload.keyval);
        else return write(payload.member);
    }

    @Override
    public boolean send(Address target, Payload payload) {
        try(DatagramSocket socket = new DatagramSocket(0)) {
            socket.setSoTimeout(SO_TIMEOUT);
            ByteBuffer buffer = write(payload);
            byte data[] = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(target.host()), target.port());
            socket.send(packet);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Listener listen(final Address address) {
        final ConcurrentLinkedQueue<Payload> queue = new ConcurrentLinkedQueue<>();
        final AtomicBoolean isRunning = new AtomicBoolean(true);

        Thread thread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(address.port())) {
                socket.setSoTimeout(SO_TIMEOUT);
                while (isRunning.get()) {

                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[RECEIVE_BUFFER_SIZE], RECEIVE_BUFFER_SIZE);
                        socket.receive(packet);
                        if (packet.getData().length > 0) {
                            ByteBuffer buffer = ByteBuffer.allocate(packet.getLength());
                            buffer.put(packet.getData(), 0, packet.getLength());
                            buffer.flip();
                            final Payload payload = read(buffer);
                            queue.offer(payload);
                        }
                    } catch (IOException e) {
                        /* ignore */
                    }

                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();

        return new Listener() {
            @Override
            public Queue<Payload> queue() {
                return queue;
            }

            @Override
            public void close() {
                isRunning.set(false);
            }
        };
    }
}
