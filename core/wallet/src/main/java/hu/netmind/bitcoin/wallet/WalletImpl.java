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

package hu.netmind.bitcoin.wallet;

import hu.netmind.bitcoin.Wallet;
import hu.netmind.bitcoin.Coin;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.InvalidCoinException;
import hu.netmind.bitcoin.BitCoinException;
import java.util.Observable;
import java.util.Observer;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a wallet which hold persistent coins.
 * Class is thread-safe (can be used from multiple threads).
 * @author Robert Brautigam
 */
public class WalletImpl extends Observable implements Wallet
{
   private static final Logger logger = LoggerFactory.getLogger(WalletImpl.class);
   private CoinStorage coinStorage;
   private BlockChain chain;

   public WalletImpl(BlockChain chain, CoinStorage coinStorage)
      throws BitCoinException
   {
      this.chain=chain;
      this.coinStorage=coinStorage;
      updateCoins();
      chain.addObserver(new Observer() {
            public void update(Observable o, Object arg)
            {
               try
               {
                  if ( arg == BlockChain.Event.LONGEST_PATH_CHANGE )
                     updateCoins();
               } catch ( BitCoinException e ) {
                  logger.error("wallet could not update after a longest path change, wallet possibly returns false information",e);
               }
            }
         });
   }

   /**
    * Check and update the coins in the storage so that they 
    * reflect the state in the chain. It is assumed that
    * all coins are on one single path on input, and this
    * invariant is kept in this update.
    */
   private void updateCoins()
      throws BitCoinException
   {
      synchronized ( coinStorage )
      {
         Block lastBlock = chain.getLastBlock();
         removeUnreachableCoins(lastBlock);
         createNewCoins(lastBlock);
      }
   }

   private void removeUnreachableCoins(Block lastBlock)
   {
      Block lastCheckedBlock = chain.getBlock(coinStorage.getLastCheckedBlockHash());
      if ( chain.isReachable(lastBlock,lastCheckedBlock) )
         return;
      List<CoinImpl> coins = coinStorage.getCoins();
      List<CoinImpl> removedCoins = new LinkedList<CoinImpl>();
      ListIterator<CoinImpl> coinsIterator = coins.listIterator(coins.size());
      while ( coinsIterator.hasPrevious() )
      {
         CoinImpl coin = coinsIterator.previous();
         if ( chain.isReachable(lastBlock,chain.getBlock(coin.getBlockHash())) ) 
            break;
         removedCoins.add(coin);
         coinsIterator.remove();
      }
      coinStorage.remove(
            chain.getCommonBlock(lastBlock,lastCheckedBlock).getHash(),
            removedCoins.toArray(new CoinImpl[] {}));
   }

   private void createNewCoins(Block lastBlock)
   {
      // TODO
   }

   /**
    * Get all the coins in this wallet in height order (starting from
    * the lowest/earliest height).
    */
   public List<Coin> getCoins()
   {
      return new LinkedList(coinStorage.getUnspentCoins());
   }

   public void spend(Coin... coins)
      throws InvalidCoinException
   {
      synchronized ( coinStorage )
      {
         List<CoinImpl> storedCoins = new LinkedList<CoinImpl>();
         // Make sure coin is still part of the wallet,
         // and also get the implementation class
         long now = System.currentTimeMillis();
         for ( Coin coin : coins )
         {
            CoinImpl storedCoin = coinStorage.getCoin(coin.getTransactionHash(),coin.getTransactionOutputIndex());
            if ( storedCoin == null )
               throw new InvalidCoinException("coin not part of the wallet anymore: "+coin);
            // Update
            storedCoin.setSpent(true);
            storedCoin.setSpentTime(now);
            storedCoin.setSpentHeight(0);
            storedCoins.add(storedCoin);
         }
         // Make update
         coinStorage.update(storedCoins.toArray(new CoinImpl[] {}));
         // Notify
         setChanged();
         notifyObservers(Event.COIN_CHANGE);
      }
   }

   public void reclaim(Coin... coins)
      throws InvalidCoinException
   {
      synchronized ( coinStorage )
      {
         List<CoinImpl> storedCoins = new LinkedList<CoinImpl>();
         // Make sure coin is still part of the wallet,
         // and also get the implementation class
         for ( Coin coin : coins )
         {
            CoinImpl storedCoin = coinStorage.getCoin(coin.getTransactionHash(),coin.getTransactionOutputIndex());
            if ( (storedCoin == null) || (!storedCoin.isSpent()) || (storedCoin.getSpentHeight()>0) )
               throw new InvalidCoinException("coin not part of the wallet anymore or it is not spent or it is already officially spent: "+coin);
            // Update
            storedCoin.setSpent(false);
            storedCoin.setSpentTime(0);
            storedCoin.setSpentHeight(0);
            storedCoins.add(storedCoin);
         }
         // Make update
         coinStorage.update(storedCoins.toArray(new CoinImpl[] {}));
         // Notify
         setChanged();
         notifyObservers(Event.COIN_CHANGE);
      }
   }
}


