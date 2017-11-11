package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Membership;
import edu.membership.gossip.api.Message;
import edu.membership.gossip.api.Network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpNetwork implements Network<Address, Message<Address>> {
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

    private Message<Address> read(ByteBuffer buffer) {
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

        long hb = buffer.getLong();
        long ts = buffer.getLong();
        GossipMembership.Member<Address> member = new GossipMembership.MemberNode(address, hb, ts);

        if (type == Message.Type.MEMBER_LIST) {
            List<Membership.Member<Address>> memberList = new ArrayList<>();
            int n = buffer.getInt();
            for (int i=0; i<n; i++) {
                Address a = readAddress(buffer);
                long h = buffer.getLong();
                long t = buffer.getLong();
                GossipMembership.Member<Address> m = new GossipMembership.MemberNode(a, h, t);
                memberList.add(m);
            }
            return new GossipMessage(type, member, memberList);
        } else {
            return new GossipMessage(type, member);
        }
    }

    private ByteBuffer write(Message<Address> message) {
        ByteBuffer temp = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
        temp.putInt(MAGIC);
        temp.putInt(message.type().ordinal());
        writeAddress(temp, message.sender().address());
        temp.putLong(message.sender().heartbeat());
        temp.putLong(message.sender().timestamp());
        if (message.type() == Message.Type.MEMBER_LIST) {
            int n = message.members().size();
            temp.putInt(n);
            for (int i = 0; i < n; i++) {
                writeAddress(temp, message.members().get(i).address());
                temp.putLong(message.members().get(i).heartbeat());
                temp.putLong(message.members().get(i).timestamp());
            }
        }
        int size = temp.position();
        temp.flip();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(temp);
        return buffer;
    }

    @Override
    public boolean send(Address target, Message<Address> message) {
        try(DatagramSocket socket = new DatagramSocket(0)) {
            socket.setSoTimeout(SO_TIMEOUT);
            ByteBuffer buffer = write(message);
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
    public Listener<Message<Address>> listen(final Address address) {
        final ConcurrentLinkedQueue<Message<Address>> queue = new ConcurrentLinkedQueue<>();
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
                            final Message<Address> message = read(buffer);
                            queue.offer(message);
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

        return new Listener<Message<Address>>() {
            @Override
            public Queue<Message<Address>> queue() {
                return queue;
            }

            @Override
            public void stop() {
                isRunning.set(false);
            }
        };
    }
}
