package com.liferay.portal.db.remover;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DuplicateRemover {

	void removeDuplicates(String tableName, String indexesSQL);

	Map<Long, List<HashMap<String, String>>> getDuplicates(
		String tableName, String indexesSQL);

}