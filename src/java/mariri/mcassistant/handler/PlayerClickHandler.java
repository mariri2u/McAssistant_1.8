package mariri.mcassistant.handler;

import java.util.ArrayList;
import java.util.List;

import mariri.mcassistant.helper.Comparator;
import mariri.mcassistant.helper.CropReplanter;
import mariri.mcassistant.helper.Lib;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerClickHandler {
	
	public static PlayerClickHandler INSTANCE = new PlayerClickHandler();
	
	public static boolean TORCHASSIST_ENABLE;
	
	public static boolean CROPASSIST_ENABLE = true;
	public static int CROPASSIST_REQUIRE_TOOL_LEVEL;
	public static boolean CROPASSIST_AREA_ENABLE;
	public static int[] CROPASSIST_AREA_REQUIRE_TOOL_LEVEL;
	public static int[][] CROPASSIST_AREA_AFFECT_POTION;
	public static boolean CROPASSIST_AREAPLUS_ENABLE;
	public static int[] CROPASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL;
	
	public static boolean LEAVEASSIST_ENABLE;
	public static int[][] LEAVEASSIST_AFFECT_POTION;
	public static boolean LEAVEASSIST_AREAPLUS_ENABLE;
	public static int[] LEAVEASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL;
	
	public static boolean BEDASSIST_ENABLE;
	public static boolean BEDASSIST_SET_RESPAWN_ANYTIME;
	public static String BEDASSIST_SET_RESPAWN_MESSAGE;
	public static boolean BEDASSIST_NO_SLEEP;
	public static String BEDASSIST_NO_SLEEP_MESSAGE;
	
	public static boolean CULTIVATEASSIST_ENABLE;
	public static int[] CULTIVATEASSIST_REQUIRE_TOOL_LEVEL;
	public static int[][] CULTIVATEASSIST_AFFECT_POTION;
	public static boolean CULTIVATEASSIST_AREAPLUS_ENABLE;
	public static int[] CULTIVATEASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL;
	
	public static boolean SNEAK_INVERT;
	
	private static List<EntityPlayer> isProcessing = new ArrayList<EntityPlayer>();
	
	private PlayerClickHandler(){}

	@SubscribeEvent
	public void onPlayerClick(PlayerInteractEvent e){
		if(!isProcessing.contains(e.entityPlayer) && !e.entityPlayer.worldObj.isRemote && e.entityPlayer.isSneaking() == SNEAK_INVERT){
			isProcessing.add(e.entityPlayer);
			if(e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK){
				onRightClickBlock(e);
			}else if(e.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK){
				onLeftClickBlock(e);
			}
			isProcessing.remove(e.entityPlayer);
		}
	}
	
	private void onRightClickBlock(PlayerInteractEvent e){
		World world = e.entityPlayer.worldObj;
		IBlockState state = world.getBlockState(e.pos);
		Block block = state.getBlock();
		int meta = block.getMetaFromState(state);
		// ベッド補助機能
		if(BEDASSIST_ENABLE && block == Blocks.bed){
			// いつでもリスポーンセット
			if(BEDASSIST_SET_RESPAWN_ANYTIME){
//	        	ChunkCoordinates respawn = new ChunkCoordinates(e.x, e.y, e.z);
	            if (	world.provider.canRespawnHere() &&
	            		world.getBiomeGenForCoords(e.pos) != BiomeGenBase.hell &&
	            		world.provider.isSurfaceWorld() &&
	            		e.entityPlayer.isEntityAlive() &&
	            		world.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.fromBounds(e.pos.getX() - 8, e.pos.getY() - 5, e.pos.getZ() - 8, e.pos.getX() + 8, e.pos.getY() + 5, e.pos.getZ() + 8)).isEmpty()){
	                e.entityPlayer.setSpawnChunk(e.pos, false, e.entityPlayer.dimension);
	                e.entityPlayer.addChatComponentMessage(new ChatComponentText(BEDASSIST_SET_RESPAWN_MESSAGE));
	            }
			}
			// 寝るの禁止
			if(BEDASSIST_NO_SLEEP){
                e.entityPlayer.addChatComponentMessage(new ChatComponentText(BEDASSIST_NO_SLEEP_MESSAGE));
                e.setCanceled(true);
			}
		}
		// 農業補助機能
		else if(		CROPASSIST_ENABLE && !world.isAirBlock(e.pos) &&
				Comparator.CROP.compareBlock(state) &&
				Comparator.HOE.compareCurrentItem(e.entityPlayer) &&
				Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_REQUIRE_TOOL_LEVEL)){
			if(CROPASSIST_AREA_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_AREA_REQUIRE_TOOL_LEVEL)){
				int count = 0;
				int area = (CROPASSIST_AREAPLUS_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL)) ? 2 : 1;
				for(int xi = -1 * area; xi <= area; xi++){
					for(int zi = -1 * area; zi <= area; zi++){
						BlockPos newpos = new BlockPos(e.pos.getX() + xi, e.pos.getY(), e.pos.getZ() + zi);
						IBlockState s = world.getBlockState(newpos);
						Block b = s.getBlock();
						if(block == b){
							b.onBlockActivated(world, newpos, s, e.entityPlayer, EnumFacing.values()[e.useItem.ordinal()], 0, 0, 0);
							count++;
						}
					}
				}
				ItemStack citem = e.entityPlayer.inventory.getCurrentItem();
				if(citem.getItem() instanceof ItemHoe){
					if(e.entityPlayer.inventory.getCurrentItem().attemptDamageItem(1, e.entityPlayer.getRNG())){
						e.entityPlayer.destroyCurrentEquippedItem();
			            world.playSoundAtEntity(e.entityPlayer, "random.break", 1.0F, 1.0F);
					}
				}else{
					citem.getItem().onBlockDestroyed(citem, world, Blocks.farmland, e.pos, e.entityPlayer);
					if(citem.stackSize <= 0){
						e.entityPlayer.destroyCurrentEquippedItem();
			            world.playSoundAtEntity(e.entityPlayer, "random.break", 1.0F, 1.0F);
					}
				}
				Lib.affectPotionEffect(e.entityPlayer, CROPASSIST_AREA_AFFECT_POTION, count);
				e.setCanceled(true);
			}
		}
		// 耕地化補助機能
		else if(	CULTIVATEASSIST_ENABLE &&
					(block == Blocks.dirt || block == Blocks.grass) &&
					Comparator.HOE.compareCurrentItem(e.entityPlayer) &&
					Lib.compareCurrentToolLevel(e.entityPlayer, CULTIVATEASSIST_REQUIRE_TOOL_LEVEL)){
			int count = 0;
			int area = (CULTIVATEASSIST_AREAPLUS_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, CULTIVATEASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL)) ? 2 : 1;
			for(int xi = -1 * area; xi <= area; xi++){
				for(int zi = -1 * area; zi <= area; zi++){
					ItemStack current = e.entityPlayer.inventory.getCurrentItem();
					if(current.getItem().onItemUse(current, e.entityPlayer, world, new BlockPos(e.pos.getX() + xi, e.pos.getY(), e.pos.getZ() + zi), e.face, 0, 0, 0)){
						count++;
					}
				}
			}
			Lib.affectPotionEffect(e.entityPlayer, CULTIVATEASSIST_AFFECT_POTION, count);
			e.setCanceled(true);
		}
		// 葉っぱ破壊補助機能
		else if(		LEAVEASSIST_ENABLE && !world.isAirBlock(e.pos) &&
				Comparator.LEAVE.compareBlock(state) &&
				Lib.isAxeOnEquip(e.entityPlayer) ){
			int count = 0;
			int area = (LEAVEASSIST_AREAPLUS_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, LEAVEASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL)) ? 2 : 1;
			for(int x = e.pos.getX() - area; x <= e.pos.getX() + area; x++){
				for(int y = e.pos.getY() - area; y <= e.pos.getY() + area; y++){
					for(int z = e.pos.getZ() - area; z <= e.pos.getZ() + area; z++){
						BlockPos pos = new BlockPos(x, y, z);
						IBlockState s = world.getBlockState(pos);
						Block b = s.getBlock();
						int m = b.getMetaFromState(s);
						if(Comparator.LEAVE.compareBlock(s)){
							b.dropBlockAsItem(world, pos, s, 0);
							world.setBlockToAir(pos);
							count++;
						}
					}
				}
			}
            world.playSoundAtEntity(e.entityPlayer, Block.soundTypeGrass.getBreakSound(), Block.soundTypeGrass.getVolume(), Block.soundTypeGrass.getFrequency());
            
			ItemStack citem = e.entityPlayer.inventory.getCurrentItem();
			citem.getItem().onBlockDestroyed(citem, world, block, e.pos, e.entityPlayer);
			if(citem.stackSize <= 0){
				e.entityPlayer.destroyCurrentEquippedItem();
	            world.playSoundAtEntity(e.entityPlayer, "random.break", 1.0F, 1.0F);
			}            
			Lib.affectPotionEffect(e.entityPlayer, LEAVEASSIST_AFFECT_POTION, count);
			e.setCanceled(true);
		}
		// トーチ補助機能
		else if(		TORCHASSIST_ENABLE &&
				(Lib.isPickaxeOnEquip(e.entityPlayer) || Lib.isShovelOnEquip(e.entityPlayer)) ){
			
			ItemStack current = e.entityPlayer.getCurrentEquippedItem();
			ItemStack torch = new ItemStack(Blocks.torch, 1);
			// トーチを持っている場合
			if(e.entityPlayer.inventory.hasItem(torch.getItem())){
				// トーチを設置できた場合
				if(		!world.getBlockState(e.pos).getBlock().onBlockActivated(world, e.pos, world.getBlockState(e.pos), e.entityPlayer, e.face, 0, 0, 0) &&
						!current.getItem().onItemUse(current, e.entityPlayer, world, e.pos, e.face, 0, 0, 0) &&
						torch.getItem().onItemUse(torch, e.entityPlayer, world, e.pos, e.face, 0, 0, 0)){
					e.entityPlayer.inventory.consumeInventoryItem(torch.getItem());
					// トーチの使用をクライアントに通知
					e.entityPlayer.onUpdate();
				}
				// 対象ブロックに対する右クリック処理をキャンセル
				e.setCanceled(true);
			}
		}
	}
	
	private void onLeftClickBlock(PlayerInteractEvent e){
		World world = e.entityPlayer.worldObj;
		IBlockState state = world.getBlockState(e.pos);
		Block block = state.getBlock();
		int meta = block.getMetaFromState(state);
		// 農業補助機能
		if(		CROPASSIST_ENABLE && !world.isAirBlock(e.pos) &&
				Comparator.CROP.compareBlock(state) &&
				Comparator.HOE.compareCurrentItem(e.entityPlayer) &&
				Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_REQUIRE_TOOL_LEVEL)){
			if(CROPASSIST_AREA_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_AREA_REQUIRE_TOOL_LEVEL)){
				int count = 0;
				int area = (CROPASSIST_AREAPLUS_ENABLE && Lib.compareCurrentToolLevel(e.entityPlayer, CROPASSIST_AREAPLUS_REQUIRE_TOOL_LEVEL)) ? 2 : 1;
				for(int xi = -1 * area; xi <= area; xi++){
					for(int zi = -1 * area; zi <= area; zi++){
						BlockPos p = new BlockPos(e.pos.getX() + xi, e.pos.getY(), e.pos.getZ() + zi);
						IBlockState s = world.getBlockState(p);
						Block b = s.getBlock();
						int m = b.getMetaFromState(s);
						if(block == b && meta == m && (b instanceof BlockContainer || m > 0)){
							CropReplanter harvester = new CropReplanter(world, e.entityPlayer, p, s);
							harvester.setAffectToolDamage(xi == 0 && zi == 0);
							b.harvestBlock(world, e.entityPlayer, p, s, world.getTileEntity(p));
							world.setBlockToAir(p);
							harvester.findDrops();
							harvester.harvestCrop();
							count++;
						}
					}
				}
	            world.playSoundAtEntity(e.entityPlayer, Block.soundTypeGrass.getBreakSound(), Block.soundTypeGrass.getVolume(), Block.soundTypeGrass.getFrequency());
				Lib.affectPotionEffect(e.entityPlayer, CROPASSIST_AREA_AFFECT_POTION, count);
			}else{
				// 収穫後の連続クリック対策（MOD独自の方法で成長を管理している場合は対象外）
				if(block instanceof BlockContainer || meta > 0){
					CropReplanter harvester = new CropReplanter(world, e.entityPlayer, e.pos, state);
					block.harvestBlock(world, e.entityPlayer, e.pos, state, world.getTileEntity(e.pos));
					world.setBlockToAir(e.pos);
					harvester.findDrops();
					harvester.harvestCrop();
		            world.playSoundAtEntity(e.entityPlayer, Block.soundTypeGrass.getBreakSound(), Block.soundTypeGrass.getVolume(), Block.soundTypeGrass.getFrequency());
				}
			}
			e.setCanceled(true);
		}
	}
	
	public static boolean isEventEnable(){
		return BEDASSIST_ENABLE || CROPASSIST_ENABLE || LEAVEASSIST_ENABLE || TORCHASSIST_ENABLE;
	}
}
