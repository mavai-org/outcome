# mavai rebrand recipe — the `outcome` rehearsal

**Status:** archival runbook. Not public-facing documentation.
**Written:** 2026-05-29, immediately after `outcome` completed the full
`javai → mavai` coordinate move.
**Audience:** whoever runs the same pipeline for `punit` (Wave 5b) and
`punitexamples` (Wave 5c).

`outcome` was deliberately done first as the rehearsal: smallest surface,
single artifact, near-zero external audience, and punit's only `org.javai`
runtime dependency. Everything below is what the rehearsal actually
taught — verbatim commands, dashboard URLs, turnaround times, and the
landmines that the directive's first draft got wrong. Replay it at
higher stakes for punit and punitexamples; the shape is identical, the
surface is larger.

The canonical directive is
`plan/directives/archived/DIR-MAVAI-RELOCATE-outcome.md` (orchestrator);
this doc is its operational residue.

---

## The pipeline at a glance

Six stages, strictly ordered for 1→3; 4 gated on 3; 5–6 trail.

1. **Transfer the GitHub repo** `javai-org/X` → `mavai-org/X`.
2. **Rename source / build** inside the transferred repo (`org.javai`
   → `org.mavai`: group, coordinates, package, JPMS module, POM URLs,
   version).
3. **Publish the new coordinate** `org.mavai:X:1.0.0-alpha1` to Maven
   Central.
4. **Publish one terminal relocation release** `org.javai:X:0.3.99`
   carrying a relocation POM that redirects to the new coordinate.
5. **Document** (this file).
6. **Pre-flight the consumer bump** on a throwaway branch to surface
   composite-build / module / fixture landmines before the real Wave.

The hard ordering constraints: transfer **must** precede rename;
rename **must** precede the new-coord publish; the relocation release
(§4) can only ship **after** §3 promotes, because the relocation POM
references the new coordinate by version.

---

## The six load-bearing learnings

These are the things the rehearsal got wrong on the first pass, or that
were non-obvious. Read these before touching punit.

### 1. Central Portal ≠ legacy OSSRH — there is no pre-publish staging URL

The directive's original wording ("smoke-test the staged artifact …
resolves the dep from the Central staging repo") is **pre-Portal**. It
was achievable on the old OSSRH staging repo
(`oss.sonatype.org/.../staging/`), which exposed a consumer-resolvable
URL before promotion. The current **Central Portal**
(<https://central.sonatype.com>) does **not** expose any
consumer-resolvable URL until you click *Publish*.

The realised stage-then-test pattern is therefore:

1. `./gradlew publishToMavenCentral` — uploads to the Portal deployment
   pipeline. The log line `Skipping deployment validation!` confirms
   you are on the **manual-release** flow (not auto-promote) — good,
   that is what you want.
2. Operator inspects the Portal deployment's file-tree + validation
   status in the browser.
3. **In parallel**, `./gradlew publishToMavenLocal` and smoke-test
   against `~/.m2/`. This is the substitute for the missing staging
   URL — it catches POM / manifest / module-name problems *before* they
   reach Central, with no stage URL needed.
4. Operator clicks *Publish* in the Portal.
5. Post-publish smoke against Central proper (after a
   `--refresh-dependencies` cache nuke) closes the loop.

### 2. Gradle Module Metadata silently destroys relocation POMs

This is the most dangerous landmine in the whole pipeline and it only
bites on the **relocation release (§4)**.

When Gradle publishes, it emits a `.module` file (Gradle Module
Metadata) alongside the POM. For a normal release this is fine. For a
**relocation** release it is fatal: the `.module` file overrides the
POM, so Gradle **ignores** `<distributionManagement><relocation>`
entirely and resolves the *legacy jar* instead of redirecting. This was
verified live during pre-publish smoke — the broken stage was caught
and dropped via the Portal before promotion.

**Fix**, in the relocation build only:

```kotlin
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }
```

Do **not** apply this on the canonical `mavai-org` `main` branch — only
on the short-lived `release/<version>-relocation` branch. Maven
consumers honour the relocation POM regardless; this fix is what makes
**Gradle** consumers honour it too.

### 3. `outcome` ships a real `module-info.java`, not `Automatic-Module-Name`

The directive (and the parent family plan's 5a checklist) say
"`Automatic-Module-Name`". That is stale: `outcome` shipped a real
`module-info.java` since 0.3.0. The rename therefore touches:

- `module org.javai.outcome { … }` → `module org.mavai.outcome { … }`
- **every** `exports org.javai.outcome.*;` line.

…not a manifest attribute. Modular consumers must also update their
`requires org.javai.outcome;` → `requires org.mavai.outcome;`.

**For 5b:** punit is the same — it has real JPMS, no
`Automatic-Module-Name`, and notably **no `extra-java-module-info` /
`automaticModule` shim** for outcome (outcome 0.3.0's real JPMS already
removed the need). So there is nothing to *remove* on that front; just
the `requires` line to rewrite (see §6 findings).

### 4. The version lives in `gradle.properties`, and the `release` task rejects alpha suffixes

Version is sourced from `gradle.properties` (`outcomeVersion=…`), **not**
`build.gradle.kts`. The project's custom `release` task rejects SNAPSHOT
and naively increments `parts[2]` of the version — which **fails on
alpha-suffixed versions**. So the post-release dev bump
(`1.0.0-alpha1` → `1.0.0-alpha2-SNAPSHOT`) had to be done **by hand**,
not via the `release` task. Expect the same for any alpha-suffixed
artifact in 5b.

### 5. Sonatype namespace verification: DNS TXT on `mavai.org`, `@` host, ~5–30 min

To publish under `org.mavai`, Sonatype Central requires a DNS TXT record
proving control of `mavai.org`:

- On **GoDaddy**, the host field is `@` (the apex). This is correct —
  do not prefix it.
- Propagation is 5–30 minutes. Verify with:
  ```sh
  dig +short txt mavai.org @8.8.8.8
  ```
  then click *Verify* in the Central Portal namespace page.
- **Credentials:** the existing Central Portal user tokens are
  **account-scoped, not namespace-scoped** — the
  `mavenCentralUsername` / `mavenCentralPassword` already in
  `~/.gradle/gradle.properties` worked for `org.mavai` with **no
  reissue**. Likewise the still-valid `org.javai` credentials published
  the §4 relocation release.

This is the long-pole pre-flight. **Start the verification request
first**, before any code change, so it is green by the time §3 needs it.

### 6. The §6 pre-flight is the cheap way to find every consumer landmine

Doing a throwaway consumer bump *before* the real consumer Wave surfaces
everything in isolation. The `outcome → punit` pre-flight (33 Gradle
tasks green, 67 files touched, branch discarded) found exactly these,
all of which carry into the **real** Wave 5b PR:

- `requires transitive org.javai.outcome;` in
  `punit-core/src/main/java/module-info.java` (only — `punit-sentinel`
  and `punit-report` pick up outcome transitively).
- `settings.gradle.kts:15` carries the composite-build substitution
  `substitute(module("org.javai:outcome")).using(project(":"))` — needs
  updating; rewiring works cleanly.
- `punit-core/build.gradle.kts` **and** root `build.gradle.kts` both
  carry `api("org.javai:outcome:0.3.0")` — bump **both** coord +
  version.
- **One architecture-test fixture leaks the package name as a string
  literal:**
  `punit-core/src/test/java/org/javai/punit/architecture/PackageStructureArchitectureTest.java:310`
  hardcodes `"org.javai.outcome.."` as an allowed-package pattern
  (explanatory comment at L299). Mechanical to update — but **flag it in
  the 5b PR description** so reviewers don't read it as scope creep.
- Resource-file references: **none** in punit for outcome.

Generalise this for 5b/5c: the things a pre-flight must hunt are
(a) composite-build `substitute(...)` rules in `settings.gradle.kts`,
(b) any `extra-java-module-info` shims, (c) architecture-test fixtures
hardcoding the package as a string literal, (d) resource-file
references (log4j configs, baselines, hand-rolled YAML).

---

## The concrete command sequence

What was actually run, in order. Adapt repo/coord/version per project.

### §1 — transfer

```sh
# Confirm no open PRs that the transfer would churn; merge/close first.
# (outcome had Dependabot #18, #20 — both merged 2026-05-28.)
gh api -X POST repos/javai-org/outcome/transfer \
  -f new_owner=mavai-org

# In any working orchestrator clone, follow the new URL:
git submodule sync
# Then edit .gitmodules for outcome/ (URL only; path unchanged),
# staging that single path explicitly — never `git add -A`.

# Verify the 301 redirect + clone-through:
gh repo view javai-org/outcome           # redirects to mavai-org/outcome
git clone https://github.com/javai-org/outcome.git   # resolves via redirect
```

GitHub's built-in transfer leaves **server-side 301 redirects on the old
URL, forever** — that is the forwarding guarantee the migration relies
on. Do **not** ever recreate `javai-org/outcome` (it would shadow the
redirect).

### §2 — source / build rename

Branch `feature/mavai-rebrand` (the rehearsal then committed **direct to
`main`** under operator override — the PR step was skipped on the
just-transferred repo; for higher-stakes 5b/5c, prefer the PR).

```sh
git mv src/main/java/org/javai/outcome src/main/java/org/mavai/outcome
git mv src/test/java/org/javai/outcome src/test/java/org/mavai/outcome

# Bulk-rewrite package + import declarations (BSD/macOS sed shown):
find . -name '*.java' -not -path '*/build/*' -print0 \
  | xargs -0 sed -i '' -e 's/org\.javai\.outcome/org.mavai.outcome/g'
```

Then by hand:

- `build.gradle.kts`: `group = "org.javai"` → `"org.mavai"`;
  `coordinates("org.javai", "outcome", …)` → `("org.mavai", …)`;
  POM template `<scm>` / `<url>` → `github.com/mavai-org/outcome`.
- `module-info.java`: `module org.javai.outcome` → `org.mavai.outcome`
  and every `exports` line (the bulk sed above already covers these if
  they match the dotted pattern — verify).
- `gradle.properties`: `outcomeVersion=…` → `1.0.0-SNAPSHOT`.
- `README.md` + `docs/USER-GUIDE.md`: dependency snippets
  `org.javai:outcome:…` → `org.mavai:outcome:1.0.0`; repo URL;
  package-tree examples.
- `CHANGELOG.md`: **additive** `## [1.0.0]` section noting the
  coordinate/package/module rename. Leave `[0.3.0]` and earlier intact.
  Add a `1.0.0` compare-URL footer pointing at mavai-org; leave the
  pre-1.0.0 footer URLs pointing at javai-org (the transfer redirect
  resolves them).

```sh
./gradlew clean build       # green; runtime logger names confirm org.mavai.outcome
```

### §3 — publish the new coordinate

Gated on §5-learning-5 (namespace verification green).

```sh
# Drop the -SNAPSHOT to the release version in gradle.properties first
# (resolved to 1.0.0-alpha1 per the family plan's open question #2).

./gradlew publishToMavenCentral        # uploads to Portal; manual-release flow
./gradlew publishToMavenLocal          # parallel local smoke (see learning 1)
# → smoke from a throwaway consumer at /tmp/X-smoke/: classpath AND module-path.
#   `requires org.mavai.outcome` must resolve at JPMS compile time.

# Operator clicks Publish in the Portal.

# Post-publish re-smoke against Central proper:
./gradlew --refresh-dependencies build   # in the throwaway consumer

# Tag + bump:
git tag v1.0.0-alpha1 && git push origin v1.0.0-alpha1
# Hand-edit gradle.properties → 1.0.0-alpha2-SNAPSHOT (release task can't; learning 4)
```

### §4 — terminal relocation release

```sh
# Branch off the PRE-rename head (still carries the legacy org.javai.outcome
# source + pre-rename build config). For outcome this was commit e9c2f2c.
git switch -c release/0.3.99-relocation e9c2f2c

# In gradle.properties: version → 0.3.99 (terminal-flavoured patch).
```

In `build.gradle.kts` on this branch only, inject the relocation POM and
**kill module metadata** (learning 2):

```kotlin
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

// in the vanniktech mavenPublishing POM config:
pom {
    withXml {
        asNode().appendNode("distributionManagement")
            .appendNode("relocation").apply {
                appendNode("groupId", "org.mavai")
                appendNode("artifactId", "outcome")
                appendNode("version", "1.0.0-alpha1")
                appendNode("message",
                    "outcome has moved to org.mavai. See https://mavai.org.")
            }
    }
}
```

```sh
./gradlew publishToMavenCentral        # operator promotes in Portal

# Verify the redirect from a throwaway Gradle consumer:
#   implementation("org.javai:outcome:0.3.99")
# dependency tree must show:
#   \--- org.javai:outcome:0.3.99 \--- org.mavai:outcome:1.0.0-alpha1

git tag v0.3.99 && git push origin release/0.3.99-relocation v0.3.99
# Branch stays UNMERGED — archival side history. The tag is the durable pointer.
```

Publish the relocation POM **exactly once** per coordinate. Sonatype
permits a single relocation release per coordinate; back-publishing one
under each historical `0.3.x` line would require new versions and is not
worth it at zero external consumers.

### §6 — consumer pre-flight (throwaway, never merged)

```sh
# In punit, on a throwaway branch:
#   - bump api("org.javai:outcome:0.3.0") → api("org.mavai:outcome:1.0.0-alpha1")
#     in BOTH punit-core/build.gradle.kts AND root build.gradle.kts
#   - settings.gradle.kts:15 substitute(module("org.javai:outcome")) → org.mavai
#   - module-info.java: requires transitive org.javai.outcome → org.mavai
#   - PackageStructureArchitectureTest.java:310 string literal
./gradlew build      # 33 tasks green, 67 files touched on the rehearsal
git branch -D <throwaway>     # discard; findings feed the real 5b PR
```

---

## Resolved decisions (carry into 5b / 5c verbatim)

- **New-coord version pattern: `1.0.0-alpha1`.** Couples the coordinate
  rename to the major bump; reserves `1.0.0` proper for API
  stabilisation. (Resolved the family plan's open question #2.)
- **Relocation-release version: `0.3.99`.** Obviously-terminal patch.
- **Relocation branch end-state: branch + tag both pushed, no PR.**
  Visible alongside `main` as side history; the tag is the durable
  pointer.
- **Direct-to-`main` (no PR) on the just-transferred repo** was an
  operator override for the small `outcome` surface. For punit /
  punitexamples (larger, published, more consumers) prefer the standard
  branch + PR workflow.

---

## Dashboards & URLs

| Thing | URL |
|---|---|
| Central Portal (deployments, namespaces, publish button) | <https://central.sonatype.com> |
| Published artifact search | <https://central.sonatype.com/artifact/org.mavai/outcome> |
| GitHub transfer (server-side, no UI artefact) | `gh api -X POST repos/<old-org>/<repo>/transfer -f new_owner=<new-org>` |
| DNS TXT verify | `dig +short txt mavai.org @8.8.8.8` |

---

## Live state at the close of the `outcome` rehearsal

- `org.mavai:outcome:1.0.0-alpha1` — **live on Central**; tag
  `v1.0.0-alpha1`.
- `org.javai:outcome:0.3.99` relocation POM — **live on Central**;
  branch `release/0.3.99-relocation` + tag `v0.3.99` (unmerged side
  history on `mavai-org/outcome`).
- `outcome` `main` = `1.0.0-alpha2-SNAPSHOT` on `org.mavai`.
- **punitexamples is intentionally broken** until Wave 5c: its
  `settings.gradle.kts` `includeBuild(../outcome)` substitution still
  names `org.javai:outcome`, which no longer matches the sibling's
  `org.mavai` group. This breakage is the intended, visible consequence
  — do **not** "fix" it in isolation; Wave 5c does it as part of the
  consumer bump.
