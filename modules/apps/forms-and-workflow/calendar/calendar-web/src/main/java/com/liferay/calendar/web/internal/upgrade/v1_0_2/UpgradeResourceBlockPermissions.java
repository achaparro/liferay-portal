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

package com.liferay.calendar.web.internal.upgrade.v1_0_2;

import com.liferay.calendar.model.CalendarResource;
import com.liferay.calendar.service.CalendarResourceLocalService;
import com.liferay.portal.kernel.model.ResourceBlock;
import com.liferay.portal.kernel.model.ResourceBlockConstants;
import com.liferay.portal.kernel.model.ResourceBlockPermissionsContainer;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.service.ResourceBlockLocalService;
import com.liferay.portal.kernel.service.ResourceBlockPermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author José María Muñoz
 */
public class UpgradeResourceBlockPermissions extends UpgradeProcess {

	public UpgradeResourceBlockPermissions(
		CalendarResourceLocalService calendarResourceLocalService,
		ResourceBlockPermissionLocalService
			resourceBlockPermissionLocalService,
		RoleLocalService roleLocalService) {

		_calendarResourceLocalService = calendarResourceLocalService;
		_resourceBlockPermissionLocalService =
			resourceBlockPermissionLocalService;
		_roleLocalService = roleLocalService;
	}

	public void UpgradeGuestResourceBlockPermissions() throws Exception {
		int contCalendarResource =
			_calendarResourceLocalService.getCalendarResourcesCount();

		List<CalendarResource> calendarResources =
			_calendarResourceLocalService.getCalendarResources(
				0, contCalendarResource);

		for (CalendarResource calendarResource : calendarResources) {
			Role guestRole = _roleLocalService.getRole(
				calendarResource.getCompanyId(), RoleConstants.GUEST);

			_resourceBlockPermissionLocalService.updateResourceBlockPermission(
				calendarResource.getResourceBlockId(), guestRole.getRoleId(), 0,
				ResourceBlockConstants.OPERATOR_SET);
		}
	}

	@Override
	protected void doUpgrade() throws Exception {
		UpgradeGuestResourceBlockPermissions();
	}

	private final CalendarResourceLocalService _calendarResourceLocalService;
	private final ResourceBlockPermissionLocalService
		_resourceBlockPermissionLocalService;
	private final RoleLocalService _roleLocalService;

}