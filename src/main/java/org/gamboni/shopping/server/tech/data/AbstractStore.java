package org.gamboni.shopping.server.tech.data;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.Optional;
import java.util.function.BiConsumer;

public class AbstractStore {
    @Inject
    protected EntityManager em;


    public <T> TypedQuery<T> search(Class<T> entity, BiConsumer<CriteriaQuery<T>, Root<T>> criteria) {
        final CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(entity);
        final Root<T> root = query.from(entity);
        criteria.accept(query, root);
        return em.createQuery(query);
    }

    public <T> Optional<T> find(Class<T> entityType, Object id) {
        return Optional.ofNullable(em.find(entityType, id));
    }

    public EntityManager getEm() {
        return em;
    }
}
