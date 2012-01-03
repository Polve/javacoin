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

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
public class TxMessage extends ChecksummedMessage
{
   private Tx tx;

   public TxMessage(long magic, Tx tx)
      throws IOException
   {
      super(magic,"tx");
      this.tx=tx;
   }

   TxMessage()
      throws IOException
   {
      super();
   }

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input,protocolVersion,param);
      tx = new Tx();
      tx.readFrom(input,protocolVersion,param);
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output,protocolVersion);
      tx.writeTo(output,protocolVersion);
   }

   public String toString()
   {
      return super.toString()+" tx: "+tx;
   }

   public Tx getTx()
   {
      return tx;
   }
}

