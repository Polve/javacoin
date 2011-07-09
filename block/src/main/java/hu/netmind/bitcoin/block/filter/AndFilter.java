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

import hu.netmind.bitcoin.Transaction;
import java.util.List;
import java.util.ArrayList;

/**
 * This filter aggregates with an "and" operator.
 * @author Robert Brautigam
 */
public class AndFilter extends AggregateFilter
{
   /**
    * Returns the transaction filtered if at least one filter
    * wants it filtered.
    */
   public boolean isFiltered(Transaction transaction)
   {
      for ( DNFFilter filter : getFilters() )
         if ( filter.isFiltered(transaction) )
            return true;
      return false;
   }

   /**
    * Convert this AND aggregate to DNF. This expression represents
    * a1 and a2 and ... an. DNF(a1 and a2 and ... an) ==
    * DNF(DNF(a1) and DNF(a2) and ... DNF(an)) == 
    * DNF( (b11 or ... b1m1) and (b21 or .. b2m2) and ... ) ==
    * (b11 and b21 and ... bn1) or ( b11 and b21 and ... bn2) or ... .
    * So all combinations of all the conjuctive clauses (in all sub-dnfs).
    */
   public DNFFilter getDNF()
   {
      // First get all the conjuctive clauses from all the sub-DNFs.
      List<List<DNFFilter>> conjuctiveClauses = new ArrayList<List<DNFFilter>>();
      for ( DNFFilter subFilter : getFilters() )
      {
         DNFFilter dnfSubFilter = subFilter.getDNF();
         List<DNFFilter> conjuctiveSubClauses = new ArrayList<DNFFilter>();
         conjuctiveClauses.add(conjuctiveSubClauses);
         if ( dnfSubFilter instanceof OrFilter )
         {
            // Sub DNF is an OR filter, so include its conjuctive clauses
            // into the bucket
            for ( DNFFilter subsub : ((OrFilter)dnfSubFilter).getFilters() )
               conjuctiveSubClauses.add(subsub);
         }
         else
         {
            // This is either a conjuctive clause itself, or a literal
            // so add it
            conjuctiveSubClauses.add(dnfSubFilter);
         }
      }
      // Now we must produce all combinations of all conjuction clauses which
      // then will be included in the disjunction. This will be done by constructing
      // a multi-digit counter where each digit will have a maximum value corresponding
      // to the size of one of the conjuctive clause list sizes.
      OrFilter result = new OrFilter();
      int[] counter = new int[conjuctiveClauses.size()]; // Start with 0-0-...-0
      boolean hasMore = counter.length > 0;
      while ( hasMore )
      {
         // Create combination according to counter, and also flatten the conjuctions
         // into a single conjunction
         AndFilter conjunction = new AndFilter();
         for ( int i=0; i<conjuctiveClauses.size(); i++ )
         {
            DNFFilter clause = conjuctiveClauses.get(i).get(counter[i]);
            if ( clause instanceof AndFilter )
            {
               // Clause is an AND clause, so move its literals to top level
               for ( DNFFilter filter : ((AndFilter)clause).getFilters() )
                  conjunction.addFilter(filter);
            }
            else
            {
               // This should be a literal, so simply add
               conjunction.addFilter(clause);
            }
         }
         result.addFilter(conjunction);
         // Now increment the counter
         boolean carryOver = true;
         for ( int i=conjuctiveClauses.size()-1; i>=0; i-- )
         {
            counter[i]++;
            if ( counter[i] >= conjuctiveClauses.get(i).size() )
               counter[i]=0; // Overflow, carryover stays, we try next digit
            else
               carryOver = false; // Successful increment without carryover
         }
         if ( carryOver )
            hasMore = false; // Can not increment anymore, so all options exhausted
      }
      // We are ready, return the disjunction
      return result;
   }
}

