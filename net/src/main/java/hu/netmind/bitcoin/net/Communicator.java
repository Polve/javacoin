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

package hu.netmind.bitcoin.net;

/**
 * The communicator is the central piece of the network stack, communicating
 * with the bitcoin "network". Note that this interface does not impose any
 * restrictions on the communication strategy or topology, the only responsibility
 * is to send and receive network messages.
 * @author Robert Brautigam
 */
public interface Communicator
{
   /**
    * Send a message to the network. The exact algorithm of
    * distributing the message is up to the implementation. 
    * It can broadcast the message through direct connections,
    * it can send out emails, or post the message as a haiku to
    * a blog. Either way the only responsibility is to make best
    * effort to distribute.
    */
   void send(Message message)
      throws CommunicationException;

   /**
    * Register a listener to get messages from "the network".
    */
   void addReceiver(Receiver receiver);

   /**
    * Remove a receiver from this communicator.
    */
   void removeReceiver(Receiver receiver);
}

