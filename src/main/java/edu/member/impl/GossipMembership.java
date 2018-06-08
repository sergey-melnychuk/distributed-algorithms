package edu.member.impl;

import edu.common.api.Address;
import edu.member.api.Member;
import edu.member.api.Membership;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class GossipMembership implements Membership {
    private static final Logger logger = LogManager.getLogger(GossipMembership.class);
    private final String TAG;

    private final Address self;
    private final long timeFailedMillis;
    private final long timeCleanupMillis;

    public GossipMembership(Address self, long timeFailedMillis, long timeCleanupMillis) {
        this.self = self;
        this.timeFailedMillis = timeFailedMillis;
        this.timeCleanupMillis = timeCleanupMillis;

        TAG = self.host() + ":" + self.port();
    }

    private final Map<String, Member> members = new HashMap<>();

    @Override
    public void add(Member member, long now) {
        if (now - member.timestamp < timeFailedMillis) {
            if (members.containsKey(member.id)) {
                Member entry = members.get(member.id);
                if (member.heartbeat > entry.heartbeat) {
                    members.put(member.id, member.updated(now, member.heartbeat));
                    logger.trace("[{}] Updated existing member {}", TAG, member);
                }
            } else {
                members.put(member.id, member.updated(now));
                logger.info("[{} T={}] Added new member {}", TAG, now, member);
            }
        }
    }

    private List<Member> cleanup(long now) {
        Set<String> failed = new HashSet<>();
        for (Map.Entry<String, Member> entry : members.entrySet()) {
            if (entry.getValue().address.equals(self)) continue;
            if (now - entry.getValue().timestamp >= timeFailedMillis) {
                failed.add(entry.getKey());
                logger.debug("[{} T={}] Member {} detected as failed", TAG, now, entry.getKey());
            }
        }
        List<Member> result = new ArrayList<>();
        for (String id : failed) {
            if (now - members.get(id).timestamp >= timeFailedMillis + timeCleanupMillis) {
                members.remove(id);
                result.add(members.get(id));
                logger.info("[{} T={}] Member {} removed from member list", TAG, now, id);
            }
        }
        return result;
    }

    @Override
    public List<Member> list(long now) {
        cleanup(now);
        return new ArrayList<>(members.values());
    }

    @Override
    public List<Member> failed(long now) {
        return cleanup(now);
    }

    @Override
    public void reset() {
        members.clear();
    }

    @Override
    public Address self() { return self; }
}
