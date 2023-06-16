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

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.model.ShardedModel;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;

import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.spi.FlushEntityEvent;

/**
 * @author Alberto Chaparro
 */
public class CompanySynchronizerFlushEntityEventListener
	extends DefaultFlushEntityEventListener {

	public static final CompanySynchronizerFlushEntityEventListener INSTANCE =
		new CompanySynchronizerFlushEntityEventListener();

	@Override
	public void onFlushEntity(FlushEntityEvent flushEntityEvent)
		throws HibernateException {

		Object entity = flushEntityEvent.getEntity();

		if (entity instanceof ShardedModel) {
			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.
						setInitializingCompanyIdWithSafeCloseable(
							((ShardedModel)entity).getCompanyId())) {

				super.onFlushEntity(flushEntityEvent);
			}
		}
		else {
			super.onFlushEntity(flushEntityEvent);
		}
	}

}