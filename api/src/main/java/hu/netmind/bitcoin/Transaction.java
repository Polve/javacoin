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
    * Return the hash of this transaction.
    */
   byte[] getHash();
}



