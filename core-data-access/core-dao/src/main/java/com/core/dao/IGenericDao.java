package com.retcon.core.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;

import com.retcon.core.dao.exception.DaoException;
import com.retcon.core.dao.filter.Filter;
import com.retcon.core.dao.filter.Sort;

/**
 * Generic DAO for Hibernate
 */
public interface IGenericDao<E, PK extends Serializable> {

    PK persist(E newInstance) throws DaoException;

    void update(E transientObject);

    void saveOrUpdate(E transientObject);

    boolean delete(E persistentObject);

    boolean deleteByPrimaryKey(PK primaryKey);

    boolean deleteAll();

    //boolean deleteByPrimaryKeys(PK... pks);

    Long countAll();

    Long count(List<Filter> filters);

    E findByPrimaryKey(PK primaryKey);

    //	List<E> findAllByProperty(String propertyName, Object value);
    //
    //	E findByUniqueProperty(String propertyName, Object value);

    List<E> findAll();

    List<E> search(List<Filter> filters, List<Sort> sorts, int firstResult, int maxResults);

    E search(List<Filter> filters);

    /**
     * Get the current Hibernate session
     */
    public Session getSession();

    public Session getNewSession();
}
