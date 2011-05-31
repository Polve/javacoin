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
import java.security.MessageDigest;

/**
 * @author Robert Brautigam
 */
public class ChecksummedMessageImpl extends MessageImpl implements ChecksummedMessage
{
   private long checksum = 0;
   private long calculatedChecksum = 0;
   private MessageDigest digest = null;

   public ChecksummedMessageImpl(long magic, String command)
      throws IOException
   {
      super(magic,command);
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( Exception e ) {
         throw new IOException("could not initialize SHA-256 digest",e);
      }
   }

   ChecksummedMessageImpl()
      throws IOException
   {
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( Exception e ) {
         throw new IOException("could not initialize SHA-256 digest",e);
      }
   }

   void readFrom(BitCoinInputStream input, Object param)
      throws IOException
   {
      super.readFrom(input,param);
      checksum = input.readUInt32();
      // Now let's make sure we keep track of the real checksum
      input.setListener(new BitCoinInputStream.Listener() {
               public void update(int value)
               {
                  digest.update((byte) value);
               }
            });
   }

   void postReadFrom(BitCoinInputStream input)
   {
      // Calculate sha256(sha256(content))
      input.clearListener();
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Calculate first 4 bytes
      calculatedChecksum = ((long) result[0]) | (((long) result[1])>>8) |
         (((long) result[2])>>16) | (((long) result[3])>>24);
   }

   void writeTo(BitCoinOutputStream output)
      throws IOException
   {
      // Write placeholder
      output.writeUInt32(0);
   }

   void postWriteTo(byte[] serializedBytes)
      throws IOException
   {
      // Calculate checksum
      digest.reset();
      digest.update(serializedBytes,20,serializedBytes.length-20);
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Overwrite previous 0 value with first 4 bytes
      OverwriterBitCoinOutputStream output = new OverwriterBitCoinOutputStream(serializedBytes,20);
      output.writeU(result[0]);
      output.writeU(result[1]);
      output.writeU(result[2]);
      output.writeU(result[3]);
   }

   public long getChecksum()
   {
      return checksum;
   }

   public boolean verify()
   {
      return calculatedChecksum == checksum;
   }
}

