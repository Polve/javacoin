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

package hu.netmind.bitcoin.block.bdb;

import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * A secondary key creator which uses tuple serialization without using a class catalog.
 * It also assumes that the key and the database object are not needed to create the key.
 * @author Robert Brautigam
 */
public abstract class TupleSecondaryKeyCreator<K,V> implements SecondaryKeyCreator
{
   private TupleBinding<K> keyBinding;
   private TupleBinding<V> dataBinding;

   public TupleSecondaryKeyCreator(TupleBinding<K> keyBinding, TupleBinding<V> dataBinding)
   {
      this.keyBinding=keyBinding;
      this.dataBinding=dataBinding;
   }

   /**
    * Implement this method to return a secondary key to the given data object.
    * @return The key create, or null if no key was created.
    */
   public abstract K createSecondaryKey(V data);

   @Override
   public boolean createSecondaryKey(SecondaryDatabase secondary, DatabaseEntry key,
         DatabaseEntry data, DatabaseEntry result)
   {
      K resultKey = createSecondaryKey(dataBinding.entryToObject(data));
      if ( resultKey != null )
      {
         keyBinding.objectToEntry(resultKey,result);
         return true;
      }
      return false;
   }
}

