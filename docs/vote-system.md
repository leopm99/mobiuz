# Vote system

The l2Journey vote manager keeps its existing SQL tables, NPC HTML and voiced command.
The provider adapters also use endpoint references from
https://github.com/Sage-BR/l2j-votesystem/tree/main/HighFive/L2JServer.
Hopzone.eu contracts were compared against https://hopzone.eu/docs.html/.
A reference implementation or a reachable website does not establish that authenticated API calls work.

## Setup

1. Import `dist/db_installer/sql/updates/update_vote_system.sql` into the game database if needed.
2. Configure credentials in `dist/game/config/admin/votesystem.ini`.
3. Set the selected sites and rewards in `dist/game/data/votesystem.xml`.
4. Enable the desired global/individual features in the INI and restart the server.
5. Test one provider at a time, including refusal of duplicate rewards and persistence after restart.

New providers are disabled in the XML until explicitly configured. Credentials are blank.
No live authenticated provider requests or production database updates were performed during development.

## Stable site IDs

| ID | Site |
| --- | --- |
| 0 | L2.TopGameServer.net |
| 1 | ITopZ.com |
| 2 | L2Top.co |
| 3 | L2Votes.com |
| 4 | Hopzone.net |
| 5 | L2Network.eu |
| 6 | L2TopServers.com (legacy) |
| 7 | top.l2jbrasil.com |
| 8 | MMOTOP / legacy l2jtop adapter |
| 9 | L2Topzone.com |
| 10 | L2Servers.com |
| 11 | Shared global rewards, NOT a provider |
| 12 | Hopzone.eu |
| 13 | Hotservers.org |
| 14 | L2Rankzone.com |
| 15 | Top4TeamBR |

IDs 0 through 10 are unchanged. Use `VoteSite.getId()`, not `ordinal()`.
No database migration is needed for the new IDs; existing vote history remains valid.
New provider contracts other than Hopzone.eu are based on the reference source and still need authenticated verification.
Legacy integrations remain available but may require API updates.
The L2JBrasil global adapter now expects JSON rather than scraping its ranking page.

## Per-site XML settings

```xml
<votesite ordinal="12" name="Hopzone.eu"
          individualEnabled="true" globalEnabled="true"
          globalStep="20" checkInterval="60" timeZone="UTC">
    <items>
        <item itemId="57" itemCount="100000" />
    </items>
    <globalItems>
        <item itemId="57" itemCount="50000" />
    </globalItems>
</votesite>
```

- `individualEnabled` and `globalEnabled` default to true for compatibility with existing XML.
- `globalStep` is the number of votes per reward round; zero uses the shared INI setting.
- `checkInterval` is the minimum global polling interval in seconds; zero uses the INI setting.
- `timeZone` is the provider timezone for timestamps without an offset. Numeric Unix timestamps need no timezone conversion.
- `globalItems` optionally replaces entry 11 for that site's global rewards. Omit it to use shared rewards.
- Individual vote reuse still uses `IntervalTimeToNextVote` in hours for all providers.

The global scheduler uses the shortest configured interval, while each site retains its own polling limit.
Unconfigured providers do not perform HTTP calls. The NPC and voice command skip disabled/unconfigured sites.
The existing HTML layout is retained; enable only the sites actually used by the server.

## Queries and errors

`VoteConnectTimeout` and `VoteReadTimeout` are milliseconds, clamped to 1000..30000.
`VoteQueryCooldown` is seconds (1..300), shared by IP and site across NPC and voiced requests.
Already claimed votes are checked locally before contacting a provider.
HTTP failures, malformed JSON, missing counters, provider errors and out-of-range numbers never become zero votes.
Provider URLs, response bodies, credentials and player identifiers are not written to request error logs.
Automatic HTTP redirects are refused; update an endpoint if its provider requires a new URL.
JSON responses are limited to 1 MiB and 32 nesting levels.
Boolean-only legacy APIs cannot supply an original vote timestamp; they continue to use the local confirmation time.

## Hopzone.eu personalized links

With site 12 enabled and its API key/server ID configured, use `.getreward link`
(or the configured VotingCommand followed by `link`). The generated URL is sent privately in chat.
Open it in a browser, vote, then use the ordinary reward command or the NPC.
Old clients may require manually copying the link; no client browser feature is assumed.
The server keeps the generated vote ID bound to the connecting IP and account and never accepts a typed vote ID.
Only a completed vote with a valid timestamp is eligible. This supports the provider's IPv4/IPv6 mismatch workflow.
Pending links are cached in memory for 12 hours; after a restart, generate a new link.
The IP-based reward restriction still applies, even when a personalized link is used.

## Delivery limitation

Claims and global checkpoints are saved before item delivery to prevent replay of partial rewards.
A crash or item-delivery failure between those steps may require GM review.
No automatic retry ledger, donation delivery manager, random reward chances or external GUI was imported.
