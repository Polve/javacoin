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

import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import org.testng.annotations.Test;
import org.testng.annotations.Factory;
import org.testng.Assert;
import java.io.File;

/**
 * @author Robert Brautigam
 */
@Test
public class SimpleSerializingStorageTests
{
   private StorageProvider<SimpleSerializingStorage> createProvider()
   {
      return new StorageProvider<SimpleSerializingStorage>() {
         public SimpleSerializingStorage newStorage()
         {
            SimpleSerializingStorage storage = new SimpleSerializingStorage();
            storage.init("target/bitcoin-db.ser");
            return storage;
         }

         public void closeStorage(SimpleSerializingStorage storage)
         {
            if ( storage != null )
               storage.close();
         }

         public void cleanStorage()
         {
            File dbFile = new File("target/bitcoin-db.ser");
            if ( dbFile.isFile() )
               dbFile.delete();
         }
      };
   }

   @Factory
   public Object[] createTestSuite()
   {
      return TestSuiteFactory.getTestSuite(createProvider());
   }

}

