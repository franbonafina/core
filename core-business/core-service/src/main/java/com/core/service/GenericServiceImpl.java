package com.retcon.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.retcon.core.dao.IGenericDao;
import com.retcon.core.dao.exception.DaoException;
import com.retcon.core.service.filter.Filter;
import com.retcon.core.service.filter.Filter.OP;
import com.retcon.core.service.filter.Sort;
import com.retcon.core.service.filter.Sort.ORDER;

public class GenericServiceImpl<E, PK extends Serializable> implements IGenericService<E, PK> {

    protected static final Logger LOG = LoggerFactory.getLogger(GenericServiceImpl.class);

    protected IGenericDao<E, PK> dao;

    public GenericServiceImpl() {}

    public GenericServiceImpl(IGenericDao<E, PK> dao) {
        super();
        this.dao = dao;
    }

    public IGenericDao<E, PK> getDao() {
        return dao;
    }

    public void setDao(IGenericDao<E, PK> dao) {
        this.dao = dao;
    }

    @Transactional
    public PK save(E newInstance) throws DaoException {
        PK pk = null;
        try {
            pk = (PK) dao.persist(newInstance);
        } catch (ConstraintViolationException e) {
            throw new DaoException(e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new DaoException(e.getMessage(), e);
        } catch (Exception e) {
            throw new DaoException(e.getMessage(), e);
        }

        return pk;
    }

    @Transactional
    public void update(E transientObject) {
        dao.update(transientObject);
    }

    @Transactional
    public void saveOrUpdate(E transientObject) {
        dao.saveOrUpdate(transientObject);
    }

    @Transactional()
    public boolean delete(E persistentObject) {
        return dao.delete(persistentObject);
    }

    @Transactional
    public boolean deleteByPrimaryKey(PK primaryKey) {
        return dao.deleteByPrimaryKey(primaryKey);
    }

    @Transactional
    public boolean deleteAll() {
        return dao.deleteAll();
    }

    @Override
    public Long countAll() {
        return dao.countAll();
    }

    @Override
    public Long count(List<Filter> filters) {
        List<com.retcon.core.dao.filter.Filter> daoFilters = toDaoFilters(filters);
        return dao.count(daoFilters);
    }

    @Override
    public E findByPrimaryKey(PK primaryKey) {
        return (E) dao.findByPrimaryKey(primaryKey);
    }

    @Override
    public List<E> findAll() {
        return dao.findAll();
    }

    @Override
    public List<E> search(List<Filter> filters, List<Sort> sorts, int firstResult, int maxResults) {
        List<com.retcon.core.dao.filter.Filter> daoFilters = toDaoFilters(filters);
        List<com.retcon.core.dao.filter.Sort> daoSorts = toDaoSorts(sorts);

        return dao.search(daoFilters, daoSorts, firstResult, maxResults);
    }

    public List<E> search(List<Filter> filters, List<Sort> sorts) {
        return this.search(filters, sorts, 0, -1);
    }

    public List<E> search(List<Filter> filters) {
        return this.search(filters, null);
    }

    private List<com.retcon.core.dao.filter.Filter> toDaoFilters(List<Filter> filters) {
        List<com.retcon.core.dao.filter.Filter> daoFilters = new ArrayList<com.retcon.core.dao.filter.Filter>();
        if (null != filters) {

            com.retcon.core.dao.filter.Filter daoFilter = null;
            String property;
            OP operator;
            Object daoFilterValue;
            for (Filter filter : filters) {
                property = filter.getProperty();
                operator = filter.getOperator();

                if (Filter.OP.AND == operator || Filter.OP.OR == operator) {
                    daoFilterValue = toDaoFilters((List<Filter>) filter.getValue());
                } else if ((Filter.OP.NOT == operator || Filter.OP.SOME == operator
                                || Filter.OP.ALL == operator || Filter.OP.NONE == operator)
                                && filter.getValue() instanceof Filter) {
                    List<Filter> filterList = new ArrayList<Filter>();
                    filterList.add(((Filter) filter.getValue()));
                    daoFilterValue = toDaoFilters(filterList).get(0);
                } else {
                    daoFilterValue = filter.getValue();
                }

                daoFilter = new com.retcon.core.dao.filter.Filter(property, daoFilterValue,
                                com.retcon.core.dao.filter.Filter.OP.valueOf(operator.name()));
                daoFilters.add(daoFilter);
            }
        }
        return daoFilters;
    }

    private List<com.retcon.core.dao.filter.Sort> toDaoSorts(List<Sort> sorts) {
        List<com.retcon.core.dao.filter.Sort> daoSorts = new ArrayList<com.retcon.core.dao.filter.Sort>();
        if (null != sorts) {
            com.retcon.core.dao.filter.Sort daoSort = null;
            String property;
            ORDER order;
            boolean ignoreCase;
            for (Sort sort : sorts) {
                property = sort.getProperty();
                order = sort.getOrder();
                ignoreCase = sort.isIgnoreCase();
                daoSort = new com.retcon.core.dao.filter.Sort(property,
                                com.retcon.core.dao.filter.Sort.ORDER.valueOf(order.name()),
                                ignoreCase);
                daoSorts.add(daoSort);
            }
        }
        return daoSorts;
    }

    public List<Filter> createFilterByEqualsProperties(Map<String, Object> properties) {
        List<Filter> filters = new ArrayList<Filter>();
        List<Filter> prpFilters = new ArrayList<Filter>();
        if (properties != null && !properties.isEmpty()) {

            for (Entry<String, Object> prop : properties.entrySet()) {
                String clave = prop.getKey();
                Object valor = prop.getValue();

                if (valor != null) {
                    if (valor instanceof String
                                    && (((String) valor).charAt(0) == '*' || ((String) valor)
                                                    .charAt((((String) valor).length() - 1)) == '*')) {

                        prpFilters.add(new Filter(clave, ((String) valor).replace("*", "%"),
                                        OP.LIKE));
                    } else {
                        prpFilters.add(new Filter(clave, valor, OP.EQUAL));
                    }
                }
            }

        }
        Filter andFilter = new Filter("AND", prpFilters, OP.AND);
        filters.add(andFilter);
        return filters;

    }

    public List<Sort> generateSort(String sorts) {

        List<Sort> prpSort = new ArrayList<Sort>();
        if (sorts != null) {
            String[] split = sorts.split(",");
            if (split != null && split.length > 0) {
                for (String sort : split) {
                    if (sort.charAt(0) != '-') {
                        prpSort.add(new Sort(sort, ORDER.ASCENDING));
                    } else {
                        prpSort.add(new Sort((String) sort.substring(1), ORDER.DESCENDING));
                    }

                }
            }
        }
        return prpSort;
    }

    public List<Filter> generateAndFilters(Map<String, Object> attributeValue) {
        List<Filter> filters = new ArrayList<Filter>();

        for (Entry<String, Object> entry : attributeValue.entrySet()) {
            if (null != entry.getValue()) {
                filters = likeFilter(entry.getKey(), entry.getValue().toString(), filters);
            }
        }

        return filters;
    }

    public List<Filter> equalFilter(String property, Object value, List<Filter> filters) {
        if (null != property && !property.isEmpty() && null != value && null != value) {

            Filter filter = new Filter(property, value, Filter.OP.EQUAL);
            filters.add(filter);
        }

        return filters;
    }

    public List<Filter> likeFilter(String property, String value, List<Filter> filters) {
        if (null != property && !property.isEmpty() && null != value && !value.isEmpty()) {

            Filter filter = new Filter(property, "%" + value + "%", Filter.OP.ILIKE);
            filters.add(filter);
        }

        return filters;
    }
}
