package com.retcon.core.service;

import java.util.List;
import java.util.Map;

import com.retcon.core.dao.exception.DaoException;
import com.retcon.core.service.filter.Filter;
import com.retcon.core.service.filter.Sort;

/**
 * Generic Service
 */
public interface IGenericService<E, PK> {

    public PK save(E newInstance) throws DaoException;

    public void update(E transientObject);

    public void saveOrUpdate(E transientObject);

    public boolean delete(E persistentObject);

    public boolean deleteByPrimaryKey(PK primaryKey);

    public boolean deleteAll();

    //public boolean deleteByPrimaryKeys(PK... pks);

    public Long countAll();

    public Long count(List<Filter> filters);

    public E findByPrimaryKey(PK primaryKey);

    public List<E> findAll();

    public List<E> search(List<Filter> filters, List<Sort> sorts, int firstResult, int maxResults);

    public List<Filter> createFilterByEqualsProperties(Map<String, Object> properties);

    public List<Sort> generateSort(String sorts);
}
