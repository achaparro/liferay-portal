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

package com.liferay.revert.schema.version.service.persistence;

import com.liferay.portal.kernel.service.persistence.BasePersistence;
import com.liferay.revert.schema.version.exception.NoSuchEntityException;
import com.liferay.revert.schema.version.model.RSVEntity;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The persistence interface for the rsv entity service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see RSVEntityUtil
 * @generated
 */
@ProviderType
public interface RSVEntityPersistence extends BasePersistence<RSVEntity> {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. Always use {@link RSVEntityUtil} to access the rsv entity persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this interface.
	 */

	/**
	 * Caches the rsv entity in the entity cache if it is enabled.
	 *
	 * @param rsvEntity the rsv entity
	 */
	public void cacheResult(RSVEntity rsvEntity);

	/**
	 * Caches the rsv entities in the entity cache if it is enabled.
	 *
	 * @param rsvEntities the rsv entities
	 */
	public void cacheResult(java.util.List<RSVEntity> rsvEntities);

	/**
	 * Creates a new rsv entity with the primary key. Does not add the rsv entity to the database.
	 *
	 * @param rsvEntryId the primary key for the new rsv entity
	 * @return the new rsv entity
	 */
	public RSVEntity create(long rsvEntryId);

	/**
	 * Removes the rsv entity with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity that was removed
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	public RSVEntity remove(long rsvEntryId) throws NoSuchEntityException;

	public RSVEntity updateImpl(RSVEntity rsvEntity);

	/**
	 * Returns the rsv entity with the primary key or throws a <code>NoSuchEntityException</code> if it could not be found.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity
	 * @throws NoSuchEntityException if a rsv entity with the primary key could not be found
	 */
	public RSVEntity findByPrimaryKey(long rsvEntryId)
		throws NoSuchEntityException;

	/**
	 * Returns the rsv entity with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity, or <code>null</code> if a rsv entity with the primary key could not be found
	 */
	public RSVEntity fetchByPrimaryKey(long rsvEntryId);

	/**
	 * Returns all the rsv entities.
	 *
	 * @return the rsv entities
	 */
	public java.util.List<RSVEntity> findAll();

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
	public java.util.List<RSVEntity> findAll(int start, int end);

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
	public java.util.List<RSVEntity> findAll(
		int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<RSVEntity>
			orderByComparator);

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
	public java.util.List<RSVEntity> findAll(
		int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<RSVEntity>
			orderByComparator,
		boolean useFinderCache);

	/**
	 * Removes all the rsv entities from the database.
	 */
	public void removeAll();

	/**
	 * Returns the number of rsv entities.
	 *
	 * @return the number of rsv entities
	 */
	public int countAll();

}