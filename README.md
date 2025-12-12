# SimpleHomes
> *Core: Paper 1.21+*
> 
> *Version: 1.0.0*
> 
> https://modrinth.com/plugin/simple--homes

### Commands:
- /homes - Opens the homes menu
- /simplehomes - Admin command to reload yml files
- /SHStreamerMode - Allows you to hide the coordinates of houses in /homes
- /home - You can teleport to your homes without opening the menu (/home 1, /home 2, etc.)

### Permissions:
- simplehomes.homes
- simplehomes.simplehomes
- simplehomes.quadruple
- simplehomes.triple
- simplehomes.double
- simplehomes.home
- simplehomes.shstreamermode

### Config
````yml
# config.yml
delay: 5
tp-delay: 5
````
````yml
# lang.yml
# SimpleHomes Plugin Messages
menu-title: "§fHomes"
menu-sign-text: "§funtitled.server.net"

# Success messages
home-set-success: "§aHome #%index% set! Total homes: %current%/%max%"
teleport-success: "§aTeleport to home #%index%!"

# Error messages
max-homes-reached: "§cYou have reached the maximum number of homes (%max%)!"
bed-already-added: "§cThis bed is already added as a home!"
world-error: "§cHome world not found!"
break-bed: "§cThe bed in home #%index% was destroyed! The home has been removed from the list."
bed-destroyed: "§cOne of your homes has been destroyed!"
delay: "§cYou will be able to teleport in %secs% seconds"

# Menu texts
home-item-name: "§fʜᴏᴍᴇ #§3%index%"
empty-slot-name: "§cᴇᴍᴘᴛʏ"

# Actionbar
tp-delay: "§fYou have been teleported in %secs% seconds to home #%index%"
````