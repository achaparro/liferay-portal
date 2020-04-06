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

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used by SOAP remote services.
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
public class RSVEntitySoap implements Serializable {

	public static RSVEntitySoap toSoapModel(RSVEntity model) {
		RSVEntitySoap soapModel = new RSVEntitySoap();

		soapModel.setMvccVersion(model.getMvccVersion());
		soapModel.setRsvEntryId(model.getRsvEntryId());
		soapModel.setCompanyId(model.getCompanyId());

		return soapModel;
	}

	public static RSVEntitySoap[] toSoapModels(RSVEntity[] models) {
		RSVEntitySoap[] soapModels = new RSVEntitySoap[models.length];

		for (int i = 0; i < models.length; i++) {
			soapModels[i] = toSoapModel(models[i]);
		}

		return soapModels;
	}

	public static RSVEntitySoap[][] toSoapModels(RSVEntity[][] models) {
		RSVEntitySoap[][] soapModels = null;

		if (models.length > 0) {
			soapModels = new RSVEntitySoap[models.length][models[0].length];
		}
		else {
			soapModels = new RSVEntitySoap[0][0];
		}

		for (int i = 0; i < models.length; i++) {
			soapModels[i] = toSoapModels(models[i]);
		}

		return soapModels;
	}

	public static RSVEntitySoap[] toSoapModels(List<RSVEntity> models) {
		List<RSVEntitySoap> soapModels = new ArrayList<RSVEntitySoap>(
			models.size());

		for (RSVEntity model : models) {
			soapModels.add(toSoapModel(model));
		}

		return soapModels.toArray(new RSVEntitySoap[soapModels.size()]);
	}

	public RSVEntitySoap() {
	}

	public long getPrimaryKey() {
		return _rsvEntryId;
	}

	public void setPrimaryKey(long pk) {
		setRsvEntryId(pk);
	}

	public long getMvccVersion() {
		return _mvccVersion;
	}

	public void setMvccVersion(long mvccVersion) {
		_mvccVersion = mvccVersion;
	}

	public long getRsvEntryId() {
		return _rsvEntryId;
	}

	public void setRsvEntryId(long rsvEntryId) {
		_rsvEntryId = rsvEntryId;
	}

	public long getCompanyId() {
		return _companyId;
	}

	public void setCompanyId(long companyId) {
		_companyId = companyId;
	}

	private long _mvccVersion;
	private long _rsvEntryId;
	private long _companyId;

}