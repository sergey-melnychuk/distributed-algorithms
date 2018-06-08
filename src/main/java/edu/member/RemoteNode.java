package edu.member;

import edu.common.api.Address;
import edu.common.api.Network;
import edu.common.api.Payload;
import edu.common.impl.NodeImpl;
import edu.common.impl.RemoteAddress;
import edu.common.impl.UdpNetwork;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RemoteNode {

    private static Network.Node run(Address self) {
        final Network network = new UdpNetwork();
        final Supplier<Long> clock = System::currentTimeMillis;
        final Network.Node node = new NodeImpl(self, network, clock, 1000, 2000, 500, 3);
        final Network.Listener listener = network.listen(self);
        final Queue<Payload> queue = listener.queue();
        Runnable runnable = () -> {
            node.cycle();
            while (!queue.isEmpty()) {
                Payload payload = queue.poll();
                node.handle(payload);
            }
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        executor.scheduleAtFixedRate(runnable, 100, 100, TimeUnit.MILLISECONDS);
        return node;
    }

    private static void run(Address self, Address leader) {
        run(self).join(leader);
    }

    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("Arguments: <port> <leader address>, e.g. 10001 127.0.0.1:10000");
            return;
        }

        int port = Integer.parseInt(args[0]);
        final Address self = new RemoteAddress("127.0.0.1", port);

        if (args.length >= 2) {
            String addr[] = args[1].split(":");
            final Address leader = new RemoteAddress(addr[0], Integer.parseInt(addr[1]));
            run(self, leader);
        } else {
            run(self);
        }
    }

}
