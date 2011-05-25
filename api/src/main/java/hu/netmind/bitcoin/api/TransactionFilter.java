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

package hu.netmind.bitcoin.api;

import java.util.List;

/**
 * Filters can be used to get transactions from Blocks and BlockChains, without
 * getting all transactions. Potentially Blocks will later contain hundreds of
 * thousands of transactions, not all would be required for certain operations.
 * This filter is a way to return only the transactions which match certain
 * criteria. It can be used to filter already stored transactions, and maybe
 * later also used to filter block data received through network.<br>
 * Same filter implementations are comparable to eachother, which is used to determine if
 * the list of transactions returned by one filter is potentially larger than 
 * some other filter (of same class) or not. One filter is "higher" than another
 * if it returns a superset of transactions, equal to the other if they always
 * return the exact same results, "lower" if it returns a subset of transactions,
 * and they throw a <code>NotComparableException</code> if neither is true. This
 * means that this is not a total ordering!<br>
 * Filtering works in two stages. First the filter is asked whether it is
 * interested in the contents of a Block (based on the Block data alone). If yes, 
 * the transactions of the Block are filtered in the second pass.
 */
public interface TransactionFilter extends Comparable<TransactionFilter>
{
   /**
    * Decide whether the contents of a Block should be filtered, or
    * discarded right away. Filtering based purely on the meta-data
    * in the Block is much more efficient, because otherwise all the
    * transactions need to be potentially downloaded. This method should
    * not access the transactions in the Block at all.
    * @param block The Block to filter. Note that null is a valid value
    * here if the filter should apply to transactions not yet in a Block.
    * @return True if the block's transactions should go to the filtering
    * second stage. False to reject all of the transactions in a Block right 
    * away.
    */
   boolean isFiltered(Block block);

   /**
    * If the Block is allowed into the second stage by <code>isFiltered()</code>,
    * then this method is invoked to actually filter the transactions.<br>
    * Note: depending on the Block implementation this might not be all the
    * transactions in the Block, but it is guaranteed that if it is somehow
    * pre-filtered, that filter did not take away any results from this filtering.
    * In other words any pre-filter applied was "greater or equal" to this filter.
    * Note that the transactions might not have a Block associated with them if
    * they are not yet incorporated into a Block.
    * @param transactions The list of transactions in the Block to filter.
    * @return The list of transactions after the filtering.
    */
   List<Transaction> getFilteredTransactions(List<Transaction> transactions);
}


