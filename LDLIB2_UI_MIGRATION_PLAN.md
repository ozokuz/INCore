# INCore LDLib2 UI Migration Plan

## Summary

Migrate all in-scope INCore UIs to LDLib2 using **Java-authored UI trees** plus shared **LSS stylesheets**, with **no HUD work** in this effort.

Scope and host model:
- Convert all **21 menu-backed UIs** to LDLib2 `BlockUIMenuType` UIs.
- Convert all **20 standalone in-repo screens** to LDLib2 `PlayerUIMenuType` UIs.
- Collapse only **market, shop, and gacha** into single-host feature apps with internal navigation.
- Keep other standalone features as separate LDLib2 hosts.
- Preserve INCore’s visual identity rather than switching to stock LDLib2 styling.

Execution convention:
- At implementation start, write this approved plan to `/home/ozoku/.t3/worktrees/INCore/t3code-76ce7bc7/LDLIB2_UI_MIGRATION_PLAN.md` before any code changes.

This remains a **phased migration**. Each completed phase removes the legacy runtime path for the UIs it replaces before the next phase starts.

## Target Architecture

### Shared UI foundation
- Add a shared LDLib2 UI package for INCore-specific builders, IDs, and reusable UI helpers.
- Add shared LSS resources under `assets/incore/ldlib/` for:
  - base chrome
  - machine/inventory layouts
  - research/status panels
  - market/shop/gacha app layouts
  - modal/dialog styling
- Build UI trees in Java; move reusable visual styling into LSS.

### Player UI routing and navigation
- Add `INCoreUiIds` as the authoritative `ResourceLocation` list for every Player UI route.
- Add `INCorePlayerUiNavigator` on the server, keyed by player UUID, with:
  - current route
  - back stack
  - typed route context payload
- Add one generic root-open packet, `RequestOpenIncoreUiPayload`, for client keybind opens.
- Root opens reset the back stack.
- In-scope cross-feature navigation pushes the current route before opening the next route.
- Back buttons pop and reopen the previous route.
- Keep `StatusScreenReturnTracker` only for out-of-scope external screens like Numismatics.

### Host choice by UI type
- Use `BlockUIMenuType.BlockUI` for every INCore-owned block/right-click UI.
- Use `PlayerUIMenuType` for keybind-, command-, or server-triggered standalone UIs.
- Do not keep legacy `Screen` or `AbstractContainerScreen` hosts once a feature is migrated.

### Data and sync model
- For migrated UIs, move authoritative UI state to server-side LDLib2 holders.
- Replace feature-specific UI action packets with LDLib2 bindings, server click handlers, and RPC where needed.
- Remove JSON open/snapshot flows and screen-only client caches once no non-UI consumer remains.
- Keep non-UI global caches still used elsewhere:
  - `PlayerLevelClientCache`
  - `EntropyClientCache`
  - `PartyHudClientCache`
  - unlock-state caches used outside UI rendering

## Migration Waves

### 0. Implementation bootstrap
- Write the approved plan verbatim to `/home/ozoku/.t3/worktrees/INCore/t3code-76ce7bc7/LDLIB2_UI_MIGRATION_PLAN.md`.
- Treat that file as the execution checklist and quick reference for later phases.
- Keep the file updated only if the approved plan changes in a later planning turn.

### 1. Foundation
- Add the shared LDLib2 builder/style package.
- Add `INCoreUiIds`, `INCorePlayerUiNavigator`, route/context records, and `RequestOpenIncoreUiPayload`.
- Register all Player UI holders through `PlayerUIMenuType.register(...)`.
- Change `INCoreClient` keybind handlers from `minecraft.setScreen(...)` to root-open payload sends.
- Keep current client-side unlock prechecks, and enforce unlock checks again on the server.

### 2. Block UI Wave A: shared machine/status families
Convert first using reusable builder families for inventory-grid and telemetry/status UIs:
- `Augmenter`
- `OutputPort`
- `PowerInput`
- `LogicHousing`
- `MaterialStorage`
- `OrchestrationDrive`
- `ResearchDrive`
- `WirelessLink`
- `ResearchController`
- `ResearchOrchestratorController`

Rules:
- Replace `handleInventoryButtonClick(...)` interactions with LDLib2 server click handlers.
- Fold `CorruptedDiskScreen` into `ResearchDrive` as an in-UI modal.

### 3. Block UI Wave B: custom block inventory/crafting UIs
Convert:
- `CardDeckStation`
- `MarketTerminalCard`
- `MarketTerminalMeCard`
- `ShipmentTerminal`
- `CrudeResearchStation`
- `Datalogger`
- `Translator`
- `DungeonCrystalModificationStation`
- `MeCrystalAutomationTerminal`

Rules:
- Keep gameplay logic in block entities/services.
- Bind slots directly with LDLib2 `ItemSlot` and `InventorySlots`.
- Recreate progress/status displays with S2C bindings.

### 4. Block UI Wave C: advanced block UIs
Convert:
- `MarketAutoTrader`
- `ResearchSampleFabricator`
- `VendingMachine`

Rules:
- `MarketAutoTrader` gets LDLib2 target selection, progress, output grid, and config controls.
- Rewrite EMI drag/drop to target the LDLib2 host instead of `MarketAutoTraderScreen`, resolving the target UI by holder/menu identity.
- `ResearchSampleFabricator` moves off `ResearchClientCache` to server-side holder state.
- `VendingMachineConversionConfirmScreen` becomes a modal inside the vending machine UI.

### 5. Player UI Wave A: standalone simple hosts
Create separate `PlayerUIMenuType` hosts for:
- `PlayerStatus`
- `PlayerLevelRewards`
- `DungeonDifficulty`
- `TaskOverview`
- `BattlePass`
- `PartyManagement`
- `CombatCatalog`
- `CardPackOpening`

Rules:
- `PlayerStatus` opens in-scope UIs through the navigator.
- External Numismatics return behavior stays on the existing external-return tracker.
- Remove these caches after migration if they have no remaining non-UI consumer:
  - `TaskClientCache`
  - `BattlePassClientCache`
  - `PartyClientCache`
  - `PlayerStatusCurrencyClientCache`
  - `PlayerStatusDungeonDifficultyClientCache`

### 6. Player UI Wave B: research tree
Create a dedicated `ResearchTree` Player UI using LDLib2 `GraphView` plus scrollable side panels.
- Replace `ResearchTreeScreen`.
- Remove `ResearchClientCache` and the UI-specific snapshot/action path.
- Keep the research service/model layer.
- Queue, cancel, repair, and fabricate actions become holder-side server actions.

### 7. Player UI Wave C: collapsed feature apps
Collapse only these flows into single hosts:
- Market: `MarketSelectionScreen`, `MarketDetailsScreen`, `MarketTradeConfirmScreen`
- Shop: `ShopSelectionScreen`, `ShopDetailsScreen`
- Gacha: `GachaBannerScreen`, `GachaBannerInfoScreen`, `GachaGuaranteedSixSelectionScreen`

Rules:
- Use one host per feature with internal state and modal/panel navigation.
- Use navigator context only for initial route/focus state.
- Remove:
  - `MarketPayloadUpdatable`
  - `ShopPayloadUpdatable`
  - related client payload handler classes
  - UI-open/snapshot packets that only existed for those legacy screen trees

### 8. Cleanup
- Remove migrated legacy `Screen`, `AbstractContainerScreen`, `AbstractContainerMenu`, `MenuType`, client payload handler, and client cache classes in the same phase that replaces them.
- Remove INCore-owned menu registrations from `Registration` as LDLib2 block/player UIs replace them.
- Remove INCore-owned `RegisterMenuScreensEvent` entries from `INCoreClient` once no legacy menu screens remain.

## Important API / Interface / Type Changes

### New
- `INCoreUiIds`
- `INCorePlayerUiNavigator`
- route/context records for player UI navigation
- `RequestOpenIncoreUiPayload`
- shared LDLib2 builder/style package
- shared LSS files under `assets/incore/ldlib/`
- workspace-root plan file: `/home/ozoku/.t3/worktrees/INCore/t3code-76ce7bc7/LDLIB2_UI_MIGRATION_PLAN.md`

### Modified
- `INCoreClient` keybind open flow
- INCore block classes that currently open menus, now implementing `BlockUIMenuType.BlockUI`
- feature networking classes, reduced to non-UI gameplay packets where still needed

### Removed
- INCore-owned menu screen registrations
- feature-specific UI open/snapshot payload flows for migrated features
- screen-only client caches with no remaining non-UI consumer
- legacy screen/menu classes for migrated phases

## Tests and Acceptance

### Automated
- Run `./gradlew build`
- Run `./gradlew test`
- Run `./gradlew runGameTestServer`
- Add unit tests for navigator stack/context behavior.
- Add packet serialization tests for `RequestOpenIncoreUiPayload`.

### Manual smoke scenarios
- Every migrated block UI opens from right-click and closes cleanly.
- Inventory slots, shift-click, output-only slots, and button actions match current behavior.
- Keybind opens for status, tasks, battle pass, research, party, market, shop, gacha, and arena still respect feature unlocks.
- Market, shop, and gacha work entirely inside one host each.
- Research tree graph supports pan, zoom, search, queue, and cancel.
- Vending modal confirm flow works.
- EMI drag/drop still sets the Market Auto Trader target.
- Opening Numismatics from status still returns to status.

### Acceptance criteria
- No direct `minecraft.setScreen(new ...)` remains for in-scope INCore UIs.
- No INCore-owned `RegisterMenuScreensEvent` entries remain.
- No legacy client payload handler remains for migrated UI-open flows.
- The approved migration plan is present at the workspace root.
- Build, tests, and game tests all pass.

## Assumptions and Defaults
- Preserve the current INCore visual language.
- HUD overlays, Jade overlays, and external third-party screens are out of scope.
- Only `market`, `shop`, and `gacha` are collapsed into single feature apps.
- `CorruptedDisk` and vending conversion confirmation become parent-UI modals.
- Root player UI opens are server-authoritative and navigator-driven.
- The workspace-root markdown copy is documentation only; it is not a generated artifact.

## References
- LDLib2 agent guide: https://github.com/Low-Drag-MC/LowDragMC-Doc/blob/main/docs/ldlib2/ui/agent_guide.md
- Screen/menu hosting: https://github.com/Low-Drag-MC/LowDragMC-Doc/blob/main/docs/ldlib2/ui/preliminary/screen_and_menu.md
- UI factories: https://github.com/Low-Drag-MC/LowDragMC-Doc/blob/main/docs/ldlib2/ui/factory.md
- Data bindings: https://github.com/Low-Drag-MC/LowDragMC-Doc/blob/main/docs/ldlib2/ui/preliminary/data_bindings.md
