package org.gamboni.shopping.server.domain;

import com.google.common.collect.ImmutableList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * @author tendays
 */
@ApplicationScoped
public class Store {

    @Inject
    EntityManager em;

        public long nextSequence() {
            return ((Number)em.createNativeQuery("select next value for versions").getSingleResult())
                    .longValue();
        }

        public List<Item> getAllItems() {
            Map<String, Item> map = new TreeMap<>();
            searchPictures((q, r) -> {}).getResultList()
                    .forEach(p -> map.put(p.getText(),
                            new Item(p)));
            searchItems((q, r) -> {}).getResultList()
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

        public <T> TypedQuery<T> search(Class<T> entity, BiConsumer<CriteriaQuery<T>, Root<T>> criteria) {
            final CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(entity);
            final Root<T> root = query.from(entity);
            criteria.accept(query, root);
            return em.createQuery(query);
        }

        private TypedQuery<Item> searchItems(BiConsumer<CriteriaQuery<Item>, Root<Item>> criteria) {
            return search(Item.class, criteria);
        }

        private TypedQuery<ProductPicture> searchPictures(BiConsumer<CriteriaQuery<ProductPicture>, Root<ProductPicture>> criteria) {
            return search(ProductPicture.class, criteria);
        }

        public <T> Optional<T> find(Class<T> entityType, Object id) {
            return Optional.ofNullable(em.find(entityType, id));
        }

        public Optional<ProductPicture> getProductPicture(String text) {
            return find(ProductPicture.class, text);
        }

   /* protected EntityManagerFactory startHibernate() {
        MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setCharacterEncoding("utf8");
            dataSource.setUseUnicode(true);
            dataSource.setConnectionCollation("utf8mb4_general_ci"); //utf8"); // maybe add COLLATE utf8mb4_general_ci?

        Properties prop = new Properties();
        prop.setProperty(AvailableSettings.HBM2DDL_AUTO, "update");
        prop.setProperty(AvailableSettings.SHOW_SQL, "true");
        prop.setProperty(AvailableSettings.DIALECT, MySQLDialect.class.getName());
        prop.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "50");

        // See https://www.databasesandlife.com/automatic-reconnect-from-hibernate-to-mysql/
        prop.setProperty(AvailableSettings.C3P0_MIN_SIZE, "5");
        prop.setProperty(AvailableSettings.C3P0_MAX_SIZE, "20");
        prop.setProperty(AvailableSettings.C3P0_TIMEOUT, "1800");
        prop.setProperty(AvailableSettings.C3P0_MAX_STATEMENTS, "50");
        prop.setProperty(AvailableSettings.CONNECTION_PROVIDER, C3P0ConnectionProvider.class.getName());

        return new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(pui), prop).build();

    }*/

    public EntityManager getEm() {
        return em;
    }
}
