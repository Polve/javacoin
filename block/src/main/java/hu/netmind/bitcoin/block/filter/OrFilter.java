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
 * This filter aggregates with an "or" operator.
 * @author Robert Brautigam
 */
public class OrFilter extends AggregateFilter
{
   /**
    * Returns the transaction filtered only if all filters
    * want to filter it.
    */
   public boolean isFiltered(Transaction transaction)
   {
      for ( DNFFilter filter : getFilters() )
         if ( ! filter.isFiltered(transaction) )
            return false;
      return true;
   }

   /**
    * Convert this OR aggregate to a DNF. This expression represents:
    * a1 or a2 or ... an. So DNF(a1 or a2 or ... an) == DNF(a1) or
    * DNF(a2) or ... DNF(an). Where all: DNF(ai) == (bi1 or bi2 or .. bini).
    * This method creates a top level or to contain
    * all conjuction clauses from the second level (all "bij"s).
    */
   public DNFFilter getDNF()
   {
      // The result will be always a top level OR
      OrFilter result = new OrFilter();
      // Go through the DNFs of the child filters and add them together
      for ( DNFFilter filter : getFilters() )
      {
         DNFFilter dnfFilter = filter.getDNF();
         if ( dnfFilter instanceof OrFilter )
         {
            // The child is an "or" filter, so add it's contents to top "or" filter
            // to prevent multiple levels of "or"
            for ( DNFFilter subFilter : ((OrFilter)dnfFilter).getFilters() )
               result.addFilter(subFilter);
         }
         else
         {
            // This is either a literal, or literals in an "and" clause, so simply add it
            // to the DNF
            result.addFilter(dnfFilter);
         }
      }
      return result;
   }

   public String toString()
   {
      StringBuilder builder = new StringBuilder("(");
      for ( int i=0; i<getFilters().size(); i++ )
      {
         if ( i > 0 )
            builder.append(" OR ");
         builder.append(getFilters().get(i).toString());
      }
      builder.append(")");
      return builder.toString();
   }
}

