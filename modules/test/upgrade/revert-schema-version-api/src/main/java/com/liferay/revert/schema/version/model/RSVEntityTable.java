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

package com.liferay.revert.schema.version.model;

import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.base.BaseTable;

import java.sql.Types;

/**
 * The table class for the &quot;RSVEntity&quot; database table.
 *
 * @author Brian Wing Shun Chan
 * @see RSVEntity
 * @generated
 */
public class RSVEntityTable extends BaseTable<RSVEntityTable> {

	public static final RSVEntityTable INSTANCE = new RSVEntityTable();

	public final Column<RSVEntityTable, Long> mvccVersion = createColumn(
		"mvccVersion", Long.class, Types.BIGINT, Column.FLAG_NULLITY);
	public final Column<RSVEntityTable, Long> rsvEntryId = createColumn(
		"rsvEntryId", Long.class, Types.BIGINT, Column.FLAG_PRIMARY);
	public final Column<RSVEntityTable, Long> companyId = createColumn(
		"companyId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);

	private RSVEntityTable() {
		super("RSVEntity", RSVEntityTable::new);
	}

}