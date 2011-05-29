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
 * A BitCoin network message.
 * @author Robert Brautigam
 */
public interface Message
{
   long MAGIC_MAIN = 0xF9BEB4D9;
   long MAGIC_TEST = 0xFABFB5DA;

   /**
    * Get the "magic" field of the message.
    */
   long getMagic();

   /**
    * Get the "command" for this message.
    */
   String getCommand();

   /**
    * Get the length of the "payload" in bytes. This does not include
    * the header information which is 24 bytes for all messages except
    * Version and Verack, which are 20 bytes only (no checksum).
    */
   long getLength();
}

