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

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.block.ProdnetBitcoinFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Alessandro Polverini
 */
@Test
public class StorageITTests
{

   private StorageProvider<MysqlStorage> createProvider()
   {
      return new StorageProvider<MysqlStorage>()
      {
         private final static String jdbcUrl = "jdbc:mysql://localhost/javacoin_test";
         private final static String jdbcUser = "javacoin";
         private final static String jdbcPw = "pw";
         private DataSource datasource;

         protected MysqlStorage newStorageInstance()
         {
            try
            {
               if (datasource == null)
                  datasource = DatasourceUtils.getMysqlDatasource(jdbcUrl, jdbcUser, jdbcPw);
               MysqlStorage myStorage = new MysqlStorage(new ProdnetBitcoinFactory(new ScriptFactoryImpl(null)));
               myStorage.setDataSource(datasource);
               myStorage.init();
               return myStorage;
            } catch (BitcoinException ex)
            {
               throw new RuntimeException("Can't instance JDBC storage: " + ex.getMessage(), ex);
            } catch (ClassNotFoundException ex)
            {
               throw new RuntimeException("Mysql Driver class not found", ex);
            }
         }

         @Override
         public MysqlStorage newStorage()
         {
            return newStorageInstance();
         }

         @Override
         public void closeStorage(MysqlStorage storage)
         {
            // TODO... ?
         }

         @Override
         public void cleanStorage()
         {
            MysqlStorage myStorage = newStorageInstance();
            myStorage.removeDatabase();
            closeStorage(myStorage);
         }
      };
   }

   @Factory
   public Object[] createTestSuite()
   {
      return TestSuiteFactory.getTestSuite(createProvider());
   }
}
