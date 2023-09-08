/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.exportimport.internal.messaging;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Levente Hudák
 */
public abstract class BasePublisherMessageListener implements MessageListener {

	protected void initThreadLocals(
			long userId, Map<String, String[]> parameterMap)
		throws PortalException {

		User user = UserLocalServiceUtil.getUserById(userId);

		// @WebIdRouting: if we get the user, we should be already in a company
		// This seems could not work in DB Partitioning since it could not find
		// The modification is really old, maybe it is working because this call
		// when executing a job and we set the CompanyThreadLocal accordingly
		// for DB Partitioning in that case

		CompanyThreadLocal.setCompanyId(user.getCompanyId());

		PrincipalThreadLocal.setName(userId);

		PermissionChecker permissionChecker = null;

		try {
			permissionChecker = PermissionCheckerFactoryUtil.create(user);
		}
		catch (Exception exception) {
			throw new SystemException(
				"Unable to initialize thread locals because an error occured " +
					"when creating a permission checker for user " + userId,
				exception);
		}

		PermissionThreadLocal.setPermissionChecker(permissionChecker);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(user.getCompanyId());
		serviceContext.setPathMain(PortalUtil.getPathMain());
		serviceContext.setSignedIn(!user.isGuestUser());
		serviceContext.setUserId(user.getUserId());

		Map<String, Serializable> attributes = new HashMap<>();

		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();

			if (ArrayUtil.isEmpty(values)) {
				continue;
			}

			String param = entry.getKey();

			if (values.length == 1) {
				attributes.put(param, values[0]);
			}
			else {
				attributes.put(param, values);
			}
		}

		serviceContext.setAttributes(attributes);

		ServiceContextThreadLocal.pushServiceContext(serviceContext);
	}

	protected void resetThreadLocals() {
		CompanyThreadLocal.setCompanyId(CompanyConstants.SYSTEM);
		PermissionThreadLocal.setPermissionChecker(null);
		PrincipalThreadLocal.setName(null);
		ServiceContextThreadLocal.popServiceContext();
	}

}