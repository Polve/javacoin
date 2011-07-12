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

import hu.netmind.bitcoin.TransactionContainer;
import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.NotAvailableException;
import java.util.List;
import java.util.ArrayList;

/**
 * This transaction container implements filtering behaviour, but also
 * has a pre-filtering logic. The pre-filtering applies a filter when
 * adding transactions to this container, then when transactions are returned
 * it is always checked that the filter specified to get the transactions is
 * equal or stricter than the pre-filter. This guarantees that the caller
 * doesn't see that not all transactions are actually maintained.
 * @author Robert Brautigam
 */
public abstract class PrefilteredTransactionContainer implements TransactionContainer
{
   private TransactionFilter preFilter = null;

   /**
    * Set the pre-filter on the container. Note that modifying the pre-filter at any
    * point of time after some transactions were already added might then fail to
    * detect that some transactions were filtered out previously which would fit
    * the new pre-filter. Only change pre-filter if you know there was no the previously
    * filtered transactions would be also filtered by the new pre-filter.
    */
   public void setPreFilter(TransactionFilter preFilter)
   {
      this.preFilter=preFilter;
   }

   public TransactionFilter getPreFilter()
   {
      return preFilter;
   }

   /**
    * Add a transaction to this container. This transaction will be only "stored" in
    * the container if it passes the pre-filter.
    */
   public void addTransaction(Transaction transaction)
   {
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(transaction);
      addTransactions(transactions);
   }

   /**
    * Add transactions to this container. Only those transactions will be added
    * which pass the filter.
    */
   public void addTransactions(List<Transaction> transactions)
   {
      List<Transaction> workingList = new ArrayList<Transaction>(transactions);
      if ( preFilter != null )
         preFilter.filterTransactions(workingList);
      addStoredTransactions(workingList);
   }

   /**
    * Get the list of all transactions in this container.
    * @return The list of transactions in the order it is kept in the
    * container.
    * @throws NotAvailableException If the requested transactions are not available
    * for any reason.
    */
   public List<Transaction> getTransactions()
      throws NotAvailableException
   {
      return getTransactions(null);
   }

   /**
    * Get the filtered list of transactions from this container.
    * @param filter The filter to apply to the transactions.
    * @return The list of transactions after applying the filter.
    * @throws NotAvailableException If the requested transactions are not available
    * for any reason.
    */
   public List<Transaction> getTransactions(TransactionFilter filter)
      throws NotAvailableException
   {
      if ( (filter==null) && (preFilter==null ) ) // No pre-filter, and get all transactions
      {
         return getStoredTransactions(); // Return all
      } 
      else if ( filter == null ) // No filter, but there is a pre-filter, not allowed
      {
         throw new NotAvailableException("tried to get all transactions from a container, but is prefiltered with: "+preFilter);
      }
      else
      {
         // If there is a pre-filter, then use it to determine that the given filter
         // is stricter (or equal)
         if ( (preFilter!=null) && (preFilter.compareTo(filter) < 0) )
         {
            // Prefilter is there and is stricter
            throw new NotAvailableException("prefilter given in container is stricter than query filter: "+filter+" vs. prefilter: "+preFilter); 
         }
         // Now just filter the stored transactions
         List<Transaction> workingList = new ArrayList<Transaction>(getStoredTransactions());
         filter.filterTransactions(workingList);
         return workingList;
      }
   }

   /**
    * Add a transaction to the container after pre-filtering. This needs to be 
    * implemented by the container.
    */
   protected abstract void addStoredTransactions(List<Transaction> transaction);

   /**
    * Get all the transactions that are currently stored in this container.
    */
   protected abstract List<Transaction> getStoredTransactions();
}

