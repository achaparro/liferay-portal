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

package com.liferay.portal.kernel.messaging;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;

import java.util.List;

/**
 * @author Alberto Chaparro
 */
public abstract class BaseMessageListenerCompanyScope
	extends BaseMessageListener {

	@Override
	protected void doReceive(Message message) throws Exception {
		List<Company> companies = CompanyLocalServiceUtil.getCompanies(false);

		for (Company company : companies) {
			if (!company.isActive()) {
				continue;
			}

			long companyId = CompanyThreadLocal.getCompanyId();

			try {
				CompanyThreadLocal.setCompanyId(company.getCompanyId());

				doReceive(message, company.getCompanyId());
			}
			finally {
				CompanyThreadLocal.setCompanyId(companyId);
			}
		}
	}

	protected abstract void doReceive(Message message, long companyId)
		throws Exception;

}