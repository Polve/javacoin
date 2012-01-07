/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

import hu.netmind.bitcoin.block.BlockChainLink;

/**
 * The stored version of a link. A normal link needs the path information in addition.
 * @author Robert Brautigam
 */
public class StoredLink
{
   private BlockChainLink link;
   private Path path;

   public StoredLink(BlockChainLink link, Path path)
   {
      this.link=link;
      this.path=path;
   }

   public BlockChainLink getLink()
   {
      return link;
   }
   public Path getPath()
   {
      return path;
   }

   public String toString()
   {
      return "StoredLink: "+link+", path: "+path;
   }
}

