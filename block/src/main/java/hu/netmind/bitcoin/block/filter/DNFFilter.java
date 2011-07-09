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

package hu.netmind.bitcoin.block.filter;

import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.NotComparableException;
import java.util.List;
import java.util.Iterator;

/**
 * A filter that is capable of returning the filter expression it represents
 * in a "disjunctive normal form", that is, as a one-level structure with
 * "and"s and "or"s, with possible "not"s applied to individual filters only
 * (not to groups).
 * @author Robert Brautigam
 */
public abstract class DNFFilter implements TransactionFilter
{
   /**
    * Return the disjunctive normal form of the filter expression.
    * Note if this is a "primitive" filter, the filter itself is returned
    * (this is the default implementation of this method).
    * @return The DNF filter which is after the call independent from the
    * original filters (no change will propagate from the original filters).
    */
   public DNFFilter getDNF()
   {
      return this;
   }

   /**
    * Override this method to provide filtering functionality.
    * @param transaction The transaction to examine.
    * @return True if transaction should be removed,
    * false if it should be kept in the list.
    */
   protected abstract boolean isFiltered(Transaction transaction);


   /**
    * Implementation of filtering by going through the transactions one-by-one
    * and calling the <code>isFiltered()</code> method. 
    */
   public void filterTransactions(List<Transaction> transactions)
   {
      Iterator<Transaction> transactionIterator = transactions.iterator();
      while ( transactionIterator.hasNext() )
      {
         if ( isFiltered(transactionIterator.next()) )
            transactionIterator.remove();
      }
   }

   /**
    * Compare two DNF capable filters.
    */
   public int compareTo(TransactionFilter filter)
   {
      // TODO
      throw new NotComparableException("not implemented");
   }

}

