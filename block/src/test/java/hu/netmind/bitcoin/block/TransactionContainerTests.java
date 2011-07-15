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

import org.testng.annotations.Test;
import org.testng.Assert;
import org.easymock.EasyMock;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionFilter;

/**
 * @author Robert Brautigam
 */
@Test
public class TransactionContainerTests
{
   public void testPrefilterConcept()
   {
      ListPrefilteredContainer container = new ListPrefilteredContainer();
      // Set prefilter
      container.setPreFilter(new LockTimeFilter(10));
      // Create the list of transactions to add
      List<Transaction> transactions = new ArrayList<Transaction>();
      Transaction tx1 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx1.getLockTime()).andReturn(5l);
      EasyMock.replay(tx1);
      Transaction tx2 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx2.getLockTime()).andReturn(11l);
      EasyMock.replay(tx2);
      Transaction tx3 = EasyMock.createMock(Transaction.class);
      EasyMock.expect(tx3.getLockTime()).andReturn(8l);
      EasyMock.replay(tx3);
      transactions.add(tx1);
      transactions.add(tx2);
      transactions.add(tx3);
      // Put it into the container
      container.addTransactions(transactions);
      // Check that tx2 was filtered out
      Assert.assertEquals(container.getStoredTransactions().size(),2);
      Assert.assertEquals(container.getStoredTransactions().get(0),tx1);
      Assert.assertEquals(container.getStoredTransactions().get(1),tx3);
   }

   /**
    * A dummy filter to test, it just removes all transactions above a certain lock time.
    */
   public static class LockTimeFilter implements TransactionFilter
   {
      private long lockTime;

      public LockTimeFilter(long lockTime)
      {
         this.lockTime=lockTime;
      }

      public void filterTransactions(List<Transaction> transactions)
      {
         Iterator<Transaction> transactionsIterator = transactions.iterator();
         while ( transactionsIterator.hasNext() )
         {
            Transaction transaction = transactionsIterator.next();
            if ( transaction.getLockTime() > lockTime )
               transactionsIterator.remove();
         }
      }

      public int compareTo(TransactionFilter other)
      {
         // Higher locktime actually removes less transactions
         return (int) (((LockTimeFilter) other).lockTime - lockTime);
      }
   }

   /**
    * A list-based implementation of the prefiltered container.
    */
   public static class ListPrefilteredContainer extends PrefilteredTransactionContainer
   {
      private List<Transaction> transactions = new ArrayList<Transaction>();

      protected void addStoredTransactions(List<Transaction> transactions)
      {
         this.transactions.addAll(transactions);
      }

      protected List<Transaction> getStoredTransactions()
      {
         return transactions;
      }
   }

}


