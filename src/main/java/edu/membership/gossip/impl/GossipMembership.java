package edu.membership.gossip.impl;

import edu.membership.gossip.api.Address;
import edu.membership.gossip.api.Membership;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class GossipMembership implements Membership<Address> {
    private static final Logger logger = LogManager.getLogger(GossipMembership.class);
    private final String TAG;

    public static class MemberNode implements GossipMembership.Member<Address> {
        private final String id;
        private final Address address;
        private final long timestamp;
        private final long heartbeat;

        MemberNode(Address address, long heartbeat, long timestamp) {
            this.id = address.host() + ":" + address.port();
            this.address = address;
            this.timestamp = timestamp;
            this.heartbeat = heartbeat;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Address address() {
            return address;
        }

        @Override
        public long timestamp() {
            return timestamp;
        }

        @Override
        public long heartbeat() {
            return heartbeat;
        }

        @Override
        public Member<Address> updated(long timestamp) {
            return new MemberNode(this.address, this.heartbeat, timestamp);
        }

        @Override
        public Member<Address> updated(long heartbeat, long timestamp) {
            return new MemberNode(this.address, heartbeat, timestamp);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MemberNode that = (MemberNode) o;

            return (timestamp == that.timestamp)
                    && (heartbeat == that.heartbeat)
                    && (id.equals(that.id))
                    && address.equals(that.address);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + address.hashCode();
            result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + (int) (heartbeat ^ (heartbeat >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "MemberNode{" +
                    "address=" + address +
                    ", timestamp=" + timestamp +
                    ", heartbeat=" + heartbeat +
                    '}';
        }
    }

    private final Address self;
    private final long timeFailedMillis;
    private final long timeCleanupMillis;

    public GossipMembership(Address self, long timeFailedMillis, long timeCleanupMillis) {
        this.self = self;
        this.timeFailedMillis = timeFailedMillis;
        this.timeCleanupMillis = timeCleanupMillis;

        TAG = self.host() + ":" + self.port();
    }

    private final Map<String, Member<Address>> members = new HashMap<>();

    @Override
    public void add(Member<Address> node, long now) {
        if (now - node.timestamp() < timeFailedMillis) {
            if (members.containsKey(node.id())) {
                Member<Address> entry = members.get(node.id());
                if (node.heartbeat() > entry.heartbeat()) {
                    members.put(node.id(), node.updated(node.heartbeat(), now));
                    logger.debug("[{}] Updated existing member {}", TAG, node);
                }
            } else {
                members.put(node.id(), node.updated(now));
                logger.info("[{}] Added new member {}", TAG, node);
            }
        }
    }

    private void cleanup(long now) {
        Set<String> failed = new HashSet<>();
        for (Map.Entry<String, Member<Address>> entry : members.entrySet()) {
            if (entry.getValue().address().equals(self)) continue;
            if (now - entry.getValue().timestamp() >= timeFailedMillis) {
                failed.add(entry.getKey());
                logger.debug("[{}] Member {} detected as failed", TAG, entry.getKey());
            }
        }
        for (String id : failed) {
            if (now - members.get(id).timestamp() >= timeFailedMillis + timeCleanupMillis) {
                members.remove(id);
                logger.info("[{}] Member {} removed from member list", TAG, id);
            }
        }
    }

    @Override
    public List<Member<Address>> list(long now) {
        cleanup(now);
        return new ArrayList<>(members.values());
    }

    @Override
    public void reset() {
        members.clear();
    }

    @Override
    public Address self() { return self; }
}
