package edu.member.impl;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.member.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GossipNode implements MemberNode {
    private static final Logger logger = LogManager.getLogger(GossipNode.class);
    private final String TAG;

    private final Address address;
    private final Network network;
    private final Membership membership;
    private final Supplier<Long> clock;
    private final GossipSpreadStrategy gossipSpreadStrategy;

    private long heartbeat = 0L;
    private long localtime = 0L;

    private boolean isFailed = false;

    private static final int NUMBER_OF_GOSSIP_SPREAD_TARGETS = 3;

    public GossipNode(Address address,
                      Network network,
                      Membership membership,
                      Supplier<Long> clock) {
        this.address = address;
        this.network = network;
        this.membership = membership;
        this.clock = clock;
        this.localtime = clock.get();
        this.gossipSpreadStrategy = new RandomGossipSpreadStrategy(NUMBER_OF_GOSSIP_SPREAD_TARGETS);

        assureSelfMembership();

        TAG = address.host() + ":" + address.port();
    }

    @Override
    synchronized public void handle(Message message) {
        if (isFailed) {
            return;
        }

        logger.debug("[{} T={} H={}] Received {} from {}", TAG, localtime, heartbeat, message, message.sender);

        switch (message.type) {
            case JOIN:
                membership.add(message.sender, localtime);
                break;
            case MEMBER_LIST:
                membership.add(message.sender, localtime);
                for (Member m : message.members) {
                    membership.add(m, localtime);
                }
                break;
            default:
                logger.warn("Unexpected message: {}", message);
                break;
        }
    }

    private void assureSelfMembership() {
        Member self = new Member(address, localtime, heartbeat);
        membership.add(self, localtime);
    }

    private void gossip(List<Member> memberList) {
        Member self = new Member(address, localtime, heartbeat);
        Message message = new Message(Message.Type.MEMBER_LIST, self, memberList);

        membership.add(self, localtime);

        for (Address addr : gossipSpreadStrategy.targets(membership, localtime)) {
            if (! addr.equals(address)) {
                network.send(addr, Payload.of(message));
            }
        }
    }

    @Override
    synchronized public void cycle() {
        if (isFailed) {
            return;
        }
        heartbeat += 1;
        localtime = clock.get();

        List<Member> memberList = membership.list(localtime);
        logger.trace("[{} T={} H={}] Members: {}", TAG, localtime, heartbeat, memberList);

        gossip(memberList);
    }

    @Override
    public void join(Address master) {
        if (! address.equals(master)) {
            Member self = new Member(address, localtime, heartbeat);
            Message message = new Message(Message.Type.JOIN, self, Collections.emptyList());
            network.send(master, Payload.of(message));
        }
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public List<Address> peers() {
        return membership.list(localtime).stream()
                .map((m) -> m.address)
                .collect(Collectors.toList());
    }

    @Override
    public void fail() {
        logger.warn("[{} T={} H={}] Node failed", TAG, localtime, heartbeat);
        isFailed = true;
        membership.reset();
    }
}
