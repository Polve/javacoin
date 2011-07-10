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
import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that is capable of returning the filter expression it represents
 * in a "disjunctive normal form", that is, as a one-level structure with
 * "and"s and "or"s, with possible "not"s applied to individual filters only
 * (not to groups).
 * @author Robert Brautigam
 */
public abstract class DNFFilter implements TransactionFilter
{
   private static Logger logger = LoggerFactory.getLogger(DNFFilter.class);

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
      // Get the DNFs of both filters (this and the parameter)
      DNFFilter otherFilter = ((DNFFilter) filter).getDNF();
      DNFFilter thisFilter = getDNF();
      // Calculate the two cases separately
      boolean lessOrEqual = isLessOrEqual(thisFilter,otherFilter);
      boolean greaterOrEqual = isLessOrEqual(otherFilter,thisFilter);
      // Determine return number
      if ( lessOrEqual && greaterOrEqual )
         return 0; // Equals (would select the exact same transactions)
      else if ( lessOrEqual )
         return -1; // This filter is stricter (would select a subset of transactions)
      else if ( greaterOrEqual )
         return 1; // This filter is laxer (would select a superset of transactions)
      else
         throw new NotComparableException("filter "+toString()+" is not comparable with filter: "+filter.toString());
   }

   /**
    * Get all the filters out of an aggregate filter, or return the filter in a list.
    */
   private List<DNFFilter> getSubFilters(DNFFilter filter)
   {
      List<DNFFilter> result = new ArrayList<DNFFilter>();
      if ( filter instanceof AggregateFilter )
         result.addAll(((AggregateFilter) filter).getFilters());
      else
         result.add(filter);
      return result;
   }

   /**
    * Determine whether the first argument DNF is stricter (or equal) than the second.
    * @return True if the left filter is stricter or equal, false otherwise (including
    * the case when the two filters are not comparable).
    */
   private boolean isLessOrEqual(DNFFilter leftFilter, DNFFilter rightFilter)
   {
      // The left filter is less or equal if for each conjunction we can find
      // an equal or laxer conjunction on the right side.
      List<DNFFilter> leftConjunctions = getSubFilters(leftFilter);
      List<DNFFilter> rightConjunctions = getSubFilters(rightFilter);
      for ( DNFFilter leftConjunction : leftConjunctions )
      {
         List<DNFFilter> leftConjunctionPrimitives = getSubFilters(leftConjunction);
         // So try to find a right conjunction that is laxer
         boolean rightConjunctionFound = false;
         for ( DNFFilter rightConjunction : rightConjunctions )
         {
            // Now the right conjunction is greater if all the primitive
            // propositions in it have at least equal or more strict counterpart
            // on the left conjuction side
            List<DNFFilter> rightConjunctionPrimitives = getSubFilters(rightConjunction);
            boolean allRightConjunctionPrimitivesConvered = true;
            for ( DNFFilter rightConjunctionPrimitive : rightConjunctionPrimitives )
            {
               // So try to find a left conjunction primitive that is less or equal
               boolean leftConjunctionPrimitiveFound = false;
               for ( DNFFilter leftConjunctionPrimitive : leftConjunctionPrimitives )
               {
                  try
                  {
                     if ( leftConjunctionPrimitive.compareTo(rightConjunctionPrimitive) <= 0 )
                     {
                        leftConjunctionPrimitiveFound = true;
                        break;
                     }
                  } catch ( Exception e ) {
                     logger.debug("primitives not comparable "+leftConjunctionPrimitive+" vs. "+
                           rightConjunctionPrimitive,e);
                  }
               }
               if ( ! leftConjunctionPrimitiveFound )
               {
                  // This right conjunction primitive didn't have a counterpart
                  allRightConjunctionPrimitivesConvered = false;
                  break; // Don't need to check the rest
               }
            }
            // If all right conjunction primitives are covered, then ou full right conjection
            // is greater or equal
            if ( allRightConjunctionPrimitivesConvered )
            {
               rightConjunctionFound = true;
               break; // Don't need to search the other ones
            }
         }
         // If we're through an no pairing was found, we do not know whether
         // the left side is less or equal for sure, so return false
         if ( ! rightConjunctionFound )
            return false;
      }
      // If the algorithm did not exit that means the necessary pairing was done so
      // left is less or equal
      return true;
   }

}

