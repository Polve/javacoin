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

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Robert Brautigam
 */
public class BlockMessage extends ChecksummedMessage
{
   private BlockHeader header;
   private List<Transaction> transactions;

   public BlockMessage(long magic, BlockHeader header, List<Transaction> transactions)
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

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input,protocolVersion,param);
      header = new BlockHeader();
      header.readFrom(input,protocolVersion,param);
      long txCount = input.readUIntVar();
      if ( (txCount<0) || (txCount>=Integer.MAX_VALUE) )
         throw new IOException("too many transactions in the block: "+txCount);
      transactions = new ArrayList<Transaction>();
      for ( long i = 0; i<txCount; i++ )
      {
         Transaction transaction = new Transaction();
         transaction.readFrom(input,protocolVersion,param);
         transactions.add(transaction);
      }
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output,protocolVersion);
      header.writeTo(output,protocolVersion);
      output.writeUIntVar(transactions.size());
      for ( Transaction transaction : transactions )
         transaction.writeTo(output,protocolVersion);
   }

   public String toString()
   {
      return super.toString()+" "+header.toString()+", transactions: "+transactions;
   }

   public BlockHeader getHeader()
   {
      return header;
   }

   public List<Transaction> getTransactions()
   {
      return transactions;
   }


}

