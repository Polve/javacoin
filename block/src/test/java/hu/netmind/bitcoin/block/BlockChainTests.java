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
import org.testng.annotations.Test;
import org.easymock.EasyMock;
import org.easymock.Capture;
import org.testng.Assert;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
@Test
public class BlockChainTests
{
   public void testGenesisOk()
      throws VerificationException
   {
      // Initialize storage with known block and link
      Block genesisBlock = EasyMock.createMock(Block.class);
      BlockChainLink genesisLink = EasyMock.createMock(BlockChainLink.class);
      EasyMock.expect(genesisLink.getBlock()).andReturn(genesisBlock);
      EasyMock.replay(genesisLink);
      BlockChainLinkStorage storage = EasyMock.createMock(BlockChainLinkStorage.class);
      EasyMock.expect(storage.getGenesisLink()).andReturn(genesisLink);
      EasyMock.replay(storage);
      // Construct block chain with known genesis block, should be ok
      BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
   }

   @Test(expectedExceptions = VerificationException.class)
   public void testWrongGenesis()
      throws VerificationException
   {
      // Initialize storage with known block and link
      Block genesisBlock = EasyMock.createMock(Block.class);
      BlockChainLink genesisLink = EasyMock.createMock(BlockChainLink.class);
      EasyMock.expect(genesisLink.getBlock()).andReturn(genesisBlock);
      EasyMock.replay(genesisLink);
      BlockChainLinkStorage storage = EasyMock.createMock(BlockChainLinkStorage.class);
      EasyMock.expect(storage.getGenesisLink()).andReturn(genesisLink);
      EasyMock.replay(storage);
      // Construct block chain with known genesis block, should be ok
      Block secondGenesisBlock = EasyMock.createMock(Block.class);
      BlockChainImpl chain = new BlockChainImpl(secondGenesisBlock,storage,null,false);
   }

   public void testGenesisInitialization()
      throws VerificationException
   {
      Block genesisBlock = EasyMock.createMock(Block.class);
      // Initialize storage with nothing
      BlockChainLinkStorage storage = EasyMock.createMock(BlockChainLinkStorage.class);
      EasyMock.expect(storage.getGenesisLink()).andReturn(null);
      Capture<BlockChainLink> genesisCapture = new Capture<BlockChainLink>();
      storage.addLink( EasyMock.capture(genesisCapture) );
      EasyMock.replay(storage);
      // Construct block chain with genesis block
      BlockChainImpl chain = new BlockChainImpl(genesisBlock,storage,null,false);
      // Verify genesis block
      EasyMock.verify(storage); // Check that everything was called
      BlockChainLink link = genesisCapture.getValue();
      Assert.assertEquals(link.getHeight(), 1);
      Assert.assertEquals(link.getBlock(), genesisBlock);
      Assert.assertFalse(link.isOrphan());
   }
}



