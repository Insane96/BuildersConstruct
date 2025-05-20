# Changelog

## Planned
* Check shulker boxes for items to place
* Place random blocks in the hot bar like a Quark Trowel

## 1.3.2
* Modifiers are now listed in the Encyclopedia
* Fixed Construction tooltip

## 1.3.1
* Fixed requiring tconstruct version not yet released

## 1.3.0
* Updated to 1.20.1
* Construction modifier
  * Amount of blocks placed uses a simple formula `(2^(expanded_level + 2))`
  * Horizontal and vertical placement direction is now player dependant, and not north dependant

## 1.2.1
* Angel builder placing distance has been reduced (~~3~~ -> 2.5 blocks), but it's now affected by bonus reach distance (e.g. Reach ability on chestplates)
* Mod can now run on server
* Fixed Angel Builder making no place sound

## 1.2.0
* Added Angel Building. With this new Ability you can place blocks from your off-hand in midair
* Fixed a bug where Construction wouldn't work if the durability of the tools was 1
* Fixed modifier not applicable to any tool

## 1.1.1
* Tile Entities are now blacklisted from placing
* You can no longer put construction on chestplates
* Fixed shift left-clicking changing mode 

## 1.1.0
* It's now possible to place blocks from the off-hand
* Block placing now checks for replaceable blocks instead of air, making the modifier replace blocks like tall grass

## 1.0.0
* First Release