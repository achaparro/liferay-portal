package com.liferay.portal.service.remover;

import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Ticket;
import com.liferay.portal.kernel.service.TicketLocalServiceUtil;
import com.liferay.portal.kernel.service.persistence.TicketPersistence;
import com.liferay.portal.kernel.util.StringBundler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(property = "service.tables=Ticket", service = DuplicateRemover.class)
public class TicketDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {
		Map<String, String> indexesColumnsMap = getIndexesColumnsList(indexesSQL);

		for(Map.Entry<String, String> entry : indexesColumnsMap.entrySet()) {
			List<Ticket> ticketList = getDuplicates(entry.getKey());
			Iterator<Ticket> iterator = ticketList.iterator();
			while(iterator.hasNext()) {

				Ticket ticket = iterator.next();

				if(!iterator.hasNext()){
					break;
				}

				try{
					TicketLocalServiceUtil.deleteTicket(ticket);
				}
				catch(Exception exception){
					throw new RuntimeException(exception);
				}
				finally {
					if (_log.isWarnEnabled()) {
					_log.warn("Duplicate Ticket removed: " + ticket.getClassPK() + " Index: " + entry.getValue());
					}
				}

			}
		}

	}

	protected List<Ticket> getDuplicates(String indexSQL) {
		StringBundler sb = new StringBundler();

		String[] columns = indexSQL.split(", ");

		sb.append("SELECT t1 FROM Ticket t1 WHERE (SELECT COUNT(*) FROM Ticket t2 WHERE ");

		for (int i = 0; i < columns.length; i++) {
			sb.append("t2.");
			sb.append(columns[i]);
			sb.append(" = ");
			sb.append("t1.");
			sb.append(columns[i]);

			if (i < (columns.length - 1)) {
				sb.append(" AND ");
			}
		}

		sb.append(") > 1 ORDER BY t1.createDate DESC;");

		String sql = sb.toString();

		Session session = _ticketPersistence.getCurrentSession();

		Query query = session.createQuery(sql);

		 List<Ticket> list = query.list();

		session.flush();

		session.close();

		return list;
	}



@Reference
private TicketPersistence _ticketPersistence;

private static final Log _log = LogFactoryUtil.getLog(TicketDuplicateRemover.class);

}


