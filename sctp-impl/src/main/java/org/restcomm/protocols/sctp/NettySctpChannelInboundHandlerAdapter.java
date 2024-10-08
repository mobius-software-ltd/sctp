package org.restcomm.protocols.sctp;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.protocols.api.IpChannelType;
import org.restcomm.protocols.api.PayloadData;

import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.sctp.SctpMessage;

/**
 * @author <a href="mailto:amit.bhayani@telestax.com">Amit Bhayani</a>
 * @author yulianoifa
 * 
 */
public class NettySctpChannelInboundHandlerAdapter extends ChannelInboundHandlerAdapter {

    Logger logger = LogManager.getLogger(NettySctpChannelInboundHandlerAdapter.class);

    // Default value is 1 for TCP
    private volatile int maxInboundStreams = 1;
    private volatile int maxOutboundStreams = 1;

    protected AssociationImpl association = null;

    protected Channel channel = null;
    protected ChannelHandlerContext ctx = null;

    protected long lastCongestionMonitorSecondPart;

    /**
     * 
     */
    public NettySctpChannelInboundHandlerAdapter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("channelInactive event: association=%s", this.association));
        }

        if (this.association != null)
            this.association.markAssociationDown();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("userEventTriggered event: association=%s \nevent=%s", this.association, evt));
        }

        if (evt instanceof AssociationChangeNotification) {
            // SctpAssocChange not = (SctpAssocChange) evt;
            AssociationChangeNotification not = (AssociationChangeNotification) evt;

            switch (not.event()) {
                case COMM_UP:
                    if (not.association() != null) {
                        this.maxOutboundStreams = not.association().maxOutboundStreams();
                        this.maxInboundStreams = not.association().maxInboundStreams();
                    }

                    if (logger.isInfoEnabled()) {
                        logger.info(String.format(
                                "New association setup for Association=%s with %d outbound streams, and %d inbound streams.\n",
                                association.getName(), this.maxOutboundStreams, this.maxInboundStreams));
                    }

                    this.association.markAssociationUp(this.maxInboundStreams, this.maxOutboundStreams);
                    break;
                case CANT_START:
                    logger.error(String.format("Can't start for Association=%s", association.getName()));
                    break;
                case COMM_LOST:
                    logger.warn(String.format("Communication lost for Association=%s", association.getName()));

                    // Close the Socket
                    association.markCommunicationLost();
                    association.getAssociationListener().onCommunicationLost(association);
                    ctx.close();
//                    if (association.getAssociationType() == AssociationType.CLIENT) {
//                        association.scheduleConnect();
//                    }
                    break;
                case RESTART:
                    logger.warn(String.format("Restart for Association=%s", association.getName()));
                    try {
                    	association.markCommunicationRestart();
                        association.getAssociationListener().onCommunicationRestart(association);
                    } catch (Exception e) {
                        logger.error(String.format(
                                "Exception while calling onCommunicationRestart on AssociationListener for Association=%s",
                                association.getName()), e);
                    }
                    break;
                case SHUTDOWN:
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Shutdown for Association=%s", association.getName()));
                    }
//                    try {
//                        association.markAssociationDown();
//                    } catch (Exception e) {
//                        logger.error(String.format(
//                                "Exception while calling onCommunicationShutdown on AssociationListener for Association=%s",
//                                association.getName()), e);
//                    }
                    break;
                default:
                    logger.warn(String.format("Received unkown Event=%s for Association=%s", not.event(), association.getName()));
                    break;
            }
        }

        if (evt instanceof PeerAddressChangeNotification) {
            PeerAddressChangeNotification notification = (PeerAddressChangeNotification) evt;

            logger.warn(String.format("Peer Address changed to=%s for Association=%s", notification.address(),
                        association.getName()));            
        } else if (evt instanceof SendFailedNotification) {
            SendFailedNotification notification = (SendFailedNotification) evt;
            logger.error(String.format("Association=" + association.getName() + " SendFailedNotification, errorCode="
                    + notification.errorCode()));

        } else if (evt instanceof ShutdownNotification) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Association=%s SHUTDOWN", association.getName()));
            }

            // TODO assign Thread's ?

//            try {
//                association.markAssociationDown();
//                association.getAssociationListener().onCommunicationShutdown(association);
//            } catch (Exception e) {
//                logger.error(String.format(
//                        "Exception while calling onCommunicationShutdown on AssociationListener for Association=%s",
//                        association.getName()), e);
//            }
        }// if (evt instanceof AssociationChangeNotification)

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // try {
        PayloadData payload;
        if (this.association.getIpChannelType() == IpChannelType.SCTP) {
            SctpMessage sctpMessage = (SctpMessage) msg;
            ByteBuf byteBuf = sctpMessage.content();
            payload = new PayloadData(byteBuf.readableBytes(), byteBuf, sctpMessage.isComplete(), sctpMessage.isUnordered(),
                    sctpMessage.protocolIdentifier(), sctpMessage.streamIdentifier());
        } else {
            ByteBuf byteBuf = (ByteBuf) msg;
            payload = new PayloadData(byteBuf.readableBytes(), byteBuf, true, false, 0, 0);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Rx : Ass=%s %s", this.association.getName(), payload));
        }

        this.association.read(payload);
        // } finally {
        // ReferenceCountUtil.release(msg);
        // }
    }

    protected void writeAndFlush(Object message) {
        Channel ch = this.channel;
        if (ch != null) {
            ch.writeAndFlush(message);
        }
     }

    protected void closeChannel() {
        Channel ch = this.channel;
        if (ch != null) {
            try {
                ch.close().sync();
            } catch (InterruptedException e) {
                logger.error(String.format("Error while trying to close Channel for Associtaion %s",
                        this.association.getName(), e));
            }
        }
    }

}