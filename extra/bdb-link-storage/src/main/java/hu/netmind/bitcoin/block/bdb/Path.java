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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A path encoding scheme for deep thin trees. Paths can be compared to see
 * whether they have the same path from the root of a given length. Each path
 * is represented by the list of junctions of that path, but only the junctions
 * leading to other than the 'primary' path.
 * <pre>
 * A1 --- A2 -+- A3 -- A4 -+- A5
 *            |            \  B5
 *            |
 *            \- C3 -- C4 -+- C5 -- C6
 *                         \  D5
 * </pre>
 * The path to A5 would be empty, since it's the default path. Path to B5 would be
 * [(5,1)] meaning 1st junction at height 5. Path of C6 is [(3,1)]. Path of
 * D5 is [(3,1) (5,1)]. Note that D5 has the similar junction information to B5,
 * but is nevertheless on different paths.
 * @author Robert Brautigam
 */
public class Path
{
   private List<Junction> junctions = new ArrayList<Junction>();
   private long height;

   /**
    * Create a path to the root node.
    */
   public Path()
   {
   }

   /**
    * Re-create a path from a list of junctions and height
    * information.
    */
   public Path(List<Junction> junctions, long height)
   {
      this.junctions=new ArrayList(junctions);
      this.height=height;
   }

   public List<Junction> getJunctions()
   {
      return Collections.unmodifiableList(junctions);
   }

   public long getHeight()
   {
      return height;
   }

   /**
    * Create a path to the next node on this path with the given index.
    * @param index The index of the next node, starting at 0.
    */
   public Path createPath(int index)
   {
      List<Junction> nextJunctions = new ArrayList<Junction>(junctions);
      if ( index > 0 )
         nextJunctions.add(new Junction(height+1,index));
      return new Path(nextJunctions,height+1);
   }

   /**
    * Determine whether the given path is a prefix path. This means that it
    * starts at the root at took the same junctions.
    * @param prefix The potential prefix path.
    * @return True, if the path is not longer and is a prefix of this path. A
    * path is a prefix of itself.
    */
   public boolean isPrefix(Path prefix)
   {
      if ( getJunctions().size() < prefix.getJunctions().size() )
         return false;
      if ( ! getJunctions().subList(0,prefix.getJunctions().size()).equals(prefix.getJunctions()) )
         return false;
      if ( (getJunctions().size() > prefix.getJunctions().size()) &&
           (getJunctions().get(prefix.getJunctions().size()).getHeight() < prefix.getHeight()) )
         return false;
      return true;
   }

   public static class Junction
   {
      private long height;
      private int index;

      public Junction(long height, int index)
      {
         this.height=height;
         this.index=index;
      }

      public long getHeight()
      {
         return height;
      }

      public int getIndex()
      {
         return index;
      }

      public int hashCode()
      {
         return new Long(height*113+index).hashCode();
      }

      public boolean equals(Object o)
      {
         if ( o == null )
            return false;
         if ( !(o instanceof Junction) )
            return false;
         Junction j = (Junction) o;
         return (j.height == height) && (j.index==index);
      }

      public String toString()
      {
         return "("+height+","+index+")";
      }
   }
}

