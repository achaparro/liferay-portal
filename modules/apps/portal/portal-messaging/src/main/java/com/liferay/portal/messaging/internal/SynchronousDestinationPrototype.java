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

package com.liferay.portal.messaging.internal;

import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.portal.kernel.messaging.Destination;
import com.liferay.portal.kernel.messaging.DestinationConfiguration;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.UserLocalService;

/**
 * @author Michael C. Han
 */
public class SynchronousDestinationPrototype implements DestinationPrototype {

	public SynchronousDestinationPrototype(
		PortalExecutorManager portalExecutorManager,
		PermissionCheckerFactory permissionCheckerFactory,
		UserLocalService userLocalService) {

		_portalExecutorManager = portalExecutorManager;
		_permissionCheckerFactory = permissionCheckerFactory;
		_userLocalService = userLocalService;
	}

	@Override
	public Destination createDestination(
		DestinationConfiguration destinationConfiguration) {

		SynchronousDestination synchronousDestination =
			new SynchronousDestination();

		synchronousDestination.setDestinationType(
			destinationConfiguration.getDestinationType());
		synchronousDestination.setName(
			destinationConfiguration.getDestinationName());
		synchronousDestination.setMaximumQueueSize(
			destinationConfiguration.getMaximumQueueSize());
		synchronousDestination.setPermissionCheckerFactory(
			_permissionCheckerFactory);
		synchronousDestination.setPortalExecutorManager(_portalExecutorManager);
		synchronousDestination.setRejectedExecutionHandler(
			destinationConfiguration.getRejectedExecutionHandler());
		synchronousDestination.setUserLocalService(_userLocalService);
		synchronousDestination.setWorkersCoreSize(
			destinationConfiguration.getWorkersCoreSize());
		synchronousDestination.setWorkersMaxSize(
			destinationConfiguration.getWorkersMaxSize());

		synchronousDestination.afterPropertiesSet();

		return synchronousDestination;
	}

	private final PermissionCheckerFactory _permissionCheckerFactory;
	private final PortalExecutorManager _portalExecutorManager;
	private final UserLocalService _userLocalService;

}