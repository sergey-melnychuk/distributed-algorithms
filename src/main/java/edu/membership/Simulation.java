package edu.membership;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.common.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation {
    private static final Logger logger = LogManager.getLogger(Simulation.class);

    private long currentTime = 0L;
    private final Supplier<Long> clock = () -> currentTime * 100;

    private Network.Node makeNode(Address address, Network network) {
        return new NodeImpl(address, network, clock, 500, 1500, 300, 3);
    }

    private Network makeNetwork(boolean isLocal) {
        if (isLocal) return new LocalNetwork();
        else return new UdpNetwork();
    }

    private Address makeAddress(int port, boolean isLocal) {
        if (isLocal) return new LocalAddress(port);
        else return new RemoteAddress("127.0.0.1", port);
    }

    private List<Network.Node> makeNodes(int nrOfNodes, Network network, boolean isLocal) {
        return Stream.iterate(0, (i) -> i + 1)
                .limit(nrOfNodes)
                .map((i) -> makeNode(makeAddress(10000 + i, isLocal), network))
                .collect(Collectors.toList());
    }

    private String key(Address address) {
        return address.host() + ":" + address.port();
    }

    private void run(int nrOfNodes, boolean isLocal) {
        final Network network = makeNetwork(isLocal);

        List<Network.Node> nodes = makeNodes(nrOfNodes, network, isLocal);
        Map<String, Network.Listener> listeners = new HashMap<>();

        Network.Node master = nodes.get(0);
        nodes.forEach(n -> {
            String key = key(n.address());
            Network.Listener listener = network.listen(n.address());
            listeners.put(key, listener);
        });

        nodes.forEach(n -> n.join(master.address()));

        int EPOCHS = 100;
        for (int epoch=1; epoch<=EPOCHS; epoch++) {

            currentTime += 1;

            nodes.forEach((n) -> {
                    n.cycle();

                    String key = key(n.address());
                    Network.Listener listener = listeners.get(key);

                    while (! listener.queue().isEmpty()) {
                        Payload payload = listener.queue().poll();
                        if (payload != null) {
                            n.handle(payload);
                        }
                    }
                });

            if (epoch % 10 == 0) {
                int failedNodeIndex = (epoch / 10) - 1;
                if (failedNodeIndex < nodes.size()) {
                    nodes.get(failedNodeIndex).fail();
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                /* ignore */
            }
        }

        nodes.forEach(n -> logger.info("Members of {} : {}", n.address(), n.peers()));
    }

    public static void main(String args[]) {
        int nrOfNodes = 10;
        if (args.length > 0) {
            nrOfNodes = Integer.parseInt(args[0]);
        }
        Simulation runner = new Simulation();
        boolean isLocal = false;
        runner.run(nrOfNodes, isLocal);
    }
}
