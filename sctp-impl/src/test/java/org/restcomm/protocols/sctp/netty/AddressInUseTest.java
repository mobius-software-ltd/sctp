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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.restcomm.protocols.api.Association;
import org.restcomm.protocols.api.AssociationListener;
import org.restcomm.protocols.api.IpChannelType;
import org.restcomm.protocols.api.PayloadData;
import org.restcomm.protocols.sctp.SctpManagementImpl;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.nio.sctp.SctpServerChannel;

/**
 * 
 * @author sergey vetyutnev
 * @author yulianoifa
 * 
 */
public class AddressInUseTest {
	private static final Logger logger = LogManager.getLogger(AddressInUseTest.class);

    private static final String SERVER_NAME = "testserver";
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 12352;

    private static final String SERVER_ASSOCIATION_NAME = "serverAssociation";
    private static final String CLIENT_ASSOCIATION_NAME = "clientAssociation";

    private static final String CLIENT_HOST = "127.0.0.1";
    private static final int CLIENT_PORT = 12353;

    private SctpManagementImpl management = null;

    //private ServerImpl server = null;

    private Association serverAssociation = null;
    private Association clientAssociation = null;

    private volatile boolean clientAssocUp = false;
    private volatile boolean serverAssocUp = false;

    private volatile boolean clientAssocDown = false;
    private volatile boolean serverAssocDown = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
    	Configurator.initialize(new DefaultConfiguration());
    	Configurator.setRootLevel(Level.DEBUG);
    	logger.info("Starting " + AddressInUseTest.class.getName());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    	logger.info("Stopping " + AddressInUseTest.class.getName());
    }

    public void setUp(IpChannelType ipChannelType) throws Exception {
        this.clientAssocUp = false;
        this.serverAssocUp = false;

        this.clientAssocDown = false;
        this.serverAssocDown = false;

        this.management = new SctpManagementImpl("server-management",1,1,1);
//        this.management.setSingleThread(true);
        this.management.start();
        this.management.setConnectDelay(5000);// Try connecting every 5 secs
        this.management.removeAllResourses();

        //this.server = 
        this.management.addServer(SERVER_NAME, SERVER_HOST, SERVER_PORT, ipChannelType, false, 0, null);
        this.serverAssociation = this.management.addServerAssociation(CLIENT_HOST, CLIENT_PORT,
                SERVER_NAME, SERVER_ASSOCIATION_NAME, ipChannelType);
        this.clientAssociation =  this.management.addAssociation(CLIENT_HOST, CLIENT_PORT, SERVER_HOST,
                SERVER_PORT, CLIENT_ASSOCIATION_NAME, ipChannelType, null);
    }

    public void tearDown() throws Exception {

        this.management.removeAssociation(CLIENT_ASSOCIATION_NAME);
        this.management.removeAssociation(SERVER_ASSOCIATION_NAME);
        this.management.removeServer(SERVER_NAME);

        this.management.stop();
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test(groups = { "functional", "sctp" })
    public void testAddressInUseSctp() throws Exception {

        if (SctpTransferTest.checkSctpEnabled())
            this.testAddressInUseByProtocol(IpChannelType.SCTP);
    }

    /**
     * Simple test that creates Client and Server Association, exchanges data
     * and brings down association. Finally removes the Associations and Server
     */
    @Test(groups = { "functional", "tcp" })
    public void testAddressInUseTcp() throws Exception {

        this.testAddressInUseByProtocol(IpChannelType.TCP);
    }

    private void testAddressInUseByProtocol(IpChannelType ipChannelType) throws Exception {

        this.setUp(ipChannelType);

        this.management.startServer(SERVER_NAME);

        this.serverAssociation.setAssociationListener(new ServerAssociationListener());
        this.management.startAssociation(SERVER_ASSOCIATION_NAME);

        // making the client association local port busy
        if (ipChannelType == IpChannelType.TCP) {
            doInitSocketServerTcp();
        } else {
            doInitSocketServerSctp();
        }
        Thread.sleep(100);
        
        this.clientAssociation.setAssociationListener(new ClientAssociationListener());
        this.management.startAssociation(CLIENT_ASSOCIATION_NAME);

        Thread.sleep(1000 * 9);

        assertFalse(clientAssocUp);
        assertFalse(serverAssocUp);
        assertFalse(this.clientAssociation.isConnected());
        assertFalse(this.serverAssociation.isConnected());

        if (ipChannelType == IpChannelType.TCP) {
            dirtyServerTcp.close();
        } else {
            dirtyServerSctp.close();
        }

        for (int i1 = 0; i1 < 15; i1++) {
            Thread.sleep(1000 * 1);
            if (clientAssocUp)
                break;
        }
        Thread.sleep(200);

        assertTrue(clientAssocUp);
        assertTrue(serverAssocUp);
        assertTrue(this.clientAssociation.isConnected());
        assertTrue(this.serverAssociation.isConnected());

        this.management.stopAssociation(CLIENT_ASSOCIATION_NAME);

        Thread.sleep(1000);

        assertTrue(clientAssocDown);
        assertTrue(serverAssocDown);
        assertFalse(this.clientAssociation.isConnected());
        assertFalse(this.serverAssociation.isConnected());
        
        this.management.stopAssociation(SERVER_ASSOCIATION_NAME);
        this.management.stopServer(SERVER_NAME);

        Thread.sleep(1000 * 2);

        Runtime.getRuntime();

        this.tearDown();
    }

    private ServerSocketChannel dirtyServerTcp;
    private SctpServerChannel dirtyServerSctp;
    
    private void doInitSocketServerTcp() throws IOException {
        dirtyServerTcp = ServerSocketChannel.open();
        dirtyServerTcp.configureBlocking(false);

        // Bind the server socket to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(CLIENT_HOST, CLIENT_PORT);
        dirtyServerTcp.bind(isa);
    }

    private void doInitSocketServerSctp() throws IOException {
        // Create a new non-blocking server socket channel
        dirtyServerSctp = SctpServerChannel.open();
        dirtyServerSctp.configureBlocking(false);

        // Bind the server socket to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(CLIENT_HOST, CLIENT_PORT);
        dirtyServerSctp.bind(isa);
    }
    
    private class ClientAssociationListener implements AssociationListener {
        
        private final Logger logger = LogManager.getLogger(ClientAssociationListener.class);

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
