# Operational Evaluation Comparison: RAG vs No-RAG

## Scope

This report compares two completed operational evaluation runs over the same:

- backend: `http://localhost:18080`
- historical corpus: `evaluation/operational/historical-rag-100.json`
- live workload: `evaluation/operational/live-grievances-100.json`
- submission pause: `20000 ms`
- per-case timeout: `180000 ms`

The only intentional difference between the two runs was `icrs.ai.rag.enabled`.

## Run Summary

| Metric | RAG Enabled | RAG Disabled | Notes |
| --- | ---: | ---: | --- |
| Total cases | 100 | 100 | Matched workload |
| Submission success rate | 100.0% | 100.0% | No submission failures |
| AI completion rate | 100.0% | 100.0% | All cases completed |
| Sentiment completion rate | 100.0% | 100.0% | All cases completed |
| Auto-resolved cases | 42 | 42 | No top-line gain from RAG |
| Auto-resolution rate | 42.0% | 42.0% | Identical top-line outcome |
| Sensitive auto-resolved | 0 / 14 | 0 / 14 | Policy gate held in both runs |
| Non-sensitive auto-resolution rate | 48.84% | 48.84% | Identical top-line outcome |
| Mean confidence | 0.73465 | 0.73370 | Negligible difference |
| Median confidence | 0.73 | 0.73 | No difference |
| Mean latency | 25.37 s | 22.05 s | No-RAG faster by 3.32 s on average |
| Median latency | 23.93 s | 21.17 s | No-RAG faster by 2.77 s |
| Average retrieved references per case | 3.0 | 0.0 | Ablation behaved as expected |
| Share with at least one reference | 100.0% | 0.0% | Ablation behaved as expected |
| Category match rate | 92.67% | N/A | Only defined for RAG run |
| Full run duration | 1 h 16 m 29 s | 1 h 10 m 50 s | No-RAG finished 5 m 39 s sooner |

## Category-Level Auto-Resolution

| Category | RAG Enabled | RAG Disabled | Delta |
| --- | ---: | ---: | ---: |
| Academic | 0 / 12 (0.0%) | 0 / 12 (0.0%) | 0 |
| Administrative | 5 / 10 (50.0%) | 4 / 10 (40.0%) | -1 no-RAG |
| IT Support | 13 / 18 (72.2%) | 14 / 18 (77.8%) | +1 no-RAG |
| Hostel & Accommodation | 13 / 16 (81.2%) | 13 / 16 (81.2%) | 0 |
| Finance & Scholarships | 6 / 16 (37.5%) | 5 / 16 (31.2%) | -1 no-RAG |
| Discipline & Safety | 0 / 8 (0.0%) | 0 / 8 (0.0%) | 0 |
| Examinations | 5 / 14 (35.7%) | 6 / 14 (42.9%) | +1 no-RAG |
| Harassment / PoSH | 0 / 6 (0.0%) | 0 / 6 (0.0%) | 0 |

The category-level differences cancel out at the overall level, leaving both runs at `42/100` auto-resolved cases.

## Case-Level Differences

- Shared cases compared: `100`
- Cases with any observed difference in `finalStatus`, `aiResolved`, or `aiConfidence`: `31`
- Cases with a final resolution flip: `4`
- Cases with confidence-only change and no resolution flip: `27`

### Resolution Flips

| Case ID | Category | Subcategory | RAG Enabled | RAG Disabled |
| --- | --- | --- | --- | --- |
| `live-013-bonafide-delay` | Administrative | Certificates | RESOLVED | IN_PROGRESS |
| `live-029-reset-mail-missing` | IT Support | LMS / Email | IN_PROGRESS | RESOLVED |
| `live-069-duplicate-payment-refund` | Finance & Scholarships | Refunds | RESOLVED | IN_PROGRESS |
| `live-091-blocked-wrong-dues` | Examinations | Hall Ticket | IN_PROGRESS | RESOLVED |

These four flips cancel each other numerically:

- RAG-only resolved: `2`
- No-RAG-only resolved: `2`

## Interpretation

The current ablation supports the following narrow conclusions:

- RAG materially changes the retrieval trace and audit context.
- RAG did not improve top-line auto-resolution rate on this 100-case synthetic workload.
- The deterministic safety gate dominated sensitive-case handling in both variants.
- The no-RAG variant completed faster, so the retrieval step introduced measurable latency overhead.

The current ablation does **not** support stronger claims about:

- decision correctness
- resolution quality
- retrieval usefulness as judged by experts
- causal improvement in automation outcomes

## Suggested Manuscript Use

For the KBS paper, the comparison should be framed as:

- evidence that the system can operate with and without case retrieval under matched conditions,
- evidence that retrieval increases grounding visibility,
- evidence that policy-gated safety behavior does not depend on retrieval,
- and evidence that the present synthetic workload is not sufficient, by itself, to demonstrate a top-line automation advantage from RAG.
