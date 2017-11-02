# Developing JSettlers

## Contents

- Overall structure of the code and project
- Tips for debugging
- Setup instructions for JSettlers as an Eclipse project
- Build Setup and Results
- Recommended debug/run configurations for testing
- To configure a sqlite database for testing
- Current partially-done work
- To do: The current TODO list
- Developing with a database (JDBC)
- Internationalization (I18N)
- Robots (AI)
- Network Communication and interop with other versions or languages
- Coding Style
- Release Testing
- JSettlers on Github



## Overall structure of the code and project

### Project layout

This project uses ant (or IDEs) to build. For developer familiarity, the project
uses the directory structure/layout of a maven/gradle project. v2 and later
versions use gradle to build. Stable versions 1.x.xx used ant.

Also see the "Build Setup and Results" section.

### Packages and notable classes

The most important major classes have several paragraphs of class javadocs
describing their structure and interactions. If something is unclear after
reading those docs and this README section, please file an issue at github
or email `jsettlers@nand.net` to clarify things.

The main server class is `soc.server.SOCServer`; clients' requests and actions
are dispatched into `SOCServerMessageHandler`, `SOCGameMessageHandler`, and
`SOCGameHandler`. The client communication and game-list window is in
`soc.client.SOCPlayerClient`, and in-game interface is in
`soc.client.SOCPlayerInterface`. Game state is held at the server in
`soc.game.SOCGame` and its fields; only partial game state is known at clients.
The game's "business logic" is mostly in `SOCGame`, `SOCPlayer`, and `SOCBoard`.
The sea board and scenarios use `SOCBoardLarge`.

Communication is described in soc.message.SOCMessage. Robots talk with the
server like normal human clients. Most robot messages are per-game; instead
of being handled in SOCRobotClient, these are handled in a loop in
SOCRobotBrain.run().

Game options and scenario rules are controlled through SOCGameOption;
see initAllOptions javadoc for a list. Options have flags for their properties
("hidden internal option", "drop if not set", etc). All scenario-related
game option keynames start with `_SC_`, and provide special rules for the
scenario.

Coding is done in Java 7, but should compile cleanly in newer JDKs. (v1.2 and
v2.0 used java 5 for backwards compatibility; earlier versions used java 1.4.)
The build system is Ant, which is natively understood by Eclipse, or Gradle.
Use any IDE you want, including vi.  Use spaces, not tabs.  Please try to keep
the other conventions of the code already there (see "Coding Style" below for
more details.).

When adding new methods or fields, describe them in javadoc, including the
`@since` marker and the one-sentence summary (even though some old methods
don't have that summary).

When submitting patches, please use unified diff (`-ur`) format.

For more information about the AI, please see the "Robots (AI)" section
and Robert S Thomas' dissertation.

For more information about the board coordinates, see the dissertation appendix A,
or javadocs in `soc.game.SOCBoard`, and `/docs/hexcoord.gif` and `hexcoord-6player.gif`.
To show piece coordinates in the board's tooltips, in the game window chat box type:
`=*= showcoords`  To no longer show those coordinates, type: `=*= hidecoords`

The client's structure was refactored in 2.0 by Paul Bilnoski.  Paul's description of this work:

> Added two major new pieces of API: PlayerClientListener and GameDisplay. These are spliced between the `SOCPlayerClient` and AWT UI code, primarily found in `SOCPlayerInterface`. The APIs are used by the player client abstract network communication away from the UI - to communicate with the UI about network events received and receive events to send to the server. Some of it is done using inner classes and interfaces which should be split out into an improved package structure.


## Tips for Debugging

You can use debug commands in any practice game, by typing into the game chat
textfield.  Type `*help*` for a list of commands.  Debug commands can give resources
or dev cards to players, freely place pieces on the board, remove a bot from the
game, etc.  There are also a few robot debugging commands, not listed in `*help*`;
see the "Robots (AI)" section.

To help with security and prevent cheats, by default debug commands are disabled
except for practice games.  If you need to use debug commands on a multi-player
server, start that server with `-Djsettlers.allow.debug=Y` and connect with username `debug`.
For security, please use sqlite or another database and make a "debug" account with
a password (see [Readme.md](../Readme.md) section "Database Setup").  Except for
practice games, no other username can use debug commands.

To print the contents of messages sent between the server and client, start the
client with vm argument `-Djsettlers.debug.traffic=Y` (this goes before `-jar` if using
the command line). This works for the player client and the robot client, including
bots started as part of the SOCServer.

If you want to inspect the game object state at the server or robot:

- Set a breakpoint at `SOCServer.processDebugCommand` or `SOCServerMessageHandler.handleGAMETEXTMSG`
- send a text message from a client's chat textfield, and inspect the game obj
  at that point
- To inspect game state at robot, breakpoint `SOCRobotClient.treat` and send a
  text message like `*BOTLIST*` or `robot2:current-plans`
- On Linux/Unix JVMs, you can print a stack trace / thread dump at the server by
  sending `SIGQUIT (kill -QUIT pidnumber)` . In deadlocks the thread dump won't
  show what has an object locked, but may show what's waiting on the object.

Some game options are meant to be set by the server during game creation,
not requested by the client.  Their option keynames all start with '_'.
(These are used for game scenario rules.)  The New Game Options frame normally
hides them during game setup.  If you want to show them:

- Launch the client
- Start a Server, or Join a Server
- In the Nickname field, type: `debug`
- Click Practice or New Game

The values you set may still be overridden by the server in SOCGameOption.adjustOptionsToKnown.

To use the "Free Placement" debug mode, type this debug command on your turn:

	*FREEPLACE* 1

You can then build and place pieces without consuming resources, by clicking
or right-clicking on the board.  (Don't use the "Buy" buttons for Free
Placement.) The placements must follow game rules, except that they cost
no resources. To place for another player, click their face icon.  You cannot
remove a piece once it is placed.

During initial placement, you can use Free Placement for any player/bot to
set up some or all of the initial settlements and roads/ships. The number of
settlements must equal the number of roads/ships before the game can continue.

The placed pieces accumulate victory points for their players as normal, and
Longest Road is tracked.  If a player gets 10 VP total, the game will be over
when their turn arrives; your own player would win the game immediately.

To exit Free Placement mode, either click "Done" (which will end your turn)
or type this debug command:

	*FREEPLACE* 0

When exiting Free Placement mode during initial placement, all players must have
either 1 settlement and road, or at least 2 settlements and roads.  This allows
the game setup routines to work properly during this debug mode.  The current
player will be set to the first player (if 2+ settlements/roads placed) or the
last player (1 settlement placed).

In Eclipse, increase the console buffer size before long test runs, so you can
scroll up to see the entire game's debug output.  Go to Eclipse Preferences ->
`Run/Debug` -> `Console`, and either un-check "Limit console output", or set it
to a size larger than the default 80000 characters.  Robots and network messages
can create a lot of output, so can javadoc generation.

These debug commands are used at the client to help show the board's status:

- `=*= showcoords` or `=*= hidecoords`: show or hide coordinates of the playing
  piece, hex, etc currently at the mouse pointer's location.

- `=*= show: all` or `=*= hide: all`: show or hide potential and legal piece
  placements which are currently valid for player 0 (sits in the upper-left
  corner); available only for the sea board layout, not the classic 4- or
  6-player boards. Yellow shapes show legal coordinates, green shows potential,
  red shows "on land". To show only some piece types' legals and potentials, 
  instead of 'all' use an index number from the
  `SOCBoardPanel.debugShowPotentials` javadoc.

If you want the robots to play a few games without any humans, set this
property on the command line when starting the server:
`-Djsettlers.bots.botgames.total=5`. As each game finishes, a new bot-only
game will be started until 5 have been played. You can optionally use the client
to observe a bot-only game. Also if `jsettlers.bots.botgames.total` != 0, at any
time the client can create a new game and start it as bots-only using the debug
command `*STARTBOTGAME* [maxBots]`. See the "Robots (AI)" section for more
details on bot testing.

### JSettlers client properties for debugging and testing

To use any of these, specify them in the IDE or java command line as JVM
parameters (before the SOCPlayerClient class name, not after):

- `-Djsettlers.locale=es_MX` - Use a different locale
- `-Djsettlers.debug.traffic=Y` - Print network traffic; see above for details
- `-Djsettlers.debug.clear_prefs=PI_width,PI_height` - Remove these persistent
  preferences at startup. See SOCPlayerClient PREF_* fields for all name keys.


## Setup instructions for JSettlers as an Eclipse project

Written for Eclipse 3.6, should be applicable to other versions with minor changes.
These instructions can be adapted to import JSettlers and its `build.xml` into
other IDEs.

- For protobuf:
     - Locate `protobuf-lite-3.0.1.jar` by
       [searching maven for protobuf-lite](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22protobuf-lite%22),
       which will get you to a maven repo folder like http://repo1.maven.org/maven2/com/google/protobuf/protobuf-lite/3.0.1/
     - Download it to the project's top-level `lib` directory
- Choose File -> New -> Project... -> Java -> Java Project from Existing Ant Buildfile.
- Browse to the cloned repo's `build.xml`, select the "javac" task in target "compile"
- Check the box "Link to the buildfile in the file system"
- Hit Finish.
- Project -> Properties: Resource: Text file encoding: UTF-8 -> OK
- Project properties -> Java build path -> Libraries -> Add External JARs
  -> Browse to `lib/protobuf-lite-3.0.1.jar`
- To add the `soc.proto.*` classes generated by protobuf:  
  Project properties -> Java build path -> Source tab -> Link source...
     - Linked folder location: Browse to generated/src/proto/main/java
     - Folder name: proto-gen
     - click Finish
- To add the `resources` directory:
     - In the Package Explorer tree right-click the project name -> New ->
       Folder. In that dialog click Advanced, select the radio button
       "Link to alternate location (Linked Folder)", then Browse to
       `src/main/resources/resources`, click Finish.
- To add the `test` directory and JUnit tests:
     - In the Package Explorer tree right-click the project name -> New ->
       Folder. Click Advanced, select "Link to alternate location (Linked
       Folder)", Browse to `src/test`, click Finish.
     - In Package Explorer expand the `test` folder and right-click `java` ->
       Build Path -> Use as Source Folder.
- You'll need to run the `build` target once before you run JSettlers,
  to copy resources into `target` from `src/main/resources/`.

Continue reading to see how to set up the builds and the coding style in Eclipse.


## Build Setup and Results

Before building, make sure you have the Java Development Kit version 7 or later.
If you simply want to run the client or server, you only need the Java Runtime
(JRE). If you wish to maintain a user database for your server, you need MySQL
or PostgreSQL installed and configured, or the sqlite jdbc driver for a
file-based local database.

This project was designed to build with eclipse, or with the Ant tool available
from [http://ant.apache.org/](http://ant.apache.org/).

If not using an IDE like eclipse, check the `build.xml` file. There may be
build variables you may want to change locally. These can be changed by
creating a `build.properties` file, or from the ant command line by passing
a `-Dname=value` parameter.

There are several build targets, here are the most useful ones:

- `build`: create project jar files. (default)
- `clean`: clean the project of all generated files
- `dist-src`: create a tarball of the source tree (jsettlers-3.x.xx-src.tar.gz)
- `dist-full`: `build` & `dist-src` and a tarball of the source + built JARs (jsettlers-3.x.xx-full.tar.gz)
- `javadoc`: create JavaDoc files in "target/docs/api"
- `build-i18neditor`: create `PTE.jar` for maintaining i18n translations (not built by default)

All files created by building are in the `target` directory, including
JARs, Java .class files, and JavaDoc files. Distribution tarballs, zip
files, and installation files are placed in `target/dist`. If you run dist-src or
dist-full, run the `dist-tar-clean` target afterwards to remove temp files.

Note: Even if you're in an IDE running SOCServer or SOCPlayerClient as Java apps,
first build either the `build` or `compile` target to copy resources into
`target/classes/resources/` from `src/main/resources`; otherwise startup will
fail with this error:

    Packaging error: Cannot determine JSettlers version


## Recommended debug/run configurations for testing

In my IDE's JSettlers project, I've created these debug/run configurations:

    Java applet: soc.client.SOCApplet
        width 700, height 500
        parameters: PORT = 8880

    Java applications:
        cli-noargs: soc.client.SOCPlayerClient

        socserver: soc.server.SOCServer
            arguments: -o N7=t7 -o RD=y -Djsettlers.startrobots=7
                -Djsettlers.allow.debug=Y 8880 20 dbuser dbpass

        socserver-sqlite (_optional_): soc.server.SOCServer
            arguments: -o N7=t7 -o RD=y -Djsettlers.db.url=jdbc:sqlite:jsettlers.sqlite
                -Djsettlers.startrobots=7 -Djsettlers.allow.debug=Y
                -Djsettlers.accounts.admins=adm 8880 20 dbuser dbpass
            working directory: filesystem: JSettlers2/target

  The server will start 7 bots with the above configuration.  If you need to
stop and start your own bots, then add `-Djsettlers.bots.cookie=cook` to the
server configuration arguments, and create these Java application configs:

    robot1: soc.robot.SOCRobotClient
        arguments: localhost 8880 robot1 r1 cook
    robot2: soc.robot.SOCRobotClient
        arguments: localhost 8880 robot2 r2 cook

For automated functional testing, the project also includes the script
`src/test/bin/test_func_srv_startup_params.py`; run and update this script if
you are developing anything related to game options or jsettlers properties.


## To configure a sqlite database for testing

These instructions are written for Eclipse 3.6. See also the "Developing with a
database (JDBC)" section of this readme. JSettlers+sqlite works with standard
Eclipse; the j2ee eclipse also has a convenient data browser. Note that
[Readme.md](../Readme.md) mentions a command-line option
`-Djsettlers.db.jar=driverfile.jar`; that's needed only while running the
jsettlers JAR from the command line, not running inside the IDE.

- See the `socserver-sqlite` IDE Run Configuration in the previous section;
  this config includes the sqlite database you're about to configure.
- Download the driver from https://bitbucket.org/xerial/sqlite-jdbc.
  The downloaded JAR might have a name like `sqlite-jdbc-3.15.1.jar`.
  These instructions use a generic name `sqlite-jdbc-3.xx.y`.
- Project properties -> Java build path -> Libraries -> Add External JARs ->
     Browse to `sqlite-jdbc-3.xx.y.jar`
- If using eclipse j2ee instead of basic Eclipse:
  - Eclipse menu -> prefs -> data mgmt -> connectivity -> driver definitions -> Add
  - Select `SQLite JDBC Driver`
  - Jar list -> edit (or add, if empty) -> navigate to `sqlite-jdbc-3.xx.y.jar` -> OK
- Build the project from `build.xml`, to copy `target/classes/resources/version.info`
  and other resources from `src/main/resources`

- Create and initialize the db file:
  - in Run Configurations dialog:
    - duplicate `socserver-sqlite` -> `socserver-sqlite-setup`
        - program arguments: add at beginning of args: `-Djsettlers.db.script.setup=../src/main/bin/sql/jsettlers-tables-sqlite.sql`
  - Run the `socserver-sqlite-setup` configuration
  - This line should appear in the console: `Setup script was successful. Exiting now.`
- If using eclipse j2ee, add the database so you can add tables and query it:
  - `Window -> show view -> other... -> data mgmt -> data source explorer`
  - Right-click Database Connections, choose New
    - Type: SQLite ; give a description and click Next
    - Browse to `jsettlers.sqlite` (it's most likely in your workspace, or the
      server's working directory)
  - Click "Test Connection"
  - Click Finish
- Run the `socserver-sqlite` configuration
  - This line should appear in the console: `User database initialized.`
- The database is now ready for use, development, and debugging.
- To create player users, see [Readme.md](../Readme.md) and use `SOCAccountClient`.
  The first account you create should be `adm` (named in property
  `-Djsettlers.accounts.admins=adm`) which can then create others.


## Current partially-done work

- Refactor SOCMessage classes to use templates
- Some SOCMessage classes (SOCGames, SOCJoinGameRequest) accept objects like
  SOCGame and parse/create them; over-the-network communication will always be
  strings only.
- Search the source for `/*18N*/` and externalize those strings; see "I18N"
  section below


## To do: The current TODO list

Work on these possible roadmap items has not yet begun. This list is ranked
from easier, to more difficult. You can also search the source for TODO for
ideas.

- Visual reminder to player when they've made a trade offer
- Show # VP when choosing where to sit, if game is in progress
- Keyboard shortcuts for "roll", "done" buttons
- Occasionally the board does not re-scale at game reset
- Kick robots if inactive but current player in game, assume they're buggy (use forceEndTurn)
- Control the speed of robots in practice games
  - Adjust `SOCRobotBrain.pause`, `ROBOT_FORCE_ENDTURN_TRADEOFFER_SECONDS`, etc
- Sound effects
- Add more functional and unit tests, in `src/test/bin/` and `src/test/java/` directories and `build.xml`
- Possible: Auto-add robots when needed as server runs, with server active-game count
    - Only do so if `jsettlers.startrobots` property is set
- refactor: `ga.getPlayer(ga.getCurrentPlayer())` or `getClient().getClientManager()`
- Refactor `SOCRobotClient`: Move simple handle-methods which don't put the
  message into brainQ, but only update game fields/methods, into
  SOCDisplayless if possible.
- Refactor: `SOCGameOption` static methods to check and change values within a set
- Refactor: name of dev-cards consolidate
- Refactor: resource-type constants consolidate somewhere (Clay, Wheat, etc)
    - Currently in 2 places: `SOCResourceConstants.CLAY` vs `SOCPlayerElement.CLAY`
- Refactor: combine the `cli/displayless/robot` endturn-like methods. For example,
  search for `ga.setCurrentDice(0)`, or `newToOld`, or `ga.resetVoteClear`
- Machine-readable specs for each network message type's parameters (type,
  required/optional, etc) which could potentially generate a client library
  in other languages or validate the java message classes
- Sample client code for the network protocol in python, js, or another language
- Docker (dockerfile in git) or other containerization, including sqlite jdbc and
  a `jsserver.properties` using sqlite
    - Related bootstrapping issue: admin doc reminder to create admin account
      right away, name that admin in jsserver.properties
- User documentation is out of date; unsure if any user ever reads it anyway
- At board reset, game observers not currently handled properly
- Property for read-only database use without errors
- Game "owner" with extra powers (kick out player, etc)
    - What happens if owner loses connection?
- Customize bot names (txt file or startup property) in srv.setupLocalRobots
- "Hot seat" mode (multiple human players sharing a screen on 1 client)
- Monitoring: Command line utility or html-based dashboard: Uptime, health of
  bots, currently active/total games from `*STATS*` cmd, client versions, any
  errors, etc
- Per-game thread/message queue at server (use SOCMessageForGame.getGame)
- HTML5 client
- Cities & Knights support
    - UI mock-ups
    - state change / network message plans
    - robot support
- Support for multiple game types
    - At server: Other game types would extend GameHandler (like SOCGameHandler)
    - When client connects: Game list, scenario list, and game options would
      need to deal with multiple game types
    - SOCJoinGame message: Add optional gametype field at end (blank for
      current type SOC; older clients wouldn't send the field)


## Developing with a database (JDBC)

JSettlers can optionally use a database to store users and passwords, game
score history, and robot parameters.  If databases interest you, the project
welcomes contributions. Please keep these things in mind:

- The DB is an optional part of jsettlers, other functions can't rely on it.
- DB code should be vendor-neutral and run on mysql, postgres, sqlite, oracle, etc.
  Please test against sqlite and at least one other db type before sending a pull request.
- See [Readme.md](../Readme.md) for JDBC driver download sites, URL syntax,
  and server command-line arguments.
- For test runs inside Eclipse, add the JDBC driver to the project's
  build path -> Libraries -> Add External JAR, or add it to the classpath tab of
  SOCServer's eclipse Run Configuration; that option is
  useful when testing against multiple database types.
- See also the "To configure a sqlite database for testing" section of this readme.


## Internationalization (I18N)

An internationalization framework was put into place for v2.0.00.
Temporary work in progress is surrounded by marker comments:
`/*I*/"{0} has won the game with {1} points."/*18N*/`

When building strings that the user will see, don't use + to build the strings;
use strings.get or strings.getSpecial with parameter placeholders such as {0}.
(At the server, use `messageToPlayerKeyed`, `messageToPlayerKeyedSpecial`,
`messageToGameKeyed`, etc.). The client strings live in
`soc/client/strings/data*.properties`.  An example commit is 68c3972.
The server strings live in `soc/server/strings/*.properties`.  An example
commit is 3e062b7. See the comments at the top of any *.properties file for
format details.

If an i18n string lookup's english text isn't obvious from the key, add it as a
comment to make searching the source for strings easier:

	setTooltipText(strings.get("hpan.points.total.yours"));  // "Your victory point total"

Note that all java properties files use `ISO-8859-1`, not `UTF-8`, as their
encoding. Make sure your editor or IDE honors this encoding format.
Unfortunately you must use `\uXXXX` for characters outside that 8-bit encoding's
range, directly or with the `native2ascii` utility:

	native2ascii -encoding utf8 examplefile.utf8 examplefile.properties

You can use the included editor `net.nand.util.i18n.gui.PTEMain` to edit
localized strings for two locales side by side; this editor has unicode
support and color hilighting, and will convert to `ISO-8859-1` (with unicode
escapes) automatically when saving. See `src/main/java/net/nand/util/i18n/README.txt`
for more details.

Before running PTEMain for the first time, you must build the JSettlers build
target (using Ant or with `build.xml` imported into your IDE) so that the
PTEMain editor's own externalized strings will be available.

When starting the editor this message is harmless, because preferences are stored per-user:

    Dec 6, 2013 3:59:16 PM java.util.prefs.WindowsPreferences <init>
    WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0x80000002. Windows RegCreateKeyEx(...) returned error code 5.

To override system locales for testing, launch the client with vm argument
`-Djsettlers.locale=es` (this goes before -jar if using the command line).

Pseudolocalization for testing (en_AA locale) uses StringUtil from the JBoss
Ant-Gettext utilities. See command-line utility
`net.nand.util.i18n.PropsFilePseudoLocalizer`.

Thanks to Luis A. Ramirez for building the initial client i18n framework and
advocating i18n-friendly programming practices throughout the code base with
helpful specific suggestions.


## Robots (AI)

If you're interested in experimental changes to the jsettlers robots, or writing
a "third-party" robot to connect to jsettlers, read this section.

The project was originally started as Robert S Thomas' PhD dissertation about AI
agents, so there's some instrumentation for the bots but it's not entirely
documented.  For a technical overview of how the bots plan their actions, start
at the SOCRobotBrain class javadoc.

There are a few bot debugging commands, such as print-vars and stats. To send
them, type `botname:command` into the chat textbox while playing or observing a
game with bots: `robot 7:stats`. See `SOCRobotClient.handleGAMETEXTMSG_debug`
for more details.

You can also build pieces for the bots using "Free Placement" debug mode (see
above) to help set up debugging or testing situations, including the game's
initial placement.

When testing with the robots, you may need to send them resources or commands
on their turn. The easiest way to do that is to type the debug command but don't
hit enter, then wait for the bot to offer a trade. The trade offer will give a
few seconds to send the command. On the other hand if you want the bots to move
quickly, use the No Trading house rule and play on the 6-player board, where the
bots have shorter delays since there might be more players (but you can also use
this board with 2-4 players).

If you're looking to make minor changes, it's probably best to fork the
`soc.robot` package and go from the classes and javadocs there.  For a larger
change, some parts of soc.robot might still be useful to talk with the server
and track the game's other players.  Robert S Thomas' dissertation also has
some discussion on those structures.

Right now bots are instantiated within the server for security; for a 3rd-party
bot, start with the `soc.robot.SOCRobotClient` class. You can start a bot
separately from the server through the `SOCRobotClient` constructor or main
method. For trivial example subclasses extending `SOCRobotClient` and
`SOCRobotBrain`, see `soc.robot.sample3p.Sample3PClient` and `Sample3PBrain`.
The `Sample3PClient` class javadoc mentions useful server properties such as
`jsettlers.bots.percent3p` and `jsettlers.bots.timeout.turn`.

The server generates a security cookie that bots must send at connect. You can
view the cookie by starting the server with `-Djsettlers.bots.showcookie=Y` or
override it with something like `-Djsettlers.bots.cookie=foo`

For bot testing and statistics, you can have the server run some robot-only
games (no human players) with the jsettlers.bots.botgames.total property.
To run 7 robot-only games in a row, with each game randomly choosing from 10
robot players, you could start the server with:
`-Djsettlers.startrobots=10 -Djsettlers.bots.botgames.total=7`. The robot-only
games run at a quick pace, about 2 minutes for a 4-player game. You can use the
jsettlers client to observe a bot game as it plays.

To speed up or slow down robot-only games, start the server with this tuning
option to set the length of SOCRobotBrain pauses during bot-only games: For
example `-Djsettlers.bots.fast_pause_percent=10` will pause for only 10% as long
as in normal games.

If `jsettlers.bots.botgames.total` != 0 (including < 0), at any time the client
can create a new game, join but not sit down at a seat, and start that game as
bots-only using the debug command `*STARTBOTGAME* [maxBots]` to test the bots
with any given combination of game options and scenarios.

For robustness testing, the `SOCRobotClient.debugRandomPause` flag can be enabled
by editing its declaration to inject random delays into handling messages and
commands from the server.


## Network Communication and interop with other versions or languages

Players' clients talk to the JSettlers server using a simple string-based
message format. See "Overall Structure" for an overview on network message
handling. Communication format and more details are described in
`soc.message.SOCMessage`. To see all message traffic from a client, set
`jsettlers.debug.traffic=Y` (see "Tips for Debugging" section).

Keeping the network protocol simple helps with interoperability between different
versions and implementations. At the TCP level, JSettlers messages are unicode
Strings sent using `DataOutputStream.writeUTF(String)` and
`DataInputStream.readUTF()`.

AI bots, server monitors, or other client programs can also be written in
non-Java languages. Such clients could communicate with JSettlers by
implementing `writeUTF` and `readUTF` (searching online finds sample code
for popular languages), looking for the message types they need to work with
(listed in `SOCMessage`), and decoding/encoding those message types. See
"Overall Structure" above and class javadocs of `SOCMessage`,
`SOCDisplaylessPlayerClient`, `SOCRobotClient`,
`soc.server.genericServer.Server`, and `SOCServer`.


## Coding Style

Scroll down a bit if you just want to see how to set up the style in Eclipse.

This is the project's coding style, based on the original authors' style.
This section has more detail than you need to know, but it's here if you're
interested. Although we may not agree with every detail of it, for consistency's
sake please keep the conventions of the code that's already there.

Use spaces, not tabs.  The basic indent is 4.  Place braces on their own line.
Lines should be less than about 120 characters long; if you have to indent too
much, consider refactoring into a new method.

Use 1 blank line between methods.  If you have nested classes, use 3 blank lines
between them, and comment each one's closing brace with the class name. If you
have a long method whose work can be divided into "sections", preface each
section with a `/** javadoc-style */` multi-line comment.

Import individual classes, not entire packages like `soc.game.*`. Some classes
deliberately don't import some others for better separation, especially across
packages, or will import a few specific classes for javadocs only.

If a declaration line is getting too long (about 120 characters), break it and
indent it slightly from the first line, not aligned with the method name. `throws`
declarations are also always indented on the next line:

``` java
    public SOCGameOption(String key, int minVers, int lastModVers,
        boolean defaultValue, boolean dropIfUnused, String desc)
        throws IllegalArgumentException
```

If related methods have the same name but different sets of arguments, indent
all their declarations the same way for easier comparison.

Some methods return in the middle of their body, depending on conditions.
That kind of early return should be marked with a prominent comment such as:

    return;   // <--- Early return: Temporary piece ---

If an i18n string lookup's english text isn't obvious from the key, add it as a
comment to make searching the source for strings easier:

``` java
    setTooltipText(strings.get("hpan.points.total.yours"));  // "Your victory point total"
```

Use parentheses around all boolean expressions and their parts, to make them
easier to see as such:

``` java
    flagvalue = (state == xyz);
    somevar = (testflag) ? a : b;
    somecondition = ((state == xyz) || (players < 4));
```

Some methods end with this style of code:

``` java
    if (x)
        return y;
    else
        return z;
```

In those cases, the return is part of the logical flow of the if-statement:
Both y and z are normal and valid, and deserve equal "visual weight" and indenting.
That section of code should not be reformatted to something like:

``` java
    if (x)
        return y;
    return z;
```

because conventionally in jsettlers, that would mean z is the usual case
and y is less common or is an edge condition.

In emacs, you can place this in your .emacs file to use spaces instead of tabs:

``` emacs
(setq-default c-basic-offset 4)
(setq indent-tabs-mode nil)
```

(courtesy http://www.jwz.org/doc/tabs-vs-spaces.html, which also mentions vi)

You will also want this to have this, which disables auto-reindenting:
`(setq-default c-electric-flag nil)`

### Eclipse coding style setup and tips:

    preferences -> general -> editors -> text editors:
    [x] insert spaces for tabs
    displayed tab width: 8
    [x] show print margin
    print margin column: 120
    [x] show whitespace characters
        configure visibility -> trailing space, trailing ideographic space, trailing tab

    project properties -> java compiler -> configure workspace settings
        compiler compliance level: 1.7

    prefs -> java -> code style -> formatter
        Click "Enable Project Specific Settings", then New
        {
            Profile name: 'jsettlers'
            "based on Eclipse built-in standard"
            [X] Open the edit dialog now

            (Indentation)
            Tab policy: Spaces only
            Indentation: 4
            Tab size: 8
            confirm is unchecked: [ ] Indent empty lines

            (Brace positions)
            All 'next line' except:
            Blocks in case stmt: Next line indented
            Array init: Next line indented
            [X] Keep empty array initializer on one line

            (Control Statements)
            [X] New line before else in if
            [X] New line before catch in try
            [X] New line before finally in try
            [ ] New line before while in do
            [X] Keep 'else if' on one line
            [ ] (all other options)

            (All other tabs)
            Take defaults
        }

        Hit OK
        Make sure the formatter "active profile" is jsettlers
        restart eclipse

        go to prefs -> java -> code style -> formatter
        if it's not active: set active profile to jsettlers & restart eclipse

        prefs -> java -> editor -> typing
            Verify tab 8 spaces, indent 4 spaces:
            [x] Tab key adjusts indentation of current line
        "The tab display value (currently 8) and whether spaces are used to indent lines
        are configured on the formatter preference page.  The current indentation size
        is 4, using spaces."

When you hit enter twice to skip a line in Eclipse, watch for unwanted whitespace because
Eclipse will add whitespace to each blank line to match the current indent.

If desired, in Eclipse preferences, you can bind a key combination to Remove Trailing Whitespace.
This will trim it from the entire file when the key is pressed.

To manually clean up trailing whitespace:

- Eclipse preferences -> general -> editors -> text editors -> `[x] Show whitespace characters`
- Find/Replace: Regular expressions: `Find [\t ]+$`

### Style notes for graphics

The rotated 3:1 port hexes' font is Optima Bold, 21 pt.


## Release Testing

When preparing to release a new version, testing should include:

- Before building the JARs to be tested, `git status` should have no untracked or uncommitted changes
  (the `dist-src` build target also checks this)
- Message Traffic debug prints during all tests, to help debugging if needed:  
  Run server and clients with JVM property `-Djsettlers.debug.traffic=Y`
- Basic functional tests
    - Game setup, join, and reset:
        - Create and start playing a practice game with 1 locked space & 2 bots, past initial placement
          into normal play (roll dice, etc) with default options
        - Create and start playing a practice game on the 6-player board (5 bots), with options like Roll No 7s for First 7 Turns
        - JSettlersServer.jar: Start a dedicated server on another ("remote") machine's text-only console
        - Join that remote server & play a full game, then reset board and start another game
            - `*STATS*` command should include the finished game
            - Bots should rejoin and play
        - JSettlers.jar: Start a local server and a game, start another client, join and start playing game
          (will have 2 human clients & 2 bots)
        - Ensure the 2 clients can talk to each other in the game's chat area
        - Client leaves game (not on their turn): bot should join to replace them & then plays their turn (not unresponsive)
        - New client joins and replaces bot; verify all of player info is sent
        - On own turn, leave again, bot takes over
        - Lock 1 bot seat and reset game: that seat should remain empty, no bot
    - Game play: (as debug user or in practice game)
        - Get and play all non-VP dev card types, and give 1 VP card, with debug commands

                dev: 0 playername
                ...
                dev: 4 playername
        - Road Building with 1 road left, after resource debug command to build the others

                rsrcs: 10 0 0 0 10 playername
                dev: 1 playername

          Should see "You may place your 1 remaining road." & be able to do other actions afterwards
        - 6-player board: On server game with a player and observer, request and use Special Building Phase
    - Basic GUI functions
        - Board resizes with window
        - Sound works
        - Bots' face icons match their name (Robots smarter than Droids)
    - 2 clients: While both connected to a server, start and join a chat channel and talk to each other there
- Automated tests in build.xml `test` target
- New features in this version from [Versions.md](Versions.md)
- Regression testing
    - Start a remote server on a console (linux, etc), should stay up for several days including activity (bot games)
        - v2.0.00+: Run several bot games (`jsettlers.bots.botgames.total=5`);
          join one as observer to make sure the pause is shorter than normal games
    - New features in previous 2 versions from [Versions.md](Versions.md)
    - Each available game option
    - Basic rules and game play
        - Can build pieces by right-clicking board or with the Build Panel
        - Can trade with ports by right-clicking board or using Trade Offer Bank/Port button
        - Trade offer, rejection, counter-offer accept/rejection
        - Can play dev card before dice roll
        - Can win only on your own turn
    - Game reset voting, with: 1 human 2 bots, 2 humans 1 bot, 2 humans 0 bots:
      Humans can vote No to reject bots auto-vote Yes; test No and Yes
    - Version compatibility testing
        - Other versions to use: **1.1.06** before Game Options; **1.1.11** with 6-player board and client bugfixes;
          latest **1.x.xx**; latest **2.0.xx**
        - New client, old server
        - New server, old client
        - Test these specific things for each version:
            - With a 1.x.xx client connected to a 2.0.xx server, available new-game options
              should be the same as a 1.x.xx server (adapts to older client version)
            - Create and start playing a 4-player game, and a 6-player game; allow trading in one of them
            - In the 6-player game, request and use the Special Building Phase
            - Create and start playing a 4-player game with no options (this uses a different message type)
            - In any of those games, lock a bot seat and game reset; make sure that works
              (seatlockstate changes between 1.x.xx and 2.0.xx)
            - On a 2.0.xx server, have 2.0.xx client create game with a scenario (1.x.xx can't join),
              1.x.xx client should see it in gamelist with "(cannot join)" prefix.
              Start another 1.x.xx client and connect, should see in list with that same prefix
    - Command line and jsserver.properties
        - Server and client: `-h` / `--help` / `-?`, `--version`
        - Server: Unknown args `-x -z` should print both, then not continue startup
        - Start client w/ no args, start client with host & port on command line
        - Game option defaults on command line, in `jsserver.properties`: `-oVP=t11 -oN7=t5 -oRD=y`
        - Server prop for no chat channels (`jsettlers.client.maxcreatechannels=0`):
          Client main panel should not see channel create/join/list controls
    - Database setup, including Account Admins list `-Djsettlers.accounts.admins=adm,name2,etc`
        - SOCAccountClient with a server not using a DB: At connect, should see a message like "This server does not use accounts"
        - Test with supported DB types: sqlite first, then mysql and postgres
            - Basics: account setup, password validation
            - Try login to account client with a name on, and not on, Account Admins list
            - With a new or upgraded db, verify account are searched case-insensitive
            - Test server parameter `--pw-reset username` , login afterwards with new password and start a game
            - (v2.0.00+) After setup, run automated DB tests with `-Djsettlers.test.db=y`
        - Set up a new DB with instructions from the "Database Creation" section of [Database.md](Database.md),
          including (for any 1 DB type) running `-Djsettlers.db.bcrypt.work_factor=test`
          and then specifying a non-default `jsettlers.db.bcrypt.work_factor` when running the SQL setup script
        - Create those admin accounts, some non-admin accounts
        - SOCAccountClient should allow only admin accounts to log in
        - SOCPlayerClient: Nonexistent usernames with a password specified should have a pause before returning
          status from server, as if they existed with wrong pw
        - SOCPlayerClient: login as non-admin user, create game: `*who*` works (not an admin command) works,
          `*who* testgame` and `*who* *` shouldn't ; `*help*` shouldn't show any admin commands
        - prop to require accounts (`jsettlers.accounts.required=Y`)
        - prop for games saved in DB (`jsettlers.db.save.games=Y`): Play a complete game, check for results there
        - Test creating as old schema (before v1.2.00) and upgrading
            - Get the old schema SQL files you'll need from the git repo by using any pre-1.2.00 release tag, for example:

                  git show release-1.1.20:src/bin/sql/jsettlers-tables.sql > ../tmp/jsettlers-tables-1200.sql

            - Files for mysql: jsettlers-create-mysql.sql, jsettlers-tables.sql
            - For postgres: jsettlers-create-postgres.sql, jsettlers-tables.sql, jsettlers-sec-postgres.sql
            - For sqlite: Only jsettlers-tables.sql
	    - Run DB setup scripts with instructions from the "Database Creation" section of [Database.md](Database.md)
	      and beginning-of-file comments in jsettlers-create-mysql.sql or -postgres.sql
	    - Run SOCServer with the old schema and property `-Djsettlers.accounts.admins=adm`;
	      startup should print `Database schema upgrade is recommended`
	    - Create an admin user named `adm` using: `java -cp JSettlers.jar soc.client.SOCAccountClient yourserver.example.com 8880`
	    - Run DB upgrade by running SOCServer with `-Djsettlers.db.upgrade_schema=Y` property
	    - Run SOCServer as usual; startup should print `User database initialized`
	    - Run JSettlers.jar; log in as `Adm` to test case-insensitive nicknames.  
	      Make sure you can create a game, to test password encoding conversion.  
	      Run the `*DBSETTINGS*` admin command to verify BCrypt password encoding is being used.
    - Other misc testing:
        - "Replace/Take Over" on lost connection:
            - Start a game at server with player client
            - Start a second client under Eclipse debugger & join that game
            - Start game, go through initial placement and into normal game play
            - In Eclipse, pause the debugged client to simulate network connection loss
            - Start a new client and connect as that same username; should allow after appropriate number of seconds
        - Leave a practice game idle for hours, then finish it; bots should not time out or leave game
- Platform-specific: Recent and less-recent OSX and Windows; oldest JRE (1.7) and new JRE
    - Keyboard shortcuts including game-reset dialog's esc/enter keys, FaceChooserFrame arrow keys
    - Sound, including 2 clients in same game for overlapping piece-place sound
    - Start or join networked game
    - Graphics, including scaling and antialiasing after window resize
    - Persistent user prefs (sound, auto-reject bot offer, window size)  
      Then, re-run to check default size with `-Djsettlers.debug.clear_prefs=PI_width,PI_height`
    - SQLite database setup, from instructions in [Database.md](Database.md)
- Instructions and Setup
    - [Readme.md](../Readme.md), `Readme.developer.md`, [Database.md](Database.md):
      Validate all URLs, including JDBC driver downloads
    - Follow server setup instructions in [Readme.md](../Readme.md)
    - Set up a new DB: Covered above in "Platform-specific"


## JSettlers on Github

The project code lives at https://github.com/jdmonin/JSettlers2 .
Patches can be sent by email or by pull request.
Please make sure your patch follows the project coding style.

The master branch receives new features and enhancements for the next 'minor'
release.  As soon as a bug is fixed or a feature's design is fairly stable, it
should be committed to master.

The master branch has new 2.0.xx development.  Until 2.0.00 is ready,
there's a stable-1.x.xx branch in case of urgent bugfixes, so we can release
new stable versions.  Most work on 1.x.xx is backported from 2.0; changeset
comments often mention a hash from a master commit.  Version 2.0.00 was
split off right after releasing version 1.1.13.

v3 is the experimental branch with major architectural changes.

Once 2.0.00 is out, we'll follow the usual jsettlers model: Because
jsettlers2.x.xx is mature at this point, Each minor release is a
stable release.

Each release's files are tagged for the release ("release-1.1.14").
The last commit for the release updates VERSIONS.txt with the final build number,
with a commit message like: Version 1.1.14 is build OV20120930
Then: git tag -a release-1.1.14 -m 'Version 1.1.14 is build OV20120930'

The github repo includes the full JSettlers2 CVS history formerly hosted at
http://sourceforge.net/projects/jsettlers2/ through 2012-09-28.
The old old source history from Robert S Thomas (2004-2005) can be found at
http://sourceforge.net/projects/jsettlers/ .