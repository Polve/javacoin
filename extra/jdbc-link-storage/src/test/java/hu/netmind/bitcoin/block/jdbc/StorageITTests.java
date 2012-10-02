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

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.block.StandardBitcoinFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

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
            try
            {
            JdbcChainLinkStorage storage = new JdbcChainLinkStorage(new StandardBitcoinFactory(new ScriptFactoryImpl(null)));
               storage.setDataSource(DatasourceUtils.getMysqlDatasource("jdbc:mysql://localhost/javacoin_test", "javacoin", "pw"));
            return storage;
            } catch (BitCoinException ex) {
               throw new RuntimeException("Can't instance JDBC storage: "+ex.getMessage(), ex);
            } catch (ClassNotFoundException ex)
            {
               throw new RuntimeException("Mysql Driver class not found", ex);
            }
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
