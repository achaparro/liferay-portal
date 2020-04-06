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

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides the local service utility for RSVEntity. This utility wraps
 * <code>com.liferay.revert.schema.version.service.impl.RSVEntityLocalServiceImpl</code> and
 * is an access point for service operations in application layer code running
 * on the local server. Methods of this service will not have security checks
 * based on the propagated JAAS credentials because this service can only be
 * accessed from within the same VM.
 *
 * @author Brian Wing Shun Chan
 * @see RSVEntityLocalService
 * @generated
 */
public class RSVEntityLocalServiceUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to <code>com.liferay.revert.schema.version.service.impl.RSVEntityLocalServiceImpl</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * Adds the rsv entity to the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was added
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
		addRSVEntity(
			com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return getService().addRSVEntity(rsvEntity);
	}

	/**
	 * @throws PortalException
	 */
	public static com.liferay.portal.kernel.model.PersistedModel
			createPersistedModel(java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return getService().createPersistedModel(primaryKeyObj);
	}

	/**
	 * Creates a new rsv entity with the primary key. Does not add the rsv entity to the database.
	 *
	 * @param rsvEntryId the primary key for the new rsv entity
	 * @return the new rsv entity
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
		createRSVEntity(long rsvEntryId) {

		return getService().createRSVEntity(rsvEntryId);
	}

	/**
	 * @throws PortalException
	 */
	public static com.liferay.portal.kernel.model.PersistedModel
			deletePersistedModel(
				com.liferay.portal.kernel.model.PersistedModel persistedModel)
		throws com.liferay.portal.kernel.exception.PortalException {

		return getService().deletePersistedModel(persistedModel);
	}

	/**
	 * Deletes the rsv entity with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity that was removed
	 * @throws PortalException if a rsv entity with the primary key could not be found
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
			deleteRSVEntity(long rsvEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return getService().deleteRSVEntity(rsvEntryId);
	}

	/**
	 * Deletes the rsv entity from the database. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was removed
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
		deleteRSVEntity(
			com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return getService().deleteRSVEntity(rsvEntity);
	}

	public static com.liferay.portal.kernel.dao.orm.DynamicQuery
		dynamicQuery() {

		return getService().dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	public static <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return getService().dynamicQuery(dynamicQuery);
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
	public static <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end) {

		return getService().dynamicQuery(dynamicQuery, start, end);
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
	public static <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end,
		com.liferay.portal.kernel.util.OrderByComparator<T> orderByComparator) {

		return getService().dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return getService().dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return getService().dynamicQueryCount(dynamicQuery, projection);
	}

	public static com.liferay.revert.schema.version.model.RSVEntity
		fetchRSVEntity(long rsvEntryId) {

		return getService().fetchRSVEntity(rsvEntryId);
	}

	public static com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return getService().getActionableDynamicQuery();
	}

	public static
		com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
			getIndexableActionableDynamicQuery() {

		return getService().getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	public static String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	public static com.liferay.portal.kernel.model.PersistedModel
			getPersistedModel(java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return getService().getPersistedModel(primaryKeyObj);
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
	public static java.util.List
		<com.liferay.revert.schema.version.model.RSVEntity> getRSVEntities(
			int start, int end) {

		return getService().getRSVEntities(start, end);
	}

	/**
	 * Returns the number of rsv entities.
	 *
	 * @return the number of rsv entities
	 */
	public static int getRSVEntitiesCount() {
		return getService().getRSVEntitiesCount();
	}

	/**
	 * Returns the rsv entity with the primary key.
	 *
	 * @param rsvEntryId the primary key of the rsv entity
	 * @return the rsv entity
	 * @throws PortalException if a rsv entity with the primary key could not be found
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
			getRSVEntity(long rsvEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return getService().getRSVEntity(rsvEntryId);
	}

	/**
	 * Updates the rsv entity in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * @param rsvEntity the rsv entity
	 * @return the rsv entity that was updated
	 */
	public static com.liferay.revert.schema.version.model.RSVEntity
		updateRSVEntity(
			com.liferay.revert.schema.version.model.RSVEntity rsvEntity) {

		return getService().updateRSVEntity(rsvEntity);
	}

	public static RSVEntityLocalService getService() {
		return _serviceTracker.getService();
	}

	private static ServiceTracker<RSVEntityLocalService, RSVEntityLocalService>
		_serviceTracker;

	static {
		Bundle bundle = FrameworkUtil.getBundle(RSVEntityLocalService.class);

		ServiceTracker<RSVEntityLocalService, RSVEntityLocalService>
			serviceTracker =
				new ServiceTracker
					<RSVEntityLocalService, RSVEntityLocalService>(
						bundle.getBundleContext(), RSVEntityLocalService.class,
						null);

		serviceTracker.open();

		_serviceTracker = serviceTracker;
	}

}