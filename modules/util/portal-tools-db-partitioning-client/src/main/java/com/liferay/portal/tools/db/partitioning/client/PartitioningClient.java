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

package com.liferay.portal.tools.db.partitioning.client;

import com.mysql.cj.jdbc.Driver;

import java.io.IOException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jline.console.ConsoleReader;

/**
 * @author Alberto Chaparro
 */
public class PartitioningClient {

	public static void main(String[] args) {
		try {
			System.out.println(
				"Tool to enable data physical partitioning for your MySQL " +
					"Liferay database. Please, shutdown your Liferay server " +
						"and make a database backup first");

			ConsoleReader consoleReader = new ConsoleReader();

			System.out.println("Please enter your database url connection: ");

			String url = consoleReader.readLine();

			if (!url.contains("mysql")) {
				System.err.println("Enter a valid MySQL url");

				return;
			}

			System.out.println("Please enter your database username: ");

			String userName = consoleReader.readLine();

			System.out.println("Please enter your database password: ");

			String password = consoleReader.readLine('*');

			_initConnection(url, userName, password);

			_initDatabaseVariables();

			//validateTablesAlreadyPartitioned

			_validateCompanyIds();

			_getPartitionsInfo(consoleReader);

			_addCompanyIdToUniqueIndexes();

			_addPartitions();

			_printNonpartitionedTables();
		}
		catch (Exception e) {
			System.err.println("Error running the data partitioning tool");

			e.printStackTrace();
		}

		System.out.println("The process has been executed successfully");
	}

	private static void _addCompanyIdToUniqueIndex(
			String tableName, String indexName, List<String> columnNames)
		throws SQLException {

		String dropIndexClause = null;
		String createIndexClause = null;

		if (indexName.equals("PRIMARY")) {
			dropIndexClause = "ALTER TABLE " + tableName + " DROP PRIMARY KEY";
			createIndexClause =
				"ALTER TABLE " + tableName + " ADD PRIMARY KEY(";
		}
		else {
			dropIndexClause = "drop index " + indexName + " on " + tableName;
			createIndexClause =
				"create index " + indexName + " on " + tableName + "(";
		}

		for (String columnName : columnNames) {
			createIndexClause += columnName + ", ";
		}

		createIndexClause += "companyId)";

		try (PreparedStatement ps1 = _connection.prepareStatement(
				dropIndexClause);
			PreparedStatement ps2 = _connection.prepareStatement(
				createIndexClause)) {

			ps1.executeUpdate();
			ps2.executeUpdate();

			System.out.println(
				"Index " + tableName + "." + indexName +
					" now contains the companyId field");
		}
	}

	private static void _addCompanyIdToUniqueIndexes() throws SQLException {
		System.out.println("Adding companyId field to unique indexes");

		for (String tableName : _companyIdTableNames) {
			try (ResultSet rs = _metadata.getIndexInfo(
					_connection.getCatalog(), null, tableName, true, false)) {

				List<String> columnNames = new ArrayList<>();

				String indexName = null;

				while (rs.next()) {
					if (indexName == null) {
						indexName = rs.getString("INDEX_NAME");
					}
					else if (!indexName.equals(rs.getString("INDEX_NAME"))) {
						if (columnNames.indexOf("companyId") == -1) {
							_addCompanyIdToUniqueIndex(
								tableName, indexName, columnNames);
						}

						indexName = rs.getString("INDEX_NAME");

						columnNames = new ArrayList();
					}

					columnNames.add(rs.getString("COLUMN_NAME"));
				}

				if (columnNames.indexOf("companyId") == -1) {
					_addCompanyIdToUniqueIndex(
						tableName, indexName, columnNames);
				}
			}
		}
	}

	private static void _addPartitions() throws SQLException {
		System.out.println("Configuring Physical partitioning for every table");

		String addPartitionClause;

		for (String tableName : _companyIdTableNames) {
			addPartitionClause =
				"ALTER TABLE " + tableName + " PARTITION BY LIST(companyId) (";

			for (Partition partition : _partitions) {
				addPartitionClause +=
					"PARTITION " + partition.getName() + " VALUES IN (" +
						partition.getCompanyId() + ")";

				String dataDirectory = partition.getDataDirectory();

				if (!dataDirectory.equals("")) {
					addPartitionClause +=
						" DATA DIRECTORY = '" + dataDirectory + "'";
				}

				String indexDirectory = partition.getIndexDirectory();

				if (!indexDirectory.equals("")) {
					addPartitionClause +=
						" INDEX DIRECTORY = '" + indexDirectory + "'";
				}

				addPartitionClause += ", ";
			}

			addPartitionClause += "PARTITION global VALUES IN (0, null))";

			try (PreparedStatement ps = _connection.prepareStatement(
					addPartitionClause)) {

				ps.executeUpdate();
			}

			System.out.println(tableName + " has been partitioned");
		}
	}

	private static void _getPartitionsInfo(ConsoleReader consoleReader)
		throws IOException {

		for (Company company : _companies) {
			System.out.println(
				"Please enter the information for the company " +
					company.getWebId() + " with companyId " +
						company.getCompanyId());

			System.out.println("Please enter the partition name: ");

			String partitionName = consoleReader.readLine();

			System.out.println(
				"Please enter the partition data directory[optional]: ");

			String partitionDataDirectory = consoleReader.readLine();

			System.out.println(
				"Please enter the partition index directory[optional]: ");

			String partitionIndexDirectory = consoleReader.readLine();

			_partitions.add(
				new Partition(
					company.getCompanyId(), partitionName,
					partitionDataDirectory, partitionIndexDirectory));
		}
	}

	private static boolean _hasColumn(
			Connection con, DatabaseMetaData metadata, String tableName,
			String columnName)
		throws SQLException {

		try (ResultSet rs = metadata.getColumns(
				con.getCatalog(), null, tableName, columnName)) {

			if (!rs.next()) {
				return false;
			}
		}

		return true;
	}

	private static void _initCompanies() throws Exception {
		try (PreparedStatement ps = _connection.prepareStatement(
				"select companyId, webId from Company");
			ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				_companies.add(new Company(rs.getLong(1), rs.getString(2)));
			}
		}

		if (_companies.size() == 0) {
			throw new Exception("Company table has not been initialized");
		}
	}

	private static void _initConnection(
			String url, String userName, String password)
		throws SQLException {

		DriverManager.registerDriver(new Driver());

		_connection = DriverManager.getConnection(url, userName, password);
	}

	private static void _initDatabaseVariables() throws Exception {
		_metadata = _connection.getMetaData();

		_initCompanies();

		_initTableNames();
	}

	private static void _initTableNames() throws SQLException {
		try (ResultSet rs = _metadata.getTables(
				_connection.getCatalog(), null, "%", null)) {

			while (rs.next()) {
				String tableType = rs.getString("TABLE_TYPE");

				if (!Objects.equals("TABLE", tableType)) {
					continue;
				}

				String tableName = rs.getString("TABLE_NAME");

				if (!tableName.equals("Company") && _hasColumn(
						_connection, _metadata, tableName, "companyId")) {

					_companyIdTableNames.add(tableName);
				}
				else {
					_nonCompanyIdTableNames.add(tableName);
				}
			}
		}
	}

	private static void _printNonpartitionedTables() {
		System.out.println(
			"The following tables are global and can not be partitioned: ");

		for (String tableName : _nonCompanyIdTableNames) {
			System.out.println(tableName);
		}
	}

	private static void _validateCompanyIds() throws Exception {
		boolean incorrectCompanyIds = false;

		String companyIdsClause = "(";

		for (Company company : _companies) {
			companyIdsClause += company.getCompanyId() + ",";
		}

		companyIdsClause += "0, null)";

		for (String tableName : _companyIdTableNames) {
			try (PreparedStatement ps = _connection.prepareStatement(
					"select DISTINCT(companyId) from " + tableName + " where " +
						"companyId not in " + companyIdsClause);
				ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					System.err.println(
						"Table " + tableName + " has records with the " +
							"invalid companydId " + rs.getLong(1));

					incorrectCompanyIds = true;
				}
			}
		}

		if (incorrectCompanyIds) {
			throw new Exception(
				"Several tables contain records associated to a non-existent " +
					"companyId. Please fix it before partitioning.");
		}
	}

	private static final List<Company> _companies = new ArrayList<>();
	private static final List<String> _companyIdTableNames = new ArrayList<>();
	private static Connection _connection;
	private static DatabaseMetaData _metadata;
	private static final List<String> _nonCompanyIdTableNames =
		new ArrayList<>();
	private static final List<Partition> _partitions = new ArrayList<>();

	private static class Company {

		public Company(Long companyId, String webId) {
			_companyId = companyId;
			_webId = webId;
		}

		public Long getCompanyId() {
			return _companyId;
		}

		public String getWebId() {
			return _webId;
		}

		public void setCompanyId(Long companyId) {
			_companyId = companyId;
		}

		public void setWebId(String webId) {
			_webId = webId;
		}

		private Long _companyId;
		private String _webId;

	}

	private static class Partition {

		public Partition(Long companyId, String name) {
			_companyId = companyId;
			_name = name;
		}

		public Partition(
			Long companyId, String name, String dataDirectory,
			String indexDirectory) {

			_companyId = companyId;
			_name = name;
			_dataDirectory = dataDirectory;
			_indexDirectory = indexDirectory;
		}

		public Long getCompanyId() {
			return _companyId;
		}

		public String getDataDirectory() {
			return _dataDirectory;
		}

		public String getIndexDirectory() {
			return _indexDirectory;
		}

		public String getName() {
			return _name;
		}

		public void setCompanyId(Long companyId) {
			_companyId = companyId;
		}

		public void setDataDirectory(String dataDirectory) {
			_dataDirectory = dataDirectory;
		}

		public void setIndexDirectory(String indexDirectory) {
			_indexDirectory = indexDirectory;
		}

		public void setName(String name) {
			_name = name;
		}

		private Long _companyId;
		private String _dataDirectory;
		private String _indexDirectory;
		private String _name;

	}

}