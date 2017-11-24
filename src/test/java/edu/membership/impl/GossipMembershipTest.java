package edu.membership.impl;

import edu.common.impl.LocalAddress;
import edu.common.api.Address;
import edu.membership.api.Member;
import edu.membership.api.Membership;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GossipMembershipTest {

    @Test
    void memberAddedToEmptyList() {
        Address self = new LocalAddress(0);
        Membership membership = new GossipMembership(self, 10, 20);
        assertTrue(membership.list(0).isEmpty());
        Member member = new Member(self, 0, 0);
        membership.add(member, 0);

        List<Member> list = membership.list(0);
        assertEquals(1, list.size());
        assertEquals(member, list.get(0));
    }

    @Test
    void memberRemovedAfterTimeout() {
        Address self = new LocalAddress(0);
        Membership membership = new GossipMembership(self, 10, 20);
        assertTrue(membership.list(0).isEmpty());
        Member member = new Member(self, 0, 0);
        membership.add(member, 0);
        assertEquals(Collections.singletonList(member), membership.list(0));

        Address that = new LocalAddress(1);
        Member other = new Member(that, 0, 0);
        membership.add(other, 0);
        assertEquals(Arrays.asList(member, other), membership.list(0));

        List<Member> list = membership.list(30);
        assertEquals(Collections.singletonList(member), list);
    }

    @Test
    void memberHeartbeatAndTimestampUpdated() {
        Address self = new LocalAddress(0);
        Membership membership = new GossipMembership(self, 10, 20);
        assertTrue(membership.list(0).isEmpty());
        Member member = new Member(self, 0, 0);
        membership.add(member, 0);
        assertEquals(1, membership.list(0).size());

        membership.add(member.updated(1, 1), 2);
        List<Member> list = membership.list(2);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).heartbeat);
        assertEquals(2, list.get(0).timestamp);
    }

    @Test
    void memberWithLowerHeartbeatNotApplied() {
        Address self = new LocalAddress(0);
        Membership membership = new GossipMembership(self, 10, 20);
        assertTrue(membership.list(0).isEmpty());
        Member member = new Member(self, 10, 10);
        membership.add(member, 10);
        assertEquals(1, membership.list(10).size());

        membership.add(member.updated(11, 9), 10);
        List<Member> list = membership.list(10);
        assertEquals(1, list.size());
        assertEquals(10, list.get(0).heartbeat);
        assertEquals(10, list.get(0).timestamp);
    }
}
