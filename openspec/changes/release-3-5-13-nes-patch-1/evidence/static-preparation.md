# Static RELEASE preparation evidence

Recorded: `2026-07-27T19:35:18+0800`

## Ownership and repository

- Component: `spring-data-keyvalue-3.5`.
- Change: `release-3-5-13-nes-patch-1` (`spec-driven`, apply-ready).
- Owner lease: `coordinator-wave5-keyvalue-35`, acquired at
  `2026-07-27T11:33:47Z` and expiring at `2026-07-27T17:33:47Z`.
- Branch: `3.5.x-bjca-patch`.
- Preparation base HEAD: `72bd185`.
- `origin`: `https://github.com/atbjca/spring-data-keyvalue.git`.
- `gitlab`: `git@192.168.131.1:NES/spring-data-keyvalue.git`.
- Active OpenSpec change: only `release-3-5-13-nes-patch-1`.
- Build and verification target: Java 17; verification JDK: Amazon Corretto
  17.0.17; Maven wrapper distribution: 3.9.16.
- Credentials remain exclusively in user-level Maven configuration and were
  neither read nor recorded.

Before RELEASE edits, the tracked worktree was clean. The only untracked
content was this generated release OpenSpec. No unrelated file or local tool
directory is approved for staging.

## Upstream RELEASE gate

The central run manifest records both declared upstreams at `tagged`, which is
past the required `nexus-verified` state:

- Framework `6.2.19-nes.patch.1`, release commit
  `b2bb78eb3c6571aa17d1c1a52e6f258d4b768acb`.
- Data Commons `3.5.13-nes.patch.1`, release commit
  `837175de73a34f0017b779ad3ae551b6906460ca`.

Data Commons remote verification recorded POM SHA-256
`5aa427e406eafeb8308c202b192900bef4b47b596f5fa900a01936832b8adf71`
and JAR SHA-256
`eea3f813c87f951ff142a8a26af7fbb8abc6512f4772df7442fff444ab632749`.
Its RELEASE-only consumer also resolved Framework `6.2.19-nes.patch.1` with
zero internal SNAPSHOT dependencies.

## RELEASE preparation

The project and all internal dependency properties now use RELEASE versions:

- KeyValue `3.5.13-nes.patch.1`.
- Data Commons `3.5.13-nes.patch.1`.
- Spring Framework `6.2.19-nes.patch.1`.

The root POM has no modules and uses default JAR packaging, so the statically
inferred publication set is one GAV:

`cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:jar:3.5.13-nes.patch.1`

There are no approved publication exclusions. Active component documentation
now identifies the RELEASE coordinate, RELEASE-only upstream dependencies,
incremental local-install command, Nexus RELEASE destination, and
coordinator-controlled deployment constraint. A scan of `pom.xml`,
`README.adoc`, and `doc/` found no target internal `-SNAPSHOT` version.

The release diff contains only `pom.xml`, component documentation, this
OpenSpec change, and its evidence. Production and test source diffs are empty.

## Reused development validation

The operator explicitly directed the release train not to repeat `make build`
or `make test`. The archived development change records 374 tests with zero
failures and zero errors, plus a successful install and the security regression.
Since the RELEASE diff changes no production source, test source, or executable
build logic, those results are reused under the approved release-only-diff
exception.

This exception does not cover local publication, generated-POM scanning, or
consumer verification. The coordinator must run the incremental
`./mvnw -DskipTests install` command alone in the global build slot before a
release commit is created.

## Local RELEASE publication and consumer

The coordinator ran the following command alone in the global build slot:

```text
./mvnw -DskipTests install
```

It completed successfully without `clean` in Maven `20.485s` (wall clock
`25.61s`). Test execution was skipped, while Maven compiled production and test
sources as part of the normal install lifecycle. The remote build-cache request
was rejected with HTTP 403 and disabled; this did not affect local publication.

The complete local publication set contains exactly one GAV and three
versioned assets:

- POM: `bjca-footstone-bpring-data-keyvalue-3.5.13-nes.patch.1.pom` (`3692` bytes).
- Main JAR: `bjca-footstone-bpring-data-keyvalue-3.5.13-nes.patch.1.jar` (`138224` bytes).
- Sources JAR: `bjca-footstone-bpring-data-keyvalue-3.5.13-nes.patch.1-sources.jar` (`94383` bytes).

The structured generated-POM scan reported `filesScanned=1`, `clean=true`,
and `findings=0`. An offline local Maven `dependency:tree` consumer completed
successfully in `1.520s` and resolved KeyValue and Data Commons
`3.5.13-nes.patch.1` plus Framework `6.2.19-nes.patch.1`, with no internal
SNAPSHOT dependency.

Post-gate checks confirm production and test source diffs remain empty, the
approved release diff is unchanged, and generated `target/` content is not
staged.
