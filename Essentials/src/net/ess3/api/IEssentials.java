/*
 * Essentials - a bukkit plugin
 * Copyright (C) 2011 - 2012  Essentials Team, http://contributors.ess3.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ess3.api;

import java.util.List;
import java.util.logging.Logger;
import net.ess3.EssentialsTimer;
import net.ess3.economy.register.Methods;
import net.ess3.metrics.Metrics;
import net.ess3.settings.SpawnsHolder;
import net.ess3.storage.StorageQueue;
import net.ess3.user.UserMatch;
import org.bukkit.Server;
import org.bukkit.World;


public interface IEssentials extends IComponent
{
	void addReloadListener(IReload listener);

	int broadcastMessage(IUser sender, String message);

	II18n getI18n();

	ISettings getSettings();

	IRanks getRanks();

	IJails getJails();

	IKits getKits();

	IWarps getWarps();

	IWorth getWorth();

	IItemDb getItemDb();

	IUserMap getUserMap();

	IBackup getBackup();

	ICommandHandler getCommandHandler();

	World getWorld(String name);

	Methods getPaymentMethod();

	void setRanks(IRanks groups);

	void removeReloadListener(IReload groups);

	IEconomy getEconomy();

	Server getServer();

	Logger getLogger();

	IPlugin getPlugin();

	List<String> getVanishedPlayers();

	EssentialsTimer getTimer();

	Metrics getMetrics();

	void setMetrics(Metrics metrics);

	SpawnsHolder getSpawns();

	StorageQueue getStorageQueue();

}
