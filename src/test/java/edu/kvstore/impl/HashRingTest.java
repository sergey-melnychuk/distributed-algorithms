package edu.kvstore.impl;

import edu.common.api.Address;
import edu.membership.api.Member;
import edu.membership.impl.GossipMembership;
import edu.common.impl.LocalAddress;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashRingTest {

    private Member member(int port, long hb, long ts) {
        return new Member(new LocalAddress(port), hb, ts);
    }

    @Test
    void ringNodesOrderedProperly() {
        HashRing ring = new HashRing(Byte.MAX_VALUE, new GossipMembership(new LocalAddress(101), 100, 100));

        ring.add(member(101, 0, 0), 0);
        ring.add(member(102, 0, 0), 0);
        ring.add(member(103, 0, 0), 0);
        ring.add(member(104, 0, 0), 0);
        ring.add(member(105, 0, 0), 0);

        List nodes = ring.ordered(0);

        List expected = Stream.of(101, 102, 103, 104, 105)
                .map(LocalAddress::new)
                .collect(Collectors.toList());

        assertEquals(expected, nodes);
    }

    @Test
    void ringNodesAssignedProperly() {
        GossipMembership membership = new GossipMembership(new LocalAddress(110), 100, 100);
        HashRing ring = new HashRing(Byte.MAX_VALUE, membership);

        ring.add(member(100, 0, 0), 0);
        ring.add(member(105, 0, 0), 0);
        ring.add(member(110, 0, 0), 0);

        List<Address> nodes = ring.ordered(0);

        int expected = 0;
        assertEquals(nodes.get(expected).port(), ring.pick("a", 0).port()); // "a".hashCode() = 97
        assertEquals(nodes.get(expected).port(), ring.pick("b", 0).port()); // "b".hashCode() = 98
        assertEquals(nodes.get(expected).port(), ring.pick("c", 0).port()); // "c".hashCode() = 99
        assertEquals(nodes.get(expected).port(), ring.pick("d", 0).port()); // "d".hashCode() = 100

        expected = 1;
        assertEquals(nodes.get(expected).port(), ring.pick("e", 0).port()); // "e".hashCode() = 101
        assertEquals(nodes.get(expected).port(), ring.pick("f", 0).port()); // "f".hashCode() = 102
        assertEquals(nodes.get(expected).port(), ring.pick("g", 0).port()); // "g".hashCode() = 103
        assertEquals(nodes.get(expected).port(), ring.pick("h", 0).port()); // "h".hashCode() = 104
        assertEquals(nodes.get(expected).port(), ring.pick("i", 0).port()); // "i".hashCode() = 105

        expected = 2;
        assertEquals(nodes.get(expected).port(), ring.pick("j", 0).port()); // "j".hashCode() = 106
        assertEquals(nodes.get(expected).port(), ring.pick("k", 0).port()); // "k".hashCode() = 107
        assertEquals(nodes.get(expected).port(), ring.pick("l", 0).port()); // "l".hashCode() = 108
        assertEquals(nodes.get(expected).port(), ring.pick("m", 0).port()); // "m".hashCode() = 109
        assertEquals(nodes.get(expected).port(), ring.pick("n", 0).port()); // "n".hashCode() = 110

        expected = 0;
        assertEquals(nodes.get(expected).port(), ring.pick("o", 0).port()); // "o".hashCode() = 111
        assertEquals(nodes.get(expected).port(), ring.pick("p", 0).port()); // "p".hashCode() = 112
        assertEquals(nodes.get(expected).port(), ring.pick("q", 0).port()); // "q".hashCode() = 113
        assertEquals(nodes.get(expected).port(), ring.pick("r", 0).port()); // "r".hashCode() = 114
    }

    @Test
    void reset() {
        HashRing ring = new HashRing(Byte.MAX_VALUE, new GossipMembership(new LocalAddress(101), 100, 100));
        ring.add(member(101, 0, 0), 0);
        ring.add(member(102, 0, 0), 0);
        ring.add(member(103, 0, 0), 0);
        ring.add(member(104, 0, 0), 0);
        ring.add(member(105, 0, 0), 0);

        assertEquals(5, ring.list(0).size());
        ring.reset();
        assertEquals(0, ring.list(0).size());
    }

    @Test
    void self() {
        HashRing ring = new HashRing(Byte.MAX_VALUE, new GossipMembership(new LocalAddress(101), 100, 100));
        assertEquals(new LocalAddress(101), ring.self());
    }

    @Test
    void list() {
        HashRing ring = new HashRing(Byte.MAX_VALUE, new GossipMembership(new LocalAddress(101), 100, 100));
        List<Member> members = Arrays.asList(
                member(101, 0, 0),
                member(102, 0, 0),
                member(103, 0, 0),
                member(104, 0, 0),
                member(105, 0, 0)
        );
        members.forEach((m) -> ring.add(m, 0));

        Set<Member> set = new HashSet<>();
        set.addAll(ring.list(0));

        members.forEach(m ->
            assertTrue(set.contains(m), "member must be present: " + m)
        );
    }
}
