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

import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BitCoinException;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.Assert;
import java.io.File;

/**
 * @author Robert Brautigam
 */
@Test
public class StorageITTests
{
   BDBChainLinkStorage storage = null;

   @BeforeMethod
   protected void setupStorage()
   {
      File dbFile = new File("target/bitcoin-db");
      if ( dbFile.isDirectory() )
      {
         File[] files = dbFile.listFiles();
         for ( File file : files )
            file.delete();
      }
      storage = new BDBChainLinkStorage(new ScriptFactoryImpl(null));
      storage.setDbPath("target/bitcoin-db");
      storage.init();
   }

   @AfterMethod
   protected void closeStorage()
   {
      if ( storage != null )
         storage.close();
   }

   public void testGenesisStoreRecall()
      throws BitCoinException
   {
      // Store
      BlockChainLink genesisLink = new BlockChainLink(BlockImpl.MAIN_GENESIS,
            new Difficulty(),1,false);
      storage.addLink(genesisLink);
      // Recall
      BlockChainLink readLink = storage.getLink(BlockImpl.MAIN_GENESIS.getHash());
      // Check link data
      Assert.assertEquals(readLink.getHeight(),1);
      Assert.assertFalse(readLink.isOrphan());
      // Check block integrity
      Block readBlock = readLink.getBlock();
      readBlock.validate();
      BlockImpl copy = new BlockImpl(readBlock.getTransactions(),
            readBlock.getCreationTime(),readBlock.getNonce(),readBlock.getCompressedTarget(),
            readBlock.getPreviousBlockHash(),readBlock.getMerkleRoot());
      copy.validate();
   }
}

