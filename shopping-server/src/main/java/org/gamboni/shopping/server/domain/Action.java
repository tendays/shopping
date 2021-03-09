package org.gamboni.shopping.server.domain;

import com.google.common.collect.ImmutableSet;

/**
 * @author tendays
 */
public enum Action {
    REMOVE_FROM_LIST(State.UNUSED, State.TO_BUY),
    ADD_TO_LIST(State.TO_BUY, State.UNUSED),
    MARK_AS_BOUGHT(State.BOUGHT, State.TO_BUY, State.UNUSED),
    MARK_AS_NOT_BOUGHT(State.TO_BUY, State.BOUGHT);

    public final ImmutableSet<State> from;
    public final State to;
    private Action(State to, State... from) {
        this.from = ImmutableSet.copyOf(from);
        this.to = to;
    }
}
