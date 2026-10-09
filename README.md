# \*\*Style Mod\*\* is a client-side Minecraft Fabric mod that brings \*\*Ultrakill-style combat ranking\*\* to PvP. Land stylish hits, build a combo, climb ranks from \*\*D\*\* to \*\*ULTRAKILL\*\*, time your \*\*jump resets\*\*, and cash out your score when you kill a player.

# 

# \### \*\*How it works\*\*

# \- Every successful melee hit awards \*\*style points\*\*

# \- A falling \*\*combo meter\*\* sets your rank and multiplies the permanent \*\*score\*\*

# \- \*\*Movement speed\*\* acts as an extra multiplier on points gained and lost

# \- Taking damage cuts your score/combo and \*\*breaks sprint chains\*\*

# \- \*\*Jump reset timer\*\* grades your timing after being hit (with optional ping offset)

# \- Killing a player shows a short total on screen \*(reset is configurable)\*

# \- Dying / changing gamemode can clear your style \*(configurable)\*

# 

# \### \*\*Style rewards\*\*

# \- \*\*Any hit\*\* — \*\*+25\*\*

# \- \*\*2+ sprint hits\*\* in a row without taking damage — \*\*+100\*\* each

# \- \*\*Mace hit\*\* — \*\*+300\*\* \*(scales with fall distance, capped)\*

# \- \*\*Critical hit\*\* — \*\*+50\*\*

# \- \*\*Shield break\*\* \*(axe/mace)\* — \*\*+100\*\*

# \- \*\*Kill\*\* — \*\*+200\*\* + on-screen total

# \- \*\*Taking damage\*\* — \*\*−75\*\* \*(scaled by speed)\*

# 

# \### \*\*Jump reset\*\*

# After taking combat damage, a short timing window opens. Jump in the right range:

# 

# | Grade | Result |

# |--------|--------|

# | \*\*PERFECT\*\* | \*\*+80\*\* |

# | \*\*GOOD\*\* | \*\*+40\*\* |

# | \*\*LATE\*\* | \*\*−15\*\* |

# | \*\*EARLY\*\* | \*\*−25\*\* |

# | \*\*MISSED\*\* | \*\*−35\*\* |

# 

# \- Timer bar shows zones in the HUD

# \- Optional \*\*ping offset\*\*: average tab-list ping shifts the scale \*(zone widths stay fixed)\*

# \- Can be toggled in config

# 

# \### \*\*Ranks\*\*

# \*\*D → C → B → A → S → SS → SSS → ULTRAKILL\*\*  

# \*\*ULTRAKILL\*\* unlocks before the combo bar is completely full.

# 

# \### \*\*Config\*\* (`config/style-mod.json`)

# \*\*HUD\*\*

# \- `hudOffsetX` / `hudOffsetY` — position \*(from right / from top)\*

# \- `showRank` / `showScore` / `showMultiplier` / `showComboBar` / `showBonuses` / `showJumpReset` / `showKillBanner`

# 

# \*\*Reset rules\*\* \*(default: true)\*

# \- `resetOnGameModeChange`

# \- `resetOnKill`

# \- `resetOnDeath`

# 

# \*\*Jump reset\*\*

# \- `jumpResetEnabled`

# \- `jumpResetPingCompensation`

# \- `jumpResetPingFactor` \*(default 0.35)\*

# \- `jumpResetMaxShiftMs` \*(default 120)\*

# 

# \### \*\*Commands\*\*

# \- `/style add <points>` — add test points

# \- `/style trigger <id>` — fire a registered action \*(e.g. `mace\_hit`)\*

# \- `/style info` — score, combo, rank, multipliers

# \- `/style reset` — clear everything

# \- `/style jr\_test` — start a jump-reset window \*(for testing)\*

# \- `/style config` — show current config

# \- `/style config hudx <px>` / `hudy <px>` — move the HUD

# \- `/style config show <part> true|false` — toggle HUD parts  

# &#x20; \*(rank, score, multiplier, combo\_bar, bonuses, jump\_reset, kill\_banner)\*

# \- `/style config jump\_reset true|false`

# \- `/style config jump\_reset\_ping true|false`

# \- `/style config reset\_on\_gamemode|reset\_on\_kill|reset\_on\_death true|false`

# \- `/style config reload`

# 

# \### \*\*Install\*\*

# Requires \*\*Fabric Loader\*\* and \[Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft \*\*1.21.11\*\*. Drop the mod jar into your `mods` folder and launch the game.

