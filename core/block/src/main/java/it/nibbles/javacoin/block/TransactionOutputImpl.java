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

import java.util.List;
import it.nibbles.javacoin.ScriptFragment;
import it.nibbles.javacoin.Transaction;
import it.nibbles.javacoin.TransactionInput;
import it.nibbles.javacoin.TransactionOutput;

/**
 * @author Robert Brautigam
 */
public class TransactionOutputImpl implements TransactionOutput
{
   private long value;
   private ScriptFragment script;
   private Transaction transaction; // Filled add-time
   private int index; // Filled add-time

   public TransactionOutputImpl(long value, ScriptFragment script)
   {
      this.value=value;
      this.script=script;
   }

   public long getValue()
   {
      return value;
   }
   public ScriptFragment getScript()
   {
      return script;
   }
   public Transaction getTransaction()
   {
      return transaction;
   }

   /**
    * Get the output index among all outputs for this output.
    */
   public int getIndex()
   {
      return index;
   }

   void setIndex(int index)
   {
      this.index=index;
   }
   void setTransaction(Transaction transaction)
   {
      this.transaction=transaction;
   }

   public String toString()
   {
      return "Out #"+index+": "+value+" units";
   }
}

