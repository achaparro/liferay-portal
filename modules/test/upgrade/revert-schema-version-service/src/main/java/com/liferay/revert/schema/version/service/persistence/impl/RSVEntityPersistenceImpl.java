/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.revert.schema.version.service.persistence.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.revert.schema.version.exception.NoSuchEntityException;
import com.liferay.revert.schema.version.model.RSVEntity;
import com.liferay.revert.schema.version.model.impl.RSVEntityImpl;
import com.liferay.revert.schema.version.model.impl.RSVEntityModelImpl;
import com.liferay.revert.schema.version.service.persistence.RSVEntityPersistence;
import com.liferay.revert.schema.version.service.persistence.impl.constants.RSVPersistenceConstants;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The persistence implementation for the rsv entity service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
@Component(service = RSVEntityPersistence.class)
public class RSVEntityPersistenceImpl
	extends BasePersistenceImpl<RSVEntity> implements RSVEntityPersistence {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use <code>RSVEntityUtil</code> to access the rsv entity persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static final String FINDER_CLASS_NAME_ENTITY =
		RSVEntityImpl.class.getName();

	public static final String FINDER_CLASS_NAME_LIST_WITH_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List1";

	public static final String FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List2";

	private FinderPath _finderPathWithPaginationFindAll;
	private FinderPath _finderPathWithoutPaginationFindAll;
	private FinderPath _finderPathCountAll;

	public RSVEntityPersistenceImpl() {
		setModelClass(RSVEntity.class);

		setModelImplClass(RSVEntityImpl.class);
		setModelPKClass(long.class);
	}

	/**
	 * Caches the rsv entity in the entity cache if it is enabled.
	 *
	 * @param rsvEntity the rsv entity
	 */
	@Override
	public void cacheResult(RSVEntity rsvEntity) {
		entityCache.putResult(
			entityCacheEnabled, RSVEntityImpl.class, rsvEntity.getPrimaryKey(),
			rsvEntity);

		rsvEntity.resetOriginalValues();
	}

	/**
	 * Caches the rsv entities in the entity cache if it is enabled.
	 *
	 * @param rsvEntities the rsv entities
	 */
	@Override
	public void cacheResult(List<RSVEntity> rsvEntities) {
		for (RSVEntity rsvEntity : rsvEntities) {
			if (entityCache.getResult(
					entityCacheEnabled, RSVEntityImpl.class,
					rsvEntity.getPrimaryKey()) == null) {

				cacheResult(rsvEntity);
			}
			else {
				rsvEntity.resetOriginalValues();
			}
		}
	}

	/**
	 * Clears the cache for all rsv entities.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache() {
		entityCache.clearCache(RSVEntityImpl.class);

		finderCache.clearCache(FINDER_CLASS_NAME_ENTITY);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION);
	}

	/**
	 * Clears the cache for the rsv entity.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache(RSVEntity rsvEntity) {
		entityCache.removeResult(
			entityCacheEnabled, RSVEntityImpl.class, rsvEntity.getPrimaryKey());

		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION);
	}

	@Override
	public void clearCache(List<RSVEntity> rsvEntities) {
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION);

		for (RSVEntity rsvEntity : rsvEntities) {
			entityCache.removeResult(
				entityCacheEnabled, RSVEntityImpl.class,
				rsvEntity.getPrimaryKey());
		}
	}

	@Override
	public void clearCache(Set<Serializable> primaryKeys) {
		finderCache.clearCache(FINDER_CLASS_NAME_ENTITY);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);
		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION);

		for (Serializable primaryKey : primaryKeys) {
			entityCache.removeResult(
				entityCacheEnabled, RSVEntityImpl.class, primaryKey);
		}
	}

	/**
	 * Creates a new rsv entity with the primary key. Does not add the rsv entity to the database.
	 *
	 * @param rsvEntryId the primary key for the new rsv entity
	 * @return the new rsv entity
	 */
	@Override
	public RSVEntity create(long rsvEntryId) {
		RSVEntity rsvEntity = new RSVEntityImpl();

		rsvEntity.setNew(true);
		rsvEntity.setPrimaryKey(rsvEntryId);

		rsvEntity.setCompanyId(CompanyThreadLocal.getCompanyId());

		return rsvEntity;
	}

	/**
	 * Removes the rsv entity with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity that was removed
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	@Override
	public RSVEntity remove(long rsvEntryId) throws NoSuchEntityException {
		return remove((Serializable)rsvEntryId);
	}

	/**
	 * Removes the rsv entity with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param primaryKey the primary key of the rsv entity
	 * @return the rsv entity that was removed
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	@Override
	public RSVEntity remove(Serializable primaryKey)
		throws NoSuchEntityException {

		Session session = null;

		try {
			session = openSession();

			RSVEntity rsvEntity = (RSVEntity)session.get(
				RSVEntityImpl.class, primaryKey);

			if (rsvEntity == null) {
				if (_log.isDebugEnabled()) {
					_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
				}

				throw new NoSuchEntityException(
					_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			return remove(rsvEntity);
		}
		catch (NoSuchEntityException noSuchEntityException) {
			throw noSuchEntityException;
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}
	}

	@Override
	protected RSVEntity removeImpl(RSVEntity rsvEntity) {
		Session session = null;

		try {
			session = openSession();

			if (!session.contains(rsvEntity)) {
				rsvEntity = (RSVEntity)session.get(
					RSVEntityImpl.class, rsvEntity.getPrimaryKeyObj());
			}

			if (rsvEntity != null) {
				session.delete(rsvEntity);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		if (rsvEntity != null) {
			clearCache(rsvEntity);
		}

		return rsvEntity;
	}

	@Override
	public RSVEntity updateImpl(RSVEntity rsvEntity) {
		boolean isNew = rsvEntity.isNew();

		Session session = null;

		try {
			session = openSession();

			if (rsvEntity.isNew()) {
				session.save(rsvEntity);

				rsvEntity.setNew(false);
			}
			else {
				rsvEntity = (RSVEntity)session.merge(rsvEntity);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		finderCache.clearCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);

		if (isNew) {
			finderCache.removeResult(_finderPathCountAll, FINDER_ARGS_EMPTY);
			finderCache.removeResult(
				_finderPathWithoutPaginationFindAll, FINDER_ARGS_EMPTY);
		}

		entityCache.putResult(
			entityCacheEnabled, RSVEntityImpl.class, rsvEntity.getPrimaryKey(),
			rsvEntity, false);

		rsvEntity.resetOriginalValues();

		return rsvEntity;
	}

	/**
	 * Returns the rsv entity with the primary key or throws a <code>com.liferay.portal.kernel.exception.NoSuchModelException</code> if it could not be found.
	 *
	 * @param primaryKey the primary key of the rsv entity
	 * @return the rsv entity
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	@Override
	public RSVEntity findByPrimaryKey(Serializable primaryKey)
		throws NoSuchEntityException {

		RSVEntity rsvEntity = fetchByPrimaryKey(primaryKey);

		if (rsvEntity == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			throw new NoSuchEntityException(
				_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
		}

		return rsvEntity;
	}

	/**
	 * Returns the rsv entity with the primary key or throws a <code>NoSuchEntityException</code> if it could not be found.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	@Override
	public RSVEntity findByPrimaryKey(long rsvEntryId)
		throws NoSuchEntityException {

		return findByPrimaryKey((Serializable)rsvEntryId);
	}

	/**
	 * Returns the rsv entity with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity, or <code>null</code> if a rsv entity with the primary key could not be found
	 */
	@Override
	public RSVEntity fetchByPrimaryKey(long rsvEntryId) {
		return fetchByPrimaryKey((Serializable)rsvEntryId);
	}

	/**
	 * Returns all the rsv entities.
	 *
	 * @return the rsv entities
	 */
	@Override
	public List<RSVEntity> findAll() {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
	}

	/**
	 * Returns a range of all the rsv entities.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of rsv entities
	 * @param end the upper bound of the range of rsv entities (not inclusive)
	 * @return the range of rsv entities
	 */
	@Override
	public List<RSVEntity> findAll(int start, int end) {
		return findAll(start, end, null);
	}

	/**
	 * Returns an ordered range of all the rsv entities.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of rsv entities
	 * @param end the upper bound of the range of rsv entities (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of rsv entities
	 */
	@Override
	public List<RSVEntity> findAll(
		int start, int end, OrderByComparator<RSVEntity> orderByComparator) {

		return findAll(start, end, orderByComparator, true);
	}

	/**
	 * Returns an ordered range of all the rsv entities.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of rsv entities
	 * @param end the upper bound of the range of rsv entities (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of rsv entities
	 */
	@Override
	public List<RSVEntity> findAll(
		int start, int end, OrderByComparator<RSVEntity> orderByComparator,
		boolean useFinderCache) {

		FinderPath finderPath = null;
		Object[] finderArgs = null;

		if ((start == QueryUtil.ALL_POS) && (end == QueryUtil.ALL_POS) &&
			(orderByComparator == null)) {

			if (useFinderCache) {
				finderPath = _finderPathWithoutPaginationFindAll;
				finderArgs = FINDER_ARGS_EMPTY;
			}
		}
		else if (useFinderCache) {
			finderPath = _finderPathWithPaginationFindAll;
			finderArgs = new Object[] {start, end, orderByComparator};
		}

		List<RSVEntity> list = null;

		if (useFinderCache) {
			list = (List<RSVEntity>)finderCache.getResult(
				finderPath, finderArgs, this);
		}

		if (list == null) {
			StringBundler sb = null;
			String sql = null;

			if (orderByComparator != null) {
				sb = new StringBundler(
					2 + (orderByComparator.getOrderByFields().length * 2));

				sb.append(_SQL_SELECT_RSVENTITY);

				appendOrderByComparator(
					sb, _ORDER_BY_ENTITY_ALIAS, orderByComparator);

				sql = sb.toString();
			}
			else {
				sql = _SQL_SELECT_RSVENTITY;

				sql = sql.concat(RSVEntityModelImpl.ORDER_BY_JPQL);
			}

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				list = (List<RSVEntity>)QueryUtil.list(
					query, getDialect(), start, end);

				cacheResult(list);

				if (useFinderCache) {
					finderCache.putResult(finderPath, finderArgs, list);
				}
			}
			catch (Exception exception) {
				if (useFinderCache) {
					finderCache.removeResult(finderPath, finderArgs);
				}

				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Removes all the rsv entities from the database.
	 *
	 */
	@Override
	public void removeAll() {
		for (RSVEntity rsvEntity : findAll()) {
			remove(rsvEntity);
		}
	}

	/**
	 * Returns the number of rsv entities.
	 *
	 * @return the number of rsv entities
	 */
	@Override
	public int countAll() {
		Long count = (Long)finderCache.getResult(
			_finderPathCountAll, FINDER_ARGS_EMPTY, this);

		if (count == null) {
			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(_SQL_COUNT_RSVENTITY);

				count = (Long)query.uniqueResult();

				finderCache.putResult(
					_finderPathCountAll, FINDER_ARGS_EMPTY, count);
			}
			catch (Exception exception) {
				finderCache.removeResult(
					_finderPathCountAll, FINDER_ARGS_EMPTY);

				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	@Override
	protected EntityCache getEntityCache() {
		return entityCache;
	}

	@Override
	protected String getPKDBName() {
		return "rsvEntryId";
	}

	@Override
	protected String getSelectSQL() {
		return _SQL_SELECT_RSVENTITY;
	}

	@Override
	protected Map<String, Integer> getTableColumnsMap() {
		return RSVEntityModelImpl.TABLE_COLUMNS_MAP;
	}

	/**
	 * Initializes the rsv entity persistence.
	 */
	@Activate
	public void activate() {
		RSVEntityModelImpl.setEntityCacheEnabled(entityCacheEnabled);
		RSVEntityModelImpl.setFinderCacheEnabled(finderCacheEnabled);

		_finderPathWithPaginationFindAll = new FinderPath(
			entityCacheEnabled, finderCacheEnabled, RSVEntityImpl.class,
			FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findAll", new String[0]);

		_finderPathWithoutPaginationFindAll = new FinderPath(
			entityCacheEnabled, finderCacheEnabled, RSVEntityImpl.class,
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findAll",
			new String[0]);

		_finderPathCountAll = new FinderPath(
			entityCacheEnabled, finderCacheEnabled, Long.class,
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countAll",
			new String[0]);
	}

	@Deactivate
	public void deactivate() {
		entityCache.removeCache(RSVEntityImpl.class.getName());
		finderCache.removeCache(FINDER_CLASS_NAME_ENTITY);
		finderCache.removeCache(FINDER_CLASS_NAME_LIST_WITH_PAGINATION);
		finderCache.removeCache(FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION);
	}

	@Override
	@Reference(
		target = RSVPersistenceConstants.SERVICE_CONFIGURATION_FILTER,
		unbind = "-"
	)
	public void setConfiguration(Configuration configuration) {
		super.setConfiguration(configuration);

		_columnBitmaskEnabled = GetterUtil.getBoolean(
			configuration.get(
				"value.object.column.bitmask.enabled.com.liferay.revert.schema.version.model.RSVEntity"),
			true);
	}

	@Override
	@Reference(
		target = RSVPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Override
	@Reference(
		target = RSVPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	private boolean _columnBitmaskEnabled;

	@Reference
	protected EntityCache entityCache;

	@Reference
	protected FinderCache finderCache;

	private static final String _SQL_SELECT_RSVENTITY =
		"SELECT rsvEntity FROM RSVEntity rsvEntity";

	private static final String _SQL_COUNT_RSVENTITY =
		"SELECT COUNT(rsvEntity) FROM RSVEntity rsvEntity";

	private static final String _ORDER_BY_ENTITY_ALIAS = "rsvEntity.";

	private static final String _NO_SUCH_ENTITY_WITH_PRIMARY_KEY =
		"No RSVEntity exists with the primary key ";

	private static final Log _log = LogFactoryUtil.getLog(
		RSVEntityPersistenceImpl.class);

	static {
		try {
			Class.forName(RSVPersistenceConstants.class.getName());
		}
		catch (ClassNotFoundException classNotFoundException) {
			throw new ExceptionInInitializerError(classNotFoundException);
		}
	}

}