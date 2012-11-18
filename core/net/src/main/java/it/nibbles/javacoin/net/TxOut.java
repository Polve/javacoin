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

package it.nibbles.javacoin.net;

import it.nibbles.javacoin.utils.BtcUtil;
import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class TxOut
{
   private long value;
   private byte[] script;

   public TxOut(long value, byte[] script)
   {
      this.value=value;
      this.script=script;
   }

   TxOut()
   {
   }

   void readFrom(BitcoinInputStream input)
      throws IOException
   {
      value = input.readUInt64();
      if ( value < 0 )
         throw new IOException("tx value too large, overflow detected: "+value);
      long scriptLength = input.readUIntVar();
      if ( (scriptLength<0) || (scriptLength>Integer.MAX_VALUE) )
         throw new IOException("script length outside of normal boundaries: "+scriptLength);
      script = input.readBytes((int) scriptLength);
   }

   void writeTo(BitcoinOutputStream output)
      throws IOException
   {
      output.writeUInt64(value);
      output.writeUIntVar(script.length);
      output.write(script);
   }

   @Override
   public String toString()
   {
      return "value: "+value+", script: "+BtcUtil.hexOut(script);
   }

   public long getValue()
   {
      return value;
   }

   public byte[] getScript()
   {
      return script;
   }
}

