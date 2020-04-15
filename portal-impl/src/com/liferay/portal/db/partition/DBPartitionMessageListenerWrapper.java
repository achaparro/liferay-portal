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

package com.liferay.portal.db.partition;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.scheduler.SchedulerEngine;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;

import java.util.List;

/**
 * @author Alberto Chaparro
 */
public class DBPartitionMessageListenerWrapper implements MessageListener {

	public DBPartitionMessageListenerWrapper(MessageListener messageListener) {
		_messageListener = messageListener;
	}

	@Override
	public void receive(Message message) throws MessageListenerException {
		long companyId = message.getLong("companyId");

		if (companyId != CompanyConstants.SYSTEM) {
			_messageListener.receive(message);

			return;
		}

		List<Company> companies = CompanyLocalServiceUtil.getCompanies(false);

		for (Company company : companies) {
			if (!company.isActive()) {
				continue;
			}

			companyId = CompanyThreadLocal.getCompanyId();

			try {
				CompanyThreadLocal.setCompanyId(company.getCompanyId());

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Executing ",
							message.get(SchedulerEngine.GROUP_NAME),
							" for company ", company.getCompanyId()));
				}

				_messageListener.receive(message);
			}
			finally {
				CompanyThreadLocal.setCompanyId(companyId);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DBPartitionMessageListenerWrapper.class);

	private final MessageListener _messageListener;

}