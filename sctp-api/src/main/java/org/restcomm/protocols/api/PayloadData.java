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

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

/**
 * The actual pay load data received or to be sent from/to underlying socket
 * 
 * @author amit bhayani
 * @author yulianoifa
 * 
 */
public class PayloadData {
	private final int dataLength;
	private final ByteBuf byteBuf;
	private final boolean complete;
	private final boolean unordered;
	private final int payloadProtocolId;
	private final int streamNumber;

    /**
     * @param dataLength
     *            Length of data data
     * @param byteBuf
     *            the payload data
     * @param complete
     *            if this data represents complete protocol data
     * @param unordered
     *            set to true if we don't care for oder
     * @param payloadProtocolId
     *            protocol ID of the data carried
     * @param streamNumber
     *            the SCTP stream number
     */
    public PayloadData(int dataLength, ByteBuf byteBuf, boolean complete, boolean unordered, int payloadProtocolId, int streamNumber) {
        super();
        this.dataLength = dataLength;
        this.byteBuf = byteBuf;
        this.complete = complete;
        this.unordered = unordered;
        this.payloadProtocolId = payloadProtocolId;
        this.streamNumber = streamNumber;
    }

	/**
     * @return the byteBuf
     */
    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public void releaseBuffer() {
    	ReferenceCountUtil.release(byteBuf);
    }
    
	/**
	 * @return the complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * @return the unordered
	 */
	public boolean isUnordered() {
		return unordered;
	}

	/**
	 * @return the payloadProtocolId
	 */
	public int getPayloadProtocolId() {
		return payloadProtocolId;
	}

	/**
	 * <p>
	 * This is SCTP Stream sequence identifier.
	 * </p>
	 * <p>
	 * While sending PayloadData to SCTP Association, this value should be set
	 * by SCTP user. If value greater than or equal to maxOutboundStreams or
	 * lesser than 0 is used, packet will be dropped and error message will be
	 * logged
	 * </p>
	 * </p> While PayloadData is received from underlying SCTP socket, this
	 * value indicates stream identifier on which data was received. Its
	 * guaranteed that this value will be greater than 0 and less than
	 * maxInboundStreams
	 * <p>
	 * 
	 * @return the streamNumber
	 */
	public int getStreamNumber() {
		return streamNumber;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PayloadData [dataLength=").append(dataLength).append(", complete=").append(complete).append(", unordered=")
                .append(unordered).append(", payloadProtocolId=").append(payloadProtocolId).append(", streamNumber=")
                .append(streamNumber).append("]");
        return sb.toString();
	}

}