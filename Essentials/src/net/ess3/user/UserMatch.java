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
package net.ess3.user;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.ess3.api.IUserMap;
import org.bukkit.entity.Player;


/**
 * The result of an attempt to match an input pattern to a player.
 */
public final class UserMatch implements Iterable<IUser>, Iterator<IUser>
{
	@Getter
	private boolean valid;
	@Getter
	private final String pattern;
	@Getter(AccessLevel.PRIVATE)
	@Setter(AccessLevel.PRIVATE)
	private String cleanPattern;
	@Getter
	@Setter
	private boolean offlineSearch;
	@Getter
	@Setter
	private boolean nicknameSearch;
	@Getter
	@Setter
	private boolean keywordSearch;
	@Getter
	@Setter
	private boolean exactSearch;
	@Getter
	@Setter
	private boolean usernameSearch;
	@Getter
	@Setter
	private boolean hiddenSearch;
	@Getter(AccessLevel.PRIVATE)
	private Set<String> usernamesToScan;
	private transient Iterator<String> scanIterator;
	private transient IUser cachedNext;
	private final transient IEssentials ess;
	private transient boolean canRemove;
	@Getter
	private final IUser sender;

	private UserMatch(final IEssentials ess, final IUser sender, final String pattern)
	{
		this.ess = ess;
		this.sender = sender;
		this.pattern = pattern.toLowerCase(Locale.US);

		if (pattern == null || pattern.isEmpty())
		{
			cleanPattern = "";
		}
		else
		{
			cleanPattern = pattern.trim();
		}
	}

	/**
	 * Matches the query against the player list.
	 *
	 * @param server
	 * @param pattern
	 * @return
	 */
	public static UserMatch match(final IEssentials ess, final IUser sender, final String pattern)
	{
		final UserMatch match = new UserMatch(ess, sender, pattern);

		String cleanPattern = match.getCleanPattern();
		if (cleanPattern.isEmpty())
		{
			return match.invalid();
		}

		// Check for prefixes.
		int prefixLength;
		prefix_scan:
		for (prefixLength = 0; prefixLength < pattern.length(); prefixLength++)
		{
			switch (pattern.charAt(prefixLength))
			{
			case '!': // Search offline players.
				match.setOfflineSearch(true);
				break;

			case '~': // Search nicknames only.  Not necessarily meant to match the nickname prefix--that is handled separately.
				match.setNicknameSearch(true);
				break;

			case '-': // Search usernames only.
				match.setUsernameSearch(true);
				break;

			case '@': // Keyword.
				match.setKeywordSearch(true);
				break;

			case '=': // Exact match.
				match.setExactSearch(true);
				break;
				
			case '#': // Match hidden players.
				match.setHiddenSearch(true);
				break;

			// It's tempting to add regular expressions to the list, but regexes are unsafe.
			// A properly crafted regex can bring down a thread, at the very least.
			default:
				break prefix_scan;
			}
		}

		// Trim the prefix.
		match.setCleanPattern(cleanPattern = cleanPattern.substring(prefixLength));
		if (cleanPattern.isEmpty())
		{
			return match.invalid();
		}

		// Check for nickname prefix.
		final String nicknamePrefix = ess.getSettings().getData().getChat().getNicknamePrefix().toLowerCase(Locale.US);
		if (cleanPattern.startsWith(nicknamePrefix))
		{
			match.setCleanPattern(cleanPattern = cleanPattern.substring(nicknamePrefix.length()));
			if (cleanPattern.isEmpty())
			{
				return match.invalid();
			}
		}

		// If neither the ~ nor - options were specified, default to both.
		if (!match.isNicknameSearch() && !match.isUsernameSearch())
		{
			match.setNicknameSearch(true);
			match.setUsernameSearch(true);
		}

		final IUserMap userMap = ess.getUserMap();

		// Determine the set of users to scan.
		final Set<String> usernamesToScan;
		if (match.isOfflineSearch())
		{
			usernamesToScan = userMap.getAllUniqueUsers();
		}
		else
		{
			usernamesToScan = new HashSet<String>();
			for (Player player : ess.getServer().getOnlinePlayers())
			{
				usernamesToScan.add(player.getName());
			}
		}

		// Store our data.  We do the matches lazily.  No matching here.
		match.setUsernamesToScan(usernamesToScan);

		return match;
	}

	private boolean matchString(final String text)
	{
		return text.toLowerCase(Locale.US).contains(cleanPattern);
	}

	private void setUsernamesToScan(final Set<String> usernamesToScan)
	{
		this.usernamesToScan = usernamesToScan;
		reset();
	}

	private UserMatch invalid()
	{
		valid = false;
		return this;
	}

	@Override
	public Iterator<IUser> iterator()
	{
		return this; // How do you like that for some shenanigans?
	}
	
	public void reset()
	{
		cachedNext = null;
		canRemove = false;
		scanIterator = usernamesToScan.iterator();
	}

	@Override
	public boolean hasNext()
	{
		if (cachedNext != null)
		{
			return true;
		}

		cacheNext();

		return cachedNext == null;
	}

	@Override
	public IUser next()
	{
		if (cachedNext == null)
		{
			cacheNext();
		}

		final IUser current = cachedNext;

		canRemove = true;
		cachedNext = null;

		return current;
	}

	private void cacheNext()
	{
		canRemove = false;

		cachedNext = nextMatch();
	}

	private IUser nextMatch()
	{
		if (!scanIterator.hasNext())
		{
			return null;
		}
		
		for (String username = scanIterator.next(); scanIterator.hasNext(); username = scanIterator.next())
		{
			// Nickname matching.  Prioritized.  Sort of.
			if (nicknameSearch)
			{
				// Don't move this variable up a level: we don't want to load every user if it's just a username search.
				final IUser user = ess.getUserMap().getUser(username);
				
				if (matchString(user.getData().getNickname()) && checkUser(user))
				{
					return user;
				}
			}
			
			// Username matching.
			if (usernameSearch)
			{
				if (matchString(username))
				{
					final IUser user = ess.getUserMap().getUser(username);
					if (checkUser(user))
					{
						return user;
					}
				}
			}
		}
		
		// No more matches.
		return null;
	}
	
	private boolean checkUser(IUser user)
	{
		// Don't simplify this: it's perfectly verbose.
		if (sender.isOnline() && user.isOnline())
		{
			return offlineSearch || hiddenSearch || sender.getPlayer().canSee(user.getPlayer());
		}
		else
		{
			return true;
		}
	}

	@Override
	public void remove()
	{
		if (canRemove)
		{
			scanIterator.remove();
		}
		else
		{
			throw new RuntimeException("Current state does not permit removal of active entry.");
		}
	}
}
