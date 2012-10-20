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

package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.TransactionInput;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import hu.netmind.bitcoin.block.BitcoinFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedList;
import static hu.netmind.bitcoin.block.bdb.BytesBinding.readBytes;

/**
 * Serializes and deserialized links as using the "tuple" serialization methods.
 * @author Robert Brautigam
 */
public class LinkBinding extends TupleBinding<StoredLink>
{
   private BitcoinFactory bitcoinFactory;

   public LinkBinding(BitcoinFactory bitcoinFactory)
   {
      this.bitcoinFactory=bitcoinFactory;
   }

  @Override
   public StoredLink entryToObject(TupleInput in)
   {
      BlockChainLink link = readLink(in);
      int junctionCount = in.readInt();
      List<Path.Junction> junctions = new LinkedList<Path.Junction>();
      for ( int i=0; i<junctionCount; i++ )
         junctions.add(new Path.Junction(in.readLong(),in.readInt()));
      return new StoredLink(link,new Path(junctions,link.getHeight()));
   }

   private BlockChainLink readLink(TupleInput in)
   {
      Block block = readBlock(in);
      boolean orphan = in.readBoolean();
      long height = in.readLong();
      Difficulty totalDifficulty = bitcoinFactory.newDifficulty(new BigDecimal(in.readString()));
      return new BlockChainLink(block,totalDifficulty,height,orphan);
   }

   private Block readBlock(TupleInput in)
   {
      try
      {
         int transactionCount = in.readInt();
         List<TransactionImpl> transactions = new LinkedList<TransactionImpl>();
         for ( int i=0; i<transactionCount; i++ )
            transactions.add(readTransaction(in));
         long creationTime = in.readLong();
         long nonce = in.readLong();
         long compressedTarget = in.readLong();
         byte[] previousBlockHash = readBytes(in,32);
         byte[] merkleRoot = readBytes(in,32);
         byte[] hash = readBytes(in,32);
         return new BlockImpl(transactions,creationTime,nonce,compressedTarget,previousBlockHash,
               merkleRoot,hash);
      } catch ( BitcoinException e ) {
         throw new BDBStorageException("could not create block",e);
      }
   }

   private TransactionImpl readTransaction(TupleInput in)
   {
      try
      {
         int inputCount = in.readInt();
         List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
         for ( int i=0; i<inputCount; i++ )
            inputs.add(readInput(in));
         int outputCount = in.readInt();
         List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
         for ( int i=0; i<outputCount; i++ )
            outputs.add(readOutput(in));
         byte[] hash = readBytes(in,32);
         long lockTime = in.readLong();
         return new TransactionImpl(inputs,outputs,lockTime,hash);
      } catch ( BitcoinException e ) {
         throw new BDBStorageException("could not create transaction",e);
      }
   }

   private TransactionInputImpl readInput(TupleInput in)
   {
      byte[] claimedHash = readBytes(in,32);
      int claimedIndex = in.readInt();
      long sequence = in.readLong();
      int scriptLength = in.readInt();
      byte[] script = readBytes(in,scriptLength);
      return new TransactionInputImpl(claimedHash,claimedIndex,
            bitcoinFactory.getScriptFactory().createFragment(script),sequence);
   }

   private TransactionOutputImpl readOutput(TupleInput in)
   {
      long value = in.readLong();
      int scriptLength = in.readInt();
      byte[] script = readBytes(in,scriptLength);
      return new TransactionOutputImpl(value,
            bitcoinFactory.getScriptFactory().createFragment(script));
   }

  @Override
   public void objectToEntry(StoredLink link, TupleOutput out)
   {
      write(link.getLink(),out);
      List<Path.Junction> junctions = link.getPath().getJunctions();
      out.writeInt(junctions.size());
      for ( Path.Junction junction : junctions )
      {
         out.writeLong(junction.getHeight());
         out.writeInt(junction.getIndex());
      }
   }

   private void write(BlockChainLink link, TupleOutput out)
   {
      write(link.getBlock(),out);
      out.writeBoolean(link.isOrphan());
      out.writeLong(link.getHeight());
      out.writeString(link.getTotalDifficulty().getDifficulty().toString());
   }

   private void write(Block block, TupleOutput out)
   {
      out.writeInt(block.getTransactions().size());
      for ( Transaction transaction : block.getTransactions() )
         write(transaction,out);
      out.writeLong(block.getCreationTime());
      out.writeLong(block.getNonce());
      out.writeLong(block.getCompressedTarget());
      out.write(block.getPreviousBlockHash());
      out.write(block.getMerkleRoot());
      out.write(block.getHash());
   }

   private void write(Transaction transaction, TupleOutput out)
   {
      out.writeInt(transaction.getInputs().size());
      for ( TransactionInput input : transaction.getInputs() )
         write(input,out);
      out.writeInt(transaction.getOutputs().size());
      for ( TransactionOutput output : transaction.getOutputs() )
         write(output,out);
      out.write(transaction.getHash());
      out.writeLong(transaction.getLockTime());
   }

   private void write(TransactionInput input, TupleOutput out)
   {
      out.write(input.getClaimedTransactionHash());
      out.writeInt(input.getClaimedOutputIndex());
      out.writeLong(input.getSequence());
      out.writeInt(input.getSignatureScript().toByteArray().length);
      out.write(input.getSignatureScript().toByteArray());
   }

   private void write(TransactionOutput output, TupleOutput out)
   {
      out.writeLong(output.getValue());
      out.writeInt(output.getScript().toByteArray().length);
      out.write(output.getScript().toByteArray());
   }

}

