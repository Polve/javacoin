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

package hu.netmind.bitcoin;

/**
 * A single input definition for a transaction. An input references exactly
 * one output of a previous transaction (in another Block usually), and provides
 * all the information necessary to prove to third parties that the claim for
 * that output is legitimate.
 * @author Robert Brautigam
 */
public interface TransactionInput
{
   enum SignatureHashType
   {
      SIGHASH_ALL,
      SIGHASH_NONE,
      SIGHASH_SINGLE,
      SIGHASH_ANYONECANPAY
   };

   /**
    * Get the transaction this input is a part of.
    */
   Transaction getTransaction();

   /**
    * Get the transaction output this input refers to.
    * @return The output this input claims to use, or null if this is
    * a "coinbase" (money generated).
    */
   TransactionOutput getClaimedOutput();

   /**
    * Get the script fragment that confirms that the referenced output can be
    * spent. This script should prepend the script in the referenced output.
    */
   ScriptFragment getSignatureScript();

   /**
    * The sequence number of this input. If the owner transaction is not yet locked it is possible
    * to update this input by announcing a transaction input with higher sequence number.
    */
   long getSequence();

   /**
    * Provide a hash for this transaction input using the transaction it is in as a context suitable
    * for signing this input. A signature created out of this hash is used to prove that this input
    * can use the output it referenced. Note: this is done most of the time, although signatures as
    * such might not required, depending on the script used by the output referenced.
    * @param type The type of signature to generate.
    * @param subscript The byte array to use instead of the <i>output</i> script (which is used
    * by default if this parameter is null). Note: a subscript is not really a script, it
    * is a result of three transformations done before hashing:  removing signatures (constants), 
    * splitting up based on code separators, and removing code separators. These cases are used 
    * only by "special" scripts.
    * @return The signature compatible with BitCoin.
    */
   byte[] getSignatureHash(SignatureHashType type, ScriptFragment subscript)
      throws BitCoinException;
}

