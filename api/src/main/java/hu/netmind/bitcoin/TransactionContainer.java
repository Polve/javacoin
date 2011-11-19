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
 * This class can hold an ordered list of transactions with the ability to filter on them.
 * A container can also discard some transactions it is holding based on rules employed by
 * the implementation automatically anytime. In this case not all transactions will be 
 * available anymore. The container does however need to be aware what transactions were
 * discarded, and the methods react with <code>NotAvailableException</code> if the container
 * thinks that the return value is modified because of descarding transactions.
 */
public interface TransactionContainer
{
   /**
    * Get the list of all transactions in this container.
    * @return The list of transactions in the order it is kept in the
    * container.
    * @throws NotAvailableException If the requested transactions are not available
    * for any reason.
    */
   List<Transaction> getTransactions()
      throws NotAvailableException;

   /**
    * List all the available transactions from this container.
    * @return The list of the transactions this container still remembers.
    */
   List<Transaction> getAvailableTransactions();

   /**
    * Get the filtered list of transactions from this container.
    * @param filter The filter to apply to the transactions.
    * @return The list of transactions after applying the filter.
    * @throws NotAvailableException If the requested transactions are not available
    * for any reason.
    */
   List<Transaction> getTransactions(TransactionFilter filter)
      throws NotAvailableException;


}
