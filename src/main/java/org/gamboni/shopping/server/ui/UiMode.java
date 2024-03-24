package org.gamboni.shopping.server.ui;

import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Item_;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.domain.Store;

import java.util.List;

/**
 * @author tendays
 */
public enum UiMode {
    SELECT {
        @Override
        public List<Item> load(Store s) {
            return s.getAllItems();
        }

        @Override
        public boolean test(State s) {
            return true;
        }
    }, SHOP {
        @Override
        public List<Item> load(Store s) {
            var cb = s.getEm().getCriteriaBuilder();
            return s.searchItems((criteria, root) -> criteria.where(cb.notEqual(root.get(Item_.state), State.UNUSED))).getResultList();
        }

        @Override
        public boolean test(State s) {
            return s != State.UNUSED;
        }
    };

    public abstract List<Item> load(Store s);
    public abstract boolean test(State s);
}
