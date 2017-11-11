package edu.membership.gossip;

import edu.membership.gossip.api.*;
import edu.membership.gossip.impl.*;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RemoteNode {

    private static Node<Address> run(Address self) {

        final Network<Address, Message<Address>> network = new UdpNetwork();

        final Membership<Address> membership = new GossipMembership(self, 1000, 2000);

        final Supplier<Long> clock = System::currentTimeMillis;

        final Node<Address> node = new GossipNode(self, network, membership, clock);

        final Network.Listener<Message<Address>> listener = network.listen(self);

        final Queue<Message<Address>> queue = listener.queue();

        Runnable runnable = () -> {
            node.cycle();

            while (!queue.isEmpty()) {
                Message<Address> message = queue.poll();
                node.handle(message);
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
