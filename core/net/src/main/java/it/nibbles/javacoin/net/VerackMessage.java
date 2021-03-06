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

package it.nibbles.javacoin.net;

import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class VerackMessage extends Message
{
   public VerackMessage(long magic) throws IOException
   {
      super(magic,"verack");
   }

   VerackMessage() throws IOException
   {
      super();
   }

   @Override
   void readFrom(BitcoinInputStream input, long version, Object param)
      throws IOException
   {
      super.readFrom(input,version,param);
   }
}

