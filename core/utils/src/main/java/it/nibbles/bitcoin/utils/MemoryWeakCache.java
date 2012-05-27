/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTAB ILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 0211 1-13 07 USA
 */
package it.nibbles.bitcoin.utils;

import hu.netmind.bitcoin.Block;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini
 * TODO: Implement a ReferenceMap<x,y>
 */
public class MemoryWeakCache
{

   private static final Logger logger = LoggerFactory.getLogger(MemoryWeakCache.class);
   public static final int MAX_SIZE = 1000;
   private static final Map<byte[], WeakReference<Block>> cache =
      new HashMap<>();

}
