package edu.membership.gossip.impl;

import edu.membership.gossip.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GossipNode implements Node<Address> {
    private static final Logger logger = LogManager.getLogger(GossipNode.class);
    private final String TAG;

    private final Address address;
    private final Network<Address, Message<Address>> network;
    private final Membership<Address> membership;
    private final Supplier<Long> clock;
    private final GossipSpreadStrategy<Address> gossipSpreadStrategy;

    private long heartbeat = 0L;
    private long localtime = 0L;

    private boolean isFailed = false;

    private static final int NUMBER_OF_GOSSIP_SPREAD_TARGETS = 3;

    public GossipNode(Address address,
                      Network<Address, Message<Address>> network,
                      Membership<Address> membership,
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
    synchronized public void handle(Message<Address> message) {
        if (isFailed) {
            return;
        }

        logger.debug("[{} T={} H={}] Received {} from {}", TAG, localtime, heartbeat, message, message.sender());

        switch (message.type()) {
            case JOIN:
                membership.add(message.sender(), localtime);
                break;
            case MEMBER_LIST:
                membership.add(message.sender(), localtime);
                for (Membership.Member<Address> m : message.members()) {
                    membership.add(m, localtime);
                }
                break;
            default:
                logger.warn("Unexpected message: {}", message);
                break;
        }
    }

    private void assureSelfMembership() {
        Membership.Member<Address> self = new GossipMembership.MemberNode(address, heartbeat, localtime);
        membership.add(self, localtime);
    }

    private void gossip(List<Membership.Member<Address>> memberList) {
        Membership.Member<Address> self = new GossipMembership.MemberNode(address, heartbeat, localtime);
        Message<Address> message = new GossipMessage(Message.Type.MEMBER_LIST, self, memberList);

        membership.add(self, localtime);

        for (Address addr : gossipSpreadStrategy.targets(membership, localtime)) {
            if (! addr.equals(address)) {
                network.send(addr, message);
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

        List<Membership.Member<Address>> memberList = membership.list(localtime);
        logger.debug("[{} T={} H={}] Members: {}", TAG, localtime, heartbeat, memberList);

        gossip(memberList);
    }

    @Override
    public void join(Address master) {
        if (! address.equals(master)) {
            Membership.Member<Address> self = new GossipMembership.MemberNode(address, heartbeat, localtime);
            Message<Address> message = new GossipMessage(Message.Type.JOIN, self);
            network.send(master, message);
        }
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public List<Address> peers() {
        return membership.list(localtime).stream()
                .map(Membership.Member::address)
                .collect(Collectors.toList());
    }

    @Override
    public void fail() {
        logger.warn("[{} T={} H={}] Node failed", TAG, localtime, heartbeat);
        isFailed = true;
        membership.reset();
    }
}
