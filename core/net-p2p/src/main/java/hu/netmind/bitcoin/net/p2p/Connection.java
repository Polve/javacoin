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

package hu.netmind.bitcoin.net.p2p;

import hu.netmind.bitcoin.net.Message;
import java.util.Map;
import java.net.SocketAddress;

/**
 * Represents a connection to another node in the network. Message handlers
 * can use this object to interact with the connection.
 * @author Robert Brautigam
 */
public interface Connection
{
   /**
    * The connection can hold specialized information for stateful
    * conversations.
    */
   Map getSession();

   /**
    * Get the address of the remote side of this connection.
    */
   SocketAddress getRemoteAddress();

   /**
    * Get the local side of this connection.
    */
   SocketAddress getLocalAddress();

   /**
    * Get the protocol version number.
    * @return The version number, or -1 if not yet set.
    */
   long getVersion();

   /**
    * Set the protocol version number. This directly influences
    * how messages are deserialized with the node.
    */
   void setVersion(long version);

   /**
    * Send a message to this connection.
    */
   void send(Message message);

   /**
    * Terminate this connection. If a handler wants to terminate
    * the connection to the node for whatever reason, it can use
    * this method.
    */
   void close();
}

