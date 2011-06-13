/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.bitcoin.node.p2p;

import java.net.SocketAddress;

/**
 * Implement and register this interface to a Node to receive messages
 * from the bitcoin network.
 * @author Robert Brautigam
 */
public interface MessageHandler
{
   /**
    * There is a possibility to send a message to a new node on a join.
    * @param conn The connection object to node.
    */
   Message onJoin(Connection conn);

   /**
    * Called when a node disconnects.
    * @param conn The connection object to node.
    */
   void onLeave(Connection conn);

   /**
    * Handle a message from the network. There is a possibility
    * to reply to the incoming message by returning a message.
    * @param addr The address of the node who disconnected.
    * @param message The incoming message.
    * @return A message to reply to only the source node for the
    * incoming message, or null if no reply is needed.
    */
   Message onMessage(Connection conn, Message message);
}

