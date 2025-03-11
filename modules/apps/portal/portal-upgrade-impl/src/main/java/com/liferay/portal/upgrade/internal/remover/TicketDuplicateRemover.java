/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.internal.remover;

import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.TicketLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;

import java.sql.SQLException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Jorge Avalos
 */
@Component(property = "service.tables=Ticket", service = DuplicateRemover.class)
public class TicketDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {
		Map<String, String> indexesColumnsMap = getIndexesColumnsList(
			indexesSQL);

		for (Map.Entry<String, String> indexSet :
				indexesColumnsMap.entrySet()) {

			String index = indexSet.getKey();

			String columns = indexSet.getValue();

			try {
				List<HashMap<String, String>> duplicatesList = getDuplicatesSQL(
					columns, tableName, _TICKET_SELECT_CLAUSE_,
					_TICKET_ORDER_BY_);

				int duplicateCount = duplicatesList.size();

				long classPK;

				for (HashMap<String, String> duplicate : duplicatesList) {
					if (duplicateCount <= 1) {
						break;
					}

					classPK = GetterUtil.getLong(duplicate.get("ticketId"));

					try {
						TicketLocalServiceUtil.deleteTicket(classPK);
					}
					catch (PortalException portalException) {
						_log.error(
							"Unable to delete ticket: " + classPK,
							portalException);
					}
					finally {
						logDeletedDuplicates(tableName, index, duplicate);
						duplicateCount--;
					}
				}
			}
			catch (SQLException sqlException) {
				_log.error(sqlException);
			}
		}
	}

	private static final String _TICKET_ORDER_BY_ = "createDate DESC";

	private static final String _TICKET_SELECT_CLAUSE_ = "*";

	private static final Log _log = LogFactoryUtil.getLog(
		TicketDuplicateRemover.class);

}