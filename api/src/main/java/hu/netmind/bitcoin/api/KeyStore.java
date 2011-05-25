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
 * The keystore holds all the private keys of the user/owner. The private
 * keys are the evidence which transactions belong to the owner, so losing
 * the keys amount to losing all the money associated with the key. Only this
 * keystore needs to be backed up, because all the money can be re-calculated
 * from the keys themselves.
 */
public interface KeyStore extends Observable
{
   enum Event
   {
      KEY_ADDED,
      KEY_REMOVED,
   };
}
