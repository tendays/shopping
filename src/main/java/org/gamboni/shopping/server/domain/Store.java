package org.gamboni.shopping.server.domain;

import com.google.common.collect.ImmutableList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.gamboni.shopping.server.tech.data.AbstractStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * @author tendays
 */
@ApplicationScoped
public class Store extends AbstractStore {

    public long nextSequence() {
        return ((Number) em.createNativeQuery("select next value for versions").getSingleResult())
                .longValue();
    }

    public List<Item> getAllItems() {
        Map<String, Item> map = new TreeMap<>();
        searchPictures((q, r) -> {
        }).getResultList()
                .forEach(p -> map.put(p.getText(),
                        new Item(p)));
        searchItems((q, r) -> {
        }).getResultList()
                .forEach(i -> map.put(i.getText(), i));

        return ImmutableList.copyOf(map.values());
    }

    public synchronized Item getItemByName(String name) {
        return find(Item.class, name).orElseGet(() -> {
            final Item newItem = new Item(name);
            getProductPicture(name).ifPresent(newItem::setImage);
            em.persist(newItem);
            return newItem;
        });
    }


    private TypedQuery<Item> searchItems(BiConsumer<CriteriaQuery<Item>, Root<Item>> criteria) {
        return search(Item.class, criteria);
    }

    private TypedQuery<ProductPicture> searchPictures(BiConsumer<CriteriaQuery<ProductPicture>, Root<ProductPicture>> criteria) {
        return search(ProductPicture.class, criteria);
    }

    public Optional<ProductPicture> getProductPicture(String text) {
        return find(ProductPicture.class, text);
    }

    @Transactional
    public List<Item> getItemsSince(Long since) {
        var cb = em.getCriteriaBuilder();
        return search(Item.class, (query, root) ->
                        query.where(cb.gt(root.get(Item_.sequence), since)))
                .getResultList();
    }
}
