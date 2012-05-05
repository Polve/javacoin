/**
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package hu.netmind.bitcoin.block.jdbc;

import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import org.testng.annotations.Test;
import org.testng.annotations.Factory;

/**
 * @author Alessandro Polverini
 */
@Test
public class StorageITTests
{

   private StorageProvider<JdbcChainLinkStorage> createProvider()
   {
      return new StorageProvider<JdbcChainLinkStorage>()
      {

         protected JdbcChainLinkStorage getStorageInstance()
         {
            JdbcChainLinkStorage storage = new JdbcChainLinkStorage(new ScriptFactoryImpl(null), false);
            storage.setDriverClassName("com.mysql.jdbc.Driver");
            storage.setJdbcUrl("jdbc:mysql://localhost/javacoin_test");
            storage.setDbUser("javacoin");
            storage.setDbPassword("pw");
            return storage;
         }

         @Override
         public JdbcChainLinkStorage newStorage()
         {
            JdbcChainLinkStorage storage = getStorageInstance();
            storage.init();
            return storage;
         }

         @Override
         public void closeStorage(JdbcChainLinkStorage storage)
         {
            if (storage != null)
               storage.close();
         }

         @Override
         public void cleanStorage()
         {
            JdbcChainLinkStorage storage = getStorageInstance();
            storage.removeDatabase();
         }
      };
   }

   @Factory
   public Object[] createTestSuite()
   {
      return TestSuiteFactory.getTestSuite(createProvider());
   }
}
