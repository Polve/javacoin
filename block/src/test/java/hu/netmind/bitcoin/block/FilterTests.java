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

import hu.netmind.bitcoin.block.filter.*;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.NotComparableException;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.easymock.EasyMock;
import java.util.List;

/**
 * @author Robert Brautigam
 */
@Test
public class FilterTests
{
   public void testPrimitiveDNFItself()
   {
      Assert.assertEquals(createNamedFilter("A").getDNF().toString(),"A");
   }

   public void testOneAndClauseDNF()
   {
      Assert.assertEquals(createNamedFilter("A AND B").getDNF().toString(),"(A AND B)");
   }

   public void testOneAndClauseDNFMultiLiteral()
   {
      Assert.assertEquals(createNamedFilter("A AND B AND C AND D").getDNF().toString(),"(A AND B AND C AND D)");
   }

   public void testOneOrClauseDNF()
   {
      Assert.assertEquals(createNamedFilter("A OR B").getDNF().toString(),"(A OR B)");
   }

   public void testOneOrClauseDNFMultiLiteral()
   {
      Assert.assertEquals(createNamedFilter("A OR B OR C OR D").getDNF().toString(),"(A OR B OR C OR D)");
   }

   public void testSubOrFiltersDNF()
   {
      Assert.assertEquals(createNamedFilter("A AND (B OR C)").getDNF().toString(),"((A AND B) OR (A AND C))");
   }

   public void testSubAndFiltersDNF()
   {
      Assert.assertEquals(createNamedFilter("A OR (B AND C)").getDNF().toString(),"(A OR (B AND C))");
   }

   public void testMultiSubDNF()
   {
      Assert.assertEquals(createNamedFilter("A OR (B AND (C OR D))").getDNF().toString(),"(A OR (B AND C) OR (B AND D))");
   }

   public void testRandomLargeExpressionDNF()
   {
      Assert.assertEquals(createNamedFilter("(A AND (B OR C)) AND ((G OR H) AND (C OR (D OR E)))").getDNF().toString(),
            "((A AND B AND G AND C) OR (A AND B AND G AND D) OR (A AND B AND G AND E) OR "+
            "(A AND B AND H AND C) OR (A AND B AND H AND D) OR (A AND B AND H AND E) OR "+
            "(A AND C AND G AND C) OR (A AND C AND G AND D) OR (A AND C AND G AND E) OR "+
            "(A AND C AND H AND C) OR (A AND C AND H AND D) OR (A AND C AND H AND E))");
   }

   public void testComparisonPrimitivesLess()
   {
      Assert.assertTrue(createNamedFilter("A1").compareTo(createNamedFilter("A2")) < 0);
   }

   public void testComparisonPrimitivesGreater()
   {
      Assert.assertTrue(createNamedFilter("A3").compareTo(createNamedFilter("A2")) > 0);
   }

   public void testComparisonPrimitivesEqual()
   {
      Assert.assertTrue(createNamedFilter("A3").compareTo(createNamedFilter("A3")) == 0);
   }

   /**
    * Creates filter expressions using OrFilter, AndFilter and
    * NamedFilter. Syntax is like: ((A OR B) AND C).
    */
   private DNFFilter createNamedFilter(String expression)
   {
      int nextIndex = 0;
      DNFFilter result = null;
      boolean resultIsSub = false;
      while ( nextIndex < expression.length() )
      {
         char firstChar = expression.charAt(nextIndex);
         if ( firstChar == ' ' )
         {
            // This is either a " AND " or " OR "
            if ( expression.substring(nextIndex).startsWith(" AND ") )
            {
               // This is an AND
               if ( (result instanceof AggregateFilter) && (!resultIsSub) )
               {
                  // Only check that it is the same aggregation, nothing to do
                  Assert.assertTrue(result instanceof AndFilter,"A clause was first OR then tried to switch to AND at: "+nextIndex);
               }
               else
               {
                  // Result was a primitive until now
                  AggregateFilter aggregate = new AndFilter();
                  aggregate.addFilter(result);
                  result = aggregate;
                  resultIsSub=false;
               }
               nextIndex += 5;
            }
            else if ( expression.substring(nextIndex).startsWith(" OR ") )
            {
               // This is an OR
               if ( (result instanceof AggregateFilter) && (!resultIsSub) )
               {
                  // Only check that it is the same aggregation, nothing to do
                  Assert.assertTrue(result instanceof OrFilter,"A clause was first AND then tried to switch to OR at: "+nextIndex);
               }
               else
               {
                  // Result was a primitive until now
                  AggregateFilter aggregate = new OrFilter();
                  aggregate.addFilter(result);
                  result = aggregate;
                  resultIsSub=false;
               }
               nextIndex += 4;
            }
         }
         else if ( Character.isLetter(firstChar) && Character.isUpperCase(firstChar) )
         {
            // Get to next character, if it is a number, then include it in the name
            int value = 0;
            nextIndex++;
            if ( nextIndex < expression.length() )
            {
               char nextChar = expression.charAt(nextIndex);
               if ( Character.isDigit(nextChar) )
               {
                  value = Integer.parseInt(""+nextChar);
                  nextIndex++;
               }
            }
            // This is a named filter, so create it
            DNFFilter literal = new NamedFilter(""+firstChar,value);
            if ( result == null )
            {
               result = literal;
            }
            else
            {
               Assert.assertTrue((result instanceof AggregateFilter) && (!resultIsSub),
                     "Two clauses without and aggregation at index: "+nextIndex);
               ((AggregateFilter)result).addFilter(literal);
            }
         }
         else if ( firstChar == '(' )
         {
            // Create the sub-expression
            int closingBrace = findClosingBrace(expression,nextIndex);
            DNFFilter subFilter = createNamedFilter(expression.substring(nextIndex+1,closingBrace));
            nextIndex = closingBrace+1;
            // Save it
            if ( (result instanceof AggregateFilter) && (!resultIsSub) )
            {
               ((AggregateFilter)result).addFilter(subFilter);
            }
            else
            {
               Assert.assertNull(result,"There was a literal but no aggregation when subexpression starts at: "+nextIndex);
               result = subFilter;
               resultIsSub = true;
            }
         }
      }
      // Now return the result, but first check if we achieved the target string
      if ( (result instanceof AggregateFilter) && (!resultIsSub) )
         Assert.assertEquals(result.toString(),"("+expression+")");
      else
         Assert.assertEquals(result.toString(),expression);
      return result;
   }

   private int findClosingBrace(String expression, int fromIndex)
   {
      int level = 1;
      while ( level > 0 )
      {
         fromIndex++;
         if ( expression.charAt(fromIndex) == '(' )
            level++;
         if ( expression.charAt(fromIndex) == ')' )
            level--;
      }
      return fromIndex;
   }

   /**
    * A named filter has a name, and a number. Named filters can be compared if
    * they have the same name, and in that case they are compared by the number they have.
    */
   private static class NamedFilter extends DNFFilter
   {
      private String name;
      private int value;

      public NamedFilter(String name, int value)
      {
         this.name=name;
         this.value=value;
      }

      public boolean isFiltered(Transaction tx)
      {
         return true; // Doesn't matter
      }

      public String toString()
      {
         if ( value == 0 )
            return name;
         return name+value;
      }

      public int compareTo(TransactionFilter filter)
      {
         if ( ! name.equals(((NamedFilter) filter).name) )
            throw new NotComparableException("Two named filters have different names, so they are not comparable");
         return value - ((NamedFilter) filter).value;
      }
   }
}

