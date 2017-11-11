package edu.membership.gossip.api;

import java.util.List;
import java.util.Queue;

public interface Network<ADDRESS, MESSAGE> {

    interface Listener<MESSAGE> {

        /**
         * Get queue of messages received by the listener.
         * @return queue of messages
         */
        Queue<MESSAGE> queue();

        /**
         * Stop listening the target address
         */
        void stop();

    }

    /**
     * Send message `message` to `target` address over network.
     * @param target address of the receiver of the message
     * @param message the message to be send
     * @return `true` if message was sent successfully, `false` otherwise
     */
    boolean send(ADDRESS target, MESSAGE message);

    /**
     * Start listening to `address`.
     * @param address address of the receiver
     * @return Listener for the address.
     */
    Listener<MESSAGE> listen(ADDRESS address);

}
