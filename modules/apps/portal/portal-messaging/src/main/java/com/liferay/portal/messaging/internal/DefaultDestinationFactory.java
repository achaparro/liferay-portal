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
import com.liferay.portal.kernel.messaging.DestinationFactory;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.UserLocalService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 */
@Component(immediate = true, service = DestinationFactory.class)
public class DefaultDestinationFactory implements DestinationFactory {

	@Override
	public Destination createDestination(
		DestinationConfiguration destinationConfiguration) {

		String type = destinationConfiguration.getDestinationType();

		if (!_destinationTypes.contains(type)) {
			throw new IllegalArgumentException(
				"No destination prototype configured for " + type);
		}

		BaseDestination baseDestination = null;

		if (type.equals(DestinationConfiguration.DESTINATION_TYPE_PARALLEL)) {
			baseDestination = new ParallelDestination();
		}
		else if (type.equals(
					DestinationConfiguration.DESTINATION_TYPE_SERIAL)) {

			baseDestination = new SerialDestination();
		}
		else {
			baseDestination = new SynchronousDestination();
		}

		baseDestination.setDestinationType(type);
		baseDestination.setName(destinationConfiguration.getDestinationName());
		baseDestination.setMaximumQueueSize(
			destinationConfiguration.getMaximumQueueSize());
		baseDestination.setPermissionCheckerFactory(_permissionCheckerFactory);
		baseDestination.setPortalExecutorManager(_portalExecutorManager);
		baseDestination.setRejectedExecutionHandler(
			destinationConfiguration.getRejectedExecutionHandler());
		baseDestination.setUserLocalService(_userLocalService);

		if (!type.equals(DestinationConfiguration.DESTINATION_TYPE_SERIAL)) {
			baseDestination.setWorkersCoreSize(
				destinationConfiguration.getWorkersCoreSize());
			baseDestination.setWorkersMaxSize(
				destinationConfiguration.getWorkersMaxSize());
		}

		baseDestination.afterPropertiesSet();

		return baseDestination;
	}

	@Override
	public Collection<String> getDestinationTypes() {
		return Collections.unmodifiableCollection(_destinationTypes);
	}

	@Activate
	protected void activate() {
		_destinationTypes.add(
			DestinationConfiguration.DESTINATION_TYPE_PARALLEL);
		_destinationTypes.add(DestinationConfiguration.DESTINATION_TYPE_SERIAL);
		_destinationTypes.add(
			DestinationConfiguration.DESTINATION_TYPE_SYNCHRONOUS);
	}

	private final Set<String> _destinationTypes = new HashSet<>();

	@Reference
	private PermissionCheckerFactory _permissionCheckerFactory;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

	@Reference
	private UserLocalService _userLocalService;

}