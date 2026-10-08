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
bounded by the batch size) and sends a `TextExtractionRequest` —
`{"bucket": "<persisted-storage-bucket>", "key": "<line>"}` — to
`TextExtractionQueue` in batches of 10. CSVs larger than 25 MiB (a few
hundred thousand keys) are rejected up front — split bigger runs across
several files; uploads are independent. Seed CSVs are deleted automatically
after 3 days by a lifecycle rule.

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
  source limit, image-only scan, blank content, extraction error) get a flag
  object at `flags/<source-key>.json` instead, recording the reason. Text
  truncated at the 100 000 000-character limit is stored _and_ flagged.
- Extraction targets body text only: PDF annotation text (sticky notes,
  free-text markup) is never extracted, and annotations play no part in scan
  detection — a scan carrying markup annotations still counts as image-only.
- Scanned PDFs are detected structurally before any parsing — no font
  resources anywhere in the document (PDF text cannot be drawn without a
  font), no interactive form fields, and at least one embedded image — and
  flagged `IMAGE_ONLY_CONTENT` with the evidence as detail (page and image
  counts). There is no OCR in this pipeline, so the flag listing doubles as
  the OCR-candidate inventory; the `ImageOnlyPdfProcessor` port is the seam
  where an OCR implementation plugs in, replacing the flagging default with
  no other pipeline change. Beware macOS Preview's Live Text, which OCRs
  scanned PDFs on the fly — being able to copy text in Preview does not mean
  the file has a text layer (check with `pdffonts` or `pdftotext`).
  `BLANK_CONTENT` therefore means extraction genuinely ran and produced
  nothing: an empty or whitespace-only document.

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
- **Oversized CSVs**: seed CSVs larger than 25 MiB are rejected before any
  key is read — logged and thrown, so the run dead-letters after the async
  retries instead of burning repeated 15-minute attempts that can never
  finish.
- **Timeouts are visible, not silent**: the seeder logs progress every
  1 000 enqueued keys and, when less than 30 seconds of Lambda time remain,
  logs how far it got and aborts with an exception. `TextExtractionHandler`
  logs `Extracting: bucket=… key=…` before starting each file, so an
  extraction killed at the platform timeout names its culprit in the last
  log line. Both functions have Slack alarms on unusually long duration
  (over 10 of the 15 available minutes).

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
