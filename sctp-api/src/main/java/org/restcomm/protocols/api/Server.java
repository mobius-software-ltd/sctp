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

import java.util.Collection;

/**
 * A wrapper over actual server side Socket
 * 
 * @author amit bhayani
 * @author yulianoifa
 * 
 */

public interface Server {

	/**
	 * Get the Server channel type - TCP or SCTP
	 * 
	 * @return
	 */
	public IpChannelType getIpChannelType();

	/**
	 * Return if this Server accepts Anonymous connections 
	 * 
	 * @return
	 */
	public boolean isAcceptAnonymousConnections();

	/**
	 * Return the count of concurrent connections that can accept a Server. 0 means an unlimited count.  
	 * 
	 * @return
	 */
	public int getMaxConcurrentConnectionsCount();

	/**
	 * Set the count of concurrent connections that can accept a Server. 0 means an unlimited count.  
	 * 
	 * @return
	 */
	public void setMaxConcurrentConnectionsCount(int val);

	/**
	 * Get name of this Server. Should be unique in a management instance
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * The host address that this server socket is bound to
	 * 
	 * @return
	 */
	public String getHostAddress();

	/**
	 * The host port that this server socket is bound to
	 * 
	 * @return
	 */
	public int getHostport();
	
	/**
	 * When SCTP multi-homing configuration extra IP addresses are here
	 * 
	 * @return
	 */
	public String[] getExtraHostAddresses();

	/**
	 * If the server is started
	 * 
	 * @return
	 */
	public boolean isStarted();

	/**
	 * {@link Association} to add for for this Server
	 * 
	 * @return
	 */
	public void addAssociations(String association);

	/**
	 * {@link Association} to remove for for this Server
	 * 
	 * @return
	 */
	public void removeAssociations(String association);

	/**
	 * {@link Association} configured for this Server
	 * Anonymous associations are not present in this list
	 * 
	 * @return
	 */
	public Collection<String> getAssociations();

	/**
	 * Returns an unmodifiable list of anonymous associations that are connected at the moment 
	 * @return
	 */
	public Collection<Association> getAnonymAssociations();

}
