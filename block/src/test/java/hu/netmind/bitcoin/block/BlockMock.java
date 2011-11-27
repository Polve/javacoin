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

import org.easymock.EasyMock;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;

/**
 * Parses a special language to create a hierarchy of block,
 * transaction, output and inputs mocks. An full example
 * looks like this:
 * <pre>
 * # Comment
 * block [creationtime] [nonce] [compressedtarget] [prevblockhash] [merkleroot] [hash] [invalidmessage];
 *    tx [locktime] [hash] [coinbase] [invalidmessage];
 *       in [claimedtxhash] [claimedoutindex] [sequence];
 *       out [value];
 * </pre>
 * Each line can be repeated any number of times.
 * @author Robert Brautigam
 */
public class BlockMock
{
   private static Logger logger = LoggerFactory.getLogger(BlockMock.class);

   private static byte[] toByteArray(String hexString)
   {
      BigInteger result = new BigInteger(hexString,16);
      return result.toByteArray();
   }

   /**
    * Create a list of blocks.
    */
   public static List<Block> createBlocks(String def)
      throws VerificationException
   {
      List<Block> result = new ArrayList<Block>();
      StringTokenizer lines = new StringTokenizer(def,";");
      Block block = null; // Current block if there is one
      Transaction transaction = null; // Current transaction if there is one
      while ( lines.hasMoreTokens() )
      {
         // Get command line
         String line = lines.nextToken().trim();
         if ( (line.length()==0) || (line.startsWith("#")) )
            continue; // Comment or empty line
         // Parse line
         StringTokenizer words = new StringTokenizer(line," ");
         String command = words.nextToken().toLowerCase();
         List<String> params = new ArrayList<String>();
         while ( words.hasMoreTokens() )
            params.add(words.nextToken().toLowerCase());
         logger.debug("making command {} with parameters: {}",command,params);
         // Process commands
         if ( "block".equals(command) )
         {
            // New block
            block = EasyMock.createMock(Block.class);
            EasyMock.expect(block.getCreationTime()).andReturn(Long.parseLong(params.get(0))).anyTimes();
            EasyMock.expect(block.getNonce()).andReturn(Long.parseLong(params.get(1))).anyTimes();
            EasyMock.expect(block.getCompressedTarget()).andReturn(Long.parseLong(params.get(2),16)).anyTimes();
            EasyMock.expect(block.getPreviousBlockHash()).andReturn(
                  toByteArray(params.get(3))).anyTimes();
            EasyMock.expect(block.getMerkleRoot()).andReturn(
                  toByteArray(params.get(4))).anyTimes();
            EasyMock.expect(block.getHash()).andReturn(
                  toByteArray(params.get(5))).anyTimes();
            EasyMock.expect(block.getTransactions()).andReturn(new ArrayList<Transaction>()).anyTimes();
            block.validate();
            if ( params.size() > 6 )
               EasyMock.expectLastCall().andThrow(new VerificationException(params.get(6)));
            EasyMock.replay(block);
            // Add
            result.add(block);
         }
         if ( "tx".equals(command) )
         {
            if ( block == null )
               continue; // No current block, skip
            // New tx
            transaction = EasyMock.createMock(Transaction.class);
            EasyMock.expect(transaction.getLockTime()).andReturn(Long.parseLong(params.get(0))).anyTimes();
            EasyMock.expect(transaction.getHash()).andReturn(
                  toByteArray(params.get(1))).anyTimes();
            EasyMock.expect(transaction.isCoinbase()).andReturn(Boolean.parseBoolean(params.get(2))).anyTimes();
            EasyMock.expect(transaction.getInputs()).andReturn(new ArrayList<TransactionInput>()).anyTimes();
            EasyMock.expect(transaction.getOutputs()).andReturn(new ArrayList<TransactionOutput>()).anyTimes();
            transaction.validate();
            if ( params.size() > 3 )
               EasyMock.expectLastCall().andThrow(new VerificationException(params.get(3)));
            EasyMock.replay(transaction);
            // Add
            block.getTransactions().add(transaction);
         }
         if ( "in".equals(command) )
         {
            if ( transaction == null )
               continue; // No current transaction, skip
            // New in
            TransactionInput in = EasyMock.createMock(TransactionInput.class);
            EasyMock.expect(in.getClaimedTransactionHash()).andReturn(
                  toByteArray(params.get(0))).anyTimes();
            EasyMock.expect(in.getClaimedOutputIndex()).andReturn(
                  Integer.parseInt(params.get(1)));
            EasyMock.expect(in.getSequence()).andReturn(
                  Long.parseLong(params.get(2)));
            // Add
            transaction.getInputs().add(in);
         }
         if ( "out".equals(command) )
         {
            if ( transaction == null )
               continue; // No current transaction, skip
            // New in
            TransactionOutput out = EasyMock.createMock(TransactionOutput.class);
            EasyMock.expect(out.getValue()).andReturn(
                  Long.parseLong(params.get(0)));
            // Add
            transaction.getOutputs().add(out);
         }
      }
      return result;
   }

   /**
    * Gte a single block from the given definition.
    * @return The first block or null if no blocks in the definition.
    */
   public static Block createBlock(String def)
      throws VerificationException
   {
      List<Block> blocks = createBlocks(def);
      if ( blocks.isEmpty() )
         return null;
      return blocks.get(0);
   }
}

