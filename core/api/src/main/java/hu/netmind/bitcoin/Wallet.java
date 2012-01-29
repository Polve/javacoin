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

package hu.netmind.bitcoin;

/**
 * Wallet is a high level logical construct of the BitCoin infrastructure,
 * providing services to query and handle money transfers. The functions
 * supported by this interface are targeted to normal "consumer" usage,
 * and therefore are simplified and do not offer all capabilities of the
 * BitCoin network.
 * @author Robert Brautigam
 */
public interface Wallet extends Observable
{
   enum Event
   {
      BALANCE_CHANGE
   };

   /**
    * Get the current balance of the Wallet. In the BitCoin network "having"
    * a balance is not black-and-white. The balance actually contains transactions
    * with different risks associated (risks that the transaction mat be rolled back).
    * It is left to the implementation to define the risk level associated with 
    * this method's return value.
    * @return The total balance in this wallet.
    * Note: 100,000,000 (one hundred million) of units is considered 1 BTC.
    */
   long getBalance();

   /**
    * Send money to a given address.
    * @param to The string representation of the destination address.
    * @param amount The amount to send from this wallet to the destination
    * address. Note: 100,000,000 (one hundred million) of units is considered 1 BTC.
    * @throws NotEnoughMoneyException If not enough funds could be collected to cover
    * to amount specified.
    */
   void sendMoney(String to, long amount)
      throws NotEnoughMoneyException, VerificationException;

   /**
    * Create a new address in this wallet others can transfer to. It is
    * recommended not to cache this address, but to create new addresses
    * for each transaction.
    * @return A new valid address for this wallet. Money transfer to this
    * address will (eventually) show up in this wallet.
    */
   String createAddress()
      throws VerificationException;
}

