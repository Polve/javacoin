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

/**
 * Tests requiring clean storage to run.
 * @author Robert Brautigam
 */
@Test
public class CleanTests<T extends BlockChainLinkStorage> extends InitializableStorageTests<T>
{
   private ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(null);
   private T storage = null;

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
      Assert.assertEquals(readLink.getBlock().getHash(),BlockImpl.MAIN_GENESIS.getHash());
      validateBlock(readLink.getBlock());
   }

   public void testComplicatedBlockStoreRecall()
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
      BlockChainLink link = new BlockChainLink(block,new Difficulty(),1,false);
      // Store & Recall
      storage.addLink(link);
      BlockChainLink readLink = storage.getLink(link.getBlock().getHash());
      // Check link data
      Assert.assertEquals(readLink.getHeight(),1);
      Assert.assertFalse(readLink.isOrphan());
      // Check block integrity
      Assert.assertEquals(readLink.getBlock().getHash(),link.getBlock().getHash());
      validateBlock(readLink.getBlock());
   }
}

