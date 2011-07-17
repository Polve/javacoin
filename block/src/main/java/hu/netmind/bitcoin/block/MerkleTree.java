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

package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitCoinException;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Merkle tree is a binary tree of hashes. The representation inside this class
 * is capable of reconstructing the root hash with minimal information, which is the
 * leaf hashes at first. If leafs are removed from the tree, whole branches might be 
 * removed with it, and only a summary hash stays. For example:
 * <pre>
 *        A
 *      /   \
 *     B     C
 *    / \   / \
 *   D   E F   G
 * </pre>
 * If "D" and "E" are removed from the tree, "B" is sufficient to re-calculate the root while
 * being also able to proove that "F" and "G" are in the tree.
 * @author Robert Brautigam
 */
public class MerkleTree
{
   private MerkleNode root;

   /**
    * Create/compute the merkle tree with the leafs supplied. Note: order is important,
    * it should be the leafs from left to right (this also will be the order of pairing 
    * them up).
    */
   public MerkleTree(List<byte[]> leafs)
      throws BitCoinException
   {
      try
      {
         // First create the leaf nodes
         List<MerkleNode> nodes = new ArrayList<MerkleNode>();
         for ( byte[] hash : leafs )
            nodes.add(new MerkleNode(hash,null));
         // Progress levels up until only the root is there
         while ( nodes.size() > 1 )
         {
            List<MerkleNode> nextNodes = new ArrayList<MerkleNode>();
            for ( int i=0; i<nodes.size(); i+=2 )
            {
               MerkleNode node1 = nodes.get(i);
               MerkleNode node2 = null;
               if ( i+1 < nodes.size() )
                  node2 = nodes.get(i+1);
               else
                  node2 = node1;
               // Generate new hash
               MessageDigest digest = MessageDigest.getInstance("SHA-256");
               digest.update(node1.getHash());
               digest.update(node2.getHash());
               byte[] firstHash = digest.digest();
               digest.reset();
               MerkleNode parent = new MerkleNode(digest.digest(firstHash), 
                     new MerkleNode[] { node1, node2 });
               nextNodes.add(parent);
            }
            nodes = nextNodes;
         }
         // When over, the nodes will only contain one entry (the root)
         root = nodes.get(0);
      } catch ( NoSuchAlgorithmException e ) {
         throw new BitCoinException("can not find sha-256 algorithm for merkle hash calculation",e);
      }
   }

   /**
    * Remove a hash from the merkle tree. Removing the hash potentially also compacts
    * the tree somewhat if a whole branch can be then removed.
    */
   public void removeLeaf(byte[] hash)
   {
      // TODO
   }

   /**
    * Return the root hash of this tree.
    */
   public byte[] getRoot()
   {
      return root.getHash();
   }

   public static class MerkleNode
   {
      private byte[] hash;
      private MerkleNode[] children;

      public MerkleNode(byte[] hash, MerkleNode[] children)
      {
         this.hash=hash;
         this.children=children;
      }

      public byte[] getHash()
      {
         return hash;
      }
      public MerkleNode[] getChildren()
      {
         return children;
      }
   }
}


