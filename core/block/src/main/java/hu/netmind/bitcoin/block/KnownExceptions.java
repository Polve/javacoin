/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains known exceptions to the validation rules. Some blocks and transactions
 * do not conform to rules usually created later in response to some misuse or
 * attack. In these cases the old blocks still must remain valid, so exceptions
 * can be created for those old known objects.
 * @author Robert Brautigam
 */
public class KnownExceptions
{
   private static Logger logger = LoggerFactory.getLogger(KnownExceptions.class);
   private Map<BigInteger,Set<ValidationCategory>> exceptions;

   public KnownExceptions()
   {
      readExceptions();
   }

   private void readExceptions()
   {
      exceptions = new HashMap<BigInteger,Set<ValidationCategory>>();
      try
      {
         ResourceBundle bundle = ResourceBundle.getBundle("validation-exceptions");
         Enumeration<String> keys = bundle.getKeys();
         while ( keys.hasMoreElements() )
         {
            // Read
            String key = keys.nextElement();
            String value = bundle.getString(key);
            StringTokenizer tokenizer = new StringTokenizer(value,",");
            String hashString = tokenizer.nextToken();
            String category = tokenizer.nextToken();
            // Place in map
            BigInteger hash = new BigInteger(hashString.substring(2),16);
            Set<ValidationCategory> categories = exceptions.get(hash);
            if ( categories == null )
            {
               categories = new HashSet<ValidationCategory>();
               exceptions.put(hash,categories);
            }
            categories.add(ValidationCategory.valueOf(category));
         }
      } catch ( Exception e ) {
         logger.error("validation exceptions could not be read",e);
      }
   }

   /**
    * Returns whether the given object is exempt from the given
    * category of checks.
    * @param obj The transaction or block in question.
    * @param category The name of validation or validations to check.
    * @return Returns true, if the object would probably fail the given validation
    * but should be nonetheless considered safe.
    */
   public boolean isExempt(Hashable obj, ValidationCategory category)
   {
      return isExempt(obj.getHash(), category);
   }
   public boolean isExempt(byte[] hash, ValidationCategory category)
   {
      Set<ValidationCategory> categories = exceptions.get(new BigInteger(1,hash));
      return (categories!=null) && (categories.contains(category));
   }
}

