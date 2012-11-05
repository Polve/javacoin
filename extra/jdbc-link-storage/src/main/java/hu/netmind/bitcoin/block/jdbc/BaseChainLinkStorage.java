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
package hu.netmind.bitcoin.block.jdbc;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.net.HexUtil;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storing block link information using JDBC (MySql only for now).
 *
 * @author Alessandro Polverini
 */
public abstract class BaseChainLinkStorage implements BlockChainLinkStorage
{

   private static Logger logger = LoggerFactory.getLogger(BaseChainLinkStorage.class);
   private static final boolean DEFAULT_AUTOCREATE = true;
   private static final boolean DEFAULT_TRANSACTIONAL = true;
   private boolean autoCreate = DEFAULT_AUTOCREATE;
   private boolean transactional = DEFAULT_TRANSACTIONAL;
   //
   // We keep cached the top of the chain for a little performance improvement
   private BlockChainLink topLink;

   @Override
   public synchronized void addLink(final BlockChainLink link)
   {
      long startTime = System.currentTimeMillis();
      //logger.debug("addLink: " + HexUtil.toSingleHexString(link.getBlock().getHash())
      //       + " height: " + link.getHeight() + " totalDifficulty: " + link.getTotalDifficulty() + " isOrphan: " + link.isOrphan());
      if (link.isOrphan())
      {
         logger.error("Requested to persist orphan block");
         throw new JdbcStorageException("Requested to persist orphan block");
      }
      Connection connection = null;
      try
      {
         connection = newConnection();
         if (transactional)
            connection.setAutoCommit(false);
         // TODO: Check that the block does not exists and it's linkable
         long blockId = storeBlockHeader(connection, link);

         List<Transaction> transactions = link.getBlock().getTransactions();
         int pos = 0;
         for (Transaction tx : transactions)
         {
            long txId = getTransactionId(connection, tx.getHash());
            if (txId == -1)
               txId = storeTransaction(connection, tx);
            storeBlkTxLink(connection, blockId, txId, pos++);
         }

         // Little optimization: keep track of top of the chain
         if (topLink == null || link.getTotalDifficulty().compareTo(topLink.getTotalDifficulty()) > 0)
            topLink = link;

         if (transactional)
            connection.commit();
      } catch (SQLException e)
      {
         try
         {
            if (connection != null && transactional)
               connection.rollback();
         } catch (SQLException ex)
         {
         }
         logger.error("AddLinkEx: " + e.getMessage(), e);
         throw new LowLevelStorageException("Error while storing link: " + e.getMessage(), e.getCause());
      } finally
      {
         try
         {
            if (connection != null)
            {
               connection.setAutoCommit(true);
               connection.close();
            }
         } catch (SQLException e)
         {
            logger.error("AddLinkEx: " + e.getMessage(), e);
            throw new JdbcStorageException("Error while closing connection: " + e.getMessage(), e);
         }
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms height: " + link.getHeight() + " total difficulty: " + link.getTotalDifficulty());
      }
   }

   @Override
   public BlockChainLink getLink(final byte[] hash)
   {
//      long startTime = System.currentTimeMillis();
      try (Connection connection = newConnection())
      {
         // logger.debug("getLink: " + HexUtil.toSingleHexString(hash));
         return createBlockWithTxs(connection, hash, getBlockTransactions(connection, hash));
      } catch (Exception e)
      {
         logger.error("getLinkEx: " + e.getMessage(), e);
         throw new JdbcStorageException("getLinkEx: " + e.getMessage(), e);
      } finally
      {
//         long stopTime = System.currentTimeMillis();
//         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public boolean blockExists(byte[] hash)
   {
      try (Connection connection = newConnection())
      {
         return blockExists(connection, hash);
      } catch (Exception e)
      {
         logger.error("blockExists ex: " + e.getMessage(), e);
         throw new JdbcStorageException("blockExists ex: " + e.getMessage(), e);
      }
   }

   @Override
   public BlockChainLink getLinkBlockHeader(byte[] hash)
   {
      try (Connection connection = newConnection())
      {
         return createBlockWithTxs(connection, hash, null);
      } catch (Exception e)
      {
         logger.error("getLinkBlockHeader Ex: " + e.getMessage(), e);
         throw new JdbcStorageException("getLinkBlockHeader ex: " + e.getMessage(), e);
      }
   }

   @Override
   public BlockChainLink getGenesisLink()
   {
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> blocks = getBlocksAtHeight(connection, BlockChainLink.ROOT_HEIGHT);
         if (blocks.isEmpty())
            return null;
         return getLink(blocks.get(0).hash);
      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getGenesisLinkEx: " + ex.getMessage(), ex);
      }
   }

   @Override
   public BlockChainLink getLastLink()
   {
      if (topLink != null)
         return topLink;
      try (Connection connection = newConnection())
      {
         SimplifiedStoredBlock b = getHigherWorkHash(connection);
         if (b == null)
            return null;
         else
            return topLink = getLink(b.hash);
      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getLastLinkEx: " + ex.getMessage(), ex);
      }
   }

   @Override
   public long getHeight()
   {
      if (topLink != null)
         return topLink.getHeight();
      try (Connection connection = newConnection())
      {
         SimplifiedStoredBlock b = getHigherWorkHash(connection);
         if (b == null)
            return 0;
         else
            return getHigherWorkHash(connection).height;
      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getGenesisLinkEx: " + ex.getMessage(), ex);
      }
   }

   @Override
   public List<BlockChainLink> getNextLinks(final byte[] hash)
   {
      //long startTime = System.currentTimeMillis();
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> blocks = getBlocksWithPrevHash(connection, hash);
         List<BlockChainLink> storedLinks = new LinkedList<>();
         for (SimplifiedStoredBlock b : blocks)
            storedLinks.add(getLink(b.hash));
         return storedLinks;
      } catch (SQLException e)
      {
         logger.error("getNextLinks: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLinks: " + e.getMessage(), e);
      } finally
      {
         //long stopTime = System.currentTimeMillis();
         //logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getNextLink(final byte[] current, final byte[] target)
   {
      try (Connection connection = newConnection())
      {
         SimplifiedStoredBlock targetBlock = getSimplifiedStoredBlock(connection, target);
         if (targetBlock == null)
            return null;
         SimplifiedStoredBlock currentBlock = getSimplifiedStoredBlock(connection, current);
         if (currentBlock == null || targetBlock.height < currentBlock.height)
            return null;
         for (SimplifiedStoredBlock candidate : getBlocksWithPrevHash(connection, current))
            if (isReachable(connection, targetBlock, candidate))
               return getLink(candidate.hash);
         return null;
      } catch (SQLException e)
      {
         logger.error("getNextLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLink: " + e.getMessage(), e);
      }
   }

   @Override
   public boolean isReachable(final byte[] target, final byte[] source)
   {
      long startTime = System.currentTimeMillis();
      try (Connection connection = newConnection())
      {
         SimplifiedStoredBlock targetBlock = getSimplifiedStoredBlock(connection, target);
         if (targetBlock == null)
            return false;
         SimplifiedStoredBlock sourceBlock = getSimplifiedStoredBlock(connection, source);
         if (sourceBlock == null)
            return false;
         return isReachable(connection, targetBlock, sourceBlock);
      } catch (SQLException e)
      {
         logger.error("isReacheble: " + e.getMessage(), e);
         throw new JdbcStorageException("isReachable: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getClaimedLink(final BlockChainLink link, final TransactionInput in)
   {
      long startTime = System.currentTimeMillis();
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksWithTx(connection, in.getClaimedTransactionHash());
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(connection, linkBlock, b))
               return getLink(b.hash);
         return null;
      } catch (SQLException e)
      {
         logger.error("getClaimedLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimedLinks: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug(HexUtil.toSingleHexString(in.getClaimedTransactionHash()) + " exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getPartialClaimedLink(final BlockChainLink link, final TransactionInput in)
   {
      long startTime = System.currentTimeMillis();
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksWithTx(connection, in.getClaimedTransactionHash());
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(connection, linkBlock, b))
            {
               List<TransactionImpl> txs = new LinkedList<>();
               txs.add(getTransaction(connection, in.getClaimedTransactionHash()));
               return createBlockWithTxs(connection, b.hash, txs);
            }
         return null;
      } catch (SQLException | BitcoinException e)
      {
         logger.error("getClaimedLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimedLinks: " + e.getMessage(), e);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug(HexUtil.toSingleHexString(in.getClaimedTransactionHash()) + " /" + in.getClaimedOutputIndex() + " exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getClaimerLink(final BlockChainLink link, final TransactionInput in)
   {
      try (Connection connection = newConnection())
      {
         byte[] hash = getClaimerHash(connection, link, in);
         if (hash == null)
            return null;
         else
            return getLink(hash);
      } catch (SQLException e)
      {
         logger.error("getClaimerLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimerLinks: " + e.getMessage(), e);
      }
   }

   @Override
   public boolean outputClaimedInSameBranch(final BlockChainLink link, final TransactionInput in)
   {
      try (Connection connection = newConnection())
      {
         return getClaimerHash(connection, link, in) != null;
      } catch (SQLException e)
      {
         logger.error("outputClaimedInSameBranch: " + e.getMessage(), e);
         throw new JdbcStorageException("outputClaimedInSameBranch: " + e.getMessage(), e);
      }
   }

   @Override
   public BlockChainLink getCommonLink(final byte[] first, final byte[] second)
   {
      long startTime = System.currentTimeMillis();
      //logger.debug("getCommonLink " + HexUtil.toSingleHexString(first) + " " + HexUtil.toSingleHexString(second));
      try (Connection connection = newConnection())
      {
         SimplifiedStoredBlock block1 = getSimplifiedStoredBlock(connection, first);
         SimplifiedStoredBlock block2 = getSimplifiedStoredBlock(connection, second);
         if (block1 == null || block2 == null)
            return null;

         if (block1.equals(block2))
            return getLink(block1.hash);

         // We need block1 height lower than block2 height
         if (block2.height < block1.height)
         {
            SimplifiedStoredBlock tmp = block1;
            block1 = block2;
            block2 = tmp;
         }

         // loop until we reach the genesis block
         while (true)
         {
            //logger.debug("getCommonLinkLoop block1: " + block1);
            // Genesis link reaches all non-orphan blocks
            if (block1.height == BlockChainLink.ROOT_HEIGHT)
               return getGenesisLink();

            // If we have no side branches there is only one possibility to have a common link
            // and it is the lowest block between the two: block1
            if (isReachable(connection, block2, block1))
               return getLink(block1.hash);
            else if (getNumBlocksAtHeight(connection, block1.height) == 1)
               return null;

            // If the two blocks are not reachable find a lower branch intersection
            do
               block1 = getSimplifiedStoredBlock(connection, block1.prevBlockHash);
            while (getNumBlocksWithPrevHash(connection, block1.hash) == 1);
         }

      } catch (SQLException ex)
      {
         throw new JdbcStorageException("getCommonLinkEx: " + ex.getMessage(), ex);
      } finally
      {
         long stopTime = System.currentTimeMillis();
         logger.debug("exec time: " + (stopTime - startTime) + " ms");
      }
   }

   @Override
   public BlockChainLink getLinkAtHeight(long height)
   {
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> blocks = getBlocksAtHeight(connection, height);
         if (blocks.isEmpty())
            return null;
         if (blocks.size() == 1)
            return createBlockWithTxs(connection, blocks.get(0).hash, getBlockTransactions(connection, blocks.get(0).hash));
         SimplifiedStoredBlock topBlock = new SimplifiedStoredBlock(getLastLink());
         for (SimplifiedStoredBlock b : blocks)
            if (isReachable(connection, b, topBlock))
               return createBlockWithTxs(connection, b.hash, getBlockTransactions(connection, b.hash));

         // We should never arrive here
         assert false;
         return null;
      } catch (SQLException e)
      {
         logger.error("getNextLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLink: " + e.getMessage(), e);
      }
   }

   @Override
   public byte[] getHashOfMainChainAtHeight(long height)
   {
      try (Connection connection = newConnection())
      {
         List<SimplifiedStoredBlock> blocks = getBlocksAtHeight(connection, height);
         if (blocks.isEmpty())
            return null;
         if (blocks.size() == 1)
            return blocks.get(0).hash;
         SimplifiedStoredBlock topBlock = new SimplifiedStoredBlock(getLastLink());
         for (SimplifiedStoredBlock b : blocks)
            if (isReachable(connection, b, topBlock))
               return b.hash;

         // We should never arrive here
         assert false;
         return null;
      } catch (SQLException e)
      {
         logger.error("getNextLink: " + e.getMessage(), e);
         throw new JdbcStorageException("getNextLink: " + e.getMessage(), e);
      }
   }

   /*
    * This check is complicated by BIP30, that a new tx can exist with same hash
    * if the other one fully spent We sort blocks on height because we need to
    * check only the last one in the given chain The problem arise from TX
    * a1d7c19f72ce5b24a1001bf9c5452babed6734eaa478642379f8c702a46d5e27 in block
    * 0000000013aa9f67da178005f9ced61c7064dd6e8464b35f6a8ca8fabc1ca2cf
    */
   protected byte[] getClaimerHash(final Connection connection, final BlockChainLink link, final TransactionInput in)
   {
      if (link == null || in == null)
         return null;
      try
      {
         List<SimplifiedStoredBlock> potentialBlocks = getBlocksReferringTx(connection, in);
         Collections.sort(potentialBlocks);
         SimplifiedStoredBlock linkBlock = new SimplifiedStoredBlock(link);
         for (SimplifiedStoredBlock b : potentialBlocks)
            if (b.height <= link.getHeight() && isReachable(connection, linkBlock, b))
            {
               // Check if a tx with same hash has been created after being reclaimed
               List<SimplifiedStoredBlock> blocks = getBlocksWithTx(connection, in.getClaimedTransactionHash());
               // We sort blocks on height to discard the ones below
               Collections.sort(blocks);
               for (SimplifiedStoredBlock block : blocks)
               {
                  // No potential good blocks left
                  if (block.height < b.height)
                     break;
                  // If we find a block higher in the chain with the same transaction we have found a not spent tx with the same hash
                  // So we need to return null, indicating that no block is claiming it
                  if (isReachable(connection, new SimplifiedStoredBlock(link), block))
                     return null;
               }
               return b.hash;
            }
         return null;
      } catch (SQLException e)
      {
         logger.error("getClaimerHash: " + e.getMessage(), e);
         throw new JdbcStorageException("getClaimerHash: " + e.getMessage(), e);
      }
   }

   /**
    * Returns true if both blocks are in the same branch and b1 precedes b2
    *
    * @param source
    * @param target
    * @return
    */
   protected boolean isReachable(final Connection connection, SimplifiedStoredBlock target, SimplifiedStoredBlock source) throws SQLException
   {
      // Sanity checks
      if (source == null || target == null)
         return false;

      // Handle special cases
      if (source.equals(target))
         return true;

      // If at same height and not same block we are in different branches
      if (target.height == source.height)
         return false;

      // We need source height lower than target height
      if (target.height < source.height)
      {
         SimplifiedStoredBlock tmp = source;
         source = target;
         target = tmp;
      }

      long currHeight = source.height;
      List<SimplifiedStoredBlock> chains = new LinkedList<>();
      chains.add(source);

      // Follow all the chains we discover from our starting point
      // until we are certain whether we are in the same chain of the target or not
      do
      {
         //logger.debug("isRechableLoop - currHeight: " + currHeight + " chains.size: " + chains.size());
         // Check if we are the only chain at this height
         // then we are in the same branch for sure because we know the target is connected
         if (chains.size() == 1)
            if (getNumBlocksAtHeight(connection, currHeight) == 1)
               return true;
         List<SimplifiedStoredBlock> newChains = new LinkedList<>();
         for (SimplifiedStoredBlock block : chains)
            if (block.equals(target))
               return true;
            else
               newChains.addAll(getBlocksWithPrevHash(connection, block.hash));
         chains = newChains;
         currHeight++;
      } while (!chains.isEmpty() && currHeight <= target.height);

      // We reached the top of all our chain without finding the target
      // so we are in different branches
      return false;
   }

   protected abstract Connection newConnection();

   protected abstract boolean blockExists(final Connection dbConnection, byte[] hash) throws SQLException;

   protected abstract List<SimplifiedStoredBlock> getBlocksAtHeight(final Connection connection, long height) throws SQLException;

   protected abstract List<SimplifiedStoredBlock> getBlocksReferringTx(final Connection connection, final TransactionInput in) throws SQLException;

   protected abstract List<SimplifiedStoredBlock> getBlocksWithTx(final Connection connection, final byte[] hash) throws SQLException;

   protected abstract List<SimplifiedStoredBlock> getBlocksWithPrevHash(final Connection connection, final byte[] hash) throws SQLException;

   protected abstract int getNumBlocksAtHeight(final Connection connection, long height) throws SQLException;

   protected abstract int getNumBlocksWithPrevHash(final Connection connection, byte[] hash) throws SQLException;

   protected abstract long storeBlockHeader(final Connection connection, final BlockChainLink link) throws SQLException;

   protected abstract long storeTransaction(final Connection connection, final Transaction tx);

   protected abstract void storeBlkTxLink(final Connection connection, long blockId, long txId, int pos) throws SQLException;

   protected abstract List<TransactionInputImpl> loadTxInputs(final Connection connection, long txId) throws SQLException;

   protected abstract List<TransactionOutputImpl> loadTxOutputs(final Connection connection, long txId) throws SQLException;

   protected abstract long getTransactionId(final Connection connection, byte[] hash) throws SQLException;

   protected abstract TransactionImpl getTransaction(final Connection connection, byte[] hash) throws SQLException, BitcoinException;

   protected abstract List<TransactionImpl> getBlockTransactions(final Connection connection, ResultSet rs) throws SQLException, BitcoinException;

   protected abstract List<TransactionImpl> getBlockTransactions(final Connection connection, byte[] hash);

   protected abstract List<TransactionImpl> getBlockTransactions(final Connection connection, long blockId);

   protected abstract BlockChainLink createBlockWithTxs(final Connection connection, final byte[] hash, List<TransactionImpl> transactions);

   protected abstract SimplifiedStoredBlock getSimplifiedStoredBlock(final Connection connection, final byte[] hash) throws SQLException;

   protected abstract SimplifiedStoredBlock getHigherWorkHash(final Connection connection) throws SQLException;

   public boolean getTransactional()
   {
      return transactional;
   }

   public boolean getAutoCreate()
   {
      return autoCreate;
   }

   public void setAutoCreate(boolean autoCreate)
   {
      this.autoCreate = autoCreate;
   }
}
