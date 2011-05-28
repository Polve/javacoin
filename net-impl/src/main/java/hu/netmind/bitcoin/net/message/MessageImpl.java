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

package hu.netmind.bitcoin.net.message;

import hu.netmind.bitcoin.net.Message;
import java.io.IOException;

/**
 * This is the base class for all messages, provides serialization and
 * deserialization of the header.
 * @author Robert Brautigam
 */
public abstract class MessageImpl implements Message
{
   private long magic;
   private String command;

   /**
    * All messages must provide a constructor to construct
    * the message by reading from an input stream. Note, each
    * superclass will read the approriate fields already from the stream.
    */
   public MessageImpl(BitCoinInputStream input)
      throws IOException
   {
      magic = input.readUInt32();
      command = input.readString(12);
   }

   /**
    * All messages must provide a constructor also to construct
    * the message by supplying all attributes.
    */
   public MessageImpl(long magic, String command)
   {
      this.magic=magic;
      this.command=command;
   }
   
   /**
    * All messages must also override this method to serialize
    * the contents of the message to the BitCoin protocol. Note,
    * implementations must call the superclass' <code>writeTo()</code>
    * always first.
    */
   void writeTo(BitCoinOutputStream output)
      throws IOException
   {
      output.writeUInt32(magic);
      output.writeString(command,12);
   }
}

