package org.gamboni.shopping.server.domain;

import java.util.List;

/**
 * @author tendays
 */
public class WatchResult extends Dto {
    public final List<ItemState> batch;
    public final String continuation;

    public WatchResult(List<ItemState> batch, String continuation) {
        this.batch = batch;
        this.continuation = continuation;
    }
}
