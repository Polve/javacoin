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
 * Implement this interface to listen for balance changes in a wallet.
 * @author Robert Brautigam
 */
public interface WalletListener
{
   /**
    * Invoked by the wallet to notify of potential balance changes. The 
    * <code>getBalance()</code> and <code>getConfirmedBalance()</code>
    * will reflect the new values during this call. Note: listener
    * is notified even if no new transactions for the relevant wallet
    * were registered, merely enough blocks were recorded to change
    * the confirmed balance.
    */
   void notifyBalanceChange();
}

