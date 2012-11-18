/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package it.nibbles.javacoin.net.p2p;

import it.nibbles.javacoin.net.Message;
import it.nibbles.javacoin.net.VersionMessage;
import java.net.SocketAddress;
import java.util.Map;

/**
 * Represents a connection to another node in the network. Message handlers can
 * use this object to interact with the connection.
 *
 * @author Robert Brautigam, Alessandro Polverini
 */
public interface Connection
{

   /**
    * The connection can hold specialized information for stateful
    * conversations.
    */
   Map<String, Object> getSession();

   /**
    * Returns the session attribute with the given name or null if there is no
    * attribute by that name
    *
    * @param a String specifying the name of the attribute
    * @return an Object containing the value of the attribute, or null if no
    * attribute exists matching the given name
    */
   Object getSessionAttribute(String name);

   /**
    * Binds an object to a given attribute name in the session map
    *
    * @param a String specifying the name of the attribute
    * @return an Object representing the attribute to be bound
    */
   Object setSessionAttribute(String name, Object o);

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
    *
    * @return The version number, or -1 if not yet set.
    */
   long getVersion();

   /**
    * Set the protocol version number. This directly influences how messages are
    * deserialized with the node.
    */
   void setVersion(long version);

   /**
    * Set various properties of the peer like version, useragent etc
    * @param The version message as received from the peer
    */
   void setVersionAndInfo(VersionMessage versionMessage);

   /**
    * Returns the user agent string of the last VersionMessag received from the
    * peer, in any
    */
   String getUserAgent();

   /**
    * Returns the connection nonce, used to indentify connections to self
    * @return The connection nonce
    */
   long getNonce();

   /**
    * Returns true if this node can be asked for full blocks instead of just
    * headers This property is correct only after the first VersionMessage
    * received
    */
   boolean hasServiceNodeNetwork();

   /**
    * Returns true if the peer connected to us and not viceversa
    */
   boolean isIncoming();

   /**
    * Returns true if we initiated the connection to the peer
    */
   boolean isOutgoing();

   /**
    * Send a message to this connection.
    */
   void send(Message message);

   /**
    * Terminate this connection. If a handler wants to terminate the connection
    * to the node for whatever reason, it can use this method.
    */
   void close();
}
