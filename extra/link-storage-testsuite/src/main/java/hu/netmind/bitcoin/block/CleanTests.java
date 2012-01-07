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

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.Assert;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.Difficulty;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import java.math.BigDecimal;

/**
 * Tests requiring clean storage to run.
 * @author Robert Brautigam
 */
@Test
public class CleanTests<T extends BlockChainLinkStorage> extends InitializableStorageTests<T>
{
   private ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(null);
   private T storage = null;
   private Random rnd = new Random();

   @BeforeMethod
   public void setupStorage()
   {
      getProvider().cleanStorage();
      storage = getProvider().newStorage();
   }

   @AfterMethod
   public void closeStorage()
   {
      getProvider().closeStorage(storage);
   }

   public void testGenesisStoreRecall()
      throws BitCoinException
   {
      // Store
      BlockChainLink genesisLink = new BlockChainLink(BlockImpl.MAIN_GENESIS,
            new Difficulty(),BlockChainLink.ROOT_HEIGHT,false);
      storage.addLink(genesisLink);
      // Recall
      BlockChainLink readLink = storage.getLink(BlockImpl.MAIN_GENESIS.getHash());
      // Check link data
      Assert.assertEquals(readLink.getHeight(),BlockChainLink.ROOT_HEIGHT);
      Assert.assertFalse(readLink.isOrphan());
      // Check block integrity
      Assert.assertEquals(readLink.getBlock().getHash(),BlockImpl.MAIN_GENESIS.getHash());
      validateBlock(readLink.getBlock());
   }

   public void testAllAttributesCorrectlyRecalled()
      throws BitCoinException
   {
      List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
      List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
      List<Transaction> txs = new LinkedList<Transaction>();
      inputs.add(new TransactionInputImpl(new byte[] {
               0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },1,
               scriptFactory.createFragment(new byte[] { 0,1,2,3,4,5,6,7 }),44l));
      inputs.add(new TransactionInputImpl(new byte[] {
               1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },2,
               scriptFactory.createFragment(new byte[] { 2,1,2,3,4,5,6,7 }),45l));
      inputs.add(new TransactionInputImpl(new byte[] {
               3,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },5,
               scriptFactory.createFragment(new byte[] { -1,1,2,3,4,5,6,7 }),46l));
      outputs.add(new TransactionOutputImpl(100000,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,15 })));
      outputs.add(new TransactionOutputImpl(100001,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,15 })));
      txs.add(new TransactionImpl(inputs,outputs,1));
      inputs = new LinkedList<TransactionInputImpl>();
      inputs.add(new TransactionInputImpl(new byte[] {
               0,6,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },1,
               scriptFactory.createFragment(new byte[] { 0,1,2,3,6,5,6,7 }),47l));
      inputs.add(new TransactionInputImpl(new byte[] {
               1,1,7,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },2,
               scriptFactory.createFragment(new byte[] { 2,1,2,3,22,5,6,7 }),48l));
      outputs = new LinkedList<TransactionOutputImpl>();
      outputs.add(new TransactionOutputImpl(100002,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,17 })));
      txs.add(new TransactionImpl(inputs,outputs,2));
      BlockImpl block = new BlockImpl(txs,123456,654321,0x1b0404cbl, new byte[] {
               11,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 }, new byte[] {
               111,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 });
      BlockChainLink link = new BlockChainLink(block,new Difficulty(),BlockChainLink.ROOT_HEIGHT,false);
      // Store & Recall
      storage.addLink(link);
      BlockChainLink readLink = storage.getLink(link.getBlock().getHash());
      // Check link data
      Assert.assertEquals(readLink.getHeight(),BlockChainLink.ROOT_HEIGHT);
      Assert.assertFalse(readLink.isOrphan());
      // Check block integrity
      Assert.assertEquals(readLink.getBlock().getHash(),link.getBlock().getHash());
      validateBlock(readLink.getBlock());
   }

   public void testAllAttributesCorrectlyRecalledAfterRestart()
      throws BitCoinException
   {
      List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
      List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
      List<Transaction> txs = new LinkedList<Transaction>();
      inputs.add(new TransactionInputImpl(new byte[] {
               0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },1,
               scriptFactory.createFragment(new byte[] { 0,1,2,3,4,5,6,7 }),44l));
      inputs.add(new TransactionInputImpl(new byte[] {
               1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },2,
               scriptFactory.createFragment(new byte[] { 2,1,2,3,4,5,6,7 }),45l));
      inputs.add(new TransactionInputImpl(new byte[] {
               3,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },5,
               scriptFactory.createFragment(new byte[] { -1,1,2,3,4,5,6,7 }),46l));
      outputs.add(new TransactionOutputImpl(100000,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,15 })));
      outputs.add(new TransactionOutputImpl(100001,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,15 })));
      txs.add(new TransactionImpl(inputs,outputs,1));
      inputs = new LinkedList<TransactionInputImpl>();
      inputs.add(new TransactionInputImpl(new byte[] {
               0,6,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },1,
               scriptFactory.createFragment(new byte[] { 0,1,2,3,6,5,6,7 }),47l));
      inputs.add(new TransactionInputImpl(new byte[] {
               1,1,7,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 },2,
               scriptFactory.createFragment(new byte[] { 2,1,2,3,22,5,6,7 }),48l));
      outputs = new LinkedList<TransactionOutputImpl>();
      outputs.add(new TransactionOutputImpl(100002,scriptFactory.createFragment(
                  new byte[] { 10,11,12,13,14,17 })));
      txs.add(new TransactionImpl(inputs,outputs,2));
      BlockImpl block = new BlockImpl(txs,123456,654321,0x1b0404cbl, new byte[] {
               11,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 }, new byte[] {
               111,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
               16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32 });
      BlockChainLink link = new BlockChainLink(block,new Difficulty(),BlockChainLink.ROOT_HEIGHT,false);
      // Store -> Restart -> Recall
      storage.addLink(link);
      getProvider().closeStorage(storage);
      storage = getProvider().newStorage();
      BlockChainLink readLink = storage.getLink(link.getBlock().getHash());
      // Check link data
      Assert.assertNotNull(readLink,"link not found in storage after restart");
      Assert.assertEquals(readLink.getHeight(),BlockChainLink.ROOT_HEIGHT);
      Assert.assertFalse(readLink.isOrphan());
      // Check block integrity
      Assert.assertEquals(readLink.getBlock().getHash(),link.getBlock().getHash());
      validateBlock(readLink.getBlock());
   }

   public void testNonExistentGet()
      throws BitCoinException
   {
      Assert.assertNull(storage.getLink(
            new byte[] { 1,2,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }));
   }

   public void testGenesisEmpty()
      throws BitCoinException
   {
      Assert.assertNull(storage.getGenesisLink());      
   }

   public void testGenesisNormalBlocks()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      assertHash(storage.getGenesisLink(),23);
   }

   public void testGenesisOrphanBlocks()
      throws BitCoinException
   {
      addLink(24,0,0,1,true);
      addLink(25,0,1,2,true);
      addLink(26,0,2,3,true);
      addLink(23,0,0,4,false);
      assertHash(storage.getGenesisLink(),23);
   }

   public void testLastLinkEmpty()
      throws BitCoinException
   {
      Assert.assertNull(storage.getLastLink());      
   }

   public void testLastLinkConcept()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      assertHash(storage.getLastLink(),26);
   }

   public void testNoLastLinkFromOrphans()
      throws BitCoinException
   {
      addLink(23,0,0,1,true);
      addLink(24,23,1,2,true);
      addLink(25,24,2,3,true);
      addLink(26,25,3,4,true);
      Assert.assertNull(storage.getLastLink());      
   }

   public void testLastLinkOnShorterBranch()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);

      addLink(24,23,1,2,false);
      addLink(25,24,2,10,false); // This is the most difficult

      addLink(26,23,1,2,false);
      addLink(27,26,2,3,false);
      addLink(28,27,3,4,false);
      addLink(29,28,4,5,false);

      assertHash(storage.getLastLink(),25);
   }

   public void testLastLinkWithHigherDifficultyOrphan()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      addLink(27,26,4,5,true);
      assertHash(storage.getLastLink(),26);
   }

   public void testLastLinkConpetingBranches()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);

      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);

      addLink(26,23,1,2,false);
      addLink(27,26,2,3,false);
      addLink(28,27,3,4,false);
      addLink(29,28,4,5,false);

      assertHash(storage.getLastLink(),29);
   }

   public void testGetLinkConcept()
      throws BitCoinException
   {
      for ( int i=0; i<5; i++ )
         addLink(i+1,i,i,i+1,false);
      for ( int i=0; i<5; i++ )
         Assert.assertNotNull(getLink(i+1));
   }

   public void testGetLinkAfterRestart()
      throws BitCoinException
   {
      for ( int i=0; i<5; i++ )
         addLink(i+1,i,i,i+1,false);
      getProvider().closeStorage(storage);
      storage = getProvider().newStorage();
      for ( int i=0; i<5; i++ )
         Assert.assertNotNull(getLink(i+1));
   }

   public void testGetSomeOrphanLinks()
      throws BitCoinException
   {
      for ( int i=0; i<5; i++ )
         addLink(i+1,i,i,i+1,i%2==0);
      for ( int i=0; i<5; i++ )
         Assert.assertNotNull(getLink(i+1));
   }

   public void testGetNextLinksNonExistentHash()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      Assert.assertNotNull(getNextLinks(99));
      Assert.assertTrue(getNextLinks(99).isEmpty());
   }

   public void testGetNextLinksConcept()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      Assert.assertEquals(getNextLinks(24).size(),1);
      assertHash(getNextLinks(24).get(0),25);
   }

   public void testGetNextLinksMultipleBranches()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      addLink(27,24,2,3,false);
      addLink(28,24,2,3,false);
      Assert.assertEquals(getNextLinks(24).size(),3);
   }

   public void testGetNextLinkOrphan()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,true);
      addLink(26,25,3,4,true);
      Assert.assertEquals(getNextLinks(24).size(),1);
      assertHash(getNextLinks(24).get(0),25);
   }

   public void testGetClaimedLinkConcept()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,-1);
      addLink(26,25,3,4,false);
      assertHash(storage.getClaimedLink(getLink(26),createInput(11,0)),25);
   }

   public void testGetClaimedLinkIsLastInBranch()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false,11,-1);
      assertHash(storage.getClaimedLink(getLink(26),createInput(11,0)),26);
   }

   public void testGetClaimedLinkDoesNotExist()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      Assert.assertNull(storage.getClaimedLink(getLink(26),createInput(11,0)));
   }

   public void testGetClaimedLinkOnMultipleBranches()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,-1);
      addLink(26,24,2,3,false,11,-1);
      addLink(27,25,3,4,false);
      assertHash(storage.getClaimedLink(getLink(27),createInput(11,0)),25);
   }

   public void testGetClaimedLinkOnlyOnOtherBranch()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,24,2,3,false,11,-1);
      addLink(27,25,3,4,false);
      Assert.assertNull(storage.getClaimedLink(getLink(27),createInput(11,0)));
      Assert.assertNotNull(storage.getClaimedLink(getLink(26),createInput(11,0)));
   }

   public void testGetClaimedLinkNotEnoughOutputs()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,-1);
      addLink(26,25,3,4,false);
      assertHash(storage.getClaimedLink(getLink(26),createInput(11,99)),25);
   }

   public void testGetClaimerLinkConcept()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,1);
      addLink(26,25,3,4,false);
      assertHash(storage.getClaimerLink(getLink(26),createInput(11,1)),25);
   }

   public void testGetClaimerLinkIsLastInBranch()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false,11,1);
      assertHash(storage.getClaimerLink(getLink(26),createInput(11,1)),26);
   }

   public void testGetClaimerLinkDoesNotExist()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,25,3,4,false);
      Assert.assertNull(storage.getClaimerLink(getLink(26),createInput(11,1)));
   }

   public void testGetClaimerLinkOnMultipleBranches()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,1);
      addLink(26,24,2,3,false,11,1);
      addLink(27,25,3,4,false);
      assertHash(storage.getClaimerLink(getLink(27),createInput(11,1)),25);
   }

   public void testGetClaimerLinkOnlyOnOtherBranch()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false);
      addLink(26,24,2,3,false,11,1);
      addLink(27,25,3,4,false);
      Assert.assertNull(storage.getClaimerLink(getLink(27),createInput(11,1)));
      Assert.assertNotNull(storage.getClaimerLink(getLink(26),createInput(11,1)));
   }

   public void testGetClaimerLinkDifferingOutputs()
      throws BitCoinException
   {
      addLink(23,0,0,1,false);
      addLink(24,23,1,2,false);
      addLink(25,24,2,3,false,11,1);
      addLink(26,25,3,4,false);
      Assert.assertNull(storage.getClaimerLink(getLink(26),createInput(11,99)));
   }

   public void testUpdateOrphanLink()
      throws BitCoinException
   {
      addLink(23,0,0,1,true);
      BlockChainLink link = getLink(23);
      Assert.assertTrue(link.isOrphan());
      link.clearOrphan();
      storage.updateLink(link);
      link = getLink(23);
      Assert.assertFalse(link.isOrphan());
   }

   private TransactionInputImpl createInput(int claimedTxHash, int claimedOutputIndex)
   {
      return new TransactionInputImpl(
            new byte[] { (byte)claimedTxHash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            claimedOutputIndex, scriptFactory.createFragment(new byte[] {}), 0l);
   }

   /**
    * Add a link to the storage with some block data filled. The claimed txhash and output index
    * will be included in one of the many transactions randomly generated into the block.
    */
   private void addLink(int hash, int prevHash, long height, long totalDifficulty, boolean orphan,
         int claimedTxHash, int claimedOutputIndex)
      throws BitCoinException
   {
      // Generate random transactions (these are not even referentially valid)
      List<Transaction> transactions = new LinkedList<Transaction>();
      for ( int t=0; t<rnd.nextInt(5)+5; t++ )
      {
         List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
         for ( int i=0; i<rnd.nextInt(3)+1; i++ )
            inputs.add(new TransactionInputImpl(
               new byte[] { (byte)rnd.nextInt(255),1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },0,
               scriptFactory.createFragment(new byte[] {}),1));
         List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
         outputs.add(new TransactionOutputImpl(100,scriptFactory.createFragment(new byte[] {})));
         transactions.add(new TransactionImpl(inputs,outputs,0));
      }
      // If a tx hash is supplied, add a transaction with that hash
      if ( (claimedTxHash>0) && (claimedOutputIndex<0) )
      {
         transactions.add(new TransactionImpl(new LinkedList<TransactionInputImpl>(),
                  new LinkedList<TransactionOutputImpl>(),0,
            new byte[] { (byte)claimedTxHash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }));
      }
      // If a claimedtxhash and output index is supplied, then add an input which claims those
      if ( (claimedTxHash>0) && (claimedOutputIndex>0) )
      {
         List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
         inputs.add(new TransactionInputImpl(
                  new byte[] { (byte)claimedTxHash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },claimedOutputIndex,
                  scriptFactory.createFragment(new byte[] {}),1l));
         List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
         outputs.add(new TransactionOutputImpl(100,scriptFactory.createFragment(new byte[] {})));
         transactions.add(new TransactionImpl(inputs,outputs,0));
      }
      // Create block
      BlockImpl block = new BlockImpl(transactions,11223344l,11223344l,0x1b0404cbl,
            new byte[] { (byte)prevHash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            new byte[] { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            new byte[] { (byte)hash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 });
      BlockChainLink link = new BlockChainLink(block,new Difficulty(new BigDecimal(""+totalDifficulty)),height,orphan);
      storage.addLink(link);
   }

   private void addLink(int hash, int prevHash, long height, long totalDifficulty, boolean orphan)
      throws BitCoinException
   {
      addLink(hash,prevHash,height,totalDifficulty,orphan,0,0);
   }

   private BlockChainLink getLink(int hash)
   {
      return storage.getLink(
            new byte[] { (byte)hash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 });
   }

   private List<BlockChainLink> getNextLinks(int hash)
   {
      return storage.getNextLinks(
            new byte[] { (byte)hash,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 });
   }

   private void assertHash(BlockChainLink link, int hash)
   {
      Assert.assertEquals(link.getBlock().getHash()[0],(byte)hash);
   }

   /**
    * This method validates that a block is consistent by re-generating all hashes.
    */
   private void validateBlock(Block block)
      throws BitCoinException
   {
      // Copy
      List<Transaction> transactions = new LinkedList<Transaction>();
      for ( Transaction tx : block.getTransactions() )
      {
         List<TransactionInputImpl> inputs = new LinkedList<TransactionInputImpl>();
         for ( TransactionInput txIn : tx.getInputs() )
            inputs.add(new TransactionInputImpl(txIn.getClaimedTransactionHash(),
                     txIn.getClaimedOutputIndex(),txIn.getSignatureScript(), txIn.getSequence()));
         List<TransactionOutputImpl> outputs = new LinkedList<TransactionOutputImpl>();
         for ( TransactionOutput txOut : tx.getOutputs() )
            outputs.add(new TransactionOutputImpl(txOut.getValue(),txOut.getScript()));
         transactions.add(new TransactionImpl(inputs,outputs,tx.getLockTime()));
      }
      BlockImpl copy = new BlockImpl(transactions,block.getCreationTime(),block.getNonce(),
            block.getCompressedTarget(),block.getPreviousBlockHash(),block.getMerkleRoot());
      // Validate that hashes (of all data) equal
      Assert.assertEquals(copy.getHash(),block.getHash());
   }

}

