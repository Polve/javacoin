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

package it.nibbles.javacoin;

import java.io.Serializable;

/**
 * A single output of one transaction. The output does not directly refer
 * to a target address, but contains a script which decided how the funds
 * from the transaction may be used.
 * @author Robert Brautigam
 */
public interface TransactionOutput extends Serializable
{
   /**
    * Get the transaction this output is a part of.
    */
   Transaction getTransaction();

   /**
    * Get the index of this output among all outputs in the transaction.
    */
   int getIndex();

   /**
    * Get the value this output carries.
    */
   long getValue();

   /**
    * Get the script fragment to authorize spending. This fragment will
    * be prepended by the transaction input's script that wants to spend
    * this output. This script usually contains the public key of the recipient.
    */
   ScriptFragment getScript();
}

