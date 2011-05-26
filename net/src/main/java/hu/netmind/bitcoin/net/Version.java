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
 * @author Robert Brautigam
 */
public interface Version extends Message
{
   /**
    * Get the version identifier.
    */
   long getVersion();

   /**
    * Get the supported services in the 64 long bitfield.
    */
   long getServices();

   /**
    * Get the timestamp in seconds from epoch (standard UNIX time).
    */
   long getTimestamp();

   /**
    * Get the address of the node sending this message.
    */
   NodeAddress getSenderAddress();

   /**
    * Get the address of the receiving side as seen by the sender. This is only
    * available from version 106.
    */
   NodeAddress getReceiverAddress();

   /**
    * Get the "nonce", a random number to prevent connections to self. Only
    * available from version 106.
    */
   long getNonce();
   
   /**
    * Get the secondary version specifier. Only available from version 106.
    */
   String getSecondaryVersion();

   /**
    * Get the number of the last block seen by sender. Only available from version 209.
    */
   long getStartHeight();
}

