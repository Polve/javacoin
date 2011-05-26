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
 * An implementation of this interface can be registered to the Communicator
 * to receive messages from the BitCoin network.
 * @author Robert Brautigam
 */
public interface Receiver
{
   /**
    * Receive a message from the network. If the component who gets this
    * message wants to answer, it can return a <code>Message</code> object
    * to answer only to the node who sent the message. If a message is returned,
    * the Communicator will cease to ask other components to handle the message.
    * @param message The message received through from the BitCoin network.
    * @return A message if the receiver wants to answer to the sender node,
    * or null if no answer is sent back.
    */
   Message receive(Message message);
}

