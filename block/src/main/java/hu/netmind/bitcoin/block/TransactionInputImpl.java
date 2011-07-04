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
import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;

/**
 * @author Robert Brautigam
 */
public class TransactionInputImpl implements TransactionInput
{
   private TransactionOutput claimedOutput;
   private ScriptFragment signatureScript;
   private long sequence;
   private transient TransactionImpl transaction; // Parent is filled out runtime

   public TransactionInputImpl(TransactionOutput claimedOutput, 
         ScriptFragment signatureScript, long sequence)
   {
      this.claimedOutput=claimedOutput;
      this.signatureScript=signatureScript;
      this.sequence=sequence;
   }

   TransactionInputImpl copy()
   {
      return TransactionInputImpl(claimedOutput,signatureScript,sequence);
   }

   public TransactionOutput getClaimedOutput()
   {
      return claimedOutput;
   }
   public ScriptFragment getSignatureScript()
   {
      return signatureScript;
   }
   public long getSequence()
   {
      return sequence;
   }
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
   public byte[] getSignatureHash(SignatureHashType type, ScriptFragment subscript)
   {
      // Create a copy of the whole tx and then set the subscript to fulfill base requirements
      // of signature hash from BitCoin wiki
      TransactionImpl txCopy = transaction.copy();
      TransactionInput txInputCopy = txCopy.getInputs().get(transaction.getInputs().indexOf(this));
      // Set all but the copy of this input to empty script
      for ( TransactionInput input : txCopy.getInputs() )
         input.setSignatureScript(null);
      txInputCopy.setSignatureScript(subscript);
      // Handle various hash type cases
      switch ( type )
      {
         case SIGHASH_NONE:
            // Allow updates to other inputs (hash with sequence set to 0)
            for ( TransactionInput input : txCopy.getInputs() )
               if ( input != txInputCopy )
                  input.setSequence(0);
            // Allow any spending of the inputs (do not hash in outputs)
            txCopy.getOutputs().clear();
         case SIGHASH_ALL:
            // Nothing to do (every input and output is hashed in at current version/sequence)
            break;
      }
   }

}

