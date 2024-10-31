package org.gamboni.shopping.server.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.gamboni.shopping.server.ui.UiMode;
import org.gamboni.tech.history.event.ElementRemovedEvent;
import org.gamboni.tech.history.event.Event;
import org.gamboni.tech.history.event.NewStateEvent;
import org.gamboni.tech.history.event.StampedEventList;
import org.gamboni.tech.persistence.PersistedHistoryStore;
import org.gamboni.tech.web.ws.BroadcastTarget;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

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
        return new StampedEventList(getStamp(), getAllItems()
                .stream()
                .map(NewItemEvent::forItem)
                .toList());
    }

    @Override
    protected UpdateSession newTransaction(long stamp) {
        return new UpdateSession(stamp);
    }

    @Override
    protected List<? extends Event> internalAddListener(BroadcastTarget client, UiMode mode, long since) {
        var cb = em.getCriteriaBuilder();
        return search(Item.class, (query, root) ->
                query.where(cb.gt(root.get(Item_.sequence), since)))
                .getResultStream()
                .flatMap(i -> {
                    if (!mode.test(i.getState())) {
                        return Stream.of(new ElementRemovedEvent("", i.getText()));
                    } else {
                        // We can't know if this element was there before, so we can't know whether to use new-state event or new-element event.
                        // In doubt, we emit both, and one of them will be ignored by the client.
                        return Stream.of(
                                NewItemEvent.forItem(i),
                                new NewStateEvent<>("", i.getText(), i.getState()));
                    }
                })
                .toList();
    }

    public class UpdateSession extends AbstractUpdateSession {
        UpdateSession(long stamp) {
            super(stamp);
        }

        public void setItemState(String itemName, Action action) {
            final Item item = getItemByName(itemName);
            if (action.from.contains(item.getState())) {
                setItemState(item, action.to);
            }
        }

        public void setItemState(Item item, State newState) {
            State oldState = item.getState();
            item.setState(newState);
            item.setSequence(stamp);

            notifyListeners(notifications, mode -> {
                boolean visibleBefore = mode.test(oldState);
                boolean visibleAfter = mode.test(newState);
                if (visibleBefore) {
                    if (visibleAfter) {
                        return Optional.of(new NewStateEvent<>("", item.getText(), newState));
                    } else { // visibleBefore && !visibleAfter
                        return Optional.of(new ElementRemovedEvent("", item.getText()));
                    }
                } else { // !visibleBefore
                    if (visibleAfter) {
                        return Optional.of(NewItemEvent.forItem(item));
                    } else { // !visibleBefore && !visibleAfter
                        return Optional.empty();
                    }
                }
            });
        }
    }
}
