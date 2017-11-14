package edu.common.api;

import java.util.List;
import java.util.Queue;

public interface Network {

    interface Node {

        /**
         * Handle the payload received from the network
         * @param payload payload received from the network
         */
        void handle(Payload payload);

        Address address();

        void join(Address leader);

        List<Address> peers();

        void fail();

        void cycle();
    }

    interface Listener extends AutoCloseable {

        /**
         * Get queue of messages received by the listener.
         * @return queue of messages
         */
        Queue<Payload> queue();

    }

    /**
     * Send message `message` to `target` address over network.
     * @param target address of the receiver of the message
     * @param payload the payload to be send
     * @return `true` if message was sent successfully, `false` otherwise
     */
    boolean send(Address target, Payload payload);

    /**
     * Start listening to `address`.
     * @param address address of the receiver
     * @return Listener for the address.
     */
    Listener listen(Address address);

}
