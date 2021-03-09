package org.gamboni.shopping.server.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import org.gamboni.shopping.server.domain.Item;
import org.gamboni.shopping.server.domain.Item_;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.tool.schema.Action;

import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.SharedCacheMode;
import javax.persistence.TypedQuery;
import javax.persistence.ValidationMode;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

/**
 * @author tendays
 */
public class Store {

    private final EntityManagerFactory sessionFactory;

    public Store() {
        this.sessionFactory = this.startHibernate();
    }

    public interface TransactionBody<T> {
        T run(Session session) throws Exception;
    }

    public <T> T transaction(TransactionBody<T> body) throws Exception {
        EntityTransaction tx = null;
        EntityManager em = null;
        try {
            em = sessionFactory.createEntityManager();
            tx = em.getTransaction();
            tx.begin();
            T result = body.run(new Session(em));
            tx.commit();
            tx = null;

            return result;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (tx != null) { tx.rollback(); }
            if (em != null) { em.close(); }
        }
    }

    public class Session {
        private final EntityManager em;
        public final CriteriaBuilder cb;

        public Session(EntityManager em) {
            this.em = em;
            this.cb = em.getCriteriaBuilder();
        }

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
    }

    protected EntityManagerFactory startHibernate() {
        MysqlDataSource dataSource = new MysqlDataSource();
        //try {
            dataSource.setUrl("jdbc:mysql://localhost:3306/shopping");
            dataSource.setUser("shopping");
            dataSource.setPassword("shopping");
            dataSource.setCharacterEncoding("utf8");
            dataSource.setUseUnicode(true);
            dataSource.setConnectionCollation("utf8mb4_general_ci"); //utf8"); // maybe add COLLATE utf8mb4_general_ci?
        /*} catch (SQLException e) {
            throw new RuntimeException(e);
        }*/

        Properties prop = new Properties();
/*        prop.setProperty(AvailableSettings.JPA_JDBC_URL, dataSource.getUrl());
        prop.setProperty(AvailableSettings.JPA_JDBC_USER, dataSource.getUser());
        prop.setProperty(AvailableSettings.JPA_JDBC_PASSWORD, "shopping");
  */      prop.setProperty(AvailableSettings.URL, dataSource.getUrl());
        prop.setProperty(AvailableSettings.USER, dataSource.getUser());
        prop.setProperty(AvailableSettings.PASS, "shopping");
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

        PersistenceUnitInfo pui = new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "shopping";
            }

            @Override
            public String getPersistenceProviderClassName() {
                // the name sounds right but I've no idea what I'm doing
                return HibernatePersistenceProvider.class.getName();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return dataSource;
            }

            @Override
            public List<String> getMappingFileNames() {
                return ImmutableList.of();
            }

            @Override
            public List<URL> getJarFileUrls() {
                return ImmutableList.of();
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return Lists.transform(ImmutableList.of(Item.class, ProductPicture.class),
                        Class::getName);
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return SharedCacheMode.UNSPECIFIED; // what is this
            }

            @Override
            public ValidationMode getValidationMode() {
                return ValidationMode.AUTO; // sounds good
            }

            @Override
            public Properties getProperties() {
                return prop;
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return "2.1"; // == JPA_VERSION?
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }

            @Override
            public void addTransformer(ClassTransformer classTransformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return getClassLoader();
            }

        };


        new HibernatePersistenceProvider().createContainerEntityManagerFactory(pui, prop);

        ImmutableMap<Object, Object> configuration = ImmutableMap.of(); // ?
        return new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(pui), configuration).build();

    }
}
