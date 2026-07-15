# publication-file-text

Extracts plain text from persisted publication files (PDF, Word, plain text,
LaTeX) into a dedicated text bucket, so the content can be indexed or analysed
downstream.

Two lambdas, wired together in `template.yaml` under
`#===Text extraction===`:

| Lambda                      | Trigger                                                       | Role                                                   |
| --------------------------- | ------------------------------------------------------------- | ------------------------------------------------------ |
| `SeedTextExtractionHandler` | CSV upload to the seed bucket                                 | Bulk-enqueues one extraction request per object key    |
| `TextExtractionHandler`     | `TextExtractionQueue` (SQS, batch size 1, max concurrency 20) | Downloads the object, extracts text, writes the result |

## Using SeedTextExtractionHandler on AWS

The seeder bulk-populates the text bucket: upload a list of object keys, and
every listed file in the persisted-storage bucket gets queued for extraction.
Use it to seed a new environment or to re-run extraction across existing files.

### 1. Prepare the key list

Despite the `.csv` extension, the file is a single column: **one S3 object key
per line**, UTF-8, **no header row**. Each key must name an object in the
persisted-storage bucket (SSM parameter `/NVA/PublicationData`).

```
0189f1b2-3c4d-4e5f-8a6b-7c8d9e0f1a2b
0189f1b2-9999-4e5f-8a6b-7c8d9e0f1a2b
```

A UTF-8 byte order mark and surrounding whitespace are stripped from each
line; lines containing no letter or digit are skipped. There is no other
parsing — a line with commas in it is treated as one literal key — so do not
upload a genuine multi-column CSV.

To list every key in the persisted-storage bucket:

```sh
aws s3api list-objects-v2 \
    --bucket <persisted-storage-bucket> \
    --query 'Contents[].Key' --output text | tr '\t' '\n' > keys.csv
```

### 2. Upload it to the seed bucket

The seed bucket is `nva-text-extraction-seed-<account-id>` (parameter
`TextExtractionSeedBucketName` plus the account id). Any object whose key ends
in `.csv` triggers the handler.

```sh
aws s3 cp keys.csv s3://nva-text-extraction-seed-<account-id>/keys.csv
```

That is the whole operation. The handler streams the file (memory use is
bounded by the batch size, so CSV size is unlimited in that respect) and sends
a `TextExtractionRequest` — `{"bucket": "<persisted-storage-bucket>",
"key": "<line>"}` — to `TextExtractionQueue` in batches of 10. Seed CSVs are
deleted automatically after 3 days by a lifecycle rule.

### 3. Verify the run

The seeder logs one summary line per CSV:

```
Seeded 4321 keys from s3://nva-text-extraction-seed-.../keys.csv (0 failed)
```

Then watch the pipeline drain:

- `TextExtractionQueue` depth falls as `TextExtractionHandler` consumes it, at
  most 20 concurrent extractions.
- Extracted text lands in `nva-publication-text-<account-id>` (parameter
  `TextStorageBucketName`) at `<source-key>.txt`.
- Files that cannot be extracted (unsupported format, larger than the 9 GiB
  source limit, blank content, extraction error) get a flag object at
  `flags/<source-key>.json` instead, recording the reason. Text truncated at
  the 100 000 000-character limit is stored _and_ flagged.

### Failure handling and retries

- **Enqueue failures**: every key that fails to enqueue is logged as
  `Failed to enqueue key: <key>`. If any key failed, the run finishes the
  remaining batches, then throws. S3 invokes the seeder asynchronously, so
  Lambda retries the whole event twice; if it still fails, the event goes to
  `SeedTextExtractionDLQ` and its alarm notifies Slack.
- **Re-running is safe**: extraction is idempotent — the same key always
  produces the same `<key>.txt` — so retries and re-uploads only cost
  duplicate work, never duplicate data. To recover after a dead-lettered run,
  fix the cause and upload the CSV again.
- **Oversized sources**: objects larger than 9 GiB are rejected before any
  bytes are transferred (the limit fits the extraction function's 10 GiB
  ephemeral storage) and flagged `FILE_TOO_LARGE` — deliberately not retried,
  since a file the function cannot hold on disk can never succeed. Files that
  big are datasets or media, not text-dense documents, and there is no OCR in
  this pipeline anyway.
- **Extraction failures**: a message that keeps failing is retried up to 5
  times by SQS, then moves to `TextExtractionDLQ`, which also has a Slack
  alarm. A source object that no longer exists is logged and skipped, not
  retried.
- **Timeout**: the seeder has the Lambda maximum of 900 seconds per CSV,
  enough for several hundred thousand keys. For larger runs, split the key
  list across several CSV files — uploads are independent.

### Configuration

All wiring is in `template.yaml`; nothing is configured by hand in a deployed
environment.

| Environment variable                | Source                                                   | Meaning                                                      |
| ----------------------------------- | -------------------------------------------------------- | ------------------------------------------------------------ |
| `NVA_PERSISTED_STORAGE_BUCKET_NAME` | `ResourceStorageBucketName` (SSM `/NVA/PublicationData`) | Bucket every enqueued request points at                      |
| `TEXT_EXTRACTION_QUEUE_URL`         | `TextExtractionQueue`                                    | Queue the seeder writes to                                   |
| `AWS_REGION`                        | Lambda runtime                                           | Region for the SQS client; defaults to `eu-west-1` if absent |

`TextExtractionHandler` additionally reads `TEXT_STORAGE_BUCKET_NAME`
(`TextStorageBucket`), the bucket extracted text is written to.
