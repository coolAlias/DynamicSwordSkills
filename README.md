DynamicSwordSkills
==================

Mod for those who just want the combat skills from ZSS and none of the mobs, items, world gen, etc.

In addition, there are 'skill' items which grant the user use of a skill, even if the user does not know the skill - these can be found scattered throughout the land.

Mod page on Minecraft Forums: http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/1294232
Mod page on Planet Minecraft: http://www.planetminecraft.com/mod/172164-dynamic-sword-skills/

Basic Controls
--------------
This mod is designed to be used almost entirely with the vanilla control scheme.

Some skills, such as Spin Attack, may require pressing multiple movement keys at once; **gamepad** users may want to enable the **Additional Controls** setting in the config which adds WASD-equivalent keybindings that can be mapped to a separate controller button. Please note that these keys cannot actually be used to move.

**Custom Keys**
- 'x' - Activate or deactivate a targeting skill
- 'tab' - Switch to the next available target while locked on with a targeting skill
- 'p' - Opens the Skill GUI

**Optional Additional Controls**
- Up Arrow - Equivalent of vanilla 'W'
- Down Arrow - Equivalent of vanilla 'S'
- Left Arrow - Equivalent of vanilla 'A'
- Right Arrow - Equivalent of vanilla 'D'

Commands
--------
*/grantskill <player> all*  
Grants all skills at max level to the specified player

*/grantskill <player> <skill> <level>*  
Grants the designated skill and level to the specified player

*/removeskill <player> all*  
Removes all skills from the specified player

*/removeskill <player> <skill>*  
Removes all levels in the designated skill from the specified player

Skills
------
Skills are learned from Skill Orbs, which may drop occasionally from various creatures or be found in chests, depending on the server configuration settings.

Some creatures are mapped to a particular skill orb, in which case they will always try to drop an orb, usually their designated one but it may be random depending on config settings. Mobs not mapped to a particular skill orb may try to drop a random orb, but even at 100% they are not guaranteed to drop an orb, just to try.

When a mob tries to drop a skill orb, the base drop chance ranges from 0% to 10% (default for all is 5%); using weapons with the Looting enchantment significantly improves the drop rate.

Once a skill is learned, check the in-game Skill GUI (default key to open is 'p') and click on a skill orb to view information about its effect and how to activate it.
