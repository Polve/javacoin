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

package it.nibbles.javacoin.block;

import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.ScriptFragment;
import it.nibbles.javacoin.SignatureHashType;
import it.nibbles.javacoin.Transaction;
import it.nibbles.javacoin.TransactionInput;
import it.nibbles.javacoin.TransactionOutput;
import it.nibbles.javacoin.VerificationException;
import it.nibbles.javacoin.net.ArraysUtil;
import it.nibbles.javacoin.utils.BtcUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robert Brautigam
 */
public class TransactionInputImpl implements TransactionInput
{
   private byte[] claimedTransactionHash;
   private int claimedOutputIndex;
   private ScriptFragment signatureScript;
   private long sequence;
   private TransactionImpl transaction; // Parent is filled out runtime

   public TransactionInputImpl(byte[] claimedTransactionHash, int claimedOutputIndex,
         ScriptFragment signatureScript, long sequence)
   {
      this.claimedTransactionHash=claimedTransactionHash;
      this.claimedOutputIndex=claimedOutputIndex;
      this.signatureScript=signatureScript;
      this.sequence=sequence;
   }

   @Override
   public byte[] getClaimedTransactionHash()
   {
      return claimedTransactionHash;
   }

   @Override
   public int getClaimedOutputIndex()
   {
      return claimedOutputIndex;
   }

   @Override
   public ScriptFragment getSignatureScript()
   {
      return signatureScript;
   }
   @Override
   public long getSequence()
   {
      return sequence;
   }
   @Override
   public Transaction getTransaction()
   {
      return transaction;
   }

   void setTransaction(TransactionImpl transaction)
   {
      this.transaction=transaction;
   }

   /**
    * Calculate the hash for a this input suitable for creating a signature. The hash 
    * calculation is based on message serialization.
    * @param type The hash type.
    * @param subscript The subscript to use for hashing. We assume it fits the subscript requirements
    * (no signatures in it, no code separators, etc.)
    */
   @Override
   public byte[] getSignatureHash(SignatureHashType type, ScriptFragment subscript)
      throws BitcoinException
   {
      List<TransactionInputImpl> inputs = new ArrayList<>();
      List<TransactionOutputImpl> outputs = new ArrayList<>();
      // Create the inputs
      switch ( type.getInputType() )
      {
         case SIGHASH_ALL:
            // In this mode everything is hashed in other than the scripts in the other inputs,
            // meaning nothing can essentially change after the hash is signed.
            for ( TransactionInput input : getTransaction().getInputs() )
               if ( input == this )
                  inputs.add(new TransactionInputImpl(input.getClaimedTransactionHash(),
                           input.getClaimedOutputIndex(),subscript,sequence));
               else
                  inputs.add(new TransactionInputImpl(input.getClaimedTransactionHash(),
                           input.getClaimedOutputIndex(),null,input.getSequence()));
            break;
         case SIGHASH_ALLOWUPDATE:
            // Allow updates to other inputs (hash with sequence set to 0, in addition to null script)
            for ( TransactionInput input : getTransaction().getInputs() )
               if ( input == this )
                  inputs.add(new TransactionInputImpl(input.getClaimedTransactionHash(),
                           input.getClaimedOutputIndex(),subscript,sequence));
               else
                  inputs.add(new TransactionInputImpl(input.getClaimedTransactionHash(),
                           input.getClaimedOutputIndex(),null,0));
            break;
         case SIGHASH_ANYONECANPAY:
            // Only hash this input, which makes it possible to add any number of new inputs,
            // leaving the possibility open for others to contribute to the same (hashed) outputs and
            // amount.
            inputs.add(new TransactionInputImpl(claimedTransactionHash,
                     claimedOutputIndex,subscript,sequence));
            break;
      }
      // Make the outputs
      switch ( type.getOutputType() )
      {
         case SIGHASH_ALL:
            // All outputs are fixed, no other spending is allowed than expected when input
            // was created
            for ( TransactionOutput output : getTransaction().getOutputs() )
               outputs.add(new TransactionOutputImpl(output.getValue(),output.getScript()));
            break;
         case SIGHASH_SINGLE:
            // Now remove all outputs with higher index than this input (don't copy those)
            // (maybe this assumes each input will have exactly one output in these
            // kinds of transactions?) Also blank out lower outputs, so scripts
            // and value can change on those, essentially only locking this single input
            // and the assumed related output at the same index.
            int inputIndex = getTransaction().getInputs().indexOf(this);
            if ( inputIndex >= getTransaction().getOutputs().size() )
               throw new VerificationException("calculating hash type SIGHASH_SINGLE, but not enough outputs: "+inputIndex+" vs. "+getTransaction().getOutputs().size());
            for ( int i=0; i<inputIndex; i++ ) // Copy over the preceding outputs blanked out
               outputs.add(new TransactionOutputImpl(-1,null));
            TransactionOutput pairedOutput = getTransaction().getOutputs().get(inputIndex); // Copy output at same index
            outputs.add(new TransactionOutputImpl(pairedOutput.getValue(),pairedOutput.getScript()));
            break;
         case SIGHASH_NONE:
            // Copy no outputs here, any spending is allowed
            break;
      }
      // Now create the transaction copy with the modified inputs and outputs and calculate hash with
      // the type added
      TransactionImpl txCopy = new TransactionImpl(
            inputs,outputs,transaction.getLockTime(),new byte[] {},transaction.getVersion());
      byte[] hash = txCopy.calculateHash(new byte[] { (byte)type.getValue(), 0, 0, 0});
      // Return the hash of this specially created transaction with the type added
      // We have to reverse this signature hash back to original "Bitcoin" order.
      return ArraysUtil.reverse(hash);
   }

   @Override
   public String toString()
   {
      return "In (seq "+sequence+") from "+BtcUtil.hexOut(claimedTransactionHash)+"/"+claimedOutputIndex;
   }
}

