package edu.kvstore.impl;

import edu.kvstore.api.Ring;
import edu.common.api.Address;
import edu.membership.api.Member;
import edu.membership.impl.GossipMembership;
import edu.common.impl.LocalAddress;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingReplicationTest {

    private Member member(int port, long hb, long ts) {
        return new Member(new LocalAddress(port), hb, ts);
    }

    private Ring<String> ring(int n) {
        GossipMembership membership = new GossipMembership(new LocalAddress(110), 100, 100);
        HashRing ring = new HashRing(Byte.MAX_VALUE, membership);
        for (int i=0; i<n; i++) {
            int port = 100 + i * 5;
            ring.add(member(port, 0, 0), 0);
        }
        return ring;
    }

    @Test
    void returnReplicasOrderCorrectly() {
        Ring<String> ring = ring(3);
        RingReplication replication = new RingReplication(3, ring);

        List<Address> nodes = ring.ordered(0);
        System.out.println(nodes);

        Address N1 = nodes.get(0);
        Address N2 = nodes.get(1);
        Address N3 = nodes.get(2);

        Map<String, List> expected = new LinkedHashMap<>();
        expected.put("a", Arrays.asList(N1, N2, N3));
        expected.put("b", Arrays.asList(N1, N2, N3));
        expected.put("c", Arrays.asList(N1, N2, N3));
        expected.put("d", Arrays.asList(N1, N2, N3));

        expected.put("e", Arrays.asList(N2, N3, N1));
        expected.put("f", Arrays.asList(N2, N3, N1));
        expected.put("g", Arrays.asList(N2, N3, N1));
        expected.put("h", Arrays.asList(N2, N3, N1));
        expected.put("i", Arrays.asList(N2, N3, N1));

        expected.put("j", Arrays.asList(N3, N1, N2));
        expected.put("k", Arrays.asList(N3, N1, N2));
        expected.put("l", Arrays.asList(N3, N1, N2));
        expected.put("m", Arrays.asList(N3, N1, N2));
        expected.put("n", Arrays.asList(N3, N1, N2));

        expected.put("o", Arrays.asList(N1, N2, N3));
        expected.put("p", Arrays.asList(N1, N2, N3));
        expected.put("q", Arrays.asList(N1, N2, N3));
        expected.put("r", Arrays.asList(N1, N2, N3));
        expected.put("s", Arrays.asList(N1, N2, N3));

        expected.forEach((key, exp) ->
            assertEquals(exp, replication.pick(key, 0), "key=" + key)
        );
    }

    @Test
    void replicasAssignedProperly() {
        Ring<String> ring = ring(4);
        RingReplication replication = new RingReplication(3, ring);

        List<Address> nodes = ring.ordered(0);
        System.out.println(nodes);

        Address N1 = nodes.get(0);
        Address N2 = nodes.get(1);
        Address N3 = nodes.get(2);
        Address N4 = nodes.get(3);

        Map<String, List> expected = new LinkedHashMap<>();
        expected.put("a", Arrays.asList(N1, N2, N3));
        expected.put("b", Arrays.asList(N1, N2, N3));
        expected.put("c", Arrays.asList(N1, N2, N3));
        expected.put("d", Arrays.asList(N1, N2, N3));

        expected.put("e", Arrays.asList(N2, N3, N4));
        expected.put("f", Arrays.asList(N2, N3, N4));
        expected.put("g", Arrays.asList(N2, N3, N4));
        expected.put("h", Arrays.asList(N2, N3, N4));
        expected.put("i", Arrays.asList(N2, N3, N4));

        expected.put("j", Arrays.asList(N3, N4, N1));
        expected.put("k", Arrays.asList(N3, N4, N1));
        expected.put("l", Arrays.asList(N3, N4, N1));
        expected.put("m", Arrays.asList(N3, N4, N1));
        expected.put("n", Arrays.asList(N3, N4, N1));

        expected.put("o", Arrays.asList(N4, N1, N2));
        expected.put("p", Arrays.asList(N4, N1, N2));
        expected.put("q", Arrays.asList(N4, N1, N2));
        expected.put("r", Arrays.asList(N4, N1, N2));
        expected.put("s", Arrays.asList(N4, N1, N2));

        expected.put("t", Arrays.asList(N1, N2, N3));
        expected.put("u", Arrays.asList(N1, N2, N3));
        expected.put("v", Arrays.asList(N1, N2, N3));
        expected.put("x", Arrays.asList(N1, N2, N3));

        expected.forEach((key, exp) ->
            assertEquals(exp, replication.pick(key, 0), "key=" + key)
        );
    }

    @Test
    void returnEmptyListWhenNotEnoughNodes() {
        Ring<String> ring = ring(3);
        RingReplication replication = new RingReplication(4, ring);

        assertTrue(replication.pick("a", 0).isEmpty(), "list of replicas is empty");
    }
}
