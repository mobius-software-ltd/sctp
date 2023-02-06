package org.restcomm.protocols.api;
/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * Mobius Software LTD , Copyright 2022-2023
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * @author yulianoifa
 * 
 */
public enum IpChannelType {
	SCTP(0, "SCTP"), TCP(1, "TCP");

	int code;
	String type;

	private IpChannelType(int code, String type) {
		this.code = code;
		this.type = type;

	}

	public int getCode() {
		return this.code;
	}

	public String getType() {
		return type;
	}

	public static IpChannelType getInstance(int code) {
		switch (code) {
		case 0:
			return IpChannelType.SCTP;
		case 1:
			return IpChannelType.TCP;
		}

		return null;
	}

	public static IpChannelType getInstance(String type) {
		if (type.equalsIgnoreCase("SCTP")) {
			return SCTP;
		} else if (type.equalsIgnoreCase("TCP")) {
			return TCP;
		}
		
		return null;
	}
}
