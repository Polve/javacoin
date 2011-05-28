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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample implementation of the input stream based on a
 * simple byte array.
 * @author Robert Brautigam
 */
public class ByteArrayBitCoinInputStream extends BitCoinInputStream
{
   private static final Logger logger = LoggerFactory.getLogger(ByteArrayBitCoinInputStream.class);

   private byte[] bytes = null;
   private int pointer = 0;

   public ByteArrayBitCoinInputStream(byte[] bytes)
   {
      this.bytes=bytes;
   }

   public int read()
   {
      logger.debug("read called with pointer: {} / {}",pointer,bytes.length);
      if ( (bytes==null) || (pointer > bytes.length-1) )
         return -1;
      int result = (bytes[pointer++] & 0xff);
      logger.debug("returning byte: {}",result);
      return result;
   }
}

