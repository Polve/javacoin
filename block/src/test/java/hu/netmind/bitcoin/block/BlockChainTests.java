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

package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.TransactionInput;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.easymock.EasyMock;
import org.easymock.Capture;
import org.testng.Assert;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
@Test
public class BlockChainTests
{
   private ScriptFactory createScriptFactory(boolean successful)
      throws ScriptException
   {
      Script script = EasyMock.createMock(Script.class);
      EasyMock.expect(script.execute((TransactionInput)EasyMock.anyObject())).
         andReturn(successful).anyTimes();
      EasyMock.replay(script);
      ScriptFactory scriptFactory = EasyMock.createMock(ScriptFactory.class);
      EasyMock.expect(scriptFactory.createScript(
               (ScriptFragment) EasyMock.anyObject(), (ScriptFragment) EasyMock.anyObject())).
         andReturn(script).anyTimes();
      EasyMock.replay(scriptFactory);
      return scriptFactory;
   }

   public void testGenesisOk()
      throws VerificationException
   {
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      DummyStorage storage = new DummyStorage(genesisBlock);
      // Check that construction works
      BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testWrongGenesis()
      throws VerificationException
   {
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      DummyStorage storage = new DummyStorage(genesisBlock);
      // Check that construction fails with other genesis
      Block differentBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 02;");
      BlockChainImpl chain = new BlockChainImpl(differentBlock,storage,null,false);
   }

   public void testGenesisInitialization()
      throws VerificationException
   {
      // Initialize storage with nothing
      DummyStorage storage = new DummyStorage();
      // Construct block chain with genesis block
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
      // Verify genesis block
      Assert.assertEquals(storage.getNewLinks().size(),1);
      BlockChainLink link = storage.getNewLinks().get(0);
      Assert.assertEquals(link.getHeight(), 1);
      Assert.assertEquals(link.getBlock(), genesisBlock);
      Assert.assertFalse(link.isOrphan());
      Assert.assertEquals(link.getBlock().getCreationTime(),1234567);
      Assert.assertEquals(link.getBlock().getNonce(),1);
      Assert.assertEquals(link.getBlock().getCompressedTarget(),0x1b0404cbl);
      Assert.assertEquals(link.getBlock().getPreviousBlockHash(),new byte[] { 0 });
      Assert.assertEquals(link.getBlock().getMerkleRoot(), new byte[] { 01, 02, 03 });
      Assert.assertEquals(link.getBlock().getHash(),new byte[] { 01 });
   }

   private DummyStorage testAddBlockTemplate(String chainBlocks, String newBlock, boolean scriptSuccess)
      throws BitCoinException
   {
      // Construct a block chain and storage
      DummyStorage storage = new DummyStorage(BlockMock.createBlocks(chainBlocks));
      // Construct chain
      BlockChainImpl chain = new BlockChainImpl(storage.getGenesisLink().getBlock(),
            storage,createScriptFactory(scriptSuccess),false);
      // Add the block
      chain.addBlock(BlockMock.createBlock(newBlock));
      return storage;
   }

   public void testAddValidBlock()
      throws BitCoinException
   {
      DummyStorage storage = testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
      Assert.assertEquals(storage.getNewLinks().size(),1);
      BlockChainLink newLink = storage.getNewLinks().get(0);
      Assert.assertEquals(newLink.getHeight(),3);
      Assert.assertEquals(newLink.isOrphan(),false);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddInvalidBlock()
      throws BitCoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03 Notvalid;"+ // Invalid here!
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddExisting()
      throws BitCoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 02;"+ // 02 Already exists!
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   public void testAddOrphan()
      throws BitCoinException
   {
      DummyStorage storage = testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 99 010203 03;"+ // prev hash 99 doesn't exist
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
      Assert.assertEquals(storage.getNewLinks().size(),1);
      BlockChainLink newLink = storage.getNewLinks().get(0);
      Assert.assertEquals(newLink.isOrphan(),true);
   }

   public void testAddIndirectlyOrphan()
      throws BitCoinException
   {
      DummyStorage storage = testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 99 010203 02;"+ // Next block, 99 doesn't exist
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+ // prev hash 02 should be orphan
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
      Assert.assertEquals(storage.getNewLinks().size(),1);
      BlockChainLink newLink = storage.getNewLinks().get(0);
      Assert.assertEquals(newLink.isOrphan(),true);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddMoreDifficultyBlock()
      throws BitCoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0304cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddLessDifficultyBlock()
      throws BitCoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 1 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0504cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   public void testAddDifficultyChangedBlock()
      throws BitCoinException
   {
   }

   public void testAddDifficultyExtremeSmallBlock()
      throws BitCoinException
   {
   }
}



