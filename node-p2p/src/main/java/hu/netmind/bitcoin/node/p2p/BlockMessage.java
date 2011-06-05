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
   private long version;
   private byte[] prevBlock;
   private byte[] rootHash;
   private long timestamp;
   private long difficulty;
   private long nonce;
   private List<Transaction> transactions;

   public BlockMessage(long magic, long version, byte[] prevBlock, byte[] rootHash, long timestamp,
         long difficulty, long nonce, List<Transaction> transactions)
      throws IOException
   {
      super(magic,"block");
      this.version=version;
      this.prevBlock=prevBlock;
      this.rootHash=rootHash;
      this.timestamp=timestamp;
      this.difficulty=difficulty;
      this.nonce=nonce;
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
      version = input.readUInt32();
      prevBlock = input.readBytes(32);
      rootHash = input.readBytes(32);
      timestamp = input.readUInt32()*1000;
      difficulty = input.readUInt32();
      nonce = input.readUInt32();
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
      output.writeUInt32(version);
      output.write(prevBlock);
      output.write(rootHash);
      output.writeUInt32(timestamp/1000);
      output.writeUInt32(difficulty);
      output.writeUInt32(nonce);
      output.writeUIntVar(transactions.size());
      for ( Transaction transaction : transactions )
         transaction.writeTo(output,protocolVersion);
   }

   public String toString()
   {
      return super.toString()+" version: "+version+", prevBlock: "+Arrays.toString(prevBlock)+
         ", root hash: "+Arrays.toString(rootHash)+", timestamp: "+timestamp+", difficulty: "+difficulty+
         ", nonce: "+nonce+", transactions: "+transactions;
   }

   public long getVersion()
   {
      return version;
   }

   public byte[] getPrevBlock()
   {
      return prevBlock;
   }

   public byte[] getRootHash()
   {
      return rootHash;
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   public long getDifficulty()
   {
      return difficulty;
   }

   public long getNonce()
   {
      return nonce;
   }

   public List<Transaction> getTransactions()
   {
      return transactions;
   }


}

