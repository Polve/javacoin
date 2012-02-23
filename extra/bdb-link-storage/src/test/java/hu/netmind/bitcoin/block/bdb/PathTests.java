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

import org.testng.annotations.Test;
import org.testng.Assert;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
@Test
public class PathTests
{
   private static Logger logger = LoggerFactory.getLogger(PathTests.class);

   public void testSelfPrefix()
   {
      assertPrefix("a1 a2 a3 b2 b3","b3","b3");
   }

   public void testNormalPrefix()
   {
      assertPrefix("a1 a2 a3 a4 a5","a5","a2");
   }

   public void testNormalBranchPrefix()
   {
      assertPrefix("a1 a2 b2 b3 b4 b5","b5","b3");
   }

   public void testNormalCrossBranchPrefix()
   {
      assertPrefix("a1 a2 a3 b3","b3","a2");
   }

   public void testComplexPrefix()
   {
      assertPrefix("a1 a2 a3 b2 b3 b4 c3 c4 c5 d2 d3 d4 d5","c4","a2");
   }

   public void testLateNonPrefix()
   {
      assertNotPrefix("a1 a2 a3 b2 b3 b4 c3 c4 c5 d2 d3 d4 d5","d4","a3");
   }

   public void testBranchNonPrefix()
   {
      assertNotPrefix("a1 a2 a3 b2 b3 b4 c3 c4","c4","b3");
   }

   public void testNonlinearNormalPrefix()
   {
      assertPrefix("a1 a2 a3 b3 b4 a4 a5","a5","a3");
   }

   public void testRecreate()
   {
      Path path = new Tree("a1 a2 a3 b2 b3 b4 c3 c4 c5 d2 d3 d4 d5").getPath("d5");
      Path samePath = new Path(path.getJunctions(),path.getHeight());
      Assert.assertTrue(path.isPrefix(samePath));
      Assert.assertTrue(samePath.isPrefix(path));
   }

   public void testCommonPathOnDifferentBranch()
   {
      Tree tree = new Tree("a1 a2 a3 b3 b4 c3 c4 c5");
      Path common = Path.getCommonPath(tree.getPath("b4"),tree.getPath("c5"));
      Assert.assertNotNull(common);
      Assert.assertEquals(common.getHeight(),1);
   }

   public void testCommonPathOfSameBranch()
   {
      Tree tree = new Tree("a1 a2 a3 b2 b3 b4 c2 c3 c4");
      Path common = Path.getCommonPath(tree.getPath("b4"),tree.getPath("b3"));
      Assert.assertNotNull(common);
      Assert.assertEquals(common.getHeight(),2);
   }

   public void testCommonPathOnRoot()
   {
      Tree tree = new Tree("a1 a2 a3 a4 a5 a6 a7");
      Path common = Path.getCommonPath(tree.getPath("a7"),tree.getPath("a4"));
      Assert.assertNotNull(common);
      Assert.assertEquals(common.getHeight(),3);
   }

   public void testCommonPathButThenDiverge()
   {
      Tree tree = new Tree("a1 a2 a3 a4 a5 a6 a7 a8 b3 b4");
      Path common = Path.getCommonPath(tree.getPath("b4"),tree.getPath("a8"));
      Assert.assertNotNull(common);
      Assert.assertEquals(common.getHeight(),1);
   }

   private void assertPrefix(String tree, String node, String prefix)
   {
      Tree t = new Tree(tree);
      Assert.assertTrue(t.getPath(node).isPrefix(t.getPath(prefix)));
   }

   private void assertNotPrefix(String tree, String prefix, String node)
   {
      Tree t = new Tree(tree);
      Assert.assertFalse(t.getPath(node).isPrefix(t.getPath(prefix)));
   }

   /**
    * Holds a path for each node in the tree.
    */
   public static class Tree
   {
      private Map<String,Path> paths = new HashMap<String,Path>();

      /**
       * Construct a tree with a string representation. Each node is represented
       * by a word separated by spaces. Each word is 2 characters long, the first
       * being the "branch" (starting with 'a'), and the second is the height 
       * (starting with 1).
       */
      public Tree(String tree)
      {
         for ( String node : tree.split(" ") )
         {
            String precedingNode = getPrecedingNode(node);
            if ( precedingNode == null )
               paths.put(node,new Path());
            else
               paths.put(node,getPath(precedingNode).createPath(node.charAt(0)-precedingNode.charAt(0)));
            logger.debug("for tree '"+tree+"', node '"+node+"' created following junctions: "+paths.get(node).getJunctions());
         }
      }

      /**
       * The preceding node is the node with one less height on the same branch.
       * If that node does not exist, preceding branches are tried towards the 'a'
       * branch. For example for node 'c3' 'c2' is the parent. If 'c2' does not exist,
       * then 'b2', or at last 'a2' is tried.
       */
      private String getPrecedingNode(String node)
      {
         if ( (node.charAt(1)=='1') && (node.charAt(0)!='a') )
            throw new IllegalArgumentException("There can be only one root 'a1', but node was: "+node);
         if ( node.charAt(1)=='1' )
            return null; // Root doesn't have preceding nodes
         String precedingNode = ""+node.charAt(0)+((char)(node.charAt(1)-1));
         while ( getPath(precedingNode) == null )
         {
            if ( precedingNode.charAt(0) == 'a' )
               throw new IllegalArgumentException("Node '"+node+"' didn't have a parent");
            precedingNode = ""+((char)(precedingNode.charAt(0)-1))+precedingNode.charAt(1);
         }
         return precedingNode;
      }

      public Path getPath(String node)
      {
         return paths.get(node);
      }
   }
}

