# Operational Evaluation

This directory contains the reproducible operational evaluation assets for ICRS.

## Purpose

The operational runner is intended for paper-safe system evaluation:

- real API submission through `POST /grievances`
- paced workload to avoid unnecessary LLM provider load
- machine-readable result artifacts
- validation against the current catalog taxonomy before a run starts

It is not the same as the older 20-case labelled pilot files in this folder. Those legacy files were useful for the earlier internal pilot, but the operational runner uses the catalog-aligned datasets under `evaluation/operational/`.

## Input Files

Two JSON files are required.

`historical-rag-*.json`

```json
[
  {
    "documentId": "hist-it-001",
    "title": "Resolved WiFi issue in hostel",
    "description": "Students could not connect to the hostel access point.",
    "category": "IT Support",
    "subcategory": "WiFi / Network",
    "registrationNumber": "2022BIT001",
    "priority": "LOW",
    "sentiment": "NEGATIVE",
    "resolutionText": "Students were asked to forget the network and reconnect after router reset."
  }
]
```

Required fields:

- `documentId`
- `title`
- `description`
- `category`
- `subcategory`
- `registrationNumber`

Optional fields:

- `priority`
- `sentiment`
- `resolutionText`
- `comments`

`live-grievances-*.json`

```json
[
  {
    "caseId": "live-it-001",
    "title": "Campus WiFi disconnects in hostel",
    "description": "The hostel WiFi drops every few minutes.",
    "category": "IT Support",
    "subcategory": "WiFi / Network",
    "registrationNumber": "2022BIT002"
  }
]
```

Required fields:

- `caseId` or `documentId` or `id`
- `title`
- `description`
- `category`
- `subcategory`
- `registrationNumber`

## Runner

The Gradle task is:

```bash
./gradlew runOperationalEvaluation \
  -PoperationalEvaluationBackendBaseUrl=http://localhost:8080 \
  -PoperationalEvaluationHistoricalFile=evaluation/operational/historical-rag-smoke.json \
  -PoperationalEvaluationLiveFile=evaluation/operational/live-grievances-smoke.json \
  -PoperationalEvaluationPauseMs=10000 \
  -PoperationalEvaluationTimeoutMs=120000 \
  -PoperationalEvaluationOutputDir=build/reports/operational-evaluation-smoke
```

Defaults:

- backend base URL: `http://localhost:8080`
- pause: `15000` ms
- per-case timeout: `180000` ms
- output directory: `build/reports/operational-evaluation`
- student email: `evaluation.student@icrs.local`
- student password: `Test@12345`

The runner:

1. validates both datasets against the current catalog
2. verifies backend and database reachability
3. ensures one enabled student evaluation account exists
4. clears only prior evaluation data for that student plus the current run’s historical vector document ids
5. imports the historical vector cases
6. logs in through `/auth/login` for each live submission so long runs do not depend on a single JWT lifetime
7. submits live grievances through `/grievances`
8. polls `/grievances/student/me` until AI metadata appears or the case times out
9. writes:
   - `results.json`
   - `metrics.json`
   - `results.csv`

## Output Metrics

The runner reports operational metrics only:

- submission success rate
- AI completion rate
- sentiment completion rate
- auto-resolution rate
- mean and median AI latency
- confidence distribution
- experiment-state and final-status distributions
- category-wise auto-resolution rate
- sensitive vs non-sensitive handling split
- retrieval usage metrics, including source split and category-match rate
