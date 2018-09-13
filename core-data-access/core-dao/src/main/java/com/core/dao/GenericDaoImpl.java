package com.retcon.core.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.genericdao.dao.hibernate.GenericDAOImpl;
import com.googlecode.genericdao.search.Search;
import com.retcon.core.dao.exception.DaoException;
import com.retcon.core.dao.filter.Filter;
import com.retcon.core.dao.filter.Sort;
import com.retcon.core.dao.filter.Sort.ORDER;

/**
 * Generic DAO for Hibernate implementation
 * 
 */
@Transactional
public abstract class GenericDaoImpl<E, PK extends Serializable> extends GenericDAOImpl<E, PK>
                implements IGenericDao<E, PK> {

    private static final Logger LOG = LoggerFactory.getLogger("retconsample.dao");

    protected SessionFactory sf = null;
    private Class<E> entityClass = null;

    protected abstract PK getPK(E instance);

    public GenericDaoImpl() {}

    @Autowired
    @Qualifier("sessionFactory")
    public void setSessionFactory(SessionFactory sf) {
        this.sf = sf;
        super.setSessionFactory(sf);
    }

    @SuppressWarnings("unchecked")
    public PK persist(E newInstance) throws DaoException {
        PK pk = null;
        try {
            pk = getPK(newInstance);
            if (null != pk) {
                E found = findByPrimaryKey(pk);
                if (null != found) {
                    throw new DaoException("Entity already exist", null);
                }
            }

            boolean saved = super.save(newInstance);
            if (saved) {
                pk = (PK) getPK(newInstance);
            }

        } catch (ConstraintViolationException e) {
            throw new DaoException(e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new DaoException(e.getMessage(), e);
        } catch (Exception e) {
            throw new DaoException(e.getMessage(), e);
        }
        return pk;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E findByPrimaryKey(PK primaryKey) {
        return (E) super.find(primaryKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<E> findAll() {
        List<E> list = super.findAll();
        return list;
    }

    @Override
    public void update(E transientObject) {
        super.save(transientObject);
    }

    @Override
    public void saveOrUpdate(E transientObject) {
        super.save(transientObject);
    }

    public boolean delete(E persistentObject) {
        return super.remove(persistentObject);
    }

    public boolean deleteByPrimaryKey(PK primaryKey) {
        return super.removeById(primaryKey);
    }

    public boolean deleteAll() {
        Class<?> type = getEntityClass();
        Query query = getSession().createQuery(
                        "delete from " + getMetadataUtil().get(type).getEntityName() + " ");
        return query.executeUpdate() >= 0;
    }

    @Override
    public Long countAll() {
        Search search = new Search(getEntityClass());
        // search.setSearchClass(getEntityClass());
        int count = count(search);
        return (Long) Long.valueOf(count);
    }

    @Override
    public Long count(List<Filter> filters) {
        Search search = new Search(getEntityClass());
        // Filtering
        List<com.googlecode.genericdao.search.Filter> convertedFilters = convertFilters(filters);
        search = search.addFilters((com.googlecode.genericdao.search.Filter[]) convertedFilters
                        .toArray(new com.googlecode.genericdao.search.Filter[convertedFilters
                                        .size()]));

        int count = count(search);
        return (Long) Long.valueOf(count);
    }

    @SuppressWarnings("unchecked")
    protected Class<E> getEntityClass() {
        // use lazy initialization, obtain via reflection (a one time hit)
        if (null == entityClass) {
            ParameterizedType pType = (ParameterizedType) getClass().getGenericSuperclass();
            this.entityClass = (Class<E>) pType.getActualTypeArguments()[0];
        }
        return entityClass;
    }

    public List<E> search(List<Filter> filters, List<Sort> sorts, int firstResult, int maxResults) {
        Search search = new Search(getEntityClass());

        // Filtering
        List<com.googlecode.genericdao.search.Filter> convertedFilters = convertFilters(filters);
        search = search.addFilters((com.googlecode.genericdao.search.Filter[]) convertedFilters
                        .toArray(new com.googlecode.genericdao.search.Filter[convertedFilters
                                        .size()]));

        // Sorting
        search = convertSorts(sorts, search);

        // Paging
        search.setMaxResults(maxResults); // a.k.a. results per page
        search.setFirstResult(firstResult);

        List<E> list = super.search(search);
        return list;
    }

    public E search(List<Filter> filters) {
        Search search = new Search(getEntityClass());

        // Filtering
        List<com.googlecode.genericdao.search.Filter> convertedFilters = convertFilters(filters);
        search = search.addFilters((com.googlecode.genericdao.search.Filter[]) convertedFilters
                        .toArray(new com.googlecode.genericdao.search.Filter[convertedFilters
                                        .size()]));

        search.setMaxResults(1); // a.k.a. results per page
        search.setFirstResult(0);

        List<E> list = super.search(search);

        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    private Search convertSorts(List<Sort> sorts, Search search) {

        if (sorts != null && !sorts.isEmpty()) {
            Sort.ORDER sortOrder = null;
            for (Sort sort : sorts) {
                sortOrder = (Sort.ORDER) sort.getOrder();
                if (sortOrder == ORDER.ASCENDING) {
                    search.addSort(sort.getProperty(), false, sort.isIgnoreCase()); // ascending
                } else if (sortOrder == ORDER.DESCENDING) {
                    search.addSort(sort.getProperty(), true, sort.isIgnoreCase()); // descending
                } else {
                    throw new IllegalArgumentException(sortOrder.toString());
                }
            }
        }

        return search;
    }

    private List<com.googlecode.genericdao.search.Filter> convertFilters(List<Filter> filters) {
        com.googlecode.genericdao.search.Filter f = null;
        List<com.googlecode.genericdao.search.Filter> list = new ArrayList<com.googlecode.genericdao.search.Filter>();
        int operator;
        for (Filter filter : filters) {
            f = new com.googlecode.genericdao.search.Filter();
            f.setProperty(filter.getProperty());
            operator = filter.getOperator().getVal();
            f.setOperator(operator);
            if (com.googlecode.genericdao.search.Filter.OP_AND == operator
                            || com.googlecode.genericdao.search.Filter.OP_OR == operator

            ) {
                f.setValue(convertFilters((List<Filter>) filter.getValue()));
            } else {
                f.setValue(filter.getValue());
            }

            if ((com.googlecode.genericdao.search.Filter.OP_NOT == operator
                            || com.googlecode.genericdao.search.Filter.OP_SOME == operator
                            || com.googlecode.genericdao.search.Filter.OP_ALL == operator || com.googlecode.genericdao.search.Filter.OP_NONE == operator)
                            && filter.getValue() instanceof Filter) {
                ArrayList<Filter> nestedFilter = new ArrayList<Filter>();
                nestedFilter.add((Filter) filter.getValue());
                f.setValue(convertFilters(nestedFilter).get(0));
            }

            list.add(f);
        }

        return list;
    }

    public Object findManyToOneAttribute(E entity, String entityPkAttributeName,
                                         Class manyToOneClass, String manyToOneAttributeName) {
        Object result = null;
        if (null != entity && null != getPK(entity)) {
            Session session = getSession();
            Query query = session.createQuery("select r " + "from "
                            + getEntityClass().getSimpleName() + " e, "
                            + manyToOneClass.getSimpleName() + " r " + "where e."
                            + manyToOneAttributeName + " = r and e." + entityPkAttributeName
                            + "= :entityID ");
            query.setParameter("entityID", getPK(entity));
            result = query.uniqueResult();
        }

        return result;
    }

    public Collection findCollectionAttribute(E entity, String entityPkAttributeName,
                                              String manyToOneAttributeName) {
        Collection result = null;
        if (null != entity && null != getPK(entity)) {
            Session session = getSession();
            Query query = session.createQuery("select r " + "from "
                            + getEntityClass().getSimpleName() + " e join e."
                            + manyToOneAttributeName + " r " + "where e." + entityPkAttributeName
                            + "= :entityID");
            query.setParameter("entityID", getPK(entity));
            result = query.list();
        }

        return result;
    }

    public Object findManyToOneAttribute(E entity, String entityPkAttributeName,
                                         Class manyToOneClass, String manyToOneAttributeName,
                                         String manyToOnePkAttributeName) {
        Object result = null;
        if (null != entity && null != getPK(entity)) {
            Session session = getSession();
            Query query = session.createQuery("select r " + "from "
                            + getEntityClass().getSimpleName() + " e, "
                            + manyToOneClass.getSimpleName() + " r " + "where e."
                            + manyToOneAttributeName + "." + manyToOnePkAttributeName + " = r."
                            + manyToOnePkAttributeName + " and e." + entityPkAttributeName
                            + "= :entityID ");
            query.setParameter("entityID", getPK(entity));
            result = query.uniqueResult();
        }

        return result;
    }

    public Object fetch(E entity, String entityPkAttributeName, List<String> entitiesRelated) {
        Object result = null;

        if (null != entity && null != getPK(entity)) {
            Session session = getSession();

            StringBuilder queryString = new StringBuilder();
            queryString.append("select e from " + getEntityClass().getSimpleName() + " e ");

            Iterator<String> it = entitiesRelated.iterator();
            while (it.hasNext()) {
                String related = it.next();
                queryString.append(" left join fetch e." + related);
            }

            queryString.append(" where e." + entityPkAttributeName + "= :entityID");
            Query query = session.createQuery(queryString.toString());
            query.setParameter("entityID", getPK(entity));

            result = query.uniqueResult();
        }

        return result;
    }

    /**
     * Get the current Hibernate session
     */
    public Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    public Session getNewSession() {
        return getSessionFactory().openSession();
    }
}
