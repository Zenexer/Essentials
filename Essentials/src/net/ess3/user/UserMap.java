package net.ess3.user;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import static net.ess3.I18n._;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.ess3.api.IUserMap;
import net.ess3.api.InvalidNameException;
import net.ess3.storage.StorageObjectMap;
import net.ess3.utils.FormatUtil;
import org.bukkit.entity.Player;


public class UserMap extends StorageObjectMap<IUser> implements IUserMap
{
	private final Map<String, Player> prejoinedPlayers = new HashMap<String, Player>();

	public UserMap(final IEssentials ess)
	{
		super(ess, "users");
	}

	@Override
	public boolean userExists(final String name)
	{
		return objectExists(name);
	}

	@Override
	public IUser getUser(final String name)
	{
		return getObject(name);
	}

	@Override
	public IUser load(final String name) throws Exception
	{
		final String lowercaseName = name.toLowerCase(Locale.ENGLISH);
		if (!lowercaseName.equals(name))
		{
			final IUser user = getUser(lowercaseName);
			if (user == null)
			{
				throw new Exception(_("userNotFound"));
			}
			else
			{
				return user;
			}
		}
		Player player = prejoinedPlayers.get(name);
		if (player == null)
		{
			player = ess.getServer().getPlayerExact(name);
		}
		if (player != null)
		{
			return new User(ess.getServer().getOfflinePlayer(player.getName()), ess);
		}
		final File userFile = getUserFile(name);
		if (userFile.exists())
		{
			keys.add(name.toLowerCase(Locale.ENGLISH));
			return new User(ess.getServer().getOfflinePlayer(name), ess);
		}
		throw new Exception(_("userNotFound"));
	}

	@Override
	public void removeUser(final String name) throws InvalidNameException
	{
		removeObject(name);
	}

	@Override
	public Set<String> getAllUniqueUsers()
	{
		return getAllKeys();
	}

	@Override
	public int getUniqueUsers()
	{
		return getKeySize();
	}

	@Override
	public File getUserFile(String name) throws InvalidNameException
	{
		return getStorageFile(name);
	}

	@Override
	public IUser getUser(final Player player)
	{
		return getObject(player.getName());
	}

	@Override
	public IUser matchUser(final String name, final boolean includeOffline) throws TooManyMatchesException, PlayerNotFoundException
	{
		return matchUser(name, true, includeOffline, null);
	}

	@Override
	public IUser matchUserExcludingHidden(final String name, final Player requester) throws TooManyMatchesException, PlayerNotFoundException
	{
		return matchUser(name, false, false, requester);
	}

	public IUser matchUser(final String name, final boolean includeHidden, final boolean includeOffline, final Player requester) throws TooManyMatchesException, PlayerNotFoundException
	{
		final UserMatch match = matchUsers(name, includeHidden, includeOffline, requester);
		if (!match.hasNext())
		{
			throw new PlayerNotFoundException();
		}
		
		final IUser user = match.next();
		
		if (match.hasNext())
		{
			throw new TooManyMatchesException(match);
		}

		return user;
	}

	@Override
	public UserMatch matchUsers(final String name, final boolean includeOffline)
	{
		return matchUsers(name, true, includeOffline, null);
	}

	@Override
	public UserMatch matchUsersExcludingHidden(final String name, final Player requester)
	{
		return matchUsers(name, false, false, requester);
	}

	public UserMatch matchUsers(final String name, final boolean includeHidden, final boolean includeOffline, final Player requester)
	{
		final UserMatch match = UserMatch.match(ess, ess.getUserMap().getUser(requester), name);
		
		if (includeHidden)
		{
			match.setHiddenSearch(true);
		}
		
		if (includeOffline)
		{
			match.setOfflineSearch(true);
		}
		
		return match;
	}

	@Override
	public void addPrejoinedPlayer(Player player)
	{
		prejoinedPlayers.put(player.getName().toLowerCase(Locale.ENGLISH), player);
	}

	@Override
	public void removePrejoinedPlayer(Player player)
	{
		prejoinedPlayers.remove(player.getName().toLowerCase(Locale.ENGLISH));
	}
}
