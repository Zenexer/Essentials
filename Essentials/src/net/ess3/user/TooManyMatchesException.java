package net.ess3.user;

import static net.ess3.I18n._;
import net.ess3.api.IUser;


public class TooManyMatchesException extends Exception
{
	private static final long serialVersionUID = -1458262878150217201L;
	private final UserMatch matches;

	public TooManyMatchesException()
	{
		super();
		matches = null;
	}

	public TooManyMatchesException(UserMatch users)
	{
		super();
		this.matches = users;
	}

	@Override
	public String getMessage()
	{
		matches.reset();
		
		if (matches.hasNext())
		{
			StringBuilder builder = new StringBuilder();
			for (IUser iUser : matches)
			{
				if (builder.length() > 0)
				{
					builder.append(", ");
				}
				builder.append(iUser.getPlayer().getDisplayName());
			}
			return _("tooManyMatchesWithList", builder.toString());
		}
		else
		{
			return _("tooManyMatches");
		}
	}
}
