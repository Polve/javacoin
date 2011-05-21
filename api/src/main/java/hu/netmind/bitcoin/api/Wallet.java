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

package hu.netmind.bitcoin.api;

/**
 * Wallet is a high level logical construct of the BitCoin infrastructure,
 * providing services to query and handle money transfers.
 * @author Robert Brautigam
 */
public interface Wallet
{
   /**
    * Get the "confirmed" total balance of this wallet. Confirmed means
    * that it was accepted by other peers with high probablilty as the
    * transactions that make up this balance are at least a given
    * amount of blocks "deep" in the longest block chain. By default
    * transactions have to be at least 6 blocks deep in the chain to
    * be considered confirmed (this can be set with accordance to 
    * double spending risks levels), which means a new transaction has to
    * wait at least 1 hour to be confirmed.
    * @return The confirmed balance in this wallet. Note: 100,000,000
    * (one hundred million) of units is considered 1 BTC.
    */
   long getConfirmedBalance();

   /**
    * Get the current balance as it appears at the moment from the current
    * longest block chain. This balance does include items which have
    * not yet made it to the block chain, and might be actually rejected
    * by the BitCoin network eventually.
    * @return The total balance in this wallet (inconfirmed balance
    * included). Note: 100,000,000 (one hundred million) of units is considered 1 BTC.
    */
   long getBalance();

   /**
    * Send money to a given address.
    * @param to The string representation of the destination address.
    * @param amount The amount to send from this wallet to the destination
    * address. Note: 100,000,000 (one hundred million) of units is considered 1 BTC.
    */
   void sendMoney(String to, long amount);

   /**
    * Set a listener to be notified when any of the balance methods would
    * potentially return different values.
    */
   void setListener(WalletListener listener);
}

