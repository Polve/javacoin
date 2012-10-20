package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;

/**
 *
 * @author Alessandro Polverini
 */
public class TesterBitcoinFactory extends StandardBitcoinFactory
{
   private Block genesisBlock;

   public TesterBitcoinFactory(Block genesisBlock, ScriptFactory scriptFactory) throws BitcoinException
   {
      super(scriptFactory);
      this.genesisBlock = genesisBlock;
   }

   @Override
   public Block getGenesisBlock()
   {
      return genesisBlock;
   }

   
}
