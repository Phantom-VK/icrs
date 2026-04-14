# Operational Evaluation

This directory contains the reproducible operational evaluation assets for ICRS, including the catalog-aligned datasets, the runner entrypoints, and the checked-in outputs from the matched RAG vs no-RAG ablation.

## What This Evaluation Measures

The operational runner exercises the live application through the real HTTP API. It is designed to measure workflow behavior rather than decision correctness:

- live submission through `POST /grievances`
- paced execution to avoid unnecessary provider load
- AI completion and sentiment completion rates
- auto-resolution behavior under policy gating
- retrieval usage when RAG is enabled
- machine-readable artifacts for later analysis

This is separate from the older 20-case pilot material. The current operational study uses the catalog-aligned datasets under `evaluation/operational/`.

## Inputs

Two JSON inputs are required.

`historical-rag-*.json`

- seeded into the vector store before the live run
- represents resolved historical cases
- supports `resolutionText` and optional `comments`

Required fields:

- `documentId`
- `title`
- `description`
- `category`
- `subcategory`
- `registrationNumber`

`live-grievances-*.json`

- submitted through the live backend during the run
- represents the evaluation workload to be classified and processed

Required fields:

- `caseId` or `documentId` or `id`
- `title`
- `description`
- `category`
- `subcategory`
- `registrationNumber`

## How To Run

RAG-enabled baseline:

```bash
./gradlew runOperationalEvaluation \
  -PoperationalEvaluationBackendBaseUrl=http://localhost:8080 \
  -PoperationalEvaluationHistoricalFile=evaluation/operational/historical-rag-100.json \
  -PoperationalEvaluationLiveFile=evaluation/operational/live-grievances-100.json \
  -PoperationalEvaluationPauseMs=20000 \
  -PoperationalEvaluationTimeoutMs=180000 \
  -PoperationalEvaluationOutputDir=build/reports/operational-evaluation-rag
```

No-RAG ablation:

```bash
./gradlew runOperationalEvaluationNoRag \
  -PoperationalEvaluationBackendBaseUrl=http://localhost:8080 \
  -PoperationalEvaluationHistoricalFile=evaluation/operational/historical-rag-100.json \
  -PoperationalEvaluationLiveFile=evaluation/operational/live-grievances-100.json \
  -PoperationalEvaluationPauseMs=20000 \
  -PoperationalEvaluationTimeoutMs=180000 \
  -PoperationalEvaluationOutputDir=build/reports/operational-evaluation-no-rag
```

Defaults:

- backend base URL: `http://localhost:8080`
- pause: `15000 ms`
- per-case timeout: `180000 ms`
- output directory: `build/reports/operational-evaluation`
- evaluation student: `evaluation.student@icrs.local`

Variant handling:

- `runOperationalEvaluation` defaults to `experimentVariant=rag_enabled`
- `runOperationalEvaluationNoRag` forces `icrs.ai.rag.enabled=false` and `experimentVariant=rag_disabled`
- `results.json`, `metrics.json`, and `results.csv` record the experiment variant

The runner validates the datasets against the live catalog, refreshes only evaluation-specific data, imports the historical corpus, logs in for each live submission so JWT expiry does not terminate long runs, submits grievances one by one, polls for AI completion, and writes per-run artifacts.

## Checked-In Ablation Results

The paired 100-case ablation results are checked into:

- `evaluation/reports/operational-evaluation-rag/`
- `evaluation/reports/operational-evaluation-no-rag/`
- `evaluation/reports/operational-evaluation-comparison/`

Headline outcome from the matched runs:

- both variants completed `100/100` cases
- both variants auto-resolved `42/100` cases
- both variants kept sensitive auto-resolution at `0/14`
- RAG-enabled retrieval averaged `3.0` references per case with `92.67%` category match rate
- no-RAG reduced mean latency from `25.37 s` to `22.05 s`

The comparison report concludes that, on the current synthetic workload, RAG improves grounding visibility and audit context but does not produce a top-line auto-resolution gain. The deterministic policy gate remains the dominant safety control in both variants.

## Artifacts

Each run writes:

- `results.json`: full case-level output
- `metrics.json`: aggregate metrics and run summary
- `results.csv`: flat export for inspection and plotting

The comparison package adds:

- `comparison.md`: human-readable matched-run summary
- `comparison.json`: structured top-line and case-delta summary
