# ServerTools: MC2FA
MC2FA is a server mod that will enable two-factor-authentication using a discord bot.
Part of the ServerTools utility mod collection.

# Mod requirements
1. A Minecraft [Cyan](https://aerialworks.ddns.net/cyan) installation
2. The latest [ProtocolHooks](https://github.com/Stefan0436/ProtocolHooks) coremod

# Runs on

### Supported versions
- Minecraft 1.17
- Minecraft 1.17.1
- Minecraft 1.16.5

### Runs on
- The Minecraft Server


<br/>
<b>NOTE:</b> Tested with pure Cyan, no other mods had been installed. <br/>

# Building the mod
### Building commands
On Linux, you need to run the following commands:
```bash
chmod +x gradlew
./gradlew build
```

On Windows, you need to run only the following:
```batch
.\gradlew build
```

<b>TIP:</b> you can add `-PoverrideGameVersion=<version>` to select a game version.

## Installing the mod
<b>Main installation:</b>

1. Find the `build/cmf` folder
2. Copy the latest `cmf` file
3. Find your Cyan installation
4. Navigate to `.cyan-data/mods`
5. Paste the mod file
6. The mod will be kept up-to-date by the trust server


# Setting up the development environment
### Developing the mod
On Linux, you need to run the following commands to prepare the debug environment:
```bash
chmod +x gradlew
./gradlew creatEclipse
```

On Windows, you need to run only the following:
```batch
.\gradlew creatEclipse
```

This process will take a lot of time if run for the first time.
After running the commands, you can import this project in the Eclipse IDE.

<b>TIP:</b> you can add `-PoverrideGameVersion=<version>` to select a game version.
