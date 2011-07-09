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
 * Filters can be used to get transactions from Blocks and BlockChains, without
 * getting all transactions. Potentially Blocks will later contain hundreds of
 * thousands of transactions, not all would be required for certain operations.
 * This filter is a way to return only the transactions which match certain
 * criteria. It can be used to filter already stored transactions, or filter
 * incoming transactions.<br>
 * Same filter implementations are comparable to eachother, which is used to determine if
 * the list of transactions returned by one filter is potentially larger than 
 * some other filter (of same class) or not. One filter is "higher" than another
 * if it returns a superset of transactions, equal to the other if they always
 * return the exact same results, "lower" if it returns a subset of transactions,
 * and they throw a <code>NotComparableException</code> if neither is true. This
 * means that this is not a total ordering!<br>
 */
public interface TransactionFilter extends Comparable<TransactionFilter>
{
   /**
    * This method is invoked to actually filter the transactions.<br>
    * Note: depending on the container imlpementation the supplied transactions
    * might not be all transactions that belong to a specific container, 
    * but it is guaranteed that if it is somehow
    * pre-filtered, that filtering did not take away any results from this filtering.
    * In other words any pre-filter applied was "greater or equal" to this filter.
    * Note that the transactions might not have a Block associated with them if
    * they are not yet incorporated into a Block.
    * @param transactions The list of transactions. Transactions are not guaranteed
    * to be in any specific order, or that the belong to the same block, or any block
    * for that matter.
    * @return The list of transactions after the filtering, input order preserved.
    */
   List<Transaction> getFilteredTransactions(List<Transaction> transactions);
}


