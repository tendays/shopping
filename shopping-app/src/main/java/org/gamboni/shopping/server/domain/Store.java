package org.gamboni.shopping.server.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.ui.UiMode;
import org.gamboni.tech.history.StampedEventList;
import org.gamboni.tech.persistence.PersistedHistoryStore;
import org.gamboni.tech.web.ws.BroadcastTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * @author tendays
 */
@ApplicationScoped
@Slf4j
public class Store extends PersistedHistoryStore<
        UiMode, // Query type
        StampedEventList, // Snapshot type
        Store.UpdateSession // Session type
        > {

    private final Map<BroadcastTarget, UiMode> sessions = new HashMap<>();

    @Override
    protected long getStamp() {
        // It seems we can unfortunately not query the sequence directly. Maybe we should just use a single-celled table instead
        final CriteriaQuery<Long> query = em.getCriteriaBuilder().createQuery(Long.class);
        final Root<Item> root = query.from(Item.class);
        query.select(em.getCriteriaBuilder()
                .max(root.get(Item_.sequence)));
        return em.createQuery(query).getSingleResult();
    }

    public List<Item> getAllItems() {
        return searchItems((q, r) ->
            q.orderBy(
                    em.getCriteriaBuilder()
                            .asc(r.get(Item_.text))
            )).getResultList();
    }

    public synchronized Item getItemByName(String name) {
        return find(Item.class, name).orElseGet(() -> {
            final Item newItem = new Item(name);
            getProductPicture(name).ifPresent(newItem::setImage);
            em.persist(newItem);
            return newItem;
        });
    }

    public TypedQuery<Item> searchItems(BiConsumer<CriteriaQuery<Item>, Root<Item>> criteria) {
        return search(Item.class, criteria);
    }

    private TypedQuery<ProductPicture> searchPictures(BiConsumer<CriteriaQuery<ProductPicture>, Root<ProductPicture>> criteria) {
        return search(ProductPicture.class, criteria);
    }

    public Optional<ProductPicture> getProductPicture(String text) {
        return find(ProductPicture.class, text);
    }

    @Override
    @Transactional
    public synchronized StampedEventList getSnapshot(UiMode query) {
        return new StampedEventList(getStamp(), getAllItems());
    }

    @Override
    protected UpdateSession newTransaction(long stamp) {
        return new UpdateSession(stamp);
    }

    @Override
    protected List<ItemTransition> internalAddListener(BroadcastTarget client, UiMode mode, long since) {
        log.debug("Adding {} to broadcast list", client);
        sessions.put(client, mode);

        var cb = em.getCriteriaBuilder();
        return search(Item.class, (query, root) ->
                query.where(cb.gt(root.get(Item_.sequence), since)))
                .getResultStream()
                .map(i -> ItemTransition.forItem(mode, i))
                .toList();
    }

    public class UpdateSession extends AbstractUpdateSession {
        UpdateSession(long stamp) {
            super(stamp);
        }

        public State setItemState(String itemName, Action action) {
            final Item item = getItemByName(itemName);
            if (action.from.contains(item.getState())) {
                setItemState(item, action.to);
            }
            return item.getState();
        }

        public void setItemState(Item item, State newState) {
            item.setState(newState);
            item.setSequence(stamp);
            sessions.forEach((target, mode) -> notifications.put(target,
                    ItemTransition.forItem(mode, item)));
        }
    }
}
