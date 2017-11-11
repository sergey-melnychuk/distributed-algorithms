package edu.membership.gossip.api;

import java.util.List;

public interface Message<ADDR> {

    enum Type {
        JOIN,
        MEMBER_LIST
    }

    Type type();

    Membership.Member<ADDR> sender();

    List<Membership.Member<ADDR>> members();

}
