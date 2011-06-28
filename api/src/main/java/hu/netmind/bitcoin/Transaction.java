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

import java.util.List;

/**
 * A Transaction represents a transfer of BitCoin money. There are
 * two types of transactions:
 * <ul>
 *    <li>A "coinbase", which means money is created, and not just transferred.
 * The network agrees how much money can be "created" this way, with the maximum
 * amount limited to 21 million BTC. This serves as an incentive for Miners, as well
 * distributes the money among Miners.</li>
 *    <li>A "normal" transaction which transfers money from one or more input transactions
 * to an output. The transaction is then allowed to spend the combined amount of the
 * inputs and to define as many outputs as needed.</li>
 * </ul>
 * Usually Blocks contain one "coinbase" transaction, and 0 or more "normal" transactions.
 * Each transaction may specify an output sum of less than the input sums, in which case
 * the difference can be claimed by any Miner who successfully incorporates the transaction
 * into a Block. Normally however, the money to be transferred does not exactly match the
 * sum of the input transactions, in this case one of the outputs is
 * actually the sender itself (this is called the "change").
 * @author Robert Brautigam
 */
public interface Transaction
{
   enum SignatureHashType
   {
      SIGHASH_ALL,
      SIGHASH_NONE,
      SIGHASH_SINGLE,
      SIGHASH_ANYONECANPAY
   };

   /**
    * Get the block in which this transaction resides. Note that the same transaction content
    * can be in different blocks also (two miners creating a competing block at the same time), but
    * each transaction will be a separate instance, and hence have a separate parent block.
    * @return The block this transaction is in, or null if transaction is not yet part of any blocks.
    */
   Block getBlock();

   /**
    * Get the all the transaction input specifications. These are the sources
    * where the money is coming from. 
    */
   List<TransactionInput> getInputs();

   /**
    * Get the output definitions for this transaction. These are the specificiations
    * how the money is to be used, or in most of the cases, who should receive
    * the amount (or parts of it) provided by the inputs.
    */
   List<TransactionOutput> getOutputs();

   /**
    * Get block number or timestamp at which this transaction becomes locked. Locked means
    * miners would no longer accept any updates to the transaction, and it's time to include
    * the transaction in a block.
    * @return The lock time. If this number is smaller than 500.000.000 then it is a block number,
    * otherwise it is the time in millis when the lock will engage. It is 0 then the transaction is locked.
    */
   long getLockTime();

   /**
    * Provide a hash of this transaction applicable to a given input 
    * suitable for signature of that input.
    * @param type The type of signature to generate.
    * @param txIn The input to generate the hash for. Depending on the signature type
    * this input may get special treatment compared to the other inputs.
    * @param subscript The byte array to use instead of the <i>output</i> script (which is used
    * by default if this parameter is null). Note: a subscript is not really a script, it
    * is a result of three transformations done before hashing:  removing signatures (constants), 
    * splitting up based on code separators, and removing code separators. These cases are used 
    * only by "special" scripts.
    * @param block Which script block to include in the hash. By default this should be 0
    * (include full output script). It is different from 0 only when called from a script
    * itself which has blocks.
    * @return The signature compatible with BitCoin.
    */
   byte[] getSignatureHash(SignatureHashType type, TransactionInput txIn, byte[] subscript);
}



