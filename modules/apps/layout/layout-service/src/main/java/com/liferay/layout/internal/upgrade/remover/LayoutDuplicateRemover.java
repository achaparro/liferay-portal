package com.liferay.layout.internal.upgrade.remover;

import com.liferay.portal.db.remover.DuplicateRemover;
import com.liferay.portal.db.remover.PortalDuplicateRemover;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.persistence.LayoutPersistence;
import com.liferay.portal.kernel.util.StringBundler;

import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(property = "service.tables=portal", service = DuplicateRemover.class)
public class LayoutDuplicateRemover extends PortalDuplicateRemover {

	@Override
	public void removeDuplicates(String tableName, String indexesSQL) {
		StringBundler sb = new StringBundler(0);

		// Build String to collect duplicates

		String sql = sb.toString();

		Session session = null;

		try {
			session = _layoutPersistence.getCurrentSession();

			Query query = session.createQuery(sql);

			List<Layout> list = query.list();

			if (list.size() > 1) {
				Collections.sort(list, Collections.reverseOrder());

				// Delete Entries

			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Reference
	private LayoutPersistence _layoutPersistence;

}