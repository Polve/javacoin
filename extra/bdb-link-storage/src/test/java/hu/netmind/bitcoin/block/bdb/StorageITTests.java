/**
 * Copyright (C) 2011 NetMind Consulting Bt.
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
package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.StandardBitcoinFactory;
import hu.netmind.bitcoin.block.StorageProvider;
import hu.netmind.bitcoin.block.TestSuiteFactory;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Robert Brautigam
 */
@Test
public class StorageITTests {

  private static Logger logger = LoggerFactory.getLogger(StorageITTests.class);

  private StorageProvider<BDBChainLinkStorage> createProvider() {
    return new StorageProvider<BDBChainLinkStorage>() {
      @Override
      public BDBChainLinkStorage newStorage() {
        BitcoinFactory bitcoinFactory;
        try {
          bitcoinFactory = new StandardBitcoinFactory(new ScriptFactoryImpl(null));
        } catch (BitCoinException ex) {
          logger.error("Cant instantiate StandardBitcoinFactory: " + ex.getMessage(), ex);
          return null;
        }
        BDBChainLinkStorage storage = new BDBChainLinkStorage(bitcoinFactory);
        storage.setDbPath("target/bitcoin-db");
        storage.init();
        return storage;
      }

      @Override
      public void closeStorage(BDBChainLinkStorage storage) {
        if (storage != null) {
          storage.close();
        }
      }

      @Override
      public void cleanStorage() {
        File dbFile = new File("target/bitcoin-db");
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
