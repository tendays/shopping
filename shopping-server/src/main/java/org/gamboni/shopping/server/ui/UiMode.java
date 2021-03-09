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
        public List<Item> load(Store.Session s) {
            return s.getAllItems();
        }
    }, SHOP {
        @Override
        public List<Item> load(Store.Session s) {
            return s.search(Item.class, (criteria, root) -> criteria.where(s.cb.notEqual(root.get(Item_.state), State.UNUSED))).getResultList();
        }
    };

    public abstract List<Item> load(Store.Session s);
}
