package org.gamboni.shopping.server;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.gamboni.shopping.server.domain.Item_;
import org.gamboni.shopping.server.domain.State;
import org.gamboni.shopping.server.domain.Store;

@ApplicationScoped
public class NightlyCleanup {
    @Inject
    Store store;
    @Inject
    ShoppingSocket socket;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void clearBoughtItems() {
        socket.broadcast(
        store.update(session -> {
            store.searchItems((criteria, root) ->
                            criteria.where(store.getEm().getCriteriaBuilder().equal(root.get(Item_.state), State.BOUGHT))).getResultList()
                    .forEach(item -> session.setItemState(item, State.UNUSED));
        })::get);
    }
}
