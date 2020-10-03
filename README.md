# LemonadeBot
Bot for using on [Discord](https://discord.com/)

## Features
1. Event system
    Allows users to create events other people can join to keep track interests.
    Bot allows for sending a ping to event members if there is a need to send
    info to everyone in event. For example for use by raid groups in MMORPG games
    to organize scheduling based on interest.
2. User created commands
    * Simple template based user created commands, generate random
        numbers, select random option from list, pick a random event member from event.
    * Aliases, user created commands can be used as versatile alias system
        reordering arguments or including part of the command without having to type it.
        Don't like "music play \<url\>" create alias
        >template create music !play music play {argument 1,0}
        
        Now you can use "play \<url\>" to play music using the custom command,
        permission and cooldown systems will still behave same as if the normal
        full command was run instead, but there can be additional permission or
        cooldown set for the user created command also.
3. Reminders
    Bot can send reminders out daily,weekly,monthly or yearly, 
    time decided by [cron](https://en.wikipedia.org/wiki/Cron) -like time specification,
    reminders can use the same features as user created commands and as such can
    be user for periodic task like removing all members from event.
4. Music playback
    Bot can play music using [LavaPlayer](https://github.com/sedmelluq/lavaplayer)
    from various online sources such as YouTube and SoundCloud and many more.
    Full playlist support and even support for skipping all the upcoming songs
    that appear in another playlist.
5. Permission system
    Fully configurable permission system for commands using both a rank based
    system and discord roles. Every command in lemonadebot is based on actions
    that start with command name and include every argument for the command,
    permissions can be set for any start of action, for example limiting music
    playback to only certain roles but allowing anyone to list upcoming songs.
6. Command cooldown system
    A cooldown can be set for any action, limiting how often a command can be run
    for example
    > cooldown set 1 minute music play
    
    sets a one minute cooldown on how often music can be added to playlist but
    has no effect on skipping track using "music skip" or any other command.
    >cooldown set 30 seconds music
    
    Set a 30 second cooldown on all actions for music command.
7. Keyword reactions
    Bot can react to keywords in user typed messages where keyword is a regular
    expression in java and the reaction for for keyword can use all the same
    functionalities as user created custom commands.
    Use keywords to make bot remind you of the the typo you always make, have
    bot play a song when someone says the keyword or just have the bot annoy
    everyone all the time (not recommended).
8. Message log
    Bot can log messages when user edits or deletes a message, if 
    message log was enabled when the message was originally created and the max
    amount of stored messages has not been received since the bot can notify of
    message being edited with the old content included.
8. Configurable
    Set a greeting for new members, the channel bot logs messages to, the prefix
    that commands need to run, max amount of messages bot can store in database
    or the language bot uses.
    Bot has full support for translation by providing a file containing all the
    translations and adding language to list of supported languages, currently
    bot has support for English and Finnish.

## List of built in commands
* help \- Help for bot usage.
* music \- Play music.
* event \- Manage events that people can join.
* template \- Manage user created commands.
* reminder \- Manage reminders that can be sent out by the bot.
* keyword \- Manage keywords that trigger actions when seen in a message.
* config \- Set configuration values used by the bot.
* cooldown \- Set cooldown for commands.
* permission \- Manage permissions required to run commands.

