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

package com.liferay.portal.dao.orm.hibernate.event;

import com.liferay.portal.kernel.model.ShardedModel;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;

import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;

/**
 * @author Alberto Chaparro
 */
public class CompanySynchronizerFlushEntityEventListener
	implements FlushEntityEventListener {

	public static final CompanySynchronizerFlushEntityEventListener INSTANCE =
		new CompanySynchronizerFlushEntityEventListener();

	@Override
	public void onFlushEntity(FlushEntityEvent flushEntityEvent)
		throws HibernateException {

		Object entity = flushEntityEvent.getEntity();

		if (entity instanceof ShardedModel) {
			long companyId = ((ShardedModel)entity).getCompanyId();

			if (!Objects.equals(CompanyThreadLocal.getCompanyId(), companyId)) {
				CompanyThreadLocal.setCompanyId(companyId);
			}
		}
	}

}