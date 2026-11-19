# Player-to-player interaction

The temporary Jenny interaction menu is available only when the represented
player explicitly opts in.

A player can enable it for themselves with the vanilla Minecraft command:

```mcfunction
/tag @s add sexmod_player_interaction_opt_in
```

They can revoke permission at any time with:

```mcfunction
/tag @s remove sexmod_player_interaction_opt_in
```

The server checks this tag both when the menu is requested and again when an
action is submitted. Requests are also limited to nearby players, and only the
player who opened a temporary proxy menu can submit its actions. The permission
is stored as a normal entity tag, so it follows the player across sessions and
can be managed by the player or server staff.
