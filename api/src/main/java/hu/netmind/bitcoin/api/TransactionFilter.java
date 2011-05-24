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
 * This filter can be used to get transactions from Blocks and BlockChains.
 * The purpose of filters is to have unified way to describe algorithms how 
 * to get transactions in order to allow Blocks to be compressed/compacted 
 * (so transactions dropped in storage) but at the same time know when some
 * useage would be actually impacted by the compaction.<br>
 * For example the BlockChain storage mechanism could choose to drop all
 * trsansactions which are already spent to preserve disk space, but if
 * an function requests all transactions, the storage should know that it
 * will not be able to fulfill the request from the stored information.

