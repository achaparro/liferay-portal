package com.liferay.portal.upgrade.internal.remover;

import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.SQLException;
import java.util.HashMap;

import java.util.List;
import java.util.Map;


import com.liferay.portal.kernel.service.TicketLocalServiceUtil;
import org.osgi.service.component.annotations.Component;

@Component(property = "service.tables=Ticket", service = DuplicateRemover.class)
public class TicketDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {

		Map<String, String> indexesColumnsMap = getIndexesColumnsList(
			indexesSQL);

		for (Map.Entry<String, String> indexSet : indexesColumnsMap.entrySet()) {
			String index = indexSet.getKey();

			String columns = indexSet.getValue();

			try {
				List<HashMap<String, String>> duplicatesList =
					getDuplicatesSQL(columns, tableName,_TICKET_SELECT_CLAUSE_, _TICKET_ORDER_BY_);

				int duplicateCount = duplicatesList.size();

				for (HashMap<String, String> duplicate : duplicatesList) {
					long classPK = Long.parseLong(duplicate.get("classPK"));

					if(duplicateCount > 1) {
						break;
					}

					try {
						TicketLocalServiceUtil.deleteTicket(classPK);
					}
					catch (PortalException e) {
						throw new RuntimeException(e);
					}
					finally {
						_log.warn("Entry " + classPK + " deleted for Index " + index);

						duplicateCount--;

					}
				}
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private static final Log _log =
		LogFactoryUtil.getLog(TicketDuplicateRemover.class);

	private static final String _TICKET_SELECT_CLAUSE_ = "classPK, createDate";

	private static final String _TICKET_ORDER_BY_ = "createDate DESC";

}


