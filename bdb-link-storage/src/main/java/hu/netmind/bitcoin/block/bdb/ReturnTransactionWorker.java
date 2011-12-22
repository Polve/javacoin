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

package hu.netmind.bitcoin.block.bdb;

import com.sleepycat.collections.TransactionWorker;

/**
 * A special kind of transaction worker that is capable of returning a value.
 * @author Robert Brautigam
 */
public class ReturnTransactionWorker<T> implements TransactionWorker
{
   private T returnValue;

   public T getReturnValue()
   {
      return returnValue;
   }

   /**
    * You can override this to do non-returning logic.
    */
   public void doWork()
   {
      returnValue = doReturnWork();
   }

   /**
    * Override this method to provide return value.
    */
   public T doReturnWork()
   {
      return null;
   }
}

