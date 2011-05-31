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

import hu.netmind.bitcoin.net.ChecksummedMessage;
import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class ChecksummedMessageImpl extends MessageImpl implements ChecksummedMessage
{
   private long checksum = 0;

   public ChecksummedMessageImpl(long magic, String command)
   {
      super(magic,command);
   }

   ChecksummedMessageImpl()
   {
   }

   void readFrom(BitCoinInputStream input, Object param)
      throws IOException
   {
      super.readFrom(input,param);
      checksum = input.readUInt32();
      // Now let's make sure we keep track of the real checksum
   }

   public long getChecksum()
   {
      return checksum;
   }

   public boolean verify()
   {
      // TODO
      return false;
   }
}

