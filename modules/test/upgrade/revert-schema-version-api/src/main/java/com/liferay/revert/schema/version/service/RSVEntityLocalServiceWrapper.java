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

package com.liferay.revert.schema.version.service;

import com.liferay.portal.kernel.service.ServiceWrapper;

/**
 * Provides a wrapper for {@link RSVEntityLocalService}.
 *
 * @author Brian Wing Shun Chan
 * @see RSVEntityLocalService
 * @generated
 */
public class RSVEntityLocalServiceWrapper
	implements RSVEntityLocalService, ServiceWrapper<RSVEntityLocalService> {

	public RSVEntityLocalServiceWrapper(
		RSVEntityLocalService rsvEntityLocalService) {

		_rsvEntityLocalService = rsvEntityLocalService;
	}

	/**
	 * Adds the rsv entity to the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was added
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity addRSVEntity(
		com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return _rsvEntityLocalService.addRSVEntity(rsvEntity);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel createPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _rsvEntityLocalService.createPersistedModel(primaryKeyObj);
	}

	/**
	 * Creates a new rsv entity with the primary key. Does not add the rsv entity to the database.
	 *
	 * @param rsvEntryId the primary key for the new rsv entity
	 * @return the new rsv entity
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity createRSVEntity(
		long rsvEntryId) {

		return _rsvEntityLocalService.createRSVEntity(rsvEntryId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel deletePersistedModel(
			com.liferay.portal.kernel.model.PersistedModel persistedModel)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _rsvEntityLocalService.deletePersistedModel(persistedModel);
	}

	/**
	 * Deletes the rsv entity with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity that was removed
	 * @throws PortalException if a rsv entity with the primary key could not be found
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity deleteRSVEntity(
			long rsvEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _rsvEntityLocalService.deleteRSVEntity(rsvEntryId);
	}

	/**
	 * Deletes the rsv entity from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was removed
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity deleteRSVEntity(
		com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return _rsvEntityLocalService.deleteRSVEntity(rsvEntity);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery() {
		return _rsvEntityLocalService.dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _rsvEntityLocalService.dynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.revert.schema.version.model.impl.RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end) {

		return _rsvEntityLocalService.dynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.revert.schema.version.model.impl.RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end,
		com.liferay.portal.kernel.util.OrderByComparator<T> orderByComparator) {

		return _rsvEntityLocalService.dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _rsvEntityLocalService.dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return _rsvEntityLocalService.dynamicQueryCount(
			dynamicQuery, projection);
	}

	@Override
	public com.liferay.revert.schema.version.model.RSVEntity fetchRSVEntity(
		long rsvEntryId) {

		return _rsvEntityLocalService.fetchRSVEntity(rsvEntryId);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return _rsvEntityLocalService.getActionableDynamicQuery();
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		return _rsvEntityLocalService.getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _rsvEntityLocalService.getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel getPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _rsvEntityLocalService.getPersistedModel(primaryKeyObj);
	}

	/**
	 * Returns a range of all the rsv entities.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.revert.schema.version.model.impl.RSVEntityModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of rsv entities
	 * @param end the upper bound of the range of rsv entities (not inclusive)
	 * @return the range of rsv entities
	 */
	@Override
	public java.util.List<com.liferay.revert.schema.version.model.RSVEntity>
		getRSVEntities(int start, int end) {

		return _rsvEntityLocalService.getRSVEntities(start, end);
	}

	/**
	 * Returns the number of rsv entities.
	 *
	 * @return the number of rsv entities
	 */
	@Override
	public int getRSVEntitiesCount() {
		return _rsvEntityLocalService.getRSVEntitiesCount();
	}

	/**
	 * Returns the rsv entity with the primary key.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity
	 * @throws PortalException if a rsv entity with the primary key could not be found
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity getRSVEntity(
			long rsvEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _rsvEntityLocalService.getRSVEntity(rsvEntryId);
	}

	/**
	 * Updates the rsv entity in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was updated
	 */
	@Override
	public com.liferay.revert.schema.version.model.RSVEntity updateRSVEntity(
		com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return _rsvEntityLocalService.updateRSVEntity(rsvEntity);
	}

	@Override
	public RSVEntityLocalService getWrappedService() {
		return _rsvEntityLocalService;
	}

	@Override
	public void setWrappedService(RSVEntityLocalService rsvEntityLocalService) {
		_rsvEntityLocalService = rsvEntityLocalService;
	}

	private RSVEntityLocalService _rsvEntityLocalService;

}