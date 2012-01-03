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

package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import org.testng.annotations.Test;
import org.testng.annotations.Factory;
import org.testng.Assert;
import java.io.File;

/**
 * @author Robert Brautigam
 */
@Test
public class StorageITTests
{
   private StorageProvider<BDBChainLinkStorage> createProvider()
   {
      return new StorageProvider<BDBChainLinkStorage>() {
         public BDBChainLinkStorage newStorage()
         {
            BDBChainLinkStorage storage = new BDBChainLinkStorage(new ScriptFactoryImpl(null));
            storage.setDbPath("target/bitcoin-db");
            storage.init();
            return storage;
         }

         public void closeStorage(BDBChainLinkStorage storage)
         {
            if ( storage != null )
               storage.close();
         }

         public void cleanStorage()
         {
            File dbFile = new File("target/bitcoin-db");
            if ( dbFile.isDirectory() )
            {
               File[] files = dbFile.listFiles();
               for ( File file : files )
                  file.delete();
            }
         }
      };
   }

   @Factory
   public Object[] createTestSuite()
   {
      return TestSuiteFactory.getTestSuite(createProvider());
   }

}

