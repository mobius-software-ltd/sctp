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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.sctp.SctpChannel;
import io.netty.handler.codec.sctp.SctpMessageCompletionHandler;

/**
 * @author <a href="mailto:amit.bhayani@telestax.com">Amit Bhayani</a>
 * @author yulianoifa
 * 
 */
public class NettySctpClientChannelInitializer extends ChannelInitializer<SctpChannel> {

    private final AssociationImpl nettyAssociationImpl;

    /**
     * @param nettyAssociationImpl
     */
    public NettySctpClientChannelInitializer(AssociationImpl nettyAssociationImpl) {
        super();
        this.nettyAssociationImpl = nettyAssociationImpl;
    }

    @Override
    protected void initChannel(SctpChannel ch) throws Exception {
        ch.pipeline().addLast(new SctpMessageCompletionHandler(), new NettySctpClientHandler(this.nettyAssociationImpl));

    }
}