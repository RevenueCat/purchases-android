# Checkpoint Rules Ingestion Design

## Goal

Ingest the checkpoint-rules remote-config payload now served by Khepri, matching the intent of purchases-ios PR #7370 without yet evaluating rules or changing checkpoint presentation behavior.

## Backend Contract

- Read the app-level remote-config topic using the Khepri wire name `checkpoint_rules`.
- Each topic item is keyed by checkpoint identifier and points to a blob.
- A checkpoint blob contains its optional public `id` and a `rules` array in evaluation order.
- Each usable rule has non-empty public `audience` and `workflow_id` references.
- Optional `frequency_cap` and `schedule` objects are parsed when present.
- Unknown fields are ignored so Khepri can extend the payload without requiring an SDK release.

## Model

Replace the empty `CheckpointResponse` placeholder with immutable internal models for:

- the checkpoint identifier, optional public ID, and ordered rules;
- a rule's optional public ID, required audience ID, required workflow ID, frequency cap, and schedule;
- a frequency cap's required raw type and optional count/window values;
- a schedule's optional start/end `Date` bounds.

The frequency-cap type remains a raw string because evaluation and its final vocabulary are outside this change.

## Parsing and Safety

`CheckpointsConfigProvider` will resolve raw blob bytes through `RemoteConfigManager` and pass the requested checkpoint identifier into a topic-specific parser.

The parser will decode the top level as a JSON object and parse rules independently. It will preserve the served order and skip one malformed rule without discarding valid siblings. A production comment beside this logic will record that deliberate compatibility decision.

A rule is skipped when either `audience` or `workflow_id` is missing, non-string, or empty. This is fail-closed behavior: malformed targeting must not become broader targeting. Likewise, if a frequency cap or schedule is present but malformed, the rule is skipped instead of silently removing a constraint. A schedule may be open-ended when one bound is absent, but every supplied bound must be a valid ISO-8601 date.

Malformed top-level JSON, a non-object blob, or an unavailable blob returns `null`. A valid checkpoint object with no `rules` field returns an empty ordered rule list.

## Scope

This change only ingests and exposes internal parsed models. It does not:

- evaluate audiences, schedules, or frequency caps;
- replace `RandomWorkflowCheckpointResolver`;
- wire the provider into `Purchases` or checkpoint execution;
- add or change public API.

## Tests

Focused unit tests will cover:

- the actual Khepri payload and `checkpoint_rules` topic name;
- checkpoint and rule IDs, audience/workflow references, frequency caps, and dates;
- preservation of backend rule order;
- unknown fields;
- isolation of malformed sibling rules;
- rejection of missing/empty targeting references;
- rejection of malformed present frequency caps and schedule bounds;
- valid open-ended schedules;
- empty rule sets;
- unavailable blobs and malformed top-level payloads.

