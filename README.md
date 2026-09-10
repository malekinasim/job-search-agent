# Job Search Agent

A small Spring Boot service that reads your resume, searches the web daily via the
Claude API's built-in web-search tool, and saves matching job postings to a markdown
file. Includes a manual trigger endpoint for testing.

## Setup

1. **Get an API key**: https://console.anthropic.com → API Keys.

2. **Set it as an environment variable** (never commit it to code):
   ```bash
   export ANTHROPIC_API_KEY="sk-ant-..."
   ```

3. **Edit `resume.txt`** — replace the placeholder with your actual resume text
   (plain text, copy/paste is fine).

4. **Edit `src/main/resources/application.properties`** if you want to:
   - change the schedule (edit the `@Scheduled(cron = ...)` line in `JobSearchService.java`)
   - change what it searches for (`jobsearch.search-brief`)
   - change the model (`anthropic.model`)

## Run locally

```bash
./mvnw spring-boot:run
```

- It will automatically run once a day at 08:00 (server time) via the `@Scheduled` job.
- To test immediately without waiting, call the manual endpoint:
  ```bash
  curl -X POST http://localhost:8080/run-job-search
  ```
- Results are saved to `job-results/jobs-YYYY-MM-DD.md`.

## Running it continuously (so the 08:00 schedule actually fires)

`@Scheduled` only fires while the app is running, so the app needs to stay up. Options,
roughly in order of effort:

- **Build a jar and run it on any small server/VPS as a background process**
  ```bash
  ./mvnw clean package
  java -jar target/job-search-agent-1.0.0.jar
  ```
  Use `systemd`, `nohup`, or a process manager like `pm2`/`supervisord` to keep it
  alive across reboots.

- **Docker**: wrap the jar in a small container and run it on any host or cloud
  container service that supports "always-on" containers (e.g. a small VM, Fly.io,
  Railway, etc).

- **Serverless + external scheduler** (cheaper if you only need it once a day):
  instead of `@Scheduled`, expose the logic only via `/run-job-search`, deploy it as
  a container to something like Google Cloud Run or AWS Lambda, and trigger it with
  Cloud Scheduler / EventBridge on a cron schedule. In that setup the app doesn't
  need to run continuously — it wakes up once a day.

## Notes

- The web-search tool is called as raw JSON (`web_search_20250305`) via
  `java.net.http.HttpClient`, so this has no dependency on the Anthropic Java SDK
  version. If you'd rather use the official SDK's typed builders instead of raw
  JSON, add the `com.anthropic:anthropic-java` dependency and see
  https://platform.claude.com/docs/en/api/sdks/java — the request shape is the same.
- Consider adding simple de-duplication (e.g. store previously-seen job URLs in a
  file or small database) so you don't get repeat results every day.
- Treat this as a starting point, not a finished product — no retry/logging
  framework, no dedup, no notification (email/Slack) yet.
