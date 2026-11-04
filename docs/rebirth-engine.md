# Rebirth engine for l2Journey

## Deploy

This correction changes core Java classes. Rebuild the game server JAR; updating only scripts is insufficient.
Deploy the rebuilt core, the SkillList bypass handler, rebirth.ini, custom NPC and skill XMLs, the trainer HTML and the rebirth HTML directory.
Run `dist/db_installer/sql/updates/update_rebirth_engine.sql` on the game database before starting the rebuilt server.
The update preserves existing `rebirthCount` values and creates or extends `character_rebirths` with a persistent operation journal.
No SQL was executed against a live database during development.

NPC 70003 uses `Trainer`; its `SkillList` bypass routes to the configured rebirth skill tree.
NPC 70004 must use `type="RebirthInstance"`, matching the actual Java class name.
Summon or respawn them after deploying the NPC definitions.

## Rules

Rebirth is available on the main class only, outside combat, duels, trades, stores, transformations, siege, events and Olympiad participation/registration.
`RebirthEnabled` controls the system; level, maximum, trainer and rewards remain configurable in `config/player/rebirth.ini`.
`RebirthItems` contains exactly one positive itemId/count pair for each allowed rebirth.
Whitespace, a trailing semicolon and repeated protected skill IDs are handled safely.
The trainer sells only rebirth skills, only the next rank, only within interaction range and only on the main class.
Skill information, acquisition and list refresh use the same trainer/tree selection.
The normal item/SP/level requirements still apply. Failure to consume required items prevents acquisition.
Protected skills are preserved specifically during a rebirth reset; global administrative removal remains available.
The core and XML do not supply client-side skill names/icons. Custom skill IDs still require matching client data.

## Transaction and recovery

The runtime requires InnoDB for `characters`, `character_skills`, `character_shortcuts` and `character_rebirths`.
The provided update converts only the rebirth journal to InnoDB. If a legacy core table uses MyISAM, back it up and migrate it separately before enabling this service.

The reset transaction locks the character and rebirth record, validates the current count, updates main-class level/experience,
removes non-protected main-class skill records and shortcuts, and increments the counter together.
No reward or live-player reset is applied before this transaction commits.
The persistent state progresses as follows:

- `COMPLETE`: no unresolved operation; another rebirth can be requested if requirements are met.
- `RESET_PENDING`: the database reset committed. Login or the rebirth NPC can finish applying it without incrementing the count again.
- `REWARD_RESERVED`: reward delivery was reserved. Automatic retries are blocked to prevent duplicate items.

Item inventory writes and the journal use separate persistence calls. A process crash or delivery/database failure after reservation
requires GM review; the service does not blindly issue a replacement. Inspect the character's items and journal before resolving this state.
The journal retains the reset target, reward and preserved skill IDs for that review.

Class switching and skill acquisition synchronize with the rebirth operation on the player monitor; the normal character save already uses it.
No online-player cache converts database failures into a zero counter.

## Resetting learned rebirth skills

The Rebirth Manager previews the fixed fee, the skills removed and the items returned. `RebirthSkillResetPrice` defaults to 10,000,000 Adena. The reset clears learned rebirth ranks and their shortcuts from the main class. Character level, rebirth count and ordinary skills are preserved.

The refund uses the item requirements for every learned rank in the current skill tree. The shipped tree charges one Book of Reform per rank. Older characters have no purchase receipts, so the current tree is the refund reference if costs have changed. The refund is recorded before charging, so later catalog changes cannot affect an operation in progress.

- `SKILL_FEE_RESERVED` records the intent to charge. An interruption here requires GM review of the Adena payment.
- `SKILL_REFUND_PENDING` means the fee was verified and the skills and shortcuts were removed in the same transaction as the state change. Collection can resume at login or the NPC without another fee.
- `REFUND_RESERVED` prevents replay once delivery begins. An interrupted delivery requires review of the inventory and the `skillResetRefunds` snapshot before manual action.

The inventory APIs and journal write separately. Skill acquisition is suspended while a rebirth operation is unresolved. Both reward and refund completion verify the saved inventory balance. The NPC opens the existing trainer list when the configured trainer is within interaction range.

## Validation

Development tests use mocked players, effects/items and JDBC connections. They do not perform a real rebirth or modify the game database.
After deployment, test dialogue, skill list, required-item consumption, rank progression, limits, main/subclass behavior,
relogin persistence and a controlled pending operation on a disposable test character.
