/**
 * Copyright (C) 2012 nibbles.it
 *
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
package it.nibbles.javacoin.storage.bdb;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.ProdnetBitcoinFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Alessandro Polverini
 */
@Test
public class StorageITTests {

  private static Logger logger = LoggerFactory.getLogger(StorageITTests.class);

  private StorageProvider<BDBStorage> createProvider() {
    return new StorageProvider<BDBStorage>() {
      @Override
      public BDBStorage newStorage() {
        BitcoinFactory bitcoinFactory;
        try {
          bitcoinFactory = new ProdnetBitcoinFactory(new ScriptFactoryImpl(null));
        } catch (BitcoinException ex) {
          logger.error("Cant instantiate StandardBitcoinFactory: " + ex.getMessage(), ex);
          return null;
        }
        BDBStorage storage = new BDBStorage(bitcoinFactory);
        storage.setDbPath("target/test-db");
        storage.setUseExplicitTransactions(false);
        storage.setDeferredWrite(true);
        storage.init();
        return storage;
      }

      @Override
      public void closeStorage(BDBStorage storage) {
        if (storage != null) {
          storage.close();
        }
      }

      @Override
      public void cleanStorage() {
        File dbFile = new File("target/test-db");
        if (dbFile.isDirectory()) {
          File[] files = dbFile.listFiles();
          for (File file : files) {
            file.delete();
          }
        }
      }
    };
  }

  @Factory
  public Object[] createTestSuite() {
    return TestSuiteFactory.getTestSuite(createProvider());
  }
}
