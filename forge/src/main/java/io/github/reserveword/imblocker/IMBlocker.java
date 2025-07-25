package io.github.reserveword.imblocker;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.Window;

import io.github.reserveword.imblocker.common.Common;
import io.github.reserveword.imblocker.common.IMBlockerAutoConfig;
import io.github.reserveword.imblocker.common.IMBlockerConfig;
import io.github.reserveword.imblocker.common.ReflectionUtil;
import io.github.reserveword.imblocker.common.accessor.MinecraftClientAccessor;
import io.github.reserveword.imblocker.common.gui.Rectangle;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Common.MODID)
public class IMBlocker {

	public IMBlocker() {
		this(FMLJavaModLoadingContext.get());
	}
	
	@SuppressWarnings("unchecked")
	public IMBlocker(FMLJavaModLoadingContext context) {
		MinecraftClientAccessor.INSTANCE = new MinecraftClientAccessor() {
			@Override
			public void execute(Runnable runnable) {
				Minecraft.getInstance().execute(runnable);
			}
			
			@Override
			public Rectangle getWindowBounds() {
				Window gameWindow = Minecraft.getInstance().getWindow();
				return new Rectangle(gameWindow.getX(), gameWindow.getY(), 
						gameWindow.getWidth(), gameWindow.getHeight());
			}
			
			@Override
			public int getStringWidth(String text) {
				return Minecraft.getInstance().font.width(text);
			}
		};

		IMBlockerConfig.defaultScreenWhitelist.addAll(ForgeCommon.defaultScreenWhitelist);
		if(Common.hasMod("cloth_config")) {
			AutoConfig.register(IMBlockerAutoConfig.class, GsonConfigSerializer::new);
			IMBlockerConfig.INSTANCE = AutoConfig.getConfigHolder(IMBlockerAutoConfig.class).getConfig();
			Class configFactoryCls = null;
			try {
				// 1.19+
				configFactoryCls = Class.forName(
						"net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory");
			} catch (ClassNotFoundException e) {
				try {
					//1.18.x
					configFactoryCls = Class.forName(
							"net.minecraftforge.client.ConfigGuiHandler$ConfigGuiFactory");
				} catch (ClassNotFoundException e1) {
					try {
						//1.17.x
						configFactoryCls = Class.forName(
								"net.minecraftforge.fmlclient.ConfigGuiHandler$ConfigGuiFactory");
					} catch (ClassNotFoundException e2) {}
				}
			}
			Class _configFactoryCls = configFactoryCls;
			Supplier configFactorySupplier = () -> ReflectionUtil.newInstance(_configFactoryCls, 
					new Class[] {BiFunction.class}, new BiFunction<Minecraft, Screen, Screen>() {
						@Override
						public Screen apply(Minecraft client, Screen parent) {
							return IMBlockerAutoConfig.getConfigScreen(parent, Screen.class);
						}
					});
			ModLoadingContext.get().registerExtensionPoint(configFactoryCls, configFactorySupplier);
		}else {
			IMBlockerConfig.INSTANCE = new IMBlockerConfig();
			IMBlockerConfig.INSTANCE.reloadScreenWhitelist(IMBlockerConfig.defaultScreenWhitelist);
		}
	}
}
