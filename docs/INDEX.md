# Documentation Index

Complete map of every maintained document. Keep this a routing page — put
commands, endpoint lists, and API examples in the owning doc, not here.

Start at [README.md](README.md) if you want the short path instead.

## Setup & Operation
- [Installation](installation.md) — releases, launcher options, first run
- [Development Setup](development.md) — prerequisites, build/run commands, IDE setup, guardrails
- [Gallery](gallery.md) — screenshots

## Architecture
- [Architecture](ARCHITECTURE.md) — components, threading model, build topology
- [Decision Records](decisions/) — ADRs for structural choices
  - [ADR 0001](decisions/adr-0001-record-architecture-decisions.md) — record architecture decisions
  - [ADR 0002](decisions/adr-0002-composite-build-structure.md) — composite build structure
  - [ADR 0003](decisions/adr-0003-queryable-cache-pattern.md) — queryable cache pattern
  - [ADR 0004](decisions/adr-0004-shaded-distribution-packaging.md) — shaded distribution packaging

## Script Authoring (read before writing scripts)
- [Microbot Plugin & Script Development](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/AGENTS.md) — script lifecycle, threading rules, `sleepUntil`
- [State Machine Framework](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/statemachine/AGENTS.md) — required for scripts with 3+ phases
- [Settings & Widget Debugging](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/settings/AGENTS.md) — varbits, widget IDs, in-game settings
- [Auto Fighter walkthrough](combat.md) — worked example script

## API Reference
- [Queryable API — overview](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/README.md)
- [Queryable API — full guide](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/QUERYABLE_API.md) — **non-negotiable**: never instantiate caches or queryables directly
- [Helper class reference](api/README.md) — per-class pages for `Rs2Bank`, `Rs2Inventory`, `Rs2Walker`, and 34 more
- [Client-Thread Manifest](client-thread-manifest.md) — generated inventory of client-thread-only calls

## Entity Guides (read before touching `microbot/util/`)
- [Entity Guides](entity-guides/README.md) — index and contribution format
- [Items](entity-guides/items.md) — inventory, bank, ground, equipment, shops
- [Movement](entity-guides/movement.md) — walker, minimap, pathing

## Runtime Agent Tooling
- [Microbot CLI](MICROBOT_CLI.md) — `./microbot-cli`, JSON output, offline `ct` lookup
- [Agent Server](AGENT_SERVER.md) — HTTP API, port 8081 by default
- [Agent Script Tools](AGENT_SCRIPT_TOOLS.md) — full tool list
- [Agentic Testing Loop](AGENTIC_TESTING_LOOP.md) — test-mode protocol and result artifacts

## Walker & Pathfinding
- [Walker Audit & Roadmap](walker-audit.md) — executor and collision-map findings
- [Walker P2 — Unify the Obstacle Model](walker-p2-unification.md) — follow-on plan and outcome
- [F2P Web Walker Harness](F2P_WEBWALKER_HARNESS.md) — live in-game regression routes
- [Upstream Comparison](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/shortestpath/UPSTREAM_COMPARISON.md) — shortest-path divergence from upstream
- [Webwalker Improvement Plan](../runelite-client/src/main/java/net/runelite/client/plugins/microbot/shortestpath/WEBWALKER_IMPROVEMENT_PLAN.md)

## Security & Hardening
- [Detection Hardening Plan](DETECTION_HARDENING.md) — detection-surface audit and phased plan

## Testing
- [Agentic Testing Loop](AGENTIC_TESTING_LOOP.md) — automated test-mode runs
- [Integration Test Guide](../runelite-client/src/test/java/net/runelite/client/plugins/microbot/example/INTEGRATION_TEST_GUIDE.md)
- [Example plugin performance tests](../runelite-client/src/test/java/net/runelite/client/plugins/microbot/example/README.md)

## Modules
- [runelite-client](../runelite-client/README.md) — client fork hosting the Microbot plugin
- [runelite-api](../runelite-api/README.md) — shared API artifacts
- [cache](../cache/README.md) — cache tooling included build

## Contributor Rules
- [AGENTS.md](../AGENTS.md) — build commands, non-negotiable rules, review priorities, docs maintenance policy
