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
   private transient Transaction transaction; // Parent is filled out runtime

   public TransactionInputImpl(TransactionOutput claimedOutput, 
         ScriptFragment signatureScript, long sequence)
   {
      this.claimedOutput=claimedOutput;
      this.signatureScript=signatureScript;
      this.sequence=sequence;
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

   void setTransaction(Transaction transaction)
   {
      this.transaction=transaction;
   }
}

