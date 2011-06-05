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

import hu.netmind.bitcoin.net.ChecksummedMessage;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
public abstract class ChecksummedMessage extends Message
{
   private static final Logger logger = LoggerFactory.getLogger(ChecksummedMessage.class);

   private long checksum = 0;
   private long calculatedChecksum = 0;
   private MessageDigest digest = null;

   public ChecksummedMessage(long magic, String command)
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

   ChecksummedMessage()
      throws IOException
   {
      try
      {
         digest = MessageDigest.getInstance("SHA-256");
      } catch ( Exception e ) {
         throw new IOException("could not initialize SHA-256 digest",e);
      }
   }

   void readFrom(BitCoinInputStream input, long version, Object param)
      throws IOException
   {
      super.readFrom(input,version,param);
      checksum = input.readUInt32();
      // Now let's make sure we keep track of the real checksum
      input.setListener(new BitCoinInputStream.Listener() {
               public void update(int value)
               {
                  digest.update((byte) value);
               }
            });
   }

   void postReadFrom(BitCoinInputStream input, long version, Object param)
   {
      super.postReadFrom(input,version,param);
      // Calculate sha256(sha256(content))
      input.clearListener();
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Calculate checksum first 4 bytes
      calculatedChecksum = (long) ((long) result[0]&0xff) | (((long) result[1]&0xff)<<8) |
         (((long) result[2]&0xff)<<16) | (((long) result[3]&0xff)<<24);
      logger.debug("digest checksum: {}",calculatedChecksum);
   }

   void writeTo(BitCoinOutputStream output, long version)
      throws IOException
   {
      super.writeTo(output,version);
      // Write placeholder
      output.writeUInt32(0);
   }

   void postWriteTo(byte[] serializedBytes, long version)
      throws IOException
   {
      super.postWriteTo(serializedBytes,version);
      // Calculate checksum
      digest.reset();
      digest.update(serializedBytes,24,serializedBytes.length-24);
      byte[] tmp = digest.digest();
      digest.reset();
      byte[] result = digest.digest(tmp);
      // Overwrite previous 0 value with first 4 bytes
      OverwriterBitCoinOutputStream output = new OverwriterBitCoinOutputStream(serializedBytes,20);
      output.writeU(result[0]&0xff);
      output.writeU(result[1]&0xff);
      output.writeU(result[2]&0xff);
      output.writeU(result[3]&0xff);
   }

   public long getChecksum()
   {
      return checksum;
   }

   public boolean verify()
   {
      logger.debug("verifying message calculated: {} vs. {}",calculatedChecksum,checksum);
      return calculatedChecksum == checksum;
   }
}

