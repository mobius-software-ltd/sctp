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

package org.restcomm.protocols.sctp.netty;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.protocols.api.Association;
import org.restcomm.protocols.api.AssociationListener;
import org.restcomm.protocols.api.IpChannelType;
import org.restcomm.protocols.api.PayloadData;
import org.restcomm.protocols.sctp.SctpManagementImpl;

import io.netty.buffer.Unpooled;

/**
 * @author nosach kostiantyn
 * @author yulianoifa
 * 
 */
public class ModifyAssociationTest {
	private static final Logger logger = LogManager.getLogger(ModifyAssociationTest.class);

    private static final String SERVER_NAME = "testserver";
	private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT1 = 12354;
    private static final int SERVER_PORT2 = 12355;
    private static final int SERVER_PORT3 = 12356;
    private static final int SERVER_PORT4 = 12357;

	private static final String SERVER_ASSOCIATION_NAME = "serverAsscoiation";
	private static final String CLIENT_ASSOCIATION_NAME = "clientAsscoiation";

	private static final String CLIENT_HOST = "127.0.0.1";
    private static final int CLIENT_PORT1 = 12364;
    private static final int CLIENT_PORT2 = 12365;
    private static final int CLIENT_PORT3 = 12366;
    private static final int CLIENT_PORT4 = 12367;

	private final byte[] CLIENT_MESSAGE = "Client says Hi".getBytes();
	private final byte[] SERVER_MESSAGE = "Server says Hi".getBytes();

	private SctpManagementImpl management = null;
	private boolean isModified = false;

	//private ServerImpl server = null;
	

	private Association serverAssociation = null;
	private Association clientAssociation = null;

	private volatile boolean clientAssocUp = false;
	private volatile boolean serverAssocUp = false;

	private volatile boolean clientAssocDown = false;
	private volatile boolean serverAssocDown = false;

	private byte[] clientMessage;
	private byte[] serverMessage;

	@BeforeClass
	public static void setUpClass() throws Exception {
		Configurator.initialize(new DefaultConfiguration());
    	Configurator.setRootLevel(Level.DEBUG);
    	logger.info("Starting " + ModifyAssociationTest.class.getName());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		logger.info("Stopping " + ModifyAssociationTest.class.getName());
	}

	public void setUp(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {
		this.clientAssocUp = false;
		this.serverAssocUp = false;

		this.clientAssocDown = false;
		this.serverAssocDown = false;

		this.clientMessage = null;
		this.serverMessage = null;

		this.management = new SctpManagementImpl("ClientAssociationTest",1,1,1);
		this.management.start();
        this.management.setConnectDelay(1000);
		this.management.removeAllResourses();

		//this.server = 
		this.management.addServer(SERVER_NAME, SERVER_HOST, serverPort, ipChannelType, false, 0, null);
		this.serverAssociation = this.management.addServerAssociation(CLIENT_HOST, clientPort, SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);
		this.clientAssociation = this.management.addAssociation(CLIENT_HOST, clientPort, SERVER_HOST, serverPort, CLIENT_ASSOCIATION_NAME, ipChannelType, null);
	}

	public void tearDown() throws Exception {
		isModified = false;
		this.management.removeAssociation(CLIENT_ASSOCIATION_NAME);
		this.management.removeAssociation(SERVER_ASSOCIATION_NAME);
		this.management.removeServer(SERVER_NAME);

		this.management.stop();
	}

    @Test
    public void testModifyServerAndAssociationSctp() throws Exception {

        if (SctpTransferTest.checkSctpEnabled())
            this.testModifyServerAndAssociation(IpChannelType.SCTP, SERVER_PORT1, CLIENT_PORT1);
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test
    public void testModifyServerAndAssociationTcp() throws Exception {

        this.testModifyServerAndAssociation(IpChannelType.TCP, SERVER_PORT2, CLIENT_PORT2);
    }

	/**
	 * In this test we modify server port after stop and client association 
	 * 
	 * @throws Exception
	 */
	
	private void testModifyServerAndAssociation(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {

		this.setUp(ipChannelType, serverPort, clientPort);

		this.serverAssociation.setAssociationListener(new ServerAssociationListener());
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);

		this.management.startServer(SERVER_NAME);
		
		this.clientAssociation.setAssociationListener(new ClientAssociationListenerImpl());
		this.management.startAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000 * 3);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
		
		//modify server port and association
		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		Thread.sleep(1000 * 2);
		this.management.modifyServer(SERVER_NAME, null, 2344, null, null, null, null);
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);
		this.management.startServer(SERVER_NAME);
		
		this.management.modifyAssociation(null, null, null, 2344, CLIENT_ASSOCIATION_NAME, null, null);

		isModified = true;

		Thread.sleep(1000 * 3);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
	
		this.management.stopAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000);

		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		
		Thread.sleep(1000 * 2);

		assertTrue(Arrays.equals(SERVER_MESSAGE, clientMessage));
		assertTrue(Arrays.equals(CLIENT_MESSAGE, serverMessage));

		assertTrue(clientAssocDown);
		assertTrue(serverAssocDown);

		this.tearDown();
	}

    @Test
    public void testModifyServerAndClientAssociationsSctp() throws Exception {

        if (SctpTransferTest.checkSctpEnabled())
            this.testModifyServerAndClientAssociations(IpChannelType.SCTP, SERVER_PORT3, CLIENT_PORT3);
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test
    public void testModifyServerAndClientAssociationsTcp() throws Exception {

        this.testModifyServerAndClientAssociations(IpChannelType.TCP, SERVER_PORT4, CLIENT_PORT4);
    }

	/**
	 * In this test we modify port in server association and port of client 
	 * 
	 * @throws Exception
	 */
	
	private void testModifyServerAndClientAssociations(IpChannelType ipChannelType, int serverPort, int clientPort) throws Exception {

		this.setUp(ipChannelType, serverPort, clientPort);

		this.serverAssociation.setAssociationListener(new ServerAssociationListener());
		this.management.startAssociation(SERVER_ASSOCIATION_NAME);

		this.management.startServer(SERVER_NAME);
		
		this.clientAssociation.setAssociationListener(new ClientAssociationListenerImpl());
		this.management.startAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000 * 2);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
		
		this.management.modifyServerAssociation(SERVER_ASSOCIATION_NAME, null, 2347, null, null);
		this.management.modifyAssociation(null, 2347, null, null, CLIENT_ASSOCIATION_NAME, null, null);
		
		isModified = true;

		Thread.sleep(1000 * 2);

		assertTrue(clientAssocUp);
		assertTrue(serverAssocUp);
	
		this.management.stopAssociation(CLIENT_ASSOCIATION_NAME);

		Thread.sleep(1000);

		this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
		this.management.stopServer(SERVER_NAME);
		
		Thread.sleep(1000 * 2);

		assertTrue(Arrays.equals(SERVER_MESSAGE, clientMessage));
		assertTrue(Arrays.equals(CLIENT_MESSAGE, serverMessage));

		assertTrue(clientAssocDown);
		assertTrue(serverAssocDown);

		this.tearDown();
	}

	private class ClientAssociationListenerImpl implements AssociationListener {

		private final Logger logger = LogManager.getLogger(ClientAssociationListenerImpl.class);
		
        /*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationUp
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationUp(Association association, int maxInboundStreams, int maxOutboundStreams) {
			logger.info(this + " onCommunicationUp");

			clientAssocUp = true;

			PayloadData payloadData = new PayloadData(CLIENT_MESSAGE.length, Unpooled.copiedBuffer(CLIENT_MESSAGE), true, false, 3, 1);

			try {
				if(isModified)
					association.send(payloadData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationShutdown
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationShutdown(Association association) {
			logger.info(this + " onCommunicationShutdown");
			clientAssocDown = true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationLost
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationLost(Association association) {
			logger.info(this + " onCommunicationLost");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationRestart
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationRestart(Association association) {
			logger.info(this + " onCommunicationRestart");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onPayload(org.mobicents
		 * .protocols.sctp.Association,
		 * org.mobicents.protocols.sctp.PayloadData)
		 */
		@Override
		public void onPayload(Association association, PayloadData payloadData) {
			logger.info(this + " onPayload");

			clientMessage = new byte[payloadData.getByteBuf().readableBytes()];
			payloadData.getByteBuf().readBytes(clientMessage);

			logger.info(this + "received " + new String(clientMessage));

		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.AssociationListener#inValidStreamId(org.mobicents.protocols.api.PayloadData)
		 */
		@Override
		public void inValidStreamId(PayloadData payloadData) {
			// TODO Auto-generated method stub
			
		}

	}

	private class ServerAssociationListener implements AssociationListener {

		private final Logger logger = LogManager.getLogger(ServerAssociationListener.class);
        
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationUp
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationUp(Association association, int maxInboundStreams, int maxOutboundStreams) {
			logger.info(this + " onCommunicationUp");

			serverAssocUp = true;

			PayloadData payloadData = new PayloadData(SERVER_MESSAGE.length, Unpooled.copiedBuffer(SERVER_MESSAGE), true, false, 3, 1);

			try {
				if(isModified)
					association.send(payloadData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationShutdown
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationShutdown(Association association) {
			logger.info(this + " onCommunicationShutdown");
			serverAssocDown = true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationLost
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationLost(Association association) {
			logger.info(this + " onCommunicationLost");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onCommunicationRestart
		 * (org.mobicents.protocols.sctp.Association)
		 */
		@Override
		public void onCommunicationRestart(Association association) {
			logger.info(this + " onCommunicationRestart");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.mobicents.protocols.sctp.AssociationListener#onPayload(org.mobicents
		 * .protocols.sctp.Association,
		 * org.mobicents.protocols.sctp.PayloadData)
		 */
		@Override
		public void onPayload(Association association, PayloadData payloadData) {
			logger.info(this + " onPayload");

			serverMessage = new byte[payloadData.getByteBuf().readableBytes()];
			payloadData.getByteBuf().readBytes(serverMessage);

			logger.info(this + "received " + new String(serverMessage));
		}

		/* (non-Javadoc)
		 * @see org.mobicents.protocols.api.AssociationListener#inValidStreamId(org.mobicents.protocols.api.PayloadData)
		 */
		@Override
		public void inValidStreamId(PayloadData payloadData) {
			// TODO Auto-generated method stub
			
		}

	}
}
