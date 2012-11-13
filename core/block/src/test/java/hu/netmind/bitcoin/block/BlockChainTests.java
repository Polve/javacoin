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
import hu.netmind.bitcoin.BitcoinException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
@Test
public class BlockChainTests
{
   private static Logger logger = LoggerFactory.getLogger(BlockChainTests.class);

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
      throws VerificationException, BitcoinException
   {
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      // Check that construction works
      //BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
      BitcoinFactory factory = new TesterBitcoinFactory(genesisBlock, null);
      DummyStorage storage = new DummyStorage(factory, genesisBlock);
      BlockChainImpl chain = new BlockChainImpl(factory,storage,false);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testWrongGenesis()
      throws VerificationException, BitcoinException
   {
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      // Check that construction fails with other genesis
      Block differentBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 02;");
      //BlockChainImpl chain = new BlockChainImpl(differentBlock,storage,null,false);
      BitcoinFactory factory = new TesterBitcoinFactory(differentBlock, null);
      DummyStorage storage = new DummyStorage(factory,genesisBlock);
      BlockChainImpl chain = new BlockChainImpl(factory,storage,false);
   }

   public void testGenesisInitialization()
      throws VerificationException, BitcoinException
   {
      // Construct block chain with genesis block
      Block genesisBlock = BlockMock.createBlock(
            "block 1234567 1 1b0404cb 00 010203 01;");
      //BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
      BitcoinFactory testerFactory = new TesterBitcoinFactory(genesisBlock,null);
      // Initialize storage with nothing
      DummyStorage storage = new DummyStorage(testerFactory);
      BlockChainImpl chain = new BlockChainImpl(testerFactory, storage,false);
      // Verify genesis block
      Assert.assertEquals(storage.getNewLinks().size(),1);
      BlockChainLink link = storage.getNewLinks().get(0);
      Assert.assertEquals(link.getHeight(), 0);
      Assert.assertEquals(link.getBlock(), genesisBlock);
      Assert.assertEquals(link.getBlock().getCreationTime(),1234567);
      Assert.assertEquals(link.getBlock().getNonce(),1);
      Assert.assertEquals(link.getBlock().getCompressedTarget(),0x1b0404cbl);
      Assert.assertEquals(link.getBlock().getPreviousBlockHash(),new byte[] { 0 });
      Assert.assertEquals(link.getBlock().getMerkleRoot(), new byte[] { 01, 02, 03 });
      Assert.assertEquals(link.getBlock().getHash(),new byte[] { 01 });
   }

   private DummyStorage testAddBlockTemplate(String chainBlocks, String newBlock, boolean scriptSuccess)
      throws BitcoinException
   {
      return testAddBlockTemplate(chainBlocks,newBlock,scriptSuccess,0);
   }

   private DummyStorage testAddBlockTemplate(String chainBlocks, String newBlock, boolean scriptSuccess,
         long blockOffset)
      throws BitcoinException
   {
      // Construct a block chain and storage
      long startTime = System.currentTimeMillis();
      long stopTime = System.currentTimeMillis();
      logger.debug("created storage, lasted: "+(stopTime-startTime)+" ms");
      // Construct chain
      List<Block> blocks = BlockMock.createBlocks(chainBlocks);
      BitcoinFactory factory = new TesterBitcoinFactory(blocks.get(0),createScriptFactory(scriptSuccess));
      DummyStorage storage = new DummyStorage(factory, blocks, blockOffset);
      BlockChainImpl chain = new BlockChainImpl(factory, storage, false);
      // Add the block
      Block block = BlockMock.createBlock(newBlock);
      startTime = System.currentTimeMillis();
      chain.addBlock(block);
      stopTime = System.currentTimeMillis();
      logger.debug("added block, lasted: "+(stopTime-startTime)+" ms");
      return storage;
   }

   public void testAddValidBlock()
      throws BitcoinException
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
            "      in 990101 0 999;"+
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
      Assert.assertEquals(newLink.getHeight(),2);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddInvalidBlock()
      throws BitcoinException
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
            "      in 990101 0 999;"+
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

   public void testAddExisting()
      throws BitcoinException
   {
      // Block will be ignore, shouldn't throw exception
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
            "      in 990101 0 999;"+
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

   // This test is now not useful anymore: orphan blocks are not persisted in the storage
//   public void testAddIndirectlyOrphan()
//      throws BitcoinException
//   {
//      DummyStorage storage = testAddBlockTemplate(
//            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
//            "   tx 1234567 990101 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "block 1234568 1 1b0404cb 99 010203 02;"+ // Next block, 99 doesn't exist
//            "   tx 123458 990102 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
//            "      in 990101 0 999;"+
//            "      out 2000000;"+
//            "      out 3000000;",
//
//            "block 1234569 1 1b0404cb 02 010203 03;"+ // prev hash 02 should be orphan
//            "   tx 1234569 990104 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "   tx 1234580 990105 false;"+ // Using some money
//            "      in 990103 1 999;"+
//            "      out 2000000;",true);
//      Assert.assertEquals(storage.getNewLinks().size(),1);
//      BlockChainLink newLink = storage.getNewLinks().get(0);
//      Assert.assertEquals(newLink.isOrphan(),true);
//   }

   @Test(expectedExceptions = VerificationException.class)
   public void testAddMoreDifficultyBlock()
      throws BitcoinException
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
            "      in 990101 0 999;"+
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
      throws BitcoinException
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
            "      in 990101 0 999;"+
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
      throws BitcoinException
   {
      // Create a block chain that has 2016 blocks, so the next one
      // will trigger a re-calculation. Make all blocks 9 minutes apart,
      // which means the target at end should be recalculated to be 1.1
      // times as much to arrive at the target 10 minutes.
      // This is however not true, because the timespan is calculated
      // for 2015 blocks, not for 2016 blocks in the difficulty calculation.
      // So there is an "error" of 1/2016. The endresult should be thus:
      // <original target>*(9/10)*(2015/2016)
      StringBuilder blocks = new StringBuilder();
      for ( int i=0; i<2016; i++ )
         blocks.append(
            "block "+(1000000+(i*9*60*1000))+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Now test with block which has adjusted difficulty. It should be
      // accepted with no problems
      testAddBlockTemplate(blocks.toString(),
            "block "+(1000000+(2016*9*60*1000))+" 1 1b039d74 2016 010203 2017;"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testAddWrongDifficultyChangedBlock()
      throws BitcoinException
   {
      // Just as testAddDifficultyChangedBlock() but wrong block to add
      StringBuilder blocks = new StringBuilder();
      for ( int i=0; i<2016; i++ )
         blocks.append(
            "block "+(1000000+(i*9*60*1000))+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Now test with block which has adjusted difficulty-1, so it shouldn't
      // be accepted
      testAddBlockTemplate(blocks.toString(),
            "block "+(1000000+(2016*9*60*1000))+" 1 1b039d73 2016 010203 2017;"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;",true);
   }

   public void testAddDifficultyExtremeSmallBlock()
      throws BitcoinException
   {
      // Leave extremely small timeslots, and check that difficulty is not
      // cranked up all the way. The rate of change should max out at 4 times
      // the original value. That means target should be:
      // <original target>/4
      StringBuilder blocks = new StringBuilder();
      for ( int i=0; i<2016; i++ )
         blocks.append(
            "block "+(1000000+(i*1*60*1000))+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Now test with block which has adjusted difficulty. It should be
      // accepted with no problems
      testAddBlockTemplate(blocks.toString(),
            "block "+(1000000+(2016*1*60*1000))+" 1 1b010132 2016 010203 2017;"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;",true);
   }

   public void testRightMedianBlock()
      throws BitcoinException
   {
      StringBuilder blocks = new StringBuilder();
      long[] times = new long[] { 100, 10, 20, 30, 40, 50, 200, 300, 400, 500, 1000 };
      for ( int i=0; i<11; i++ )
         blocks.append(
            "block "+times[i]+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Add block with exactly the median+1, which should be ok
      testAddBlockTemplate(blocks.toString(),
            "block 101 1 1b0404cb 11 010203 12;"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testWrongMedianBlock()
      throws BitcoinException
   {
      StringBuilder blocks = new StringBuilder();
      long[] times = new long[] { 100, 10, 20, 30, 40, 50, 200, 300, 400, 500, 1000 };
      for ( int i=0; i<11; i++ )
         blocks.append(
            "block "+times[i]+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Add block with exactly the median should fail
      testAddBlockTemplate(blocks.toString(),
            "block 100 1 1b0404cb 11 010203 12;"+
            "   tx 1234567 990101 true;"+ // Tx doesn't matter here
            "      in 00 -1 999;"+
            "      out 5000000;",true);
   }

   public void testValidKnownHashes()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 1122334455;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 1122334455 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 5555555555;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testInvalidKnownHashes()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 1122334455;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "block 1234568 1 1b0404cb 1122334455 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 4444444444;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testInvalidTransactionAdd()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false Invalidtransaction;"+ // Invalid transaction
            "      in 990103 1 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testOutputIsMoreThanInput()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Invalid transaction
            "      in 990103 0 999;"+
            "      out 2000001;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testOutputsAreMoreThanInput()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Invalid transaction
            "      in 990103 0 999;"+
            "      out 2000000;"+
            "      out 1;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testNoTransactionReferredExists()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Invalid transaction
            "      in 990106 0 999;"+ // Here is the wrong reference
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testReferredTransactionExistsInSideBranch()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;"+
            "block 1234568 1 1b0404cb 01 010203 06;"+ // Next block (side branch)
            "   tx 123458 990106 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990107 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990107 false;"+ // Invalid transaction
            "      in 990106 0 999;"+ // Here is the wrong reference
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testReferToNonExistingOutput()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 2 999;"+
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testReferToYoungCoinbase()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990102 0 999;"+ // From coinbase
            "      out 5000000;",true);
   }

   public void testUseOldCoinbaseOutput()
      throws BitcoinException
   {
      // Construct a chain with sufficient length to use the coinbase (which is
      // 100 blocks, about 16 hours)
      StringBuilder blocks = new StringBuilder();
      for ( int i=0; i<101; i++ )
         blocks.append(
            "block "+(1000000+(i*1*60*1000))+" 1 1b0404cb "+i+" 010203 "+(i+1)+";"+
            "   tx 1234567 99"+i+" true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;");
      // Now test to add the block which claims the coinbase output of genesis
      testAddBlockTemplate(blocks.toString(),
            "block "+(1000000+(100*1*60*1000))+" 1 1b0404cb 101 010203 102;"+
            "   tx 1234567 99102 true;"+ 
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 99103 false;"+ // Using some money
            "      in 990 0 999;"+ // From coinbase of genesis
            "      out 5000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testAddTransactionWithWrongScript()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 0 999;"+
            "      out 2000000;",false); // Let script return false
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testSpendAlreadySpentOutput()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;"+
            "block 1234568 1 1b0404cb 02 010203 06;"+ // Next block (spends previous)
            "   tx 123458 990106 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990107 false;"+ // A normal tx spending money from 1st block
            "      in 990103 0 999;"+
            "      in 990103 1 999;"+
            "      out 5000000;",

            "block 1234569 1 1b0404cb 06 010203 07;"+ // This block will come after 2nd
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Spend 1st blocks output
            "      in 990103 0 999;"+ 
            "      out 2000000;",true);
   }

   public void testSpendAlreadySpentOutputOnOtherBranch()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;"+
            "block 1234568 1 1b0404cb 02 010203 06;"+ // Next block (spends previous)
            "   tx 123458 990106 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234568 990107 false;"+ // A normal tx spending money from 1st block
            "      in 990103 0 999;"+
            "      in 990103 1 999;"+
            "      out 5000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+ // This block will be parallel to 2nd block
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;"+
            "   tx 1234580 990105 false;"+ // Spend 1st transaction
            "      in 990103 0 999;"+ 
            "      out 2000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testCreateMoreMoneyThanAllowed()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 2000000000;"+
            "      out 3000000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000001;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 0 999;"+
            "      out 2000000000;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testUseMoreMoneyThanAvailable()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 2000000000;"+
            "      out 3000000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000002;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 0 999;"+
            "      out 1999999999;",true);
   }

   public void testUseFeesInCoinbase()
      throws BitcoinException
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
            "      in 990101 0 999;"+
            "      out 2000000;"+
            "      out 3000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000001;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 0 999;"+
            "      out 1999999;",true);
   }

   @Test(expectedExceptions=VerificationException.class)
   public void testUseMoreMoneyThanAvailableAfterMiningChange()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000000;",

            "block 1234569 1 1b0404cb 01 010203 02;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000001;",true,209999);
   }

   public void testUseExactMiningAmountRightBeforeChange()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;",

            "block 1234569 1 1b0404cb 01 010203 02;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 5000000;",true,209998);
   }

   // These tests are no more valid because we do not guarantee to keep orphans
//   public void testActivateOrphanChain()
//      throws BitcoinException
//   {
//      DummyStorage storage = testAddBlockTemplate(
//            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
//            "   tx 1234567 990101 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "block 1234569 1 1b0404cb 99 010203 02;"+ // Orphan
//            "   tx 123459 990102 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;",
//
//            "block 1234568 1 1b0404cb 01 010203 99;"+ // Block between the two
//            "   tx 1234568 990104 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;",true);
//      BlockChainLink orphanLink = storage.getLink(new byte[] { 02 });
//      Assert.assertNotNull(orphanLink);
//      Assert.assertFalse(orphanLink.isOrphan());
//   }
//
//   public void testActivateInvalidOrphan()
//      throws BitcoinException
//   {
//      DummyStorage storage = testAddBlockTemplate(
//            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
//            "   tx 1234567 990101 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "block 1234569 1 1b0404cb 99 010203 02;"+ // Orphan
//            "   tx 123459 990102 true WrongTransaction;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;",
//
//            "block 1234568 1 1b0404cb 01 010203 99;"+ // Block between the two
//            "   tx 1234568 990104 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;",true);
//      BlockChainLink orphanLink = storage.getLink(new byte[] { 02 });
//      Assert.assertNotNull(orphanLink);
//      Assert.assertTrue(orphanLink.isOrphan());
//   }
//
//   public void testAddOrphan()
//      throws BitcoinException
//   {
//      DummyStorage storage = testAddBlockTemplate(
//            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
//            "   tx 1234567 990101 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
//            "   tx 123458 990102 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
//            "      in 990101 0 999;"+
//            "      out 2000000;"+
//            "      out 3000000;",
//
//            "block 1234569 1 1b0404cb 99 010203 03;"+ // prev hash 99 doesn't exist
//            "   tx 1234569 990104 true;"+ // Coinbase
//            "      in 00 -1 999;"+
//            "      out 5000000;"+
//            "   tx 1234580 990105 false;"+ // Using some money
//            "      in 990103 1 999;"+
//            "      out 2000000;",true);
//      Assert.assertEquals(storage.getNewLinks().size(),1);
//      BlockChainLink newLink = storage.getNewLinks().get(0);
//      Assert.assertEquals(newLink.isOrphan(),true);
//   }

   public void testReferToSameBlock()
      throws BitcoinException
   {
      testAddBlockTemplate(
            "block 1234567 1 1b0404cb 00 010203 01;"+ // Genesis block
            "   tx 1234567 990101 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "block 1234568 1 1b0404cb 01 010203 02;"+ // Next block
            "   tx 123458 990102 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "   tx 1234568 990103 false;"+ // A normal tx spending money from genesis
            "      in 990101 0 999;"+
            "      out 200000000;"+
            "      out 300000000;",

            "block 1234569 1 1b0404cb 02 010203 03;"+
            "   tx 1234569 990104 true;"+ // Coinbase
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "   tx 1234580 990105 false;"+ // Using some money
            "      in 990103 1 999;"+
            "      out 200000000;"+
            "   tx 1234581 990106 false;"+ // Using some money from the previous tx in same block
            "      in 990105 0 999;"+
            "      out 200000000;",true);
   }
   
   public void testNormalCommonBlock()
      throws BitcoinException
   {
      List<Block> blocks = BlockMock.createBlocks(
         "block 1234567 1 1b0404cb 00 010203 01;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;"
         + "block 1234567 1 1b0404cb 01 010203 02;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;"
         + "block 1234567 1 1b0404cb 02 010203 03;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;"
         + "block 1234567 1 1b0404cb 03 010203 04;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;"
         + "block 1234567 1 1b0404cb 02 010203 05;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;"
         + "block 1234567 1 1b0404cb 05 010203 06;"
         + "   tx 1234567 990101 true;"
         + "      in 00 -1 999;"
         + "      out 500000000;");
      BitcoinFactory factory = new TesterBitcoinFactory(blocks.get(0),createScriptFactory(true));
      DummyStorage storage = new DummyStorage(factory, blocks,0);
      // Construct chain
      //BlockChainImpl chain = new BlockChainImpl(storage.getGenesisLink().getBlock(),
      //      storage,createScriptFactory(true),false);
      BlockChainImpl chain = new BlockChainImpl(factory,storage,false);

      // Do the check
      Block commonBlock = chain.getCommonBlock(storage.getLink(new byte[] { 06 }).getBlock(),
            storage.getLink(new byte[] { 04 }).getBlock());
      Assert.assertNotNull(commonBlock);
      Assert.assertTrue(Arrays.equals(commonBlock.getHash(),new byte[] { 02 }));
   }

   public void testNoCommonBlock()
      throws BitcoinException
   {
      List<Block> blocks = BlockMock.createBlocks(
            "block 1234567 1 1b0404cb 00 010203 01;"+
            "   tx 1234567 990101 true;"+
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "block 1234567 1 1b0404cb 01 010203 02;"+
            "   tx 1234567 990101 true;"+
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "block 1234567 1 1b0404cb 02 010203 03;"+
            "   tx 1234567 990101 true;"+
            "      in 00 -1 999;"+
            "      out 500000000;"+
            "block 1234567 1 1b0404cb 05 010203 06;"+
            "   tx 1234567 990101 true;"+
            "      in 00 -1 999;"+
            "      out 500000000;"
               );
//               ),0);
      BitcoinFactory factory = new TesterBitcoinFactory(blocks.get(0),createScriptFactory(true));
      DummyStorage storage = new DummyStorage(factory, blocks, 0);
      // Construct chain
//      BlockChainImpl chain = new BlockChainImpl(storage.getGenesisLink().getBlock(),
//            storage,createScriptFactory(true),false);
      BlockChainImpl chain = new BlockChainImpl(factory,storage,false);
      // Do the check
      Block commonBlock = chain.getCommonBlock(storage.getLink(new byte[] { 06 }).getBlock(),
            storage.getLink(new byte[] { 03 }).getBlock());
      Assert.assertNull(commonBlock);
   }
}
