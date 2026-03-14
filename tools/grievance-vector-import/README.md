## Grievance Vector Import

This tool imports grievances from a JSON file into the project's `vector_store`
using the same Spring AI embedding model and PostgreSQL pgvector database as the
main application.

Usage:

```bash
./tools/grievance-vector-import/import-grievances.sh path/to/grievances.json
```

Optional:

```bash
./tools/grievance-vector-import/import-grievances.sh path/to/grievances.json --replace
```

Accepted input formats:

1. A top-level array
2. An object with a `grievances` array

Example:

```json
[
  {
    "id": 1001,
    "title": "Hostel water issue",
    "description": "Water is unavailable in hostel from last night",
    "category": "Hostel & Accommodation",
    "subcategory": "Water Supply",
    "registrationNumber": "2022BIT052",
    "priority": "HIGH",
    "sentiment": "NEGATIVE",
    "resolutionText": "Plumbing team was dispatched."
  }
]
```

Notes:

- Each JSON grievance becomes exactly one vector document.
- The tool upserts by `documentId`/`id`, so repeated imports overwrite the same vector row.
- Imported entries are usable by the current RAG pipeline even if they do not exist in the
  `grievances` relational table, because retrieval falls back to vector metadata.
