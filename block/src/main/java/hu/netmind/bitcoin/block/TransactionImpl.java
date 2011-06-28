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

package hu.netmind.bitcoin.block;

import java.util.List;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.Block;

/**
 * @author Robert Brautigam
 */
public class TransactionImpl implements Transaction
{
   private List<TransactionInput> inputs;
   private List<TransactionOutput> outputs;
   private long lockTime;
   private transient Block block; // Only set runtime by block
   private byte[] hash;

   /**
    * Create the transaction with the inputs, outputs and locking time. This method
    * computes the transaction hash.
    */
   public TransactionImpl(List<TransactionInput> inputs, List<TransactionOutput> outputs,
         long lockTime)
   {
      this.inputs=inputs;
      this.outputs=outputs;
      this.lockTime=lockTime;
      calculateHash();
   }
   
   /**
    * Create the transaction with all the necessary parameters, but also the computed hash.
    * This constructor should be used when deserializing transactions.
    */
   public TransactionImpl(List<TransactionInput> inputs, List<TransactionOutput> outputs,
         long lockTime, byte[] hash)
   {
      this.inputs=inputs;
      this.outputs=outputs;
      this.lockTime=lockTime;
      this.hash=hash;
   }

   public List<TransactionInput> getInputs()
   {
      return inputs;
   }

   public List<TransactionOutput> getOutputs()
   {
      return outputs;
   }

   public long getLockTime()
   {
      return lockTime;
   }

   public Block getBlock()
   {
      return block;
   }

   /**
    * Block is set by the block itself when the transaction is added to it.
    */
   void setBlock(Block block)
   {
      this.block=block;
   }

   public byte[] getHash()
   {
      return hash;
   }

   /**
    * Calculate the hash of the whole transaction.
    */
   private void calculateHash()
   {
      TODO
   }

   /**
    * Calculate the hash for a specific input suitable for creating a signature. The hash 
    * calculation is based on message serialization.
    * @param type The hash type.
    * @param txIn The input to hash for.
    * @param subscript The subscript to use for hashing. We assume it fits the subscript requirements
    * (no signatures in it, no code separators, etc.)
    */
   public byte[] getSignatureHash(SignatureHashType type, TransactionInput txIn, byte[] subscript)
   {
      TODO
   }

}

