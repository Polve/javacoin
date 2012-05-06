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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robert Brautigam
 */
public class BlockMessage extends ChecksummedMessage
{
   private BlockHeader header;
   private List<Tx> transactions;

   public BlockMessage(long magic, BlockHeader header, List<Tx> transactions)
      throws IOException
   {
      super(magic,"block");
      this.header=header;
      this.transactions=transactions;
   }

   BlockMessage()
      throws IOException
   {
      super();
   }

   @Override
   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input,protocolVersion,param);
      header = new BlockHeader();
      header.readFrom(input,protocolVersion,param);
      long txCount = input.readUIntVar();
      if ( (txCount<0) || (txCount>=Integer.MAX_VALUE) )
         throw new IOException("too many transactions in the block: "+txCount);
      transactions = new ArrayList<Tx>();
      for ( long i = 0; i<txCount; i++ )
      {
         Tx tx = new Tx();
         tx.readFrom(input,protocolVersion,param);
         transactions.add(tx);
      }
   }

   @Override
   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output,protocolVersion);
      header.writeTo(output);
      output.writeUIntVar(transactions.size());
      for ( Tx tx : transactions )
         tx.writeTo(output,protocolVersion);
   }

   @Override
   public String toString()
   {
      return super.toString()+" "+header.toString()+", transactions: "+transactions;
   }

   public BlockHeader getHeader()
   {
      return header;
   }

   public List<Tx> getTransactions()
   {
      return transactions;
   }


}

