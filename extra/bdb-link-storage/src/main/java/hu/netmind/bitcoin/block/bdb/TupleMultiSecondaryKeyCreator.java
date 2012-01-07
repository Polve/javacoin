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

import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import java.util.List;
import java.util.Set;

/**
 * A multiple secondary key creator which uses tuple serialization without using a class catalog.
 * It also assumes that the key and the database object are not needed to create the key.
 * @author Robert Brautigam
 */
public abstract class TupleMultiSecondaryKeyCreator<K,V> implements SecondaryMultiKeyCreator
{
   private TupleBinding<K> keyBinding;
   private TupleBinding<V> dataBinding;

   public TupleMultiSecondaryKeyCreator(TupleBinding<K> keyBinding, TupleBinding<V> dataBinding)
   {
      this.keyBinding=keyBinding;
      this.dataBinding=dataBinding;
   }

   /**
    * Implement this method to return zero or more secondary keys to the given data object.
    * @return The keys created.
    */
   public abstract List<K> createSecondaryKeys(V data);

   @Override
   public void createSecondaryKeys(SecondaryDatabase secondary, DatabaseEntry key,
         DatabaseEntry data, Set<DatabaseEntry> results)
   {
      List<K> resultKeys = createSecondaryKeys(dataBinding.entryToObject(data));
      for ( K resultKey : resultKeys )
      {
         DatabaseEntry entry = new DatabaseEntry();
         keyBinding.objectToEntry(resultKey,entry);
         results.add(entry);
      }
   }
}

